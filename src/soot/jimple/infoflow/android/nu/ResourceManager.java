package soot.jimple.infoflow.android.nu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nu.NUDisplay;
import nu.NUSootConfig;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.nu.IResourceManager;

public class ResourceManager implements IResourceManager{
	final static boolean debug = true;
	private static ResourceManager resMgr = null;
	public static ResourceManager getInstance(){
		if(resMgr == null)
			resMgr = new ResourceManager();
		return resMgr;
	}
	
	private ARSCFileParser resParser;
	private ValueResourceParser valResParser;
	private Map<Integer, LayoutTextTreeNode> id2Node;
	private Map<String, LayoutTextTreeNode> layouts;
	private Map<String, Set<Integer>> xmlEventHandler2ViewIds;
	private List<ARSCFileParser.ResPackage> resourcePackages;
	private String appPackageName;
	
	private class ValueResourceParser {
		private final Map<String, Integer> decompiledValuesNameIDMap = new HashMap<String, Integer>();
		private final Map<Integer, String> decompiledIDNameMap = new HashMap<Integer, String>();
		private final Map<Integer, String> decompiledIDStringMap = new HashMap<Integer, String>();
		private final Map<String, String> decompiledNameStringMap = new HashMap<String, String>();
		private final String tmpDirPath;
		private final String apkToolPath;
		private final String apkFileLocation;
		
		public ValueResourceParser(String apkFileLocation, String apkToolPath, String tmpDirPath){
			this.apkToolPath = apkToolPath;
			this.tmpDirPath = tmpDirPath;
			this.apkFileLocation = apkFileLocation;
			extractDataFromCompiledValueResources(this.apkFileLocation);
		}
		
		public Integer getResourceIDFromValueResourceFile(String key){
			return decompiledValuesNameIDMap.get(key);
		}
		
		public String getResourceStringFromValueResourceFile(Integer id){
			String val = decompiledIDStringMap.get(id);
			String name = decompiledIDNameMap.get(id);
			String val2 = decompiledNameStringMap.get(name);
			if(val2 == null)
				return val;
			else if(val == null)
				return val2;
			else
				return val +","+val2;
		}
		
		public void displayDecompiledValueIDPairs(){
			for(String key : decompiledValuesNameIDMap.keySet())
				NUDisplay.debug("  ValueNameIDPair:"+key+" => "+decompiledValuesNameIDMap.get(key), 
						"displayDecompiledValueIDPairs");
		}
		
		private String getCompiledPackagePath(String filename){
			String fname = filename.toLowerCase();
			if(fname.contains(File.separator)){
				int idx = fname.lastIndexOf(File.separator);
				fname = fname.substring(idx+1, fname.length());
			}
			if(fname.endsWith(".apk"))
				fname = fname.substring(0, fname.length()-4);
			String path = tmpDirPath+fname;
			return path;
		}
		
		private void decompileApk(String filename, String path){
			try{
				String cmd = apkToolPath+" d "+filename +" -o "+path;
				NUDisplay.debug("Execute cmd: "+cmd, "decompileApk");
				Process p = Runtime.getRuntime().exec(cmd);
			    p.waitFor(); 
			    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			    String line = "";
			    while((line = reader.readLine()) != null)
			    	NUDisplay.debug("Decompiling APK: "+line,"decompileApk");
			}
			catch(Exception e){
				NUDisplay.error("Error in compiling apk:"+e.toString(), "decompileApk");
			}
		}
		
		private void extractDataFromCompiledValueResources(String filename){
			File apkF = new File(filename);
			if (!apkF.exists())
				throw new RuntimeException("file '" + filename + "' does not exist!");
			try {
				String path = getCompiledPackagePath(filename);
				String valuePath = path + "/res/values/";
				File f = new File(path);
				if(!f.exists() || !f.isDirectory())
					decompileApk(filename, path);
				f = new File(valuePath);
				System.out.println("PATH22:"+path);
				if(!f.exists() || !f.isDirectory()){
					System.out.println("failed to compile apk file: "+filename);
					return ;
				}
			    
			    SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser saxParser = factory.newSAXParser();
				DefaultHandler handler = getValueResouceHandler();
			    for(String xmlFile : f.list()){
			    	xmlFile = f.getAbsolutePath()+"/"+xmlFile;
			    	if(!xmlFile.toLowerCase().endsWith(".xml"))
			    		continue;
			    	InputStream is = new FileInputStream(xmlFile);
			    	System.out.println("analyzing file "+xmlFile);
			    	try{
			    		saxParser.parse(is, handler);
			    	}
			    	catch(Exception e){
			    		System.out.println("Error analyzing file."+e.toString());
			    	}
			    }

			}
			catch (Exception e) {
				System.err.println("Error extractNameIDPairsFromCompiledValueResources in apk "
						+ filename + ": " + e);
				e.printStackTrace();
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				else
					throw new RuntimeException(e);
			}
		}
		
		private DefaultHandler getValueResouceHandler(){
			DefaultHandler handler = new DefaultHandler() {
				String qNameField = "";
				String nameField = "";
				Integer idField = -1;
				public void startElement(String uri, String localName,String qName,
			                Attributes attributes) throws SAXException {
					String name = attributes.getValue("name");
					String id = attributes.getValue("id");
					Integer idInt = null;
					if(id!=null){
						try{
							name = name.trim();
							if(id.toLowerCase().startsWith("0x")){
								Long tt = Long.parseLong(id.substring(2, id.length()), 16);
								idInt = tt.intValue();
							}
							else
								idInt = Integer.valueOf(id);
							idField = idInt;
						}
						catch(Exception e){
							System.err.println("Error in converting integer: "+name+" "+id+" "+e.toString());
						}
					}
					if(name!=null && idInt!=null){
						decompiledValuesNameIDMap.put(name, idInt);
						decompiledIDNameMap.put(idInt, name);
					}
					qNameField = qName;
					nameField = name;
				}

				public void endElement(String uri, String localName,
					String qName) throws SAXException {}

				public void characters(char ch[], int start, int length) throws SAXException {
					try{
						
						String str = new String(ch, start, length);
						str = str.trim();
						if(str.length()>0 && idField!=-1){
							decompiledIDStringMap.put(idField, str);
							idField = -1;
						}
						else if(str.length()>0 && nameField!=null){
							decompiledNameStringMap.put(nameField, str);
							nameField = null;
						}
					}
					catch(Exception e){
						System.out.println("Error in parsing values."+e);
					}
					
				}
			};
			   return handler;
		}
	}
	
	private ResourceManager(){
		NUSootConfig nuConfig = NUSootConfig.getInstance();
		ProcessManifest processMan = null;
		ResourceManager resMgr = null;
		this.appPackageName = null;
		try{
			processMan = new ProcessManifest(nuConfig.getFullAPKFilePath());
			this.appPackageName = processMan.getPackageName();
		}
		catch(Exception e){
			NUDisplay.error("failed to extract app package name: "+e, "ResourceManager");
			System.exit(1);
		}
		
		init(nuConfig.getFullAPKFilePath(), this.appPackageName, nuConfig.getApkToolPath(), 
				nuConfig.getDecompiledAPKOutputPath());
	}
	
	public int resourcePakcageSize(){
		if(resourcePackages == null) return -1;
		return resourcePackages.size();
	}
	
	private void init(String apkFileLocation, String appPackageName, String apkToolPath, String tmpDirPath){
		if(apkFileLocation==null || appPackageName==null || apkToolPath==null || tmpDirPath==null){
			NUDisplay.error("failed to initialize RespirceManager: null parameter", "init");
			System.exit(1);
		}
		resParser = new ARSCFileParser();
		try {
			resParser.parse(apkFileLocation);
		} catch (Exception e) {
			System.err.println("NULIST: failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
			e.printStackTrace();
		}
		resourcePackages = resParser.getPackages();
		valResParser = new ValueResourceParser(apkFileLocation, apkToolPath, tmpDirPath);
		valResParser.displayDecompiledValueIDPairs();
		
		LayoutFileParserForTextExtraction lfpTE = new LayoutFileParserForTextExtraction(appPackageName, resParser, apkToolPath, tmpDirPath);
		lfpTE.parseLayoutFileForTextExtraction(apkFileLocation);
		
		id2Node = lfpTE.getId2Node();
		layouts = lfpTE.getTextTreeMap();
		xmlEventHandler2ViewIds = lfpTE.getXmlEventHandler2ViewIds();
		
		displayResources();
	}
	
	public LayoutTextTreeNode getNodeById(int id){
		return id2Node.get(id);
	}
	
	/***Extract texts based on ID ***/
	public String getTextsById(int id){
		LayoutTextTreeNode node = id2Node.get(id);
		if(node == null)
			return null;
		return node.textObj.toString();
	}
	public LayoutTextTreeNode getLayoutById(int id){
		AbstractResource ar = resParser.findResource(id);
		if (ar == null) return null;
		String layoutName = ar.getResourceName();
		return layouts.get(layoutName);
	}
	public String getStringById(int id){
		return valResParser.getResourceStringFromValueResourceFile(id);
	}
	
	/***Extract id based on string***/
	public Integer getResourceIdByName(String name){
		return valResParser.getResourceIDFromValueResourceFile(name);
	}
	
	/*** Getter ***/
	public Map<String, Set<Integer>> getXMLEventHandler2ViewIds() {
		return xmlEventHandler2ViewIds;
	}
	
	/*** Debugger ***/
	public void displayResources(){
		for(String cls : layouts.keySet()){
			NUDisplay.debug(" LAYOUTTEXT2: "+cls+" "+layouts.get(cls).toStringTree(0,""), null);
		}
	}

	@Override
	public Integer getResourceId(String resName, String resID, String packageName) {
		AbstractResource res = findResource(resName, resID, packageName);
		if(res != null){
			return res.getResourceID();
		}
		return null;
	}
	
	private AbstractResource findResource(String resName, String resID, String packageName) {
		// Find the correct package
		for (ARSCFileParser.ResPackage pkg : this.resourcePackages) {
			// If we don't have any package specification, we pick the app's
			// default package
			boolean matches = (packageName == null || packageName.isEmpty()) && pkg.getPackageName().equals(this.appPackageName);
			matches |= pkg.getPackageName().equals(packageName);
			if (!matches)
				continue;

			// We have found a suitable package, now look for the resource
			for (ARSCFileParser.ResType type : pkg.getDeclaredTypes())
				if (type.getTypeName().equals(resID)) {
					AbstractResource res = type.getFirstResource(resName);
					return res;
				}
		}
		return null;
	}
}

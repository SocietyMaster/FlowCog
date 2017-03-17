package soot.jimple.infoflow.android.nu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ValueResourceParser {
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
			System.out.println("  ValueNameIDPair:"+key+" => "+decompiledValuesNameIDMap.get(key));
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
	
	private void compileApk(String filename, String path){
		try{
			String cmd = apkToolPath+" d "+filename +" -o "+path;
			System.out.println("Execute cmd: "+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
		    p.waitFor(); 
		    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line = "";
		    while((line = reader.readLine()) != null)
		    	System.out.print("Decompiling APK: "+line + "\n");
		}
		catch(Exception e){
			System.out.println("Error in compiling apk:"+e.toString());
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
				compileApk(filename, path);
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
					//TODO:
					decompiledIDNameMap.put(idInt, name);
				}
				
				//System.out.println("Start Element :" + qName+" N:"+name+" ID:"+id);
				qNameField = qName;
				nameField = name;
			}

			public void endElement(String uri, String localName,
				String qName) throws SAXException {
					
			}

			public void characters(char ch[], int start, int length) throws SAXException {
				try{
					
					String str = new String(ch, start, length);
					str = str.trim();
//					System.out.println("NAMENAME:" + qNameField+" N:"+nameField+" ID:"+idField+" "+str);
//					System.out.println("CHARS:"+str );
					if(str.length()>0 && idField!=-1){
						decompiledIDStringMap.put(idField, str);
						//System.out.println("DECOMPILED ID STRINGMAP: "+idField+ " "+str);
						idField = -1;
					}
					else if(str.length()>0 && nameField!=null){
						//TODO:
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

package soot.jimple.infoflow.android.nu;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import pxb.android.axml.AxmlVisitor;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.nu.LayoutTextTreeNode.ViewText;
import soot.jimple.infoflow.android.nu.LayoutTextTreeNode.ViewTextType;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResConfig;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResType;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.AbstractResourceParser;
import soot.jimple.infoflow.android.resources.IResourceHandler;
import soot.jimple.infoflow.android.resources.LayoutControl;

/**
 * Parser for analyzing the layout XML files inside an android application
 * 
 * @author Steven Arzt
 *
 */
public class LayoutFileParserForTextExtraction extends AbstractResourceParser {
	
	private static final boolean DEBUG = true;
	
	//id -> [text1,text2, ...]
	private final Map<Integer, List<String>> id2Texts = new HashMap<Integer, List<String>>();
	private final Map<Integer, String> id2Type = new HashMap<Integer, String>();
	private final Map<Integer, LayoutTextTreeNode> id2Node = new HashMap<Integer, LayoutTextTreeNode>();
	//filename -> LayoutTextTree
	private final Map<String, LayoutTextTreeNode> textTreeMap = new HashMap<String, LayoutTextTreeNode>();
	private final Map<String, Set<Integer>> xmlEventHandler2ViewIds = new HashMap<String, Set<Integer>>();
	private final Map<String, Integer> decompiledValuesNameIDMap = new HashMap<String, Integer>();

	public Map<String, Integer> getDecompiledValuesNameIDMap() {
		return decompiledValuesNameIDMap;
	}

	private final String packageName;
	private final ARSCFileParser resParser;
	private final Pattern eventPattern;
	private final String apkToolPath;
	private final String tmpDirPath;
	
	private final static int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
	private final static int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
	private final static int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
	private final static int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;
	
	public LayoutFileParserForTextExtraction(String packageName, ARSCFileParser resParser,String apkToolPath, String tmppath) {
		this.packageName = packageName;
		this.resParser = resParser;
		this.eventPattern = Pattern.compile("^on[A-Z]\\w+$");
		this.apkToolPath = apkToolPath;
		this.tmpDirPath = tmppath;
		
	}
	
	private boolean isRealClass(SootClass sc) {
		if (sc == null)
			return false;
		return !(sc.isPhantom() && sc.getMethodCount() == 0 && sc.getFieldCount() == 0);
	}
	
	private SootClass getLayoutClass(String className) {
		// Cut off some junk returned by the parser
		if (className.startsWith(";"))
			className = className.substring(1);
		
		if (className.contains("(") || className.contains("<") || className.contains("/")) {
			System.err.println("Invalid class name " + className);
			return null;
		}
		
		SootClass sc = Scene.v().forceResolve(className, SootClass.BODIES);
		if ((sc == null || sc.isPhantom()) && !packageName.isEmpty())
			sc = Scene.v().forceResolve(packageName + "." + className, SootClass.BODIES);
		if (!isRealClass(sc))
			sc = Scene.v().forceResolve("android.view." + className, SootClass.BODIES);
		if (!isRealClass(sc))
			sc = Scene.v().forceResolve("android.widget." + className, SootClass.BODIES);
		if (!isRealClass(sc))
			sc = Scene.v().forceResolve("android.webkit." + className, SootClass.BODIES);
		if (!isRealClass(sc)) {
   			System.err.println("Could not find layout class " + className);
   			return null;
		}
		
		return sc;		
	}
	
	private boolean isLayoutClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
   		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOf(theClass))
   			if (parent.getName().equals("android.view.ViewGroup")) {
   				found = true;
   				break;
   			}
   		return found;
	}
	
	private boolean isViewClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(theClass))
   			if (parent.getName().equals("android.view.View")
   					|| parent.getName().equals("android.webkit.WebView")) {
   				found = true;
   				break;
   			}
   		if (!found) {
   			System.err.println("Layout class " + theClass.getName() + " is not derived from "
   					+ "android.view.View");
   			return false;
   		}
   		return true;
	}
	
	/**
	 * Checks whether the given namespace belongs to the Android operating system
	 * @param ns The namespace to check
	 * @return True if the namespace belongs to Android, otherwise false
	 */
	private boolean isAndroidNamespace(String ns) {
		if (ns == null)
			return false;
		ns = ns.trim();
		if (ns.startsWith("*"))
			ns = ns.substring(1);
		if (!ns.equals("http://schemas.android.com/apk/res/android"))
			return false;
		return true;
	}

	/*public void parseLayoutFile(final String fileName) {
		Transform transform = new Transform("wjtp.lfp", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				parseLayoutFileDirect(fileName);
			}

		});
		PackManager.v().getPack("wjtp").add(transform);
	}*/
	
	/*
	public void parseLayoutFileDirect(final String fileName) {
		handleAndroidResourceFiles(fileName, null, new IResourceHandler() {
				
			@Override
			public void handleResourceFile(final String fileName, Set<String> fileNameFilter, InputStream stream) {
				// We only process valid layout XML files
				if (!fileName.startsWith("res/layout"))
					return;
				if (!fileName.endsWith(".xml")) {
					System.err.println("Skipping file " + fileName + " in layout folder...");
					return;
				}
				
				// Get the fully-qualified class name
				String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
				if (!packageName.isEmpty())
					entryClass = packageName + "." + entryClass;
				
				// We are dealing with resource files
				if (!fileName.startsWith("res/layout"))
					return;
				if (fileNameFilter != null) {
					boolean found = false;
					for (String s : fileNameFilter)
						if (s.equalsIgnoreCase(entryClass)) {
							found = true;
							break;
						}
					if (!found)
						return;
				}
				
				try {
					AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
					parseLayoutNode(fileName, handler.getDocument().getRootNode());
					System.out.println("Found " + userControls.size() + " layout controls in file "
							+ fileName);
				}
				catch (Exception ex) {
					System.err.println("Could not read binary XML file: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		});
	}*/
	
	/**XIANG
	 * Parses all layout XML files in the given APK file and extract the text attributes.
	 */
	public void parseLayoutFileForTextExtraction(final String fileName) {
		handleAndroidResourceFiles(fileName, /*classes,*/ null, new IResourceHandler() {
				
			@Override
			public void handleResourceFile(final String fileName, Set<String> fileNameFilter, InputStream stream) {
				// We only process valid layout XML files
				if (!fileName.startsWith("res/layout"))
					return;
				if (!fileName.endsWith(".xml")) {
					System.err.println("Skipping file " + fileName + " in layout folder...");
					return;
				}
				
				// Get the fully-qualified class name
				String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
				
				
				// We are dealing with resource files
				if (!fileName.startsWith("res/layout/"))
					return;
				entryClass = entryClass.substring(entryClass.lastIndexOf('/')+1);
				if (fileNameFilter != null) {
					boolean found = false;
					for (String s : fileNameFilter)
						if (s.equalsIgnoreCase(entryClass)) {
							found = true;
							break;
						}
					if (!found)
						return;
				}
				
				try {
					AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
					//System.err.println("DEBUG parseLayoutFileForTextExtraction: parsing "+entryClass);
					
					LayoutTextTreeNode textTreeNode = new LayoutTextTreeNode("", null);
					textTreeMap.put(entryClass, textTreeNode);
					parseLayoutNode(entryClass, handler.getDocument().getRootNode(), textTreeNode, textTreeNode);
					
					updateNodeAllTextsAndId2XX(entryClass);
					updateNodeViewTextField(entryClass);
				}
				catch (Exception ex) {
					System.err.println("Could not read binary XML file: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		});
	}

	public void extractNameIDPairsFromCompiledValueResources(String filename){
		File apkF = new File(filename);
		if (!apkF.exists())
			throw new RuntimeException("file '" + filename + "' does not exist!");

		try {
			String fname = filename.toLowerCase();
			if(fname.contains(File.separator)){
				int idx = fname.lastIndexOf(File.separator);
				fname = fname.substring(idx+1, fname.length());
			}
			if(fname.endsWith(".apk"))
				fname = fname.substring(0, fname.length()-4);
			String path = tmpDirPath+fname;
			String cmd = apkToolPath+" d "+filename +" -o "+path;
			System.out.println("Execute cmd: "+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
		    p.waitFor(); 
		    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line = "";
		    while((line = reader.readLine()) != null)
		    	System.out.print("Decompiling APK: "+line + "\n");

		    path = path + "/res/values/";
		    File f = new File(path);
		    if(!f.isDirectory()){
		    	System.err.println("Error compiling: value folder doesn't exist");
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
			public void startElement(String uri, String localName,String qName,
		                Attributes attributes) throws SAXException {
				String name = attributes.getValue("name");
				String id = attributes.getValue("id");
				if(name!=null && id!=null){
					try{
						name = name.trim();
						Integer idInt = null;
						if(id.toLowerCase().startsWith("0x")){
							Long tt = Long.parseLong(id.substring(2, id.length()), 16);
							idInt = tt.intValue();
						}
						else
							idInt = Integer.valueOf(id);
						
						decompiledValuesNameIDMap.put(name, idInt);
					}
					catch(Exception e){
						System.err.println("Error in converting integer: "+name+" "+id+" "+e.toString());
					}
				}
				//System.out.println("Start Element :" + qName+" N:"+name+" ID:"+id);
			}

			public void endElement(String uri, String localName,
				String qName) throws SAXException {

			}

			public void characters(char ch[], int start, int length) throws SAXException {

			}
		};
		   return handler;
	}
	
	/**
	 * Parses the layout file with the given root node
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode The root node from where to start parsing
	 */
	private void parseLayoutNode(String layoutFile, AXmlNode rootNode, LayoutTextTreeNode textTreeNode, LayoutTextTreeNode root) {
		if (rootNode.getTag() == null || rootNode.getTag().isEmpty()) {
			System.err.println("Encountered a null or empty node name "
					+ "in file " + layoutFile + ", skipping node...");
			return;
		}
		//System.err.println("DEBUG: parseLayoutNode: "+rootNode.toString());
		String tname = rootNode.getTag().trim();
		textTreeNode.nodeType = tname;
		
		if (tname.equals("dummy")) {
			// dummy root node, ignore it
		}
		// Check for inclusions
		else if (tname.equals("include")) {
			parseIncludeAttributes(layoutFile, rootNode);
		}
		// The "merge" tag merges the next hierarchy level into the current
		// one for flattening hierarchies.
		else if (tname.equals("merge"))  {
			// do not consider any attributes of this elements, just
			// continue with the children
		}
		else if (tname.equals("fragment"))  {
			final AXmlAttribute<?> attr = rootNode.getAttribute("name");
			if (attr == null)
				System.err.println("Fragment without class name detected");
			else {
				if (attr.getType() != AxmlVisitor.TYPE_STRING)
					System.err.println("Invalid targer resource "+attr.getValue()+"for fragment class value");
				getLayoutClass(attr.getValue().toString());
			}
		}
		else {
			final SootClass childClass = getLayoutClass(tname);
			if (childClass != null && (isLayoutClass(childClass) || isViewClass(childClass))){
				parseLayoutAttributes(layoutFile, childClass, rootNode, textTreeNode, root);
			}
		}

		// Parse the child nodes
		for (AXmlNode childNode : rootNode.getChildren()){
			LayoutTextTreeNode childTextTreeNode = new LayoutTextTreeNode("null", textTreeNode);
			textTreeNode.addChildNode(childTextTreeNode);
			parseLayoutNode(layoutFile, childNode, childTextTreeNode, root);
		}
	}
	
	/**
	 * Parses the attributes required for a layout file inclusion
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param rootNode The AXml node containing the attributes
	 */
	private void parseIncludeAttributes(String layoutFile, AXmlNode rootNode) {
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();
			
    		if (attrName.equals("layout")) {
    			if ((attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)
    					&& attr.getValue() instanceof Integer) {
    				// We need to get the target XML file from the binary manifest
    				AbstractResource targetRes = resParser.findResource((Integer) attr.getValue());
    				if (targetRes == null) {
    					System.err.println("Target resource " + attr.getValue() + " for layout include not found");
    					return;
    				}
    				if (!(targetRes instanceof StringResource)) {
    					System.err.println("Invalid target node for include tag in layout XML, was "
    							+ targetRes.getClass().getName());
    					return;
    				}
    				String targetFile = ((StringResource) targetRes).getValue();
    				
    				// If we have already processed the target file, we can
    				// simply copy the callbacks we have found there
        			
    			}
    		}
		}
	}

	/**
	 * Parses the layout attributes in the given AXml node 
	 * @param layoutFile The full path and file name of the file being parsed
	 * @param layoutClass The class for the attributes are parsed
	 * @param rootNode The AXml node containing the attributes
	 */
	private void parseLayoutAttributes(String layoutFile, SootClass layoutClass, AXmlNode rootNode, 
			LayoutTextTreeNode textTreeNode, LayoutTextTreeNode root) {
		boolean isSensitive = false;
		int id = -1;
//		System.out.println("Parsing Layout:"+layoutFile+" "+rootNode.getAttribute("id"));
//		System.out.println("  "+rootNode.toString()+" // ");
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			if (entry.getKey() == null)
				continue;
			
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();
			//System.err.println("DEBUG parseLayoutAttributes: "+attrName+" "+entry.getValue());
			// On obfuscated Android malware, the attribute name may be empty
			if (attrName.isEmpty())
				continue;
			
			// Check that we're actually working on an android attribute
			if (!isAndroidNamespace(attr.getNamespace()))
				continue;
			
			// Read out the field data
			if (attrName.equals("id")
					&& (attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)){
				id = (Integer) attr.getValue();
				textTreeNode.nodeID = id;
			}
			else if (attrName.equals("password")) {
				if (attr.getType() == AxmlVisitor.TYPE_INT_HEX)
					isSensitive = ((Integer) attr.getValue()) != 0; // -1 for true, 0 for false
				else if (attr.getType() == AxmlVisitor.TYPE_INT_BOOLEAN)
					isSensitive = (Boolean) attr.getValue();
				else
					throw new RuntimeException("Unknown representation of boolean data type");
			}
			else if(attrName.startsWith("on")){ //add event listener
				Matcher m = eventPattern.matcher(attrName);
				if(m.matches() && rootNode.getAttribute("id")!=null && rootNode.getAttribute("id").getValue()!=null && attr.getValue()!=null){
					
					try{
						String clsName = (String)attr.getValue();
						Integer nodeID = Integer.valueOf(rootNode.getAttribute("id").getValue().toString());

						if(xmlEventHandler2ViewIds.containsKey(clsName))
							xmlEventHandler2ViewIds.get(clsName).add(nodeID);
						else{
							Set<Integer> tmp = new HashSet<Integer>();
							tmp.add(nodeID);
							xmlEventHandler2ViewIds.put(clsName, tmp);
						}
						System.out.println("ALERTALERT: onClick:"+clsName+" -> "+nodeID);
					}
					catch(Exception e){
						System.err.println("NULIST: error "+e.toString());
					}
					
				}
				
				//listenerCls2Ids
				
			}
			else if(attrName.equals("name")){
				System.out.println("NAME:"+attr.getValue().toString());
			}
			else if (!isSensitive && attrName.equals("inputType") && attr.getType() == AxmlVisitor.TYPE_INT_HEX) {
				int tp = (Integer) attr.getValue();
				isSensitive = ((tp & TYPE_NUMBER_VARIATION_PASSWORD) == TYPE_NUMBER_VARIATION_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_PASSWORD) == TYPE_TEXT_VARIATION_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
						|| ((tp & TYPE_TEXT_VARIATION_WEB_PASSWORD) == TYPE_TEXT_VARIATION_WEB_PASSWORD);
			}
			else if (isActionListener(attrName)
					&& attr.getType() == AxmlVisitor.TYPE_STRING
					&& attr.getValue() instanceof String) {
				String strData = ((String) attr.getValue()).trim();
				int tmpID = rootNode.getAttribute("id")==null ? 0 :(Integer)rootNode.getAttribute("id").getValue();
				String tmpName = rootNode.getAttribute("name")==null? null : (String)rootNode.getAttribute("name").getValue();
				AXmlAttribute textAttr = rootNode.getAttribute("text");
				String text = "";
				if(textAttr!=null && textAttr.getType()==AxmlVisitor.TYPE_STRING)
					text = (String)textAttr.getValue();
				else if(textAttr!=null && textAttr.getType()==AxmlVisitor.TYPE_INT_HEX)
					text = getTextStringBasedOnID((Integer)textAttr.getValue());
				
//				if(!callbackMap.containsKey(strData)){
//					FlowTriggerEventObject eventObj = new FlowTriggerEventObject(EventID.onClick, "NUXML_"+strData);		
//					//String name, String type, String declaringClass, int id	
//					EventOriginObject eoo = eventObj.createEventOriginObject(tmpName, rootNode.getTag().trim(), "NUXMLCallback",tmpID);
//					eoo.setText(text);
//					eoo.setDeclaringClsLayoutTextTree(root);
//					eventObj.addTriggerEventSrcObject(eoo);
//					callbackMap.put(strData, eventObj);
//				}
//				else{
//					EventOriginObject eoo = callbackMap.get(strData).createEventOriginObject(tmpName, rootNode.getTag().trim(), "NUXMLCallback",tmpID);
//					eoo.setText(text);
//					eoo.setDeclaringClsLayoutTextTree(root);
//					callbackMap.get(strData).addTriggerEventSrcObject(eoo);
//				}
			}
			else if (attr.getType() == AxmlVisitor.TYPE_STRING && attrName.equals("text")) {
				// To avoid unrecognized attribute for "text" field
				textTreeNode.text += attr.getValue().toString().trim();
			}
			else if (DEBUG && attr.getType() == AxmlVisitor.TYPE_STRING) {
				System.out.println("Found unrecognized XML attribute:  " + attrName);
			}
			else if(attr.getType()==AxmlVisitor.TYPE_INT_HEX && attrName.equals("text")){
				//System.err.println("DEBUG ID TEXT ATTR: "+attr.toString()+" "+ attr.getType()+" "+getTextStringBasedOnID((Integer)attr.getValue()) );
				textTreeNode.text += getTextStringBasedOnID((Integer)attr.getValue());
			}
		}
		
		// Register the new user control
		//addToMapSet(this.userControls, layoutFile, new LayoutControl(id, layoutClass, isSensitive));
	}
	
	private void parseValueAttributes(AXmlNode rootNode) {
		int id = -1;
		String name = "";
		for (Entry<String, AXmlAttribute<?>> entry : rootNode.getAttributes().entrySet()) {
			if (entry.getKey() == null)
				continue;
			
			String attrName = entry.getKey().trim();
			AXmlAttribute<?> attr = entry.getValue();
			//System.err.println("DEBUG parseLayoutAttributes: "+attrName+" "+entry.getValue());
			// On obfuscated Android malware, the attribute name may be empty
			if (attrName.isEmpty())
				continue;
			
			// Check that we're actually working on an android attribute
			if (!isAndroidNamespace(attr.getNamespace()))
				continue;
			
			// Read out the field data
			if (attrName.equals("id")
					&& (attr.getType() == AxmlVisitor.TYPE_REFERENCE || attr.getType() == AxmlVisitor.TYPE_INT_HEX)){
				id = (Integer) attr.getValue();
			}
			else if(attrName.equals("name")){
				name = attr.getValue().toString();
				System.out.println("NAME:"+attr.getValue().toString());
			}
		}
		
		System.out.println("Parse value attributes: "+name+" => "+id);
	}
	
	private String getTextStringBasedOnID(int id){
		for(ARSCFileParser.ResPackage rp : resParser.getPackages()){
			for (ResType rt : rp.getDeclaredTypes()){
				if(!rt.getTypeName().equals("string"))
					continue;
				for (ResConfig rc : rt.getConfigurations())
					for (AbstractResource res : rc.getResources()){
						ARSCFileParser.StringResource sres = (ARSCFileParser.StringResource)res;
						if(res.getResourceID() == id){
							return sres.getValue();
						}
						//System.err.println("DEBUG STRING: "+res.getResourceName()+" => "+res.toString()+" "+res.getResourceID()+" "+res.getClass());
					
					}
				}
		}
		return "NUNotFound";
	}


	/**
	 * Checks whether this name is the name of a well-known Android listener
	 * attribute. This is a function to allow for future extension.
	 * @param name The attribute name to check. This name is guaranteed to
	 * be in the android namespace.
	 * @return True if the given attribute name corresponds to a listener,
	 * otherwise false.
	 */
	private boolean isActionListener(String name) {
		return name.equals("onClick");
	}
	
	
	/** XIANG **/
	public Map<Integer, List<String>> getId2Texts() {
		return id2Texts;
	}
	
	public Map<Integer, String> getId2Type(){
		return id2Type;
	}
	
	public Map<Integer, LayoutTextTreeNode> getId2Node(){
		return id2Node;
	}

	public Map<String, LayoutTextTreeNode> getTextTreeMap() {
		return textTreeMap;
	}
	
	public Map<String, Set<Integer>> getXmlEventHandler2ViewIds() {
		return xmlEventHandler2ViewIds;
	}
	
	private void updateNodeViewTextField(String filename){
		if(!textTreeMap.containsKey(filename)){
			System.err.println("Error: no text tree for file: "+filename);
			return;
		}
		traverseTextTreeToUpdateViewTextField(textTreeMap.get(filename));
	}
	private void traverseTextTreeToUpdateViewTextField(LayoutTextTreeNode node){
		if(node.text.length() > 0){
			//ViewTextType textType, String viewType, String texts
			ViewText viewText = node.new ViewText(ViewTextType.VIEW_TEXT, node.nodeType, node.text);
			node.textObj = viewText;
		}
		else if(node.allTexts.length() > 0){
			ViewText viewText = node.new ViewText(ViewTextType.VIEW_TEXT, node.nodeType, node.allTexts);
			node.textObj = viewText;
		}
		else {
			LayoutTextTreeNode parent = node.parent;
			ViewText viewText = null;
			while(parent != null){
				if(parent.allTexts.length() > 0){
					if(parent.parent==null)
						viewText = node.new ViewText(ViewTextType.LAYOUT_TEXT, node.nodeType, parent.allTexts);
					else
						viewText = node.new ViewText(ViewTextType.PARENT_TEXT, node.nodeType, parent.allTexts);
					break;
				}
				parent = parent.parent;
			}
			if(viewText == null)
				viewText = node.new ViewText(ViewTextType.NO_TEXT, node.nodeType, "");
			node.textObj = viewText;
		}
		if(node.children != null)
			for(LayoutTextTreeNode child : node.children)
				traverseTextTreeToUpdateViewTextField(child);
	}
	
	private void updateNodeAllTextsAndId2XX(String filename){
		if(!textTreeMap.containsKey(filename)){
			System.err.println("Error: no text tree for file: "+filename);
			return;
		}
		//System.out.println("DEBUG traverseTextTree: "+filename);
		traverseTextTreeHelper(textTreeMap.get(filename), 0);
	}
	
	private void traverseTextTreeHelper(LayoutTextTreeNode node, int level){
		if(node.nodeID != 0){
			id2Type.put(node.nodeID, node.nodeType);
			id2Node.put(node.nodeID, node);
		}
		
		List<String> texts = null;
	
		if(node.nodeID!=0 && node.text.length()!=0){
			if (id2Texts.containsKey(node.nodeID)){
				texts = id2Texts.get(node.nodeID);
			}
			else{
				texts = new ArrayList<String>(1);
				id2Texts.put(node.nodeID, texts);
			}
			texts.add(node.text);	
		}
		String space = new String(new char[level*2]).replace('\0', ' ');
		//System.out.println("DEBUG: "+space+node.toString());
		String allTexts = node.text;
		if(node.children != null){
			for(LayoutTextTreeNode child : node.children){
				traverseTextTreeHelper(child, level+1);
				if(child.allTexts.length() > 0){
					if(allTexts.length() > 0) allTexts += " || ";
					allTexts += child.allTexts;
				}
			}
		}
		node.allTexts = allTexts;
	}
	
	

	
	
}
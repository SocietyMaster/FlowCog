package soot.jimple.infoflow.android.nu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;

public class ResourceManager {
	final static boolean debug = true;
	
	private ARSCFileParser resParser;
	private Map<Integer, List<String>> id2Texts;
	private Map<Integer, LayoutTextTreeNode> id2Node;
	private Map<String, LayoutTextTreeNode> layouts;
	private LayoutFileParserForTextExtraction lfpTE;
	private Map<String, Integer> valueResourceNameIDMap;
	private Map<String, Set<Integer>> xmlEventHandler2ViewIds;
	
	
	public ResourceManager(String apkFileLocation, String appPackageName, String apkToolPath, String tmpDirPath){
		resParser = new ARSCFileParser();
		try {
			resParser.parse(apkFileLocation);
		} catch (Exception e) {
			System.err.println("NULIST: failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
			e.printStackTrace();
		}
		
		lfpTE = new LayoutFileParserForTextExtraction(appPackageName, resParser, apkToolPath, tmpDirPath);
		lfpTE.parseLayoutFileForTextExtraction(apkFileLocation);
		//lfpTE.extractNameIDPairsFromCompiledValueResources(apkFileLocation);
		id2Texts = lfpTE.getId2Texts();
		id2Node = lfpTE.getId2Node();
		layouts = lfpTE.getTextTreeMap();
		xmlEventHandler2ViewIds = lfpTE.getXmlEventHandler2ViewIds();
		//valueResourceNameIDMap = lfpTE.getDecompiledValuesNameIDMap();
		
		if(debug)
			displayResources();
	}

	public LayoutTextTreeNode getNodeById(int id){
		return id2Node.get(id);
	}
	
	
	//TODO:
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
	
	public Map<String, Set<Integer>> getXMLEventHandler2ViewIds() {
		return xmlEventHandler2ViewIds;
	}

	public void displayResources(){
//		for(Integer id : lfpTE.getId2Type().keySet()){	
//			List<String> texts = id2Texts.get(id);
//			String type = lfpTE.getId2Type().get(id);
//			if(texts != null){
//				for(String msg : id2Texts.get(id))
//					System.out.println("VIEWTEXT: "+id+"("+type+") -> "+msg);
//			}
//			else if(type.endsWith("Layout") && id2Node.containsKey(id)){
//				String text = id2Node.get(id).toStringTree(0,"");
//			}
//			else
//				System.out.println("VIEWTEXT: "+id+"("+lfpTE.getId2Type().get(id)+") -> null");
//		}
		for(String cls : layouts.keySet()){
			System.out.println(" LAYOUTTEXT2: "+cls+" "+layouts.get(cls).toStringTree(0,""));
		}
	}
	
	public LayoutFileParserForTextExtraction getLfpTE() {
		return lfpTE;
	}
	
	public Map<String, Integer> getValueResourceNameIDMap() {
		return valueResourceNameIDMap;
	}
}

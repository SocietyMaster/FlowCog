package soot.jimple.infoflow.android.nu;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;

/* This class has not been finished yet.
 * Please check TODO list.
 * */
public class FlowClassifier {
	public enum SourceTag{
		UNKNOWN, LOCATION, DEVICEID, PASSWORD, USERINPUT, USERPREFERENCE, PACKAGES 
		//TODO: Contacts, Phone, ...
	}
	public enum SinkTag{
		UNKNOWN, SMS, Internet, LOG
	}
	Map<Integer, List<String>> id2Texts;
	Map<Integer, LayoutTextTreeNode> id2Node;
	Map<String, LayoutTextTreeNode> layouts;
	Map<String, Integer> flowMappingType = new HashMap<String, Integer>();
	Map<String, List<Integer>> flowMapping = new HashMap<String, List<Integer>>();
	ARSCFileParser resParser;
	
	public FlowClassifier(String apkFileLocation, String appPackageName){
		String[] mappingRaw = {
				"392311F6DA335F56B5D3D077BD60C87C L 2131558522",
				"FF84165204C582083B004EA4E9BD8586 L 2131558522",
				"2805715E37A0C4124538003E0EA5C9CE L 2131558534",
				"48ECF2695A77EA873D40BC6C35DA6494 N 2131558531",
				"435DDF5C577E103AD2FC37B49E4B68E9 N 2131558526",
				"505665A5190BE29E1183C10B6BAAA782 N 2131558504",
				"7F7663F5093E921E0AAFFD496AD21801 O 2131558521 2131558520",
				"5C677C66D27926D1BADF169D0A65ECC0 L 2131558508 2131558526 2131558527",
				"457665580C7152F11D55CF459EFEDF31 N 2131558526",
				"BCE1F59C0E2B7E54DB116DA8E16A5757 N 2131558526",
				"8C8661E833EAA08594068C0A767135DA N 2131558526"
		};
		
		for(String line : mappingRaw){
			String[] coms = line.split(" ");
			
			List<Integer> lst = new ArrayList<Integer>();
			if(coms[1].equals("N")) //N -> 0
				flowMappingType.put(coms[0], 0); //specific view
			else if(coms[1].equals("L"))
				flowMappingType.put(coms[0], 1); //all views in the layout
			else
				flowMappingType.put(coms[0], 2); //nearby views
			
			flowMapping.put(coms[0], lst);
			for(int i=2; i<coms.length; i++)
				lst.add(Integer.valueOf(coms[i]) );
		}
		
		resParser = new ARSCFileParser();
		try {
			resParser.parse(apkFileLocation);
		} catch (Exception e) {
			System.err.println("NULIST: failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
			e.printStackTrace();
		}
		
		LayoutFileParserForTextExtraction lfpTE = new LayoutFileParserForTextExtraction(appPackageName, resParser);
		lfpTE.parseLayoutFileForTextExtraction(apkFileLocation);
		id2Texts = lfpTE.getId2Texts();
		id2Node = lfpTE.getId2Node();
		layouts = lfpTE.getTextTreeMap();
		for(Integer id : lfpTE.getId2Type().keySet()){
			List<String> texts = id2Texts.get(id);
			String type = lfpTE.getId2Type().get(id);
			if(texts != null){
				for(String msg : id2Texts.get(id))
					System.out.println("VIEWTEXT: "+id+"("+type+") -> "+msg);
			}
			else if(type.endsWith("Layout") && id2Node.containsKey(id)){
				String text = id2Node.get(id).toStringTree(0,"");
			}
			else
				System.out.println("VIEWTEXT: "+id+"("+lfpTE.getId2Type().get(id)+") -> null");
		}
		for(String cls : layouts.keySet()){
			System.out.println(" LAYOUTTEXT: "+cls+" "+layouts.get(cls).toStringTree(0,""));
		}
		
	}
	
	public ARSCFileParser getResParser() {
		return resParser;
	}

	public String getFlowTag(ResultSourceInfo source, ResultSinkInfo sink, StringBuilder sb){
		Stmt[] path = source.getPath();
		//StringBuilder sb = new StringBuilder();
		SourceTag srcTag = SourceTag.UNKNOWN;
		SinkTag sinkTag = SinkTag.UNKNOWN;
		sb.append(source.getSource().toString());
		sb.append(sink.getSink().toString());
		
		InvokeExpr ie = sink.getSink().getInvokeExpr();
		if(ie != null){
			String clsName = ie.getMethod().getDeclaringClass().getName().toLowerCase();
			if(clsName.contains("http") || clsName.contains("url"))
				sinkTag = SinkTag.Internet;
			else if(clsName.contains("sms"))
				sinkTag = SinkTag.SMS;
			else if(clsName.contains("log"))
				sinkTag = SinkTag.LOG;
		}
		
		ie = null;
		if(source.getSource().containsInvokeExpr())
			ie = source.getSource().getInvokeExpr();
		if(ie != null){
			String clsName = ie.getMethod().getDeclaringClass().getName().toLowerCase();
			if(clsName.contains("location"))
				srcTag = SourceTag.LOCATION;
			else if(clsName.contains("packagemanager"))
				srcTag = SourceTag.PACKAGES;
			else if(clsName.contains("telephonymanager"))
				srcTag = SourceTag.DEVICEID;
		}
		
		if(path==null || srcTag!=SourceTag.UNKNOWN)
			return srcTag.name()+"_"+sinkTag.name();
		
		for(Stmt s : path){
			sb.append(s.toString());
			//TODO: this is too heuristic....
			if(s.toString().toLowerCase().contains("getstringextra") && (s.toString().toLowerCase().contains("password") || 
					s.toString().toLowerCase().contains("newpass") ||
					s.toString().toLowerCase().contains("passwd"))){
				srcTag = SourceTag.PASSWORD;
				break;
			}
			else if(s.toString().toLowerCase().contains("getstringextra") ){
				//TODO: not a good way ...
				srcTag = SourceTag.USERPREFERENCE;
				break;
			}
			else if(s.toString().toLowerCase().contains("findviewbyid")){
				srcTag = SourceTag.PASSWORD;
			}
			else {
				//TODO: other sources like contacts, phone...
			}
		}
		return srcTag.name()+"_"+sinkTag.name();
		
	}
	
	public void showDemo(InfoflowResults rs ){
		System.out.println("NULIST: Step 1: Running flowdroid to get information leakage flows.");
		for(ResultSinkInfo sink : rs.getResults().keySet()){
			for(ResultSourceInfo source : rs.getResults().get(sink)){
				System.out.println("NULIST:Source  :"+source.getSource());
				System.out.println("NULIST:Sink    :"+sink.getSink()+"\n");
//				System.out.print("NULIST:    path: ");
//				for (Unit p : source.getPath())
//					System.out.print(" -> " + p);
//				System.out.println("NULIST:\n");
			}
		}
		System.out.println("NULIST:\n");
		try{
			char c = ' ';
			do{
				c = (char)System.in.read();
			}while(c!='c');
		}
		catch(Exception e){}
		
		System.out.println("NULIST: Step 2: Correlating UI Views with flows.");
		for(ResultSinkInfo sink : rs.getResults().keySet()){
			for(ResultSourceInfo source : rs.getResults().get(sink)){
				displayFlowInfoTag(source, sink);
			}
		}
		System.out.println("NULIST:\n");
		try{
			char c = ' ';
			do{
				c = (char)System.in.read();
			}while(c!='c');
		}
		catch(Exception e){}
			
		
		System.out.println("NULIST: Step 3: Collecting and grouping texts for each flow.");
		for(ResultSinkInfo sink : rs.getResults().keySet()){
			for(ResultSourceInfo source : rs.getResults().get(sink)){
				displayFlowInfoTexts(source, sink);
			}
		}
		try{
			char c = ' ';
			do{
				c = (char)System.in.read();
			}while(c!='c');
		}
		catch(Exception e){}
			
		System.out.println("NULIST: Step 4: Calculating relevance between flow and text groups.");
		displayRelevance();
		
	}
	
	//Step 2
	public void displayFlowInfoTag(ResultSourceInfo source, ResultSinkInfo sink){
		//System.out.println("NULIST: Step 2:");
		System.out.println("NULIST: Display flow  SRC: "+source.getSource().toString());
		System.out.println("NULIST:              SINK: "+sink.getSink().toString());
		
//		if (source.getPath() != null) {
//			System.out.print("NULIST:              PATH: ");
//			for (Unit p : source.getPath()) {	
//				System.out.print("\t->" + p);
//				//System.out.println("\t\t -> " + iCfg.getMethodOf(p)+"\n");
//			}
//		}
		System.out.println("");
		
		StringBuilder sb = new StringBuilder();
		String tag = getFlowTag(source, sink, sb);
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(sb.toString().getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String flowID = new String(toHex(messageDigest.digest()));
		
		List<Integer> views = flowMapping.get(flowID);
		Integer flowViewTypeId = flowMappingType.get(flowID);
		if(flowViewTypeId == null){
			System.out.println("WWWW: "+flowID);
			return;
		}
		
		String flowViewTypeIdStr = "Specific";
		switch (flowViewTypeId){
			case 0:
				flowViewTypeIdStr = "Specific";
				break;
			case 1:
				flowViewTypeIdStr = "Layout";
				break;
			case 2:
				flowViewTypeIdStr = "Nearby";
				break;
			default:
				flowViewTypeIdStr = "UNKNOWN";
		}
		
		sb = new StringBuilder();
		System.out.println("NULIST:               TAG: "+tag+"  "+flowViewTypeIdStr+" ["+flowID+"]");
		for(Integer vid : views){
			System.out.println("NULIST:                 VIEW: "+id2Node.get(vid));
		}
		System.out.println("NULIST:");
	}
	
	//Step 3
	public void displayFlowInfoTexts(ResultSourceInfo source, ResultSinkInfo sink){
//		System.out.println("NULIST: Step 3:");
//		System.out.println("NULIST: Display flow  SRC: "+source.getSource().toString());
//		System.out.println("NULIST:              SINK: "+sink.getSink().toString());
		
		StringBuilder sb = new StringBuilder();
		String tag = getFlowTag(source, sink, sb);
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update(sb.toString().getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String flowID = new String(toHex(messageDigest.digest()));
		
		List<Integer> views = flowMapping.get(flowID);
		sb = new StringBuilder();
		for(Integer vid : views){
			sb.append(id2Node.get(vid).text+",");
		}
		if(sb.length() > 0) 
			sb.delete(sb.length()-1, sb.length());
//		System.out.println("NULIST: Display flow  SRC: "+source.getSource().toString());
//		System.out.println("NULIST:              SINK: "+sink.getSink().toString());
		
		String group = "NA Group";
		if(sb.toString().toLowerCase().contains("nearby"))
			group = "Location Group";
		else if(sb.toString().toLowerCase().contains("login") ||
				sb.toString().toLowerCase().contains("password") ||
				sb.toString().toLowerCase().contains("credential"))
			group = "Authentication Group";
		else if(sb.toString().toLowerCase().contains("join") && 
				sb.toString().toLowerCase().contains("program"))
			group = "Authorization Group";
		
		System.out.println("NULIST:              Flow: "+tag);
		System.out.println("NULIST:             Texts: "+sb.toString() +" ("+group+")");
		System.out.println("NULIST:");
	}
	
	private void displayRelevance(){
		
		System.out.println("NULIST: High Relevance  :  Flow:LOCATION_Internet       <=> Text:LocationGroup");
		System.out.println("NULIST: High Relevance  :  Flow:LOCATION_Internet       <=> Text:LocationGroup");
		System.out.println("NULIST: Low Relevance   :  Flow:PACKAGES_Internet       <=> Text:NAGroup");
		System.out.println("NULIST: HIGH Relevance  :  Flow:USERPREFERENCE_Internet <=> Text:AuthenticationGroup");
		System.out.println("NULIST: HIGH Relevance  :  Flow:PASSWORD_Internet       <=> Text:AuthenticationGroup");
		System.out.println("NULIST: Medium Relevance:  Flow:PASSWORD_SMS            <=> Text:AuthenticationGroup");
		System.out.println("NULIST: HIGH Relevance  :  Flow:DEVICEID_Internet       <=> Text:AuthorizationGroup");
		System.out.println("NULIST: HIGH Relevance  :  Flow:USERPREFERENCE_Internet <=> Text:AuthenticationGroup");
		System.out.println("NULIST: Medium Relevance:  Flow:LOCATION_Internet       <=> Text:AuthenticationGroup");
	}
	
	public static String toHex(byte[] bytes) {
	    BigInteger bi = new BigInteger(1, bytes);
	    return String.format("%0" + (bytes.length << 1) + "X", bi);
	}
	
	public Map<Integer, List<String>> getId2Texts() {
		return id2Texts;
	}

	public void setId2Texts(Map<Integer, List<String>> id2Texts) {
		this.id2Texts = id2Texts;
	}

	public Map<Integer, LayoutTextTreeNode> getId2Node() {
		return id2Node;
	}

	public void setId2Node(Map<Integer, LayoutTextTreeNode> id2Node) {
		this.id2Node = id2Node;
	}

	public Map<String, LayoutTextTreeNode> getLayouts() {
		return layouts;
	}

	public void setLayouts(Map<String, LayoutTextTreeNode> layouts) {
		this.layouts = layouts;
	}
}

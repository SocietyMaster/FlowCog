package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import nu.NUDisplay;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import soot.Local;
import soot.MethodOrMethodContext;
import soot.PrimType;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NumericConstant;
import soot.jimple.ParameterRef;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.SourceType;
import soot.jimple.infoflow.nu.GlobalData;
import soot.jimple.infoflow.nu.GraphTool;
import soot.jimple.infoflow.nu.NUAccessPath;
import soot.jimple.infoflow.nu.StmtPosTag;
import soot.jimple.infoflow.nu.ToolSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.util.queue.QueueReader;

public class ParameterSearch {
	final String FIND_VIEW_BY_ID = "findViewById";
	final String SET_CONTENT_VIEW = "setContentView";
	final String SET_BACKGROUND_RESOURCE= "setBackgroundResource";
	
	final String GET_IDENTIFIER_SIGNATURE = 
			"<android.content.res.Resources: int getIdentifier(java.lang.String,java.lang.String,java.lang.String)>";
	CallGraph cg = null;
	List<ARSCFileParser.ResPackage> resourcePackages;
	String appPackageName;
	BiDiInterproceduralCFG<Unit, SootMethod> cfg;
	long startingTime;
	
	public ParameterSearch(List<ARSCFileParser.ResPackage> resourcePackages,String appPackageName,
			BiDiInterproceduralCFG<Unit, SootMethod> cfg){
		this.cg = Scene.v().getCallGraph();
		this.appPackageName = appPackageName;
		this.resourcePackages = resourcePackages;
		this.cfg = cfg;
		this.startingTime = System.currentTimeMillis();
	}
	
	public Set<Stmt> findViewByIdParamSearch(){
		//first search all findViewById statements
		Set<Stmt> rs = new HashSet<Stmt>();
		int solvedCnt = 0;
		int unsolvedCnt = 0;
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    int cnt = 0;
		    for (Unit u : orderer.newList(g, false)) {
		    	cnt++;
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	if(ie.getMethod().getName().equals(FIND_VIEW_BY_ID)){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant)
		    			continue;
		    		s.addTag(new StmtPosTag(cnt, m));
		    		rs.add(s);
		    		
		    		//v2
		    		Integer id = ToolSet.findLastResIDAssignment(s, v, cfg, new HashSet<Stmt>(), FIND_VIEW_BY_ID);
		    		if(id == null) unsolvedCnt++;
		    		else {
		    			solvedCnt++;
		    			GlobalData global = GlobalData.getInstance();
		    			global.addViewID(s, cfg, id);
		    		}
		    	}
		    }
		}
		NUDisplay.debug("findViewById SolvedCnt:"+solvedCnt+" UnsolvedCnt:"+unsolvedCnt, "findViewByIdParamSearch");
		return rs;
	}
	
	public Set<Stmt> setContentViewSearch(){
		//first search all setContent statements
		Set<Stmt> rs = new HashSet<Stmt>();
		int solvedCnt = 0;
		int unsolvedCnt = 0;
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    int cnt = 0;
		    for (Unit u : orderer.newList(g, false)) {
		    	cnt++;
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	if(ie.getMethod().getName().equals(SET_CONTENT_VIEW)){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant) continue;
		    		s.addTag(new StmtPosTag(cnt, m));
		    		rs.add(s);
		    		//v2
		    		Integer id = ToolSet.findLastResIDAssignment(s, v, cfg, new HashSet<Stmt>(), SET_CONTENT_VIEW);
		    		if(id == null) unsolvedCnt++;
		    		else{
		    			solvedCnt++;
		    			GlobalData global = GlobalData.getInstance();
		    			global.addLayoutID(s, cfg, id);
		    		}
		    	}
		    }
		}
		NUDisplay.debug("SetContentBackground SolvedCnt:"+solvedCnt+" UnsolvedCnt:"+unsolvedCnt, "setContentViewSearch");
		return rs;
	}
	
	public void setBackgroundResourceProcessing(){
		GlobalData gData = GlobalData.getInstance();
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	if(ie.getMethod().getName().equals(SET_BACKGROUND_RESOURCE)){
		    		if(! (ie instanceof VirtualInvokeExpr)) continue;
		    		
		    		Value v = ie.getArg(0);
		    		if(v instanceof IntConstant){
		    			VirtualInvokeExpr vie = (VirtualInvokeExpr)ie;
		    			Value base = vie.getBase();
		    			if(base instanceof FieldRef)
		    				gData.addFieldID( ((FieldRef)base).getField(), ((IntConstant)v).value);
		    			else if(base instanceof Local){
		    				Set<SootField> ssf = resolveLocalBase(g, s, (Local)base);
		    				for(SootField sf : ssf)
		    					gData.addFieldID(sf, ((IntConstant)v).value);
		    			}
		    			continue;
		    		}
		    		
		    		Integer id = ToolSet.findLastResIDAssignment(s, v, cfg, new HashSet<Stmt>(), SET_BACKGROUND_RESOURCE);
		    		if(id == null) continue;
		    		else{
		    			VirtualInvokeExpr vie = (VirtualInvokeExpr)ie;
		    			Value base = vie.getBase();
		    			if(base instanceof FieldRef){
		    				gData.addFieldID( ((FieldRef)base).getField(), id);
		    			}
		    			else if(base instanceof Local){
		    				Set<SootField> ssf = resolveLocalBase(g, s, (Local)base);
		    				for(SootField sf : ssf)
		    					gData.addFieldID(sf, id);
		    			}
		    		}
		    	}
		    }
		}
	}
	
	public void searchMethodCall(String methodName, String className){
		GlobalData gd = GlobalData.getInstance();
		//first search all findViewById statements
		System.out.println("SearchMethodCall:"+methodName+"@"+className);
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    int cnt = 0;
		    for (Unit u : orderer.newList(g, false)) {
		    	cnt++;
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	if(ie.getMethod().getName().equals(methodName) && 
		    			(className==null || ie.getMethod().getDeclaringClass().getName().equals(className)) ){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant){
		    			//TODO: add constant to map
		    			continue;
		    		}
		    		System.out.println("Found one instance: "+s+" CLASS:"+ie.getMethod().getDeclaration());
		    		System.out.println("  "+gd.getViewID(s, cfg));	
		    	}
		    }
		}
	}
	
	//class name cannot be null and method name can be a part
	public void searchMethodCallVaguely(String methodName, String className){
		GlobalData gd = GlobalData.getInstance();
		//first search all findViewById statements
		System.out.println("SearchMethodCall:"+methodName+"@"+className);
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    int cnt = 0;
		    for (Unit u : orderer.newList(g, false)) {
		    	cnt++;
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	if(ie.getMethod().getName().contains(methodName) && ie.getMethod().getDeclaringClass().getName().equals(className) ){
//		    		Value v = ie.getArg(0);
//		    		if(v instanceof Constant){
//		    			//TODO: add constant to map
//		    			continue;
//		    		}
		    		
		    		System.out.println("NULIST Found one instance: "+s+" CLASS:"+ie.getMethod().getDeclaringClass().getName());
		    		//System.out.println("  "+gd.getViewID(s, cfg));	
		    	}
		    }
		}
	}
	
	public void findPreferenceSetMethods(){
		//first search all findViewById statements
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt s = (Stmt)u;
		    	if(!s.containsInvokeExpr()) continue;
		    	
		    	InvokeExpr ie = s.getInvokeExpr();
		    	SootMethod method = ie.getMethod();
		    	if(method.getSignature().contains("android.content.SharedPreferences$Editor") &&
		    			method.getName().contains("put")){
		    		System.out.println("PreferenceSetMethod:" + ie);
		    	}
		    }
		}
	}
	public void debugFunctionCall(){
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
			GlobalData gData = GlobalData.getInstance();
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt stmt = (Stmt)u;
		    	if(!stmt.containsInvokeExpr())
		    		continue;
		    	InvokeExpr ie = stmt.getInvokeExpr();
		    	if(ie instanceof InstanceInvokeExpr){
		    		if(ie.getMethod().getName().equals("run") || 
		    				ie.getMethod().getName().equals("start")){
		    			System.out.println("UWUW: "+stmt);
		    		}
		    	}
		    }
		}
	}
	
	public void extractDynamicTexts(){
		startingTime = System.currentTimeMillis();
		final String SET_TEXT_API = "setText";
		final String SET_TITLE_API = "setTitle";
		ResourceManager resMgr = ResourceManager.getInstance();
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
			GlobalData gData = GlobalData.getInstance();
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt stmt = (Stmt)u;
		    	if(!stmt.containsInvokeExpr())
		    		continue;
		    	InvokeExpr ie = stmt.getInvokeExpr();
		    	if(ie.getMethod().getName().equals(SET_TEXT_API) ||
		    			ie.getMethod().getName().equals(SET_TITLE_API)){
		    		if(! (ie instanceof InstanceInvokeExpr) ) continue;
		    		String texts = null;
		    		if(ie.getMethod().getParameterCount() >= 1){
		    			Type t = ie.getMethod().getParameterType(0);
		    			if(t.getEscapedName().equals("int")){
		    				//int id = Integer.valueOf(ie.getArg(0).toString());
		    				Integer id = null;
		    				Value arg = ie.getArg(0);
		    				if(arg instanceof IntConstant)
		    					id = ((IntConstant)arg).value;
		    				
		    				else if(arg instanceof Local)
		    					id = ToolSet.findLastResIDAssignment(stmt, arg, cfg, new HashSet<Stmt>(), cfg.getMethodOf(stmt).getName());
		    				
		    				if(id != null)
		    					texts = resMgr.getStringById(id);
		    				System.out.println("WWWW1: "+id+" "+texts);
		    			}
		    			else if(t.getEscapedName().equals("java.lang.CharSequence") ||
		    					t.getEscapedName().equals("java.lang.String")){//String
		    				Value arg = ie.getArg(0);
		    				if(arg instanceof StringConstant){
		    					texts = ((StringConstant) arg).value;
		    				}
		    				else if(arg instanceof Local){
//		    					texts = ToolSet.findLastResStringAssignment(stmt, (Local)arg, cfg, new HashSet<Stmt>());
		    					texts = extractArgTextsHelper(stmt, (Local)arg);
		    				}
		    				//texts = ie.getArg(0).toString();
		    				System.out.println("WWWW2:"+texts);
		    			}
		    		}//get texts
		    		else{
		    			System.out.println("cannot resolve string");
		    			continue;
		    		}
		    		
		    		InstanceInvokeExpr iie = (InstanceInvokeExpr)ie;
		    		Set<Stmt> rs = new HashSet<Stmt>();
		    		NUDisplay.debug("DEBUGTEST: target: "+stmt +"@"+cfg.getMethodOf(stmt), null);
		    		long time1 = System.currentTimeMillis();
		    		ToolSet.findViewDefStmt(stmt, iie.getBase(), new ArrayList<NUAccessPath>(),
		    				cfg, new HashSet<Stmt>(), rs);
		    		long diff = (System.currentTimeMillis()-time1)/1000;
		    		NUDisplay.debug("Done in "+diff+"s", null);
		    		for(Stmt r : rs){
		    			NUDisplay.debug("DEBUGTEST: origin: "+r+"@"+cfg.getMethodOf(stmt), null);
		    			if(texts!=null && texts.trim().length()>0)
		    			gData.addTextToDyanmicView(r, texts, cfg);
		    		}
		    		NUDisplay.debug("", null);
		    		
		    		
		    	}
		    }
		}
		
	}
	
	public void extractURLAddress(){
		NUDisplay.debug("start extracting url's addresses", "extractURLAddress");
		//GraphTool.displayAllMethodGraph();
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
			GlobalData gData = GlobalData.getInstance();
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt stmt = (Stmt)u;
		    	if(!stmt.containsInvokeExpr())
		    		continue;
		    	if(isInternetSinkStmt(stmt)){
		    		NUDisplay.debug("Found one sink:"+stmt +" @"+m.getName(), null);
		    		GraphTool.displayGraph(g, m);
		    		InvokeExpr ie = stmt.getInvokeExpr();
		    		if(ie.getMethod().getName().equals("<init>")){
		    			handleURLInitMethod(stmt);
		    		}
		    	}
		    }
		}
	}
	private void handleURLInitMethod(Stmt stmt){
		if(!stmt.containsInvokeExpr())
			return ;
		InvokeExpr ie = stmt.getInvokeExpr();
		SootMethod sm = ie.getMethod();
		if(!sm.getName().equals("<init>")) return ;
		GlobalData gData = GlobalData.getInstance();
		/* URL(String spec) Creates a URL object from the String representation.
		 * URL(String protocol, String host, int port, String file)
		 *	Creates a URL object from the specified protocol, host, port number, and file.
		 * URL(String protocol, String host, int port, String file, URLStreamHandler handler)
		 * 	Creates a URL object from the specified protocol, host, port number, file, and handler.
		 * URL(String protocol, String host, String file)
		 *	Creates a URL from the specified protocol name, host name, and file name.
		 * URL(URL context, String spec)
		 *	Creates a URL by parsing the given spec within a specified context.
		 * URL(URL context, String spec, URLStreamHandler handler)
		 *	Creates a URL by parsing the given spec with the specified handler within a specified context.
		 * 
		 * */
		//System.out.println("Type2222:"+sm.getParameterType(0));
		if(sm.getParameterCount()==1){ //URL(String spec)
			Value v = ie.getArg(0);
			String str = null;
			if(v instanceof StringConstant)
				str = ((StringConstant) v).value;
			else	
				str = ToolSet.findLastResStringAssignment(stmt, v, cfg, new HashSet<Stmt>());
			if(str != null) gData.addInternetSinkURL(stmt, str);
			NUDisplay.debug("URL case1: "+str, null);
		}
		else if(sm.getParameterCount()==2){ //URL(URL context, String spec)
			Value v = ie.getArg(1);
			String str = null;
			if(v instanceof StringConstant)
				str = ((StringConstant) v).value;
			else	
				str = ToolSet.findLastResStringAssignment(stmt, v, cfg, new HashSet<Stmt>());
			if(str != null) gData.addInternetSinkURL(stmt, str);
			NUDisplay.debug("URL case2: "+str, null);
		}
		else if(sm.getParameterCount()==3 && sm.getParameterType(0).getEscapedName().equals("java.lang.String")){ 
			//URL(String protocol, String host, String file)
			
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<sm.getParameterCount(); i++){
				Value v = ie.getArg(i);
				String str = null;
				if(v instanceof StringConstant)
					str = ((StringConstant) v).value;
				else	
					str = ToolSet.findLastResStringAssignment(stmt, v, cfg, new HashSet<Stmt>());
				switch(i){
				case 0:
					sb.append(str==null? "*://" : str+"://");
					break;
				case 1:
					sb.append(str==null? "*/" : str+"/");
					break;
				case 2:
					sb.append(str==null? "*" : str);
					break;
				}
				NUDisplay.debug("URL case3: "+i+" || "+str, null);
			}
			gData.addInternetSinkURL(stmt, sb.toString());
		}
		else if(sm.getParameterCount()==3){
			// URL(URL context, String spec, URLStreamHandler handler)
			Value v = ie.getArg(1);
			String str = null;
			if(v instanceof StringConstant)
				str = ((StringConstant) v).value;
			else	
				str = ToolSet.findLastResStringAssignment(stmt, v, cfg, new HashSet<Stmt>());
			if(str != null) gData.addInternetSinkURL(stmt, str);
			NUDisplay.debug("URL case4: "+str, null);
		}
		else if(sm.getParameterCount() == 4){
			//URL(String protocol, String host, int port, String file)
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<sm.getParameterCount(); i++){
				if(i==2) continue;
				Value v = ie.getArg(i);
				String str = null;
				if(v instanceof StringConstant)
					str = ((StringConstant) v).value;
				else	
					str = ToolSet.findLastResStringAssignment(stmt, v, cfg, new HashSet<Stmt>());
				switch(i){
				case 0:
					sb.append(str==null? "*://" : str+"://");
					break;
				case 1:
					sb.append(str==null? "*/" : str+"/");
					break;
				case 3:
					sb.append(str==null? "*" : str);
					break;
				}
				NUDisplay.debug("URL case5: "+i+" || "+str, null);
			}
			gData.addInternetSinkURL(stmt, sb.toString());
		}
	}
	
	private String extractArgTextsHelper(Stmt stmt, Local arg){
		ToolSet.setCFGStartingTime();
		return ToolSet.findLastResStringAssignment(stmt, arg, cfg, new HashSet<Stmt>());
	}
	
	private boolean isInternetSinkStmt(Stmt s){
		if(!s.containsInvokeExpr())
			return false;
		InvokeExpr ie = s.getInvokeExpr();
		String clsName = null;
		SootMethod sm = null;
		try{
			sm = ie.getMethod();
			clsName = sm.getDeclaringClass().getName();
		}
		catch(Exception e){
			NUDisplay.error(e.toString(),"isInternetSinkStmt");
		}
		if(sm==null || clsName==null)
			return false;
		if(clsName.equals("org.apache.http.impl.client.DefaultHttpClient") && sm.getName().equals("execute"))
			return true;
		else if(clsName.equals("org.apache.http.client.HttpClient") && sm.getName().equals("execute"))
			return true;
		else if(clsName.equals("java.net.URL") && sm.getName().equals("<init>"))
			return true;
		return false;
	}
//	private void findViewDefStmt(Stmt stmt, Value target, List<NUAccessPath> bases,
//			BiDiInterproceduralCFG<Unit, SootMethod> cfg, Set<Stmt> visited, Set<Stmt> rs){
//		long timeDiffSeconds = (System.currentTimeMillis() - startingTime)/1000;
//		if(timeDiffSeconds > 300){//5mins
//			NUDisplay.error("passed time diff: "+timeDiffSeconds, null);
//			return ;
//		}
//		
//		if(visited.contains(stmt))
//			return ;
//		if(cfg == null){
//			NUDisplay.error("Error: findViewDefStmt: cfg is not set", null);
//			return ;
//		}
//		
//		Queue<Stmt> queue = new LinkedList<Stmt>();
//		queue.add(stmt);
//		
//		while(!queue.isEmpty()){
//			stmt = queue.poll();
//			visited.add(stmt);
//			//System.out.println("VISITED:"+visited.size()+" "+cfg.getMethodOf(stmt).getName()+" S:"+stmt);
//			
//			if(stmt instanceof AssignStmt){
//				AssignStmt as = (AssignStmt)stmt;
//				if(sameValue(as.getLeftOp(),target) ){
//					findViewDefStmtHelper(as, target, bases, cfg, visited, rs);
//					return ;
//				}
//				else if(target instanceof InstanceFieldRef){ //as.getLeftOp() != target
//					//if left != right, we only care if target is InstanceFieldRef
//					//because its possible a different Value points to target (alias)
//					Value left = as.getLeftOp();
//					if(pointToSameValue(left, target, bases)){
//						findViewDefStmtHelper(as, target, bases, cfg, visited, rs);	
//						return ;
//					}
//					
//					//left op doesn't point to the target
//					//check if left is a prefix of one of bases
//					if (!as.containsInvokeExpr() && NUAccessPath.containsAccessPathWithPrefix(bases, left)){
//						List<NUAccessPath> lst= NUAccessPath.findAccessPathWithPrefix(bases, left);
//						for(NUAccessPath ap : lst)
//							ap.replacePrefix(left, as.getRightOp());
//					}
//					else if(NUAccessPath.containsAccessPathWithPrefix(bases, left)){
//						List<NUAccessPath> lst= NUAccessPath.findAccessPathWithPrefix(bases, left);
//						//====process called method============
//						//TODO: HAVE NOT TESTED!!!
//						InvokeExpr ie = stmt.getInvokeExpr();
//						SootMethod sm = null;
//						if(ie != null) sm = ie.getMethod();
//						if(sm!=null && sm.hasActiveBody()){
//							UnitGraph g = new ExceptionalUnitGraph(sm.getActiveBody());
//							List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//							//if needs to replace base with $r0
//							if(ie instanceof InstanceInvokeExpr){ 
//								Value base = ((InstanceInvokeExpr) ie).getBase();
//								List<NUAccessPath> tmp = NUAccessPath.findAccessPathWithPrefix(bases, base);
//								if(tmp!=null && tmp.size()>0){
//									Local thisVar = null;
//									Iterator<Unit> it = g.iterator();
//									try{
//										while(it.hasNext()){
//											Stmt s = (Stmt)it.next();
//											if(s instanceof IdentityStmt && ((IdentityStmt) s).getRightOp() instanceof ThisRef){
//												thisVar = (Local)((IdentityStmt) s).getLeftOp();
//												break;
//											}
//										}
//									}
//									catch(Exception e){}
//									
//									if(thisVar != null){
//										for(NUAccessPath ap : tmp){
//											NUAccessPath newAP = new NUAccessPath(ap);
//											newAP.replacePrefix(base, thisVar);
//											newBases.add(newAP);
//										}
//									}
//								}
//							}
//							
//							for(Unit u : g.getTails()){
//								List<NUAccessPath> newBases2 = new ArrayList<NUAccessPath>();
//								newBases2.addAll(newBases);
//								if(u instanceof ReturnStmt){
//									//replace left with return value
//									for(NUAccessPath ap : lst){
//										NUAccessPath newAP = new NUAccessPath(ap);
//										newAP.replacePrefix(left, ((ReturnStmt) u).getOp());
//										newBases2.add(newAP);
//									}
//									//System.out.println("YYYYYY:"+((ReturnStmt) u).getOp()+"  "+u);
//									Set<Stmt> newVisited = null;
//									if(g.getTails().size() == 1)
//										newVisited = visited;
//									else{
//										newVisited = new HashSet<Stmt>();
//										newVisited.addAll(visited);
//									}
//									ToolSet.findViewDefStmt((Stmt)u, target, newBases2, cfg, newVisited, rs);
//								}
//								else{
//									System.out.println("Error: findViewDefStmtHelper"+u.getClass()+"  "+u);
//								}
//							}
//						}// sm.hasActiveBody
//						//=====================================
//						
//						for(NUAccessPath ap : lst)
//							bases.remove(ap);
//						if(bases == null)
//							return ;
//					}
//				}
//			}
//			else if(stmt instanceof IdentityStmt){
//				IdentityStmt is = (IdentityStmt)stmt;
//				//left value is target or left value is a prefix of target
//				if(pointToSameValue( is.getLeftOp(),target, bases) || 
//					(target instanceof InstanceFieldRef && NUAccessPath.containsAccessPathWithPrefix(
//								bases, ((IdentityStmt) stmt).getLeftOp()))){
//					if(is.getRightOp() instanceof ParameterRef){
//						ParameterRef right = (ParameterRef)(is.getRightOp());
//						Value left = ((IdentityStmt) stmt).getLeftOp();
//						int idx = right.getIndex();
//						Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(stmt));
//						if(callers != null && callers.size()>0){
//							for(Unit caller : callers){
//								InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
//								if(idx >= ie.getArgCount()) continue;
//								Value arg = ie.getArg(idx);
//								Set<Stmt> newVisited = null;
//								if(callers.size() == 1)
//									newVisited = visited;
//								else{
//									newVisited = new HashSet<Stmt>();
//									newVisited.addAll(visited);
//								}
//								if(pointToSameValue(left, target, bases)){
//									List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//									if(arg instanceof InstanceFieldRef)
//										newBases.add(new NUAccessPath(((InstanceFieldRef) arg).getBase()));
//	//								System.out.println("   Caller 1:"+caller+"@"+cfg.getMethodOf(caller).getSignature());
//	//								System.out.println("   NAP: "+NUAccessPath.listAPToString(newBases));
//									findViewDefStmt((Stmt) caller, arg, newBases, cfg, newVisited, rs);
//								}
//								else{
//									List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//									List<NUAccessPath> fitBases = NUAccessPath.findAccessPathWithPrefix(bases, left);
//									for(NUAccessPath np: fitBases){
//										NUAccessPath newNP = new NUAccessPath(np);
//										newNP.replacePrefix(left, arg);
//										newBases.add(newNP);
//									}
//									if(arg instanceof InstanceFieldRef)
//										NUAccessPath.addUniqueAccessPath(newBases, ((InstanceFieldRef) arg).getBase());
//	//								System.out.println("   Caller 2:"+caller+"@"+cfg.getMethodOf(caller).getSignature());
//	//								System.out.println("   NAP: "+NUAccessPath.listAPToString(newBases));
//									findViewDefStmt((Stmt) caller, target, newBases, cfg, newVisited, rs);
//								}
//							}
//						}
//					}
//					else if(is.getRightOp() instanceof ThisRef){
//						if(pointToSameValue(is.getLeftOp(),target, bases)){
//							System.out.println("ALERT: shouldn't come here findViewDefStmt 1");
//							return;
//						}
//						try{
//							List<SootMethod> methods = cfg.getMethodOf(stmt).getDeclaringClass().getMethods();
//							for(SootMethod method : methods){
//								if(method == cfg.getMethodOf(stmt)) continue;
//								if(!method.hasActiveBody()) continue;
//								UnitGraph g = new ExceptionalUnitGraph(method.getActiveBody());
//	//							System.out.println("   Start "+method.getName()+"@"+method.getDeclaringClass().getName()+" T:"+target);
//	//							System.out.println("   NAP: "+NUAccessPath.listAPToString(bases));
//							    for(Unit u : g.getTails()){
//							    	List<NUAccessPath> tmpBases = new ArrayList<NUAccessPath>();
//									List<NUAccessPath> fitBases = NUAccessPath.findAccessPathWithPrefix(bases, is.getLeftOp());
//									for(NUAccessPath np: fitBases)
//										tmpBases.add(new NUAccessPath(np));
//	//								System.out.println("   NAP NewBases: "+NUAccessPath.listAPToString(tmpBases));
//									Set<Stmt> newVisited = null;
//									if(g.getTails().size() == 1)
//										newVisited = visited;
//									else{
//										newVisited = new HashSet<Stmt>();
//										newVisited.addAll(visited);
//									}
//									findViewDefStmt((Stmt)u, target, tmpBases, cfg, newVisited, rs);
//							    }
//							}
//						}
//						catch(Exception e){
//							System.out.println("Error in findViewDefStmt: "+e+" "+stmt);
//						}
//					}
//					return ;
//				}
//			}
//			else if(stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr){
//				InstanceInvokeExpr iie = (InstanceInvokeExpr)stmt.getInvokeExpr();
//				Value base = iie.getBase();
//				if(NUAccessPath.containsAccessPathWithPrefix(bases, base)){
//					List<NUAccessPath> lst= NUAccessPath.findAccessPathWithPrefix(bases, base);
//					SootMethod method = iie.getMethod();
//					if(method.hasActiveBody()){
//						UnitGraph g = new ExceptionalUnitGraph(method.getActiveBody());
//						Local thisVar = null;
//						Iterator<Unit> it = g.iterator();
//						try{
//							while(it.hasNext()){
//								Stmt s = (Stmt)it.next();
//								if(s instanceof IdentityStmt && ((IdentityStmt) s).getRightOp() instanceof ThisRef){
//									thisVar = (Local)((IdentityStmt) s).getLeftOp();
//									break;
//								}
//							}
//						}
//						catch(Exception e){}
//						if(thisVar != null){
//							for(Unit u : g.getTails()){
//								List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//								for(NUAccessPath ap : lst){
//									NUAccessPath newap = new NUAccessPath(ap);
//									newap.replacePrefix(base, thisVar);
//									newBases.add(newap);
//								}
//								
//								Set<Stmt> newVisited = null;
//								if(g.getTails().size() == 1)
//									newVisited = visited;
//								else{
//									newVisited = new HashSet<Stmt>();
//									newVisited.addAll(visited);
//								}
//								findViewDefStmt((Stmt)u, target, newBases, cfg, newVisited, rs);
//							}
//						}
//					}
//				}
//			}
//			
//			for (Unit pred : cfg.getPredsOf(stmt)) {
//				if (!(pred instanceof Stmt))
//					continue;
//				if(visited.contains(pred))
//					continue;
//				//TODO: ideally, the newVisited should be reset for each predecessor, but it's too slow...
//				//4Set<Stmt> newVisited = visited;
//				queue.add((Stmt)pred);
//				//findViewDefStmt((Stmt) pred, target, bases, cfg, newVisited, rs);
//			}	
//		}
//	}
//	
//	private void findViewDefStmtHelper(AssignStmt stmt,  Value target, List<NUAccessPath> bases,
//			BiDiInterproceduralCFG<Unit, SootMethod> cfg, Set<Stmt> visited, Set<Stmt> rs){
//		//either isSame(target, stmt.getLeftOp()) or 
//		//target.fieldName==stmt.getLeftOp().fieldName && NUAccessPath.containsAccessPath(bases, stmt.getLeftOp().getBase())
//		Value right = stmt.getRightOp();
//		if(right instanceof InvokeExpr){
//			if(stmt.getInvokeExpr().getMethod().getName().equals(FIND_VIEW_BY_ID))
//				rs.add(stmt);
//			else  {
//				InvokeExpr ie = stmt.getInvokeExpr();
//				SootMethod sm = ie.getMethod();
//				if(sm.hasActiveBody()){
//					UnitGraph g = new ExceptionalUnitGraph(sm.getActiveBody());
//					List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//					if(ie instanceof InstanceInvokeExpr){ //if needs to replace base with $r0
//						Value base = ((InstanceInvokeExpr) ie).getBase();
//						List<NUAccessPath> tmp = NUAccessPath.findAccessPathWithPrefix(bases, base);
//						if(tmp!=null && tmp.size()>0){
//							Local thisVar = null;
//							Iterator<Unit> it = g.iterator();
//							try{
//								while(it.hasNext()){
//									Stmt s = (Stmt)it.next();
//									if(s instanceof IdentityStmt && ((IdentityStmt) s).getRightOp() instanceof ThisRef){
//										thisVar = (Local)((IdentityStmt) s).getLeftOp();
//										break;
//									}
//								}
//							}
//							catch(Exception e){}
//							
//							if(thisVar != null){
//								for(NUAccessPath ap : tmp){
//									NUAccessPath newAP = new NUAccessPath(ap);
//									newAP.replacePrefix(base, thisVar);
//									newBases.add(newAP);
//								}
//							}
//						}
//					}
//					
//					for(Unit u : g.getTails()){
//						List<NUAccessPath> newBases2 = new ArrayList<NUAccessPath>();
//						newBases2.addAll(newBases);
//						if(u instanceof ReturnStmt){
//							//System.out.println("YYYYYY:"+((ReturnStmt) u).getOp()+"  "+u);
//							Set<Stmt> newVisited = null;
//							if(g.getTails().size() == 1)
//								newVisited = visited;
//							else{
//								newVisited = new HashSet<Stmt>();
//								newVisited.addAll(visited);
//							}
//							findViewDefStmt((Stmt)u, ((ReturnStmt) u).getOp(), newBases2, cfg, newVisited, rs);
//						}
//						else{
//							System.out.println("Error: findViewDefStmtHelper"+u.getClass()+"  "+u);
//						}
//					}
//				}// sm.hasActiveBody
//			}
//		}
//		else if(right instanceof NewExpr){
//			String rightName = ((NewExpr)right).getType().getEscapedName();
//			String[] elems = rightName.split("\\.");
//			if(elems!=null && elems.length>0){
//				if(elems.length>1 && elems[elems.length-2].equals("widget") && elems[0].equals("android")){
//					rs.add(stmt);  //view
//				}
//				else if(elems.length>=3 && 
//						elems[0].equals("android") && elems[1].equals("app") && 
//						elems[elems.length-1].contains("Dialog")){ //dialog
//					rs.add(stmt);
//				}
//			}
//			else  System.out.println("ATTENTION: unknown def new expr:"+stmt);
//		}
//		else if(right instanceof CastExpr ){
//			for (Unit pred : cfg.getPredsOf(stmt)) {
//				if (!(pred instanceof Stmt))
//					continue;
//				Value newTarget = ((CastExpr) right).getOp();
//				List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//				for(NUAccessPath np: bases)
//					newBases.add(new NUAccessPath(np) );
//				if(newTarget instanceof InstanceFieldRef) 
//					NUAccessPath.addUniqueAccessPath(newBases, ((InstanceFieldRef) newTarget).getBase());
//				Set<Stmt> newVisited = null;
//				if(cfg.getPredsOf(stmt).size() == 1)
//					newVisited = visited;
//				else{
//					newVisited = new HashSet<Stmt>();
//					newVisited.addAll(visited);
//				}
//				findViewDefStmt((Stmt) pred, newTarget, newBases, cfg, newVisited, rs);
//			}
//		}
//		else if(right instanceof Local || right instanceof StaticFieldRef){
//			for (Unit pred : cfg.getPredsOf(stmt)) {
//				if (!(pred instanceof Stmt))
//					continue;
//				List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//				for(NUAccessPath np: bases)
//					newBases.add(new NUAccessPath(np) );
//				//NUAccessPath current = NUAccessPath.findAccessPath(newBases, stmt.getLeftOp());
//				Set<Stmt> newVisited = null;
//				if(cfg.getPredsOf(stmt).size() == 1)
//					newVisited = visited;
//				else{
//					newVisited = new HashSet<Stmt>();
//					newVisited.addAll(visited);
//				}
//				findViewDefStmt((Stmt) pred, right, newBases, cfg, newVisited, rs);
//			}
//		}
//		else if(right instanceof InstanceFieldRef){
//			for (Unit pred : cfg.getPredsOf(stmt)) {
//				if (!(pred instanceof Stmt))
//					continue;
//				List<NUAccessPath> newBases = new ArrayList<NUAccessPath>();
//				for(NUAccessPath np: bases)
//					newBases.add(new NUAccessPath(np) );
//				NUAccessPath.addUniqueAccessPath(newBases, ((InstanceFieldRef) right).getBase());
//				Set<Stmt> newVisited = null;
//				if(cfg.getPredsOf(stmt).size() == 1)
//					newVisited = visited;
//				else{
//					newVisited = new HashSet<Stmt>();
//					newVisited.addAll(visited);
//				}
//				findViewDefStmt((Stmt) pred, right, newBases, cfg, newVisited, rs);
//			}
//		}
//		else 
//			System.out.println("ATTENTION: unknown def expr:"+stmt);
//		return;
//	}
//	private boolean sameValue(Value left, Value right){
//		if((left instanceof Local) && (right instanceof Local))
//			return ((Local)left).getName().equals(((Local)right).getName());
//		else if((left instanceof StaticFieldRef) && (right instanceof StaticFieldRef))
//			return ((StaticFieldRef)left).getFieldRef().getSignature().equals(((StaticFieldRef)right).getFieldRef().getSignature());
//		else if((left instanceof InstanceFieldRef) && (right instanceof InstanceFieldRef)){
//			if( ((InstanceFieldRef)left).getField().getName().equals( ((InstanceFieldRef)right).getField().getName()))
//				return sameValue(((InstanceFieldRef)left).getBase(), ((InstanceFieldRef)right).getBase());	
//		}
//		else if((left instanceof ArrayRef) && (right instanceof ArrayRef)){
//			//TODO: what if $r1[$r2], $r1[$r4], but $r2 is the same with $r4
//			return sameValue(((ArrayRef)left).getBase(), ((ArrayRef)right).getBase()) &&
//					sameValue(((ArrayRef)left).getIndex(), ((ArrayRef)right).getIndex());
//		}
//		else if((left instanceof Constant) && (right instanceof Constant))
//			return left.toString().equals(right.toString());
//		return false;
//	}
//	
//	private boolean pointToSameValue(Value candidate, Value target, List<NUAccessPath> bases){
//		if(sameValue(candidate, target))
//			return true;
//		if(! (candidate instanceof InstanceFieldRef) )
//			return false;
//		if(! (target instanceof InstanceFieldRef) )
//			return false;
//		
//		if(((InstanceFieldRef)candidate).getField().getName().equals(((InstanceFieldRef)target).getField().getName())){
//			if(NUAccessPath.containsAccessPath(bases, ((InstanceFieldRef)candidate).getBase()))
//				return true;
//		}
//		return false;
//	}
//	
	/*** Private Methods ***/
	private Set<SootField> resolveLocalBase(UnitGraph g, Stmt s, Local target){
		Set<SootField> rs = new HashSet<SootField>();
		HashSet<Stmt> visited = new HashSet<Stmt>();
		Queue<Stmt> queue = new LinkedList<Stmt>();
		queue.add(s);
		Stmt origin = s;
		while(!queue.isEmpty()){
			s = queue.poll();
			visited.add(s);
			if(s instanceof AssignStmt){
				AssignStmt as = (AssignStmt)s;
				if(as.getLeftOp().equals(target)){
					if(as.getRightOp() instanceof FieldRef){
						FieldRef fr = (FieldRef)as.getRightOp();
						rs.add(fr.getField());
					}
					else
						NUDisplay.alert("TODO: resolveLocalBase missing case: "+s,"resolveLocalBase");
				}
			}
			else{
				for(Unit u : g.getPredsOf(s)){
					if(visited.contains((Stmt)u))
						continue;
					queue.add((Stmt)u);
				}
			}
		}
		if(rs.size() <= 0)
			NUDisplay.alert(" failed to store background resource: "+origin, null);
		return rs;
	}
	
//	private Integer findLastResIDAssignment(Stmt stmt, Value target, BiDiInterproceduralCFG<Unit, SootMethod> cfg, 
//			Set<Stmt> visited, String methodName) {
//		if(visited.contains(stmt)){
//			return null;
//		}
//		visited.add(stmt);
//		GlobalData gData = GlobalData.getInstance();
//		ResourceManager resMgr = ResourceManager.getInstance();
//		if(cfg == null) {
//			System.err.println("Error: findLastResIDAssignment cfg is not set.");
//			return null;
//		}
//		// If this is an assign statement, we need to check whether it changes
//		// the variable we're looking for
//		if (stmt instanceof AssignStmt) {
//			AssignStmt assign = (AssignStmt) stmt;
//			if (assign.getLeftOp() == target) {
//				System.out.println("  Debug: "+assign+" "+assign.getRightOp().getClass());
//				// ok, now find the new value from the right side
//				if (assign.getRightOp() instanceof IntConstant)
//					return ((IntConstant) assign.getRightOp()).value;
//				else if (assign.getRightOp() instanceof FieldRef) {
//					SootField field = ((FieldRef) assign.getRightOp()).getField();
//					for (Tag tag : field.getTags()){
//						if (tag instanceof IntegerConstantValueTag){
//							//System.out.println("This is an integerCOnstantValue");
//							return ((IntegerConstantValueTag) tag).getIntValue();
//						}
//						else
//							System.err.println("  Constant " + field + " was of unexpected type");
//					}
//					if(assign.getRightOp() instanceof InstanceFieldRef && 
//							(!methodName.equals("setBackgroundResource"))){
//						FieldRef fr = (FieldRef)assign.getRightOp();
//						Integer id = resMgr.getResourceIdByName(fr.getFieldRef().name());
//						if(id != null)
//							return id;
//						
//						id = gData.getFieldID(fr.getField());
//						if(id != null)
//							return id;
//					}
//					if(assign.getRightOp() instanceof StaticFieldRef){
//						StaticFieldRef sfr = (StaticFieldRef)assign.getRightOp();
//						Integer id = resMgr.getResourceIdByName(sfr.getFieldRef().name());
//						if(id != null)
//							return id;
//						
//						if(gData.getFieldID(sfr.getField())!=null ){
//							return gData.getFieldID(sfr.getField());
//						}
//						System.out.println("  Field not assigned:"+sfr);
//						target = assign.getRightOp();
//					}
//				} 
//				else if(assign.getRightOp() instanceof Local){
//					target = assign.getRightOp();
//				}
//				else if (assign.getRightOp() instanceof InvokeExpr) {
//					InvokeExpr inv = (InvokeExpr) assign.getRightOp();
//					if (inv.getMethod().getName().equals("getIdentifier") && 
//							inv.getMethod().getDeclaringClass().getName().equals("android.content.res.Resources") 
//							&& this.resourcePackages != null) {
//						// The right side of the assignment is a call into the
//						// well-known
//						// Android API method for resource handling
//						if (inv.getArgCount() != 3) {
//							System.err.println("Invalid parameter count for call to getIdentifier:"+inv.getArgCount());
//							return null;
//						}
//
//						// Find the parameter values
//						String resName = "";
//						String resID = "";
//						String packageName = "";
//
//						// In the trivial case, these values are constants
//						if (inv.getArg(0) instanceof StringConstant)
//							resName = ((StringConstant) inv.getArg(0)).value;
//						if (inv.getArg(1) instanceof StringConstant)
//							resID = ((StringConstant) inv.getArg(1)).value;
//						if (inv.getArg(2) instanceof StringConstant)
//							packageName = ((StringConstant) inv.getArg(2)).value;
//						else if (inv.getArg(2) instanceof Local)
//							packageName = ToolSet.findLastResStringAssignment(stmt, (Local) inv.getArg(2), cfg, new HashSet<Stmt>());
//						else {
//							if(inv.getArg(0) instanceof Local){
//								GraphTool.displayGraph(new ExceptionalUnitGraph(cfg.getMethodOf(assign).getActiveBody()), cfg.getMethodOf(assign));
//								String key = ToolSet.findLastResStringAssignment(stmt, (Local)inv.getArg(0), cfg,  new HashSet<Stmt>());
//								
//								if(key !=null && key.length()>0)
//									return resMgr.getResourceIdByName(key);
//							}
//							System.err.println("Unknown parameter type in call to getIdentifier: "+inv.getArg(0));
//							return null;
//						}
//
//						// Find the resource
//						ARSCFileParser.AbstractResource res = findResource(resName, resID, packageName);
//						if (res != null)
//							return res.getResourceID();
//					}
//					else if((inv.getMethod().getName().startsWith("get") || inv.getMethod().getName().equals("id") ) && 
//							inv.getArgCount()>=1 && inv.getArg(0) instanceof StringConstant && 
//							inv.getMethod().getReturnType().getEscapedName().equals("int")){
//						System.out.println("ReturnType:"+inv.getMethod().getReturnType().getEscapedName().equals("int"));
//						if(inv.getArgCount() < 1) return null;
//						String resName = "";
//						if (inv.getArg(0) instanceof StringConstant)
//							resName = ((StringConstant) inv.getArg(0)).value;
//						if(!resName.equals("")){
//							return resMgr.getResourceIdByName(resName);
//						}
//					}
//					else if(inv.getMethod().getName().equals("getId") && 
//							inv.getMethod().getDeclaringClass().getName().equals("com.playhaven.android.compat.VendorCompat") ){
//						//com.playhaven.android.compat.VendorCompat: int getId(android.content.Context,com.playhaven.android.compat.VendorCompat$ID)
//						Value v = inv.getArg(1);
//						if(v instanceof Local){
//							Value vv = findFirstLocalDef(assign, (Local)v, cfg);
//							if(vv!=null && vv instanceof StaticFieldRef){
//								Integer id = resMgr.getResourceIdByName(((StaticFieldRef)vv).getField().getName());
//								if(id != null)
//									return id;
//							}
//						}
//					}
//					else if(inv.getArgCount()>=1 && inv.getMethod().getReturnType().getEscapedName().equals("int")){
//						for(Value arg : inv.getArgs()){
//							if(arg instanceof StringConstant){
//								String key = ((StringConstant) arg).value;
//								Integer id = resMgr.getResourceIdByName(key);
//								if(id != null)
//									return id;
//							}
//						}
//					}
//					else if(inv.getArgCount()>=1 && inv.getMethod().getName().equals("inflate") ){
//						Value arg = inv.getArg(0);
//						if(arg instanceof Local)
//							target = arg;
//						else if(arg instanceof IntConstant)
//							return ((IntConstant) arg).value;
//					}
//					else{
//						try{
//						GraphTool.displayGraph(new ExceptionalUnitGraph(inv.getMethod().getActiveBody()), inv.getMethod());
//						}
//						catch(Exception e){}
//					}
//					
//				}
//			}
//			
//		}
//		else if(stmt instanceof IdentityStmt){
//			IdentityStmt is = (IdentityStmt)stmt;
//			if(is.getLeftOp() == target){
//				System.out.println("From IdentityStmt: "+is);
//				if(is.getRightOp() instanceof ParameterRef){
//					ParameterRef right = (ParameterRef)(is.getRightOp());
//					int idx = right.getIndex();
//					Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(stmt));
//					if(callers != null && callers.size()>0){
//						for(Unit caller : callers){
//							System.out.println("  Caller: From IdentityStmt: "+caller);
//							InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
//							if(idx >= ie.getArgCount())
//								continue;
//							Value arg = ie.getArg(idx);
//							if(arg instanceof IntConstant)
//								return ((IntConstant) arg).value;
//							else{
//								System.out.println("Still not integer");
//								Integer lastAssignment = findLastResIDAssignment((Stmt) caller, arg, cfg, visited, methodName);
//								if (lastAssignment != null)
//									return lastAssignment;
//							}
//						}
//					}
//				}
//			}
//		}
//
//		// Continue the search upwards
//		for (Unit pred : cfg.getPredsOf(stmt)) {
//			if (!(pred instanceof Stmt))
//				continue;
//			Integer lastAssignment = findLastResIDAssignment((Stmt) pred, target, cfg, visited, methodName);
//			if (lastAssignment != null)
//				return lastAssignment;
//		}
//		return null;
//	}
	

	/**
	 * Finds the last assignment to the given String local by searching upwards
	 * from the given statement
	 * 
	 * @param stmt
	 *            The statement from which to look backwards
	 * @param local
	 *            The variable for which to look for assignments
	 * @return The last value assigned to the given variable
	 */
//	private String findLastStringAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg, Set<Stmt> visited) {
//		if(visited.contains(stmt))
//			return null;
//		visited.add(stmt);
//		if (stmt instanceof AssignStmt) {
//			AssignStmt assign = (AssignStmt) stmt;
//			if (assign.getLeftOp() == local) {
//				// ok, now find the new value from the right side
//				if (assign.getRightOp() instanceof StringConstant)
//					return ((StringConstant) assign.getRightOp()).value;
//				
//			}
//		}
//
//		// Continue the search upwards
//		for (Unit pred : cfg.getPredsOf(stmt)) {
//			if (!(pred instanceof Stmt))
//				continue;
//			String lastAssignment = findLastStringAssignment((Stmt) pred, local, cfg, visited);
//			if (lastAssignment != null)
//				return lastAssignment;
//		}
//		return null;
//	}
	
	
	
//	private Value findFirstLocalDef(Stmt sp, Local target, BiDiInterproceduralCFG<Unit, SootMethod> cfg){
//		Set<Unit> visited = new HashSet<Unit>();
//		Queue<Unit> queue = new LinkedList<Unit>();
//		queue.add(sp);
//		while(!queue.isEmpty()){
//			Stmt stmt = (Stmt)queue.poll();
//			visited.add(stmt);
//			if(stmt instanceof AssignStmt){
//				if(((AssignStmt) stmt).getLeftOp() == target){
//					Value right = ((AssignStmt) stmt).getRightOp();
//					System.out.println(" found an def of target: "+right+" ?//"+right.getClass());
//					return right;
//				}
//			}
//			for (Unit pred : cfg.getPredsOf(stmt)) {
//				if (!(pred instanceof Stmt))
//					continue;
//				if(!visited.contains(pred))
//					queue.add(pred);
//			}
//		}
//		return null;
//	}
	

	
//	/**
//	 * Finds the given resource in the given package
//	 * 
//	 * @param resName
//	 *            The name of the resource to retrieve
//	 * @param resID
//	 * @param packageName
//	 *            The name of the package in which to look for the resource
//	 * @return The specified resource if available, otherwise null
//	 */
//	private AbstractResource findResource(String resName, String resID, String packageName) {
//		// Find the correct package
//		for (ARSCFileParser.ResPackage pkg : this.resourcePackages) {
//			// If we don't have any package specification, we pick the app's
//			// default package
//			boolean matches = (packageName == null || packageName.isEmpty()) && pkg.getPackageName().equals(this.appPackageName);
//			matches |= pkg.getPackageName().equals(packageName);
//			if (!matches)
//				continue;
//
//			// We have found a suitable package, now look for the resource
//			for (ARSCFileParser.ResType type : pkg.getDeclaredTypes())
//				if (type.getTypeName().equals(resID)) {
//					AbstractResource res = type.getFirstResource(resName);
//					return res;
//				}
//		}
//		return null;
//	}
//	
	
	
}

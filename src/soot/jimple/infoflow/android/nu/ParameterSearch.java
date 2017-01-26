package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import soot.Local;
import soot.MethodOrMethodContext;
import soot.PrimType;
import soot.Scene;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
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
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.SourceType;
import soot.jimple.infoflow.nu.GlobalData;
import soot.jimple.infoflow.nu.GraphTool;
import soot.jimple.infoflow.nu.StmtPosTag;
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
	final String GET_IDENTIFIER_SIGNATURE = 
			"<android.content.res.Resources: int getIdentifier(java.lang.String,java.lang.String,java.lang.String)>";
	ValueResourceParser valResParser = null;
	CallGraph cg = null;
	List<ARSCFileParser.ResPackage> resourcePackages;
	String appPackageName;
	BiDiInterproceduralCFG<Unit, SootMethod> cfg;
	public ParameterSearch(ValueResourceParser valResParser, List<ARSCFileParser.ResPackage> resourcePackages,String appPackageName,
			BiDiInterproceduralCFG<Unit, SootMethod> cfg){
		this.valResParser = valResParser;
		this.cg = Scene.v().getCallGraph();
		this.appPackageName = appPackageName;
		this.resourcePackages = resourcePackages;
		this.cfg = cfg;
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
		    		if(v instanceof Constant){
		    			//TODO: add constant to map
		    			continue;
		    		}
		    		
		    		s.addTag(new StmtPosTag(cnt, m));
		    		System.out.println("NonConstant FindViewByID in Method:"+s);
		    		List<Tag> tt = s.getTags();
		    		if((tt != null))
		    			for(Tag t : tt)
		    				System.out.println("  TAG: "+t.toString());
		    		rs.add(s);
		    		GraphTool.displayGraph(g, m);
		    		//searchVariableDefs(g, s, v, new ArrayList<List<Object>>(), m);
		    		
		    		//v2
		    		Integer id = findLastResIDAssignment(s, v, cfg, new HashSet<Stmt>());
		    		if(id == null){
		    			System.out.println("  Failed to resolve this ID.");
		    			unsolvedCnt++;
		    		}
		    		else{
		    			System.out.println("  ID Value: "+id);
		    			solvedCnt++;
		    			GlobalData global = GlobalData.getInstance();
		    			global.addViewID(s, cfg, id);
		    		}
		    	}
		    }
		}
		System.out.println("SolvedCnt:"+solvedCnt+" UnsolvedCnt:"+unsolvedCnt);
		return rs;
	}
	
	public Set<Stmt> setContentViewSearch(){
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
		    	if(ie.getMethod().getName().equals(SET_CONTENT_VIEW)){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant){
		    			//TODO: add constant to map
		    			continue;
		    		}
		    		
		    		s.addTag(new StmtPosTag(cnt, m));
		    		System.out.println("NonConstant SetContentView in Method:"+s);
		    		System.out.println("  DeclaringCls:"+cfg.getMethodOf(s).getDeclaringClass().getName());
		    		List<Tag> tt = s.getTags();
		    		if((tt != null))
		    			for(Tag t : tt)
		    				System.out.println("  TAG: "+t.toString());
		    		rs.add(s);
		    		GraphTool.displayGraph(g, m);
		    		//searchVariableDefs(g, s, v, new ArrayList<List<Object>>(), m);
		    		
		    		//v2
		    		Integer id = findLastResIDAssignment(s, v, cfg, new HashSet<Stmt>());
		    		if(id == null){
		    			System.out.println("  Failed to resolve this ID.");
		    			unsolvedCnt++;
		    		}
		    		else{
		    			System.out.println("  ID Value: "+id);
		    			solvedCnt++;
		    			GlobalData global = GlobalData.getInstance();
		    			global.addLayoutID(s, cfg, id);
		    		}
		    	}
		    }
		}
		System.out.println("SolvedCnt:"+solvedCnt+" UnsolvedCnt:"+unsolvedCnt);
		return rs;
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
		    			(className==null || ie.getMethod().getDeclaration().equals(className)) ){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant){
		    			//TODO: add constant to map
		    			continue;
		    		}
		    		System.out.println("Found one instance: "+s+" CLASS:"+ie.getMethod().getDeclaration());
		    		System.out.println("  "+gd.getViewID(s, cfg));
//		    		List<Tag> tags = s.getTags();
//		    		if((tags != null))
//		    			for(Tag t : tags)
//		    				System.out.println("  TAG: "+t.toString());
		    		
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
	
	private UnitGraph findMethodGraph(SootMethod method){
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			if(m.equals(method)){
				UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
				return g;
			}
		}
		return null;
	}
	
	private Integer findLastResIDAssignment(Stmt stmt, Value target, BiDiInterproceduralCFG<Unit, SootMethod> cfg, Set<Stmt> visited) {
//		if (!doneSet.add(stmt))
//			return null;
		if(visited.contains(stmt)){
			return null;
		}
		visited.add(stmt);
		
		if(cfg == null) {
			System.err.println("Error: findLastResIDAssignment cfg is not set.");
			return null;
		}
		// If this is an assign statement, we need to check whether it changes
		// the variable we're looking for
		if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			if (assign.getLeftOp() == target) {
				System.out.println("Debug: "+assign+" "+assign.getRightOp().getClass());
				// ok, now find the new value from the right side
				if (assign.getRightOp() instanceof IntConstant)
					return ((IntConstant) assign.getRightOp()).value;
				else if (assign.getRightOp() instanceof FieldRef) {
					SootField field = ((FieldRef) assign.getRightOp()).getField();
					for (Tag tag : field.getTags()){
						if (tag instanceof IntegerConstantValueTag){
							//System.out.println("This is an integerCOnstantValue");
							return ((IntegerConstantValueTag) tag).getIntValue();
						}
						else
							System.err.println("  Constant " + field + " was of unexpected type");
					}
					if(assign.getRightOp() instanceof StaticFieldRef){
						StaticFieldRef sfr = (StaticFieldRef)assign.getRightOp();
						if(sfr.getFieldRef().declaringClass().getName().endsWith(".R$id")){
							Integer id = valResParser.getResourceIDFromValueResourceFile(sfr.getFieldRef().name());
							if(id != null)
								return id;
						}
						System.out.println("  Field not assigned:"+sfr);
						target = assign.getRightOp();
					}
				} 
				else if(assign.getRightOp() instanceof Local){
					target = assign.getRightOp();
				}
				else if (assign.getRightOp() instanceof InvokeExpr) {
					InvokeExpr inv = (InvokeExpr) assign.getRightOp();
					if (inv.getMethod().getName().equals("getIdentifier") && 
							inv.getMethod().getDeclaringClass().getName().equals("android.content.res.Resources") 
							&& this.resourcePackages != null) {
						// The right side of the assignment is a call into the
						// well-known
						// Android API method for resource handling
						if (inv.getArgCount() != 3) {
							System.err.println("Invalid parameter count for call to getIdentifier:"+inv.getArgCount());
							return null;
						}

						// Find the parameter values
						String resName = "";
						String resID = "";
						String packageName = "";

						// In the trivial case, these values are constants
						if (inv.getArg(0) instanceof StringConstant)
							resName = ((StringConstant) inv.getArg(0)).value;
						if (inv.getArg(1) instanceof StringConstant)
							resID = ((StringConstant) inv.getArg(1)).value;
						if (inv.getArg(2) instanceof StringConstant)
							packageName = ((StringConstant) inv.getArg(2)).value;
						else if (inv.getArg(2) instanceof Local)
							packageName = findLastStringAssignment(stmt, (Local) inv.getArg(2), cfg, new HashSet<Stmt>());
						else {
							if(inv.getArg(0) instanceof Local){
								GraphTool.displayGraph(new ExceptionalUnitGraph(cfg.getMethodOf(assign).getActiveBody()), cfg.getMethodOf(assign));
								String key = findLastStringAssignment(stmt, (Local)inv.getArg(0), cfg,  new HashSet<Stmt>());
								
								if(key !=null && key.length()>0)
									return valResParser.getResourceIDFromValueResourceFile(key);
							}
							System.err.println("Unknown parameter type in call to getIdentifier: "+inv.getArg(0));
							return null;
						}

						// Find the resource
						ARSCFileParser.AbstractResource res = findResource(resName, resID, packageName);
						if (res != null)
							return res.getResourceID();
					}
					else if((inv.getMethod().getName().startsWith("get") || inv.getMethod().getName().equals("id") ) && 
							inv.getArgCount()>=1 && inv.getArg(0) instanceof StringConstant && 
							inv.getMethod().getReturnType().getEscapedName().equals("int")){
						System.out.println("ReturnType:"+inv.getMethod().getReturnType().getEscapedName().equals("int"));
						if(inv.getArgCount() < 1) return null;
						String resName = "";
						if (inv.getArg(0) instanceof StringConstant)
							resName = ((StringConstant) inv.getArg(0)).value;
						if(!resName.equals("")){
							return valResParser.getResourceIDFromValueResourceFile(resName);
						}
					}
					else if(inv.getMethod().getName().equals("getId") && 
							inv.getMethod().getDeclaringClass().getName().equals("com.playhaven.android.compat.VendorCompat") ){
						//com.playhaven.android.compat.VendorCompat: int getId(android.content.Context,com.playhaven.android.compat.VendorCompat$ID)
						Value v = inv.getArg(1);
						if(v instanceof Local){
							Value vv = findFirstLocalDef(assign, (Local)v, cfg);
							if(vv!=null && vv instanceof StaticFieldRef){
								Integer id = valResParser.getResourceIDFromValueResourceFile(((StaticFieldRef)vv).getField().getName());
								if(id != null)
									return id;
							}
						}
					}
					else if(inv.getArgCount()>=1 && inv.getMethod().getReturnType().getEscapedName().equals("int")){
						for(Value arg : inv.getArgs()){
							if(arg instanceof StringConstant){
								String key = ((StringConstant) arg).value;
								Integer id = valResParser.getResourceIDFromValueResourceFile(key);
								if(id != null)
									return id;
							}
						}
					}
					else if(inv.getArgCount()>=1 && inv.getMethod().getName().equals("inflate") ){
						Value arg = inv.getArg(0);
						if(arg instanceof Local)
							target = arg;
						else if(arg instanceof IntConstant)
							return ((IntConstant) arg).value;
					}
					else{
						try{
						GraphTool.displayGraph(new ExceptionalUnitGraph(inv.getMethod().getActiveBody()), inv.getMethod());
						}
						catch(Exception e){}
					}
					
				}
			}
			
		}
		else if(stmt instanceof IdentityStmt){
			IdentityStmt is = (IdentityStmt)stmt;
			if(is.getLeftOp() == target){
				System.out.println("From IdentityStmt: "+is);
				if(is.getRightOp() instanceof ParameterRef){
					ParameterRef right = (ParameterRef)(is.getRightOp());
					int idx = right.getIndex();
					Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(stmt));
					if(callers != null && callers.size()>0){
						for(Unit caller : callers){
							System.out.println("  Caller: From IdentityStmt: "+caller);
							InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
							if(idx >= ie.getArgCount())
								continue;
							Value arg = ie.getArg(idx);
							if(arg instanceof IntConstant)
								return ((IntConstant) arg).value;
							else{
								System.out.println("Still not integer");
								Integer lastAssignment = findLastResIDAssignment((Stmt) caller, arg, cfg, visited);
								if (lastAssignment != null)
									return lastAssignment;
							}
						}
					}
				}
			}
		}

		// Continue the search upwards
		for (Unit pred : cfg.getPredsOf(stmt)) {
			if (!(pred instanceof Stmt))
				continue;
			Integer lastAssignment = findLastResIDAssignment((Stmt) pred, target, cfg, visited);
			if (lastAssignment != null)
				return lastAssignment;
		}
		return null;
	}
	

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
	private String findLastStringAssignment(Stmt stmt, Local local, BiDiInterproceduralCFG<Unit, SootMethod> cfg, Set<Stmt> visited) {
		if(visited.contains(stmt))
			return null;
		visited.add(stmt);
		if (stmt instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) stmt;
			if (assign.getLeftOp() == local) {
				// ok, now find the new value from the right side
				if (assign.getRightOp() instanceof StringConstant)
					return ((StringConstant) assign.getRightOp()).value;
				
			}
		}

		// Continue the search upwards
		for (Unit pred : cfg.getPredsOf(stmt)) {
			if (!(pred instanceof Stmt))
				continue;
			String lastAssignment = findLastStringAssignment((Stmt) pred, local, cfg, visited);
			if (lastAssignment != null)
				return lastAssignment;
		}
		return null;
	}
	
	private List<Stmt> searchVariableDefs(UnitGraph g, Stmt s, Value target, List<List<Object>> args, SootMethod m){
		List<Stmt> rs = new ArrayList<Stmt>();
		Queue<Unit> queue = new LinkedList<Unit>();
		Set<Unit> visited = new HashSet<Unit>();
		queue.add(s);
		
		while(!queue.isEmpty()){
			Stmt pred = (Stmt)queue.poll();
			visited.add(pred);
			boolean cont = true;
			if(pred instanceof AssignStmt){
				AssignStmt as = (AssignStmt)pred;
				if(as.getLeftOp().equals(target)){
					Value right = as.getRightOp();
					cont = false;
					System.out.println("  -"+as+" "+right.getClass());
					if(right instanceof CastExpr){
						System.out.println("  DEBUG: this is cast expr: "+right);
						target = ((CastExpr) right).getOp();
					}
					else if(right instanceof BinopExpr ||
							right instanceof UnopExpr){
						System.out.println("  DEBUG: this is binop/unop expr: "+right);
					}
					else if(right instanceof AnyNewExpr ||
							right instanceof NewArrayExpr ||
							right instanceof NewExpr ||
							right instanceof NewMultiArrayExpr){
						System.out.println("  DEBUG: this is new expr: "+right);
					}
					else if(right instanceof InvokeExpr){
						System.out.println("  DEBUG: this is invoke expr: "+right);
						InvokeExpr nie = (InvokeExpr)right;
						if(nie.getArgCount()>0){
							List<Object> newArgs = new ArrayList<Object>();
							for(int i=0; i<nie.getArgCount(); i++){
								Value arg = nie.getArg(i);
								if(arg instanceof StringConstant){
									StringConstant sc = (StringConstant)arg;
									newArgs.add(sc.value);
								}
								else if(arg instanceof Constant){
									newArgs.add(arg);
								}
							}
							args.add(newArgs);
						}
						UnitGraph targetGraph = findMethodGraph(nie.getMethod());
						if(targetGraph == null){
							if(nie.getMethod().getSignature().equals(GET_IDENTIFIER_SIGNATURE)){
								//System.out.println(nie.getMethod().getSignature());
								if(args.size()>0)
									handleGetIdentifierCase(args.get(0));
								else
									System.out.println("No args stored for getIndetifier");
							}
							else{
								System.out.println("  ALERT: no body: "+nie.getMethod());
							}
						}
						else{
							//GraphTool.displayGraph(targetGraph, nie.getMethod());
							List<Unit> tails = targetGraph.getTails();
							for(Unit t : tails){
								if(t instanceof RetStmt){//No use case
									RetStmt retStmt = (RetStmt)t;
									//System.out.println("  RetVal:"+retStmt.getStmtAddress());
									searchVariableDefs(targetGraph, (Stmt)t, retStmt.getStmtAddress(),args, nie.getMethod());
								}
								else if(t instanceof ReturnStmt){
									ReturnStmt returnStmt = (ReturnStmt)t;
									//System.out.println("  ReturnVal:"+returnStmt.getOp());
									searchVariableDefs(targetGraph, (Stmt)t, returnStmt.getOp(),args, nie.getMethod());
								}
							}
							//searchVariableDefs(g, s, v);
						}
					}
					else{
						if(right instanceof StaticFieldRef){
							try{
								StaticFieldRef sfr = (StaticFieldRef)right;
								
								searchStaticVariable(sfr);
							}
							catch(Exception e){
								System.out.println("Error converting NumbericConstant to int: "+e+" "+right);
							}
							
						}
						else if(right instanceof FieldRef){
							//TODO: regular field
							System.out.println("  DEBUG: this is other expr: "+right.getClass());
						}
						else{
							System.out.println("  DEBUG: this is other expr: "+right.getClass());
						}
					}
			
					rs.add(as);
				}
				
			}
			else if(pred instanceof IdentityStmt){
				System.out.println("  DEBUG: this is IdentityStmt: ");
				Iterator<Edge> edges = cg.edgesInto(m);
				while(edges.hasNext()){
					Edge edge = edges.next();
					MethodOrMethodContext mmc = edge.getSrc();
					System.out.println("    CalledBy:"+mmc.method().getSignature());
					//GraphTool.displayGraph(findMethodGraph(mmc.method()), mmc.method());
				}
			}
			
			if(cont){
				List<Unit> preds = g.getPredsOf(pred);
				for(Unit p : preds){
					if(visited.contains(p)) continue;
					queue.add(p);
				}
			}
		}
		return rs;
	}
	
	private Value findFirstLocalDef(Stmt sp, Local target, BiDiInterproceduralCFG<Unit, SootMethod> cfg){
		Set<Unit> visited = new HashSet<Unit>();
		Queue<Unit> queue = new LinkedList<Unit>();
		queue.add(sp);
		while(!queue.isEmpty()){
			Stmt stmt = (Stmt)queue.poll();
			visited.add(stmt);
			if(stmt instanceof AssignStmt){
				if(((AssignStmt) stmt).getLeftOp() == target){
					Value right = ((AssignStmt) stmt).getRightOp();
					System.out.println(" found an def of target: "+right+" ?//"+right.getClass());
					return right;
				}
			}
			for (Unit pred : cfg.getPredsOf(stmt)) {
				if (!(pred instanceof Stmt))
					continue;
				if(!visited.contains(pred))
					queue.add(pred);
			}
		}
		return null;
	}
	
	private void handleGetIdentifierCase(List<Object> args){
		for(int i=0; i<args.size(); i++){
			if(!(args.get(i) instanceof String))
				continue;
			String arg = (String)args.get(i);
			Integer id = valResParser.getResourceIDFromValueResourceFile(arg);
			if(id != null){
				System.out.println("FOUND ID of "+args.get(i)+" "+id);
			}
		}
	}
	
	/**
	 * Finds the given resource in the given package
	 * 
	 * @param resName
	 *            The name of the resource to retrieve
	 * @param resID
	 * @param packageName
	 *            The name of the package in which to look for the resource
	 * @return The specified resource if available, otherwise null
	 */
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
	
	private void searchStaticVariable(StaticFieldRef sfr){
		System.out.println("Start search static variable: "+sfr);
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt s = (Stmt)u;
		    	if(s instanceof AssignStmt){
		    		AssignStmt as = (AssignStmt)s;
		    		Value left = as.getLeftOp();
		    		if(left instanceof StaticFieldRef){
		    			StaticFieldRef ls = (StaticFieldRef)left;
		    			if(ls.getField().equals(sfr.getField().getName())  && 
		    					ls.getFieldRef().declaringClass().getName().equals(sfr.getFieldRef().declaringClass().getName())){
			    			System.out.println("  SEARCH_STATIC_VARIABLE:"+as+" ||"+sfr.getField().getName());
			    		}
			    		else{
			    			//System.out.println("");
			    		}
		    		}
		    	}
		    }
		}
	}
	
}

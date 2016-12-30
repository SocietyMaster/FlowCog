package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import soot.MethodOrMethodContext;
import soot.PrimType;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NumericConstant;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.nu.GraphTool;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.util.queue.QueueReader;

public class ParameterSearch {
	final String FIND_VIEW_BY_ID = "findViewById";
	final String GET_IDENTIFIER_SIGNATURE = 
			"<android.content.res.Resources: int getIdentifier(java.lang.String,java.lang.String,java.lang.String)>";
	ResourceManager resMgr = null;
	
	public ParameterSearch(ResourceManager resMgr){
		this.resMgr = resMgr;
	}
	public void findViewByIdParamSearch(){
		
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
		    	if(ie.getMethod().getName().equals(FIND_VIEW_BY_ID)){
		    		Value v = ie.getArg(0);
		    		if(v instanceof Constant){
		    			//TODO: add constant to map
		    			continue;
		    		}
		    		System.out.println("NonConstant FindViewByID in Method:"+m+" "+ie);
		    		GraphTool.displayGraph(g, m);
		    		searchVariableDefs(g, s, v, new ArrayList<List<Object>>());
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
	
	private List<Stmt> searchVariableDefs(UnitGraph g, Stmt s, Value target, List<List<Object>> args){
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
					System.out.println("  "+as);
					if(right instanceof CastExpr){
						System.out.println("  DEBUG: this is cast expr: "+right);
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
							//TODO: change to Signature
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
							GraphTool.displayGraph(targetGraph, nie.getMethod());
							List<Unit> tails = targetGraph.getTails();
							for(Unit t : tails){
								if(t instanceof RetStmt){
									RetStmt retStmt = (RetStmt)t;
									//System.out.println("  RetVal:"+retStmt.getStmtAddress());
									searchVariableDefs(targetGraph, (Stmt)t, retStmt.getStmtAddress(),args);
								}
								else if(t instanceof ReturnStmt){
									ReturnStmt returnStmt = (ReturnStmt)t;
									//System.out.println("  ReturnVal:"+returnStmt.getOp());
									searchVariableDefs(targetGraph, (Stmt)t, returnStmt.getOp(),args);
								}
							}
							//searchVariableDefs(g, s, v);
						}
					}
					else{
						if(right instanceof StaticFieldRef){
							try{
								StaticFieldRef sfr = (StaticFieldRef)right;
								Integer v = sfr.getField().getNumber();
								System.out.println("Found id: "+v);
								//TODO: add constant to map
							}
							catch(Exception e){
								System.out.println("Error converting NumbericConstant to int: "+e+" "+right);
							}
							
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
	
	private void handleGetIdentifierCase(List<Object> args){
		for(int i=0; i<args.size(); i++){
			if(!(args.get(i) instanceof String))
				continue;
			String arg = (String)args.get(i);
			if(resMgr.getValueResourceNameIDMap().containsKey(arg)){
				int id = resMgr.getValueResourceNameIDMap().get(arg);
				System.out.println("FOUND ID of "+args.get(i)+" "+id);
			}
		}
	}
	
}

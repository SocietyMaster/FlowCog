package soot.jimple.infoflow.android.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import heros.InterproceduralCFG;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.ParameterRef;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.SourceType;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.nu.GraphTool;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.infoflow.source.data.AccessPathTuple;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.util.queue.QueueReader;

public class ConstantPropogationSourceSinkManager extends AccessPathBasedSourceSinkManager {
	private static int MIN_ID_VALUE = 255;
	
	public ConstantPropogationSourceSinkManager(Set<SourceSinkDefinition> sources, Set<SourceSinkDefinition> sinks) {
		super(sources, sinks);
	}
	
	public ConstantPropogationSourceSinkManager(Set<SourceSinkDefinition> sources,
			Set<SourceSinkDefinition> sinks,
			Set<SootMethodAndClass> callbackMethods,
			LayoutMatchingMode layoutMatching,
			Map<Integer, LayoutControl> layoutControls) {
		super(sources, sinks, callbackMethods, layoutMatching, layoutControls);
	}
	
	
	@Override
	public SourceInfo getSourceInfo(Stmt stmt, InterproceduralCFG<Unit, SootMethod> cfg) {
		SourceType type = getSourceType(stmt, cfg);
		if (type!=SourceType.IntConstantSource)
			return null;
		if(stmt instanceof DefinitionStmt){
			DefinitionStmt defStmt = (DefinitionStmt) stmt;
			return new SourceInfo(AccessPathFactory.v().createAccessPath(
				defStmt.getLeftOp(), true));
		}
		else{ // e.g., ArrayList.add(Integer)
			if(!stmt.containsInvokeExpr())
				return null;
			InvokeExpr ie = stmt.getInvokeExpr();
			
			if(ie instanceof InstanceInvokeExpr){
				Value baseVal = ((InstanceInvokeExpr)ie).getBase();
				return new SourceInfo(AccessPathFactory.v().createAccessPath(
						baseVal, true));
			}
		}
		return null;
	}
	
	@Override
	public SourceType getSourceType(Stmt stmt, InterproceduralCFG<Unit, SootMethod> cfg) {
		assert cfg != null;
		assert cfg instanceof BiDiInterproceduralCFG;
		if(!(stmt instanceof DefinitionStmt)){
			return SourceType.NoSource;
		}
		// This might be a normal source method
		if(stmt instanceof AssignStmt){
			AssignStmt as = (AssignStmt)stmt;
			Value right = as.getRightOp();
			if(right instanceof CastExpr){
				CastExpr ce = (CastExpr)right;
				Value v = ce.getOp();
				if(v instanceof IntConstant){
					if(((IntConstant) v).value >= MIN_ID_VALUE)
						return SourceType.IntConstantSource;
				}
				else if(v instanceof StringConstant)
					return SourceType.StringConstantSource;
			}
			else if(right instanceof UnopExpr){
				UnopExpr ue = (UnopExpr)right;
				Value v = ue.getOp();
				if(v instanceof IntConstant)
					if(((IntConstant) v).value >= MIN_ID_VALUE)
						return SourceType.IntConstantSource;
				else if(v instanceof StringConstant)
					return SourceType.StringConstantSource;
			}
			else if(right instanceof BinopExpr ){
				BinopExpr be = (BinopExpr)right;
				Value v1 = be.getOp1();
				Value v2 = be.getOp2();
				if(v1 instanceof IntConstant && v2 instanceof IntConstant)
					if( ((IntConstant) v1).value >= MIN_ID_VALUE || ((IntConstant) v2).value >= MIN_ID_VALUE)
						return SourceType.IntConstantSource;
				else if(v1 instanceof StringConstant && v2 instanceof StringConstant)
					return SourceType.StringConstantSource;
			}
			else if(right instanceof AnyNewExpr ||
					right instanceof NewArrayExpr ||
					right instanceof NewExpr ||
					right instanceof NewMultiArrayExpr){
				//do nothing
			}
			else if(right instanceof InvokeExpr){
				InvokeExpr ie = stmt.getInvokeExpr();
				if(ie.getArgCount() == 0){
					//TODO:
					//System.out.println("ALERT: getSourceType: InvokeExpr with 0 Arg Assignment."+as);
					return SourceType.NoSource;
				}
				SootMethod sm = ie.getMethod();
				if(sm.getName().equals("valueOf") && sm.getDeclaringClass().getName().equals("java.lang.Integer")){
					Value arg = ie.getArg(0);
					if(arg instanceof IntConstant)
						if(((IntConstant) arg).value >= MIN_ID_VALUE)
							return SourceType.IntConstantSource;
					else if(arg instanceof StringConstant)
						return SourceType.StringConstantSource;
				}
				else if(sm.getName().equals("valueOf") && sm.getDeclaringClass().getName().equals("java.lang.String")){
					Value arg = ie.getArg(0);
					if(arg instanceof IntConstant)
						if(((IntConstant) arg).value >= MIN_ID_VALUE)
							return SourceType.IntConstantSource;
					else if(arg instanceof StringConstant)
						return SourceType.StringConstantSource;
				}
				else if(sm.getName().equals("get") && sm.getDeclaringClass().getName().equals("java.util.List")){
					//TODO: write this part as blacklist
					return SourceType.NoSource;
				}
				else if(sm.getName().equals("findViewById")){
					return SourceType.NoSource;
				}
				else if(sm.getParameterCount()==1){
					//TODO: this branch is too coarse.
					//It's better to provide explicit function name, like last two branches.
					Value arg = ie.getArg(0);
					if(arg instanceof IntConstant)
						if(((IntConstant) arg).value >= MIN_ID_VALUE)
							return SourceType.IntConstantSource;
					else if(arg instanceof StringConstant)
						return SourceType.StringConstantSource;
				}
			}
			else if(right instanceof IntConstant){
				if(((IntConstant) right).value >= MIN_ID_VALUE)
					return SourceType.IntConstantSource;
			}
			else if(right instanceof StringConstant){
				return SourceType.StringConstantSource;
			}
		}// stmt instanceof AssignStmt
		else if(stmt instanceof IdentityStmt){
			//$1 : parameter $1
			//it's a source if it's called with constant 
			IdentityStmt is = (IdentityStmt)stmt;
			if(is.getRightOp() instanceof ParameterRef){
				ParameterRef right = (ParameterRef)(is.getRightOp());
				int idx = right.getIndex();
				Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(stmt));
				if(callers != null && callers.size()>0){
					//System.out.println("  Callers of iden stmt: "+stmt+" "+idx);
					for(Unit caller : callers){
						InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
						if(idx >= ie.getArgCount())
							continue;
						Value arg = ie.getArg(idx);
						if(arg instanceof IntConstant)
							if(((IntConstant) arg).value >= MIN_ID_VALUE)
								return SourceType.IntConstantSource;
						else if(arg instanceof StringConstant)
							return SourceType.StringConstantSource;
					}
				}
			}
			
		}

		return SourceType.NoSource;
	}
	
	public void displayAllSourceStmts(InterproceduralCFG<Unit, SootMethod> cfg){
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody()) continue;
			
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    for (Unit u : orderer.newList(g, false)) {
		    	SourceInfo si = getSourceInfo((Stmt)u, cfg);
		    	if(si != null){
		    		System.out.println("SOURCE STMT:"+u+" ||"+getSourceType((Stmt)u, cfg));
		    		System.out.println("  Detail:"+si);
		    	}
		    	Stmt stmt = (Stmt)u;
		    	if(stmt instanceof IdentityStmt){
					//$1 : parameter $1
					//it's a source if it's called with constant 
					IdentityStmt is = (IdentityStmt)stmt;
					if(is.getRightOp() instanceof ParameterRef){
						ParameterRef right = (ParameterRef)(is.getRightOp());
						int idx = right.getIndex();
						Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(stmt));
						if(callers != null && callers.size()>0){
							for(Unit caller : callers){
								InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
								if(idx >= ie.getArgCount())
									continue;
								Value arg = ie.getArg(idx);
								if(arg instanceof Constant)
									System.out.println("    Arg: "+arg+" @"+caller);
								
							}
						}
					}		
		    	}
		    }
		    
		}
	}

}

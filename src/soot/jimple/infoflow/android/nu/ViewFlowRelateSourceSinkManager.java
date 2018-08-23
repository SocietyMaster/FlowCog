/*
 * This class is responsible for dealing with the sources and sinks of view-flow data flow analysis. 
 * Mostly copied from class AccessPathBasedSourceSinkManager.
 * By Xiang.
 * */
package soot.jimple.infoflow.android.nu;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import heros.InterproceduralCFG;
import soot.PrimType;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.source.AccessPathBasedSourceSinkManager;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.SourceType;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.data.AccessPathFactory;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.nu.FlowPathSet;
import soot.jimple.infoflow.source.SourceInfo;
import soot.jimple.infoflow.source.data.AccessPathTuple;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;
import soot.jimple.infoflow.util.SystemClassHandler;

public class ViewFlowRelateSourceSinkManager extends AccessPathBasedSourceSinkManager{
	private FlowPathSet fps;
	
	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with either strong or weak matching.
	 * 
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 */
	public ViewFlowRelateSourceSinkManager(Set<SourceSinkDefinition> sources,
			Set<SourceSinkDefinition> sinks) {
		super(sources, sinks);
		this.fps = null;
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with strong matching, i.e. the methods in
	 * the code must exactly match those in the list.
	 * 
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 * @param callbackMethods
	 *            The list of callback methods whose parameters are sources through which the application receives data
	 *            from the operating system
	 * @param weakMatching
	 *            True for weak matching: If an entry in the list has no return type, it matches arbitrary return types
	 *            if the rest of the method signature is compatible. False for strong matching: The method signature in
	 *            the code exactly match the one in the list.
	 * @param layoutMatching
	 *            Specifies whether and how to use Android layout components as sources for the information flow
	 *            analysis
	 * @param layoutControls
	 *            A map from reference identifiers to the respective Android layout controls
	 */
	public ViewFlowRelateSourceSinkManager(Set<SourceSinkDefinition> sources,
			Set<SourceSinkDefinition> sinks,
			Set<SootMethodAndClass> callbackMethods,
			LayoutMatchingMode layoutMatching,
			Map<Integer, LayoutControl> layoutControls, FlowPathSet fps) {
		super(sources, sinks, callbackMethods, layoutMatching, layoutControls);
		this.fps = fps;
	}
	
	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
		// Callbacks and UI controls have already been properly handled by parent implementation
		SourceType type = getSourceType(sCallSite, cfg);
		
		if (type == SourceType.NoSource)
			return null;
		if (type == SourceType.Callback ){
			return null;
		}
		if(type == SourceType.NewDialogSource || type==SourceType.NewWidgetSource){
			SourceInfo si =  super.getSourceInfo(sCallSite, cfg);
			return si;
		}
		
		// This is a method-based source, so we need to obtain the correct
		// access path
		final String signature = methodToSignature.getUnchecked(
				sCallSite.getInvokeExpr().getMethod());
		SourceSinkDefinition def = sourceMethods.get(signature);
				
		// If we don't have any more precise source information, we take the
		// default behavior of our parent implementation. We do the same if we
		// tried using access paths and failed, but this is a shortcut in case
		// we know that we don't have any access paths anyway.
		if (null == def || def.isEmpty()){
			SourceInfo si = super.getSourceInfo(sCallSite, cfg);
			return si;
		}
		// We have real access path definitions, so we can construct precise
		// source information objects
		Set<AccessPath> aps = new HashSet<>();
		
		// Check whether we need to taint the base object
		if (sCallSite.containsInvokeExpr()
				&& sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr
				&& def.getBaseObjects() != null) {
			Value baseVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
			for (AccessPathTuple apt : def.getBaseObjects())
				if (apt.isSource())
					aps.add(getAccessPathFromDef(baseVal, apt));
		}
		
		// Check whether we need to taint the return object
		if (sCallSite instanceof DefinitionStmt && def.getReturnValues() != null) {
			Value returnVal = ((DefinitionStmt) sCallSite).getLeftOp();
			for (AccessPathTuple apt : def.getReturnValues())
				if (apt.isSource())
					aps.add(getAccessPathFromDef(returnVal, apt));
		}
		
		// Check whether we need to taint parameters
		if (sCallSite.containsInvokeExpr()
				&& def.getParameters() != null
				&& def.getParameters().length > 0)
			for (int i = 0; i < sCallSite.getInvokeExpr().getArgCount(); i++)
				if (def.getParameters().length > i)
					for (AccessPathTuple apt : def.getParameters()[i])
						if (apt.isSource())
							aps.add(getAccessPathFromDef(sCallSite.getInvokeExpr().getArg(i), apt));
		
		// If we don't have any more precise source information, we take the
		// default behavior of our parent implementation
		if (aps.isEmpty()){
			SourceInfo si = super.getSourceInfo(sCallSite, cfg);
			return si;
		}
		
		SourceInfo si =  new SourceInfo(aps);
		return si;
	}
	
	/**
	 * Creates an access path from an access path definition object
	 * @param baseVal The base for the new access path
	 * @param apt The definition from which to create the new access path
	 * @return The newly created access path
	 */
	private AccessPath getAccessPathFromDef(Value baseVal, AccessPathTuple apt) {
		if (baseVal.getType() instanceof PrimType
				|| apt.getFields() == null
				|| apt.getFields().length == 0)
			return AccessPathFactory.v().createAccessPath(baseVal, true);
		
		SootClass baseClass = ((RefType) baseVal.getType()).getSootClass();
		SootField[] fields = new SootField[apt.getFields().length];
		for (int i = 0; i < fields.length; i++)
			fields[i] = baseClass.getFieldByName(apt.getFields()[i]);
		
		return AccessPathFactory.v().createAccessPath(baseVal, fields, true);
	}
	
	/**
	 * No sink for View-Flow dataflow analysis.
	 * */
	@Override
	public boolean isSink(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg,
			AccessPath sourceAccessPath) {
		return false;
	}
	
	/**
	 * Checks whether the given access path matches the given definition
	 * @param sourceAccessPath The access path to check
	 * @param apt The definition against which to check the access path
	 * @return True if the given access path matches the given definition,
	 * otherwise false
	 */
	private boolean accessPathMatches(AccessPath sourceAccessPath,
			AccessPathTuple apt) {
		// If the source or sink definitions does not specify any fields, it
		// always matches
		if (apt.getFields() == null
				|| apt.getFields().length == 0
				|| sourceAccessPath == null)
			return true;
		
		for (int i = 0; i < apt.getFields().length; i++) {
			// If a.b.c.* is our defined sink and a.b is tainted, this is not a
			// leak. If a.b.* is tainted, it is. 
			if (i >= sourceAccessPath.getFieldCount())
				return sourceAccessPath.getTaintSubFields();
			
			// Compare the fields
			if (!sourceAccessPath.getFields()[i].getName().equals(apt.getFields()[i]))
				return false;
		}
		return true;
	}
}

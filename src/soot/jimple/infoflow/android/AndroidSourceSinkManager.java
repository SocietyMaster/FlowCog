package soot.jimple.infoflow.android;

import java.util.List;
import java.util.Map;

import soot.SootMethod;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.source.MethodBasedSourceSinkManager;

/**
 * SourceManager implementation for AndroidSources
 * 
 * @author Steven Arzt
 */
public class AndroidSourceSinkManager extends MethodBasedSourceSinkManager {
	
	/**
	 * Possible modes for matching layout components as data flow sources
	 * 
	 * @author Steven Arzt
	 */
	public enum LayoutMatchingMode {
		/**
		 * Do not use Android layout components as sources
		 */
		NoMatch,
		
		/**
		 * Use all layout components as sources
		 */
		MatchAll,
		
		/**
		 * Only use sensitive layout components (e.g. password fields) as
		 * sources
		 */
		MatchSensitiveOnly
	}
	
	private final static String Activity_FindViewById =
			"<android.app.Activity: android.view.View findViewById(int)>";
	private final static String View_FindViewById =
			"<android.app.View: android.view.View findViewById(int)>";

	private final List<AndroidMethod> sourceMethods;
	private final List<AndroidMethod> sinkMethods;
	private final boolean weakMatching;
	
	private final LayoutMatchingMode layoutMatching;
	private final Map<Integer, LayoutControl> layoutControls;
	
	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those
	 * in the list.
	 * @param sources The list of source methods 
	 * @param sinks The list of sink methods 
	 */
	public AndroidSourceSinkManager
			(List<AndroidMethod> sources,
			List<AndroidMethod> sinks) {
		this (sources, sinks, false);
	}
	
	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * either strong or weak matching.
	 * @param sources The list of source methods
	 * @param sinks The list of sink methods
	 * @param weakMatching True for weak matching: If an entry in the list has
	 * no return type, it matches arbitrary return types if the rest of the
	 * method signature is compatible. False for strong matching: The method
	 * signature in the code exactly match the one in the list.
	 */
	public AndroidSourceSinkManager
			(List<AndroidMethod> sources,
			List<AndroidMethod> sinks,
			boolean weakMatching) {
		this(sources, sinks, weakMatching, LayoutMatchingMode.NoMatch, null);
	}

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * strong matching, i.e. the methods in the code must exactly match those
	 * in the list.
	 * @param sources The list of source methods 
	 * @param sinks The list of sink methods
	 * @param weakMatching True for weak matching: If an entry in the list has
	 * no return type, it matches arbitrary return types if the rest of the
	 * method signature is compatible. False for strong matching: The method
	 * signature in the code exactly match the one in the list.
	 * @param layoutMatching Specifies whether and how to use Android layout
	 * components as sources for the information flow analysis
	 * @param layoutControls A map from reference identifiers to the respective
	 * Android layout controls
	 */
	public AndroidSourceSinkManager
			(List<AndroidMethod> sources,
			List<AndroidMethod> sinks,
			boolean weakMatching,
			LayoutMatchingMode layoutMatching,
			Map<Integer, LayoutControl> layoutControls) {
		this.sourceMethods = sources;
		this.sinkMethods = sinks;
		this.weakMatching = false;
		this.layoutMatching = layoutMatching;
		this.layoutControls = layoutControls;
	}

	/**
	 * Checks whether the given method matches one of the methods from the list
	 * @param sMethod The method to check for a match
	 * @param aMethods The list of reference methods
	 * @return True if the given method matches an entry in the list, otherwise
	 * false
	 */
	private boolean matchesMethod(SootMethod sMethod, List<AndroidMethod> aMethods) {
		for (AndroidMethod am : aMethods) {
			if (!am.getClassName().equals(sMethod.getDeclaringClass().getName()))
				continue;
			if (!am.getMethodName().equals(sMethod.getName()))
				continue;
			if (am.getParameters().size() != sMethod.getParameterCount())
				continue;
			
			boolean matches = true;
			for (int i = 0; i < am.getParameters().size(); i++)
				if (!am.getParameters().get(i).equals(sMethod.getParameterType(i).toString())) {
					matches = false;
					break;
				}
			if (!matches)
				continue;
			
			if (!weakMatching)
				if (!am.getReturnType().isEmpty())
					if (!am.getReturnType().equals(sMethod.getReturnType().toString()))
						continue;
			
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isSourceMethod(SootMethod sMethod) {
		return this.matchesMethod(sMethod, this.sourceMethods);
	}

	@Override
	public boolean isSinkMethod(SootMethod sMethod) {
		return this.matchesMethod(sMethod, this.sinkMethods);
	}

	@Override
	public boolean isSource(Stmt sCallSite) {
		if (super.isSource(sCallSite))
			return true;
		
		if (this.layoutMatching != LayoutMatchingMode.NoMatch
				&& sCallSite.containsInvokeExpr()) {
			InvokeExpr ie = sCallSite.getInvokeExpr();
			if (ie.getMethod().getSignature().equals(Activity_FindViewById)
					|| ie.getMethod().getSignature().equals(View_FindViewById)) {
				// If we match all controls, we don't care about the specific
				// control we're dealing with
				if (this.layoutMatching == LayoutMatchingMode.MatchAll)
					return true;
				
				// If we match specific controls, we need to get the ID of
				// control and look up the respective data object
				if (ie.getArgCount() != 1) {
					System.err.println("Framework method call with unexpected "
							+ "number of arguments");
					return false;
				}
				int id = 0;
				if (ie.getArg(0) instanceof IntConstant)
					id = ((IntConstant) ie.getArg(0)).value;
				else {
					System.err.println("Framework method call with unexpected "
							+ "parameter type");
					return false;
				}
				
				LayoutControl control = this.layoutControls.get(id);
				if (control == null) {
					System.err.println("Layout control with ID " + id + " not found");
					return false;
				}
				if (this.layoutMatching == LayoutMatchingMode.MatchSensitiveOnly
						&& control.isSensitive())
 					return true;
			}
		}
		return false;
	}
	
	/**
	 * Adds a list of methods as sinks
	 * @param sinks The methods to be added as sinks
	 */
	public void addSink(List<AndroidMethod> sinks) {
		this.sinkMethods.addAll(sinks);
	}

}

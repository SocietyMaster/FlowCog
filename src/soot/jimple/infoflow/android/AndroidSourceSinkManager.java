package soot.jimple.infoflow.android;

import java.util.List;

import soot.SootMethod;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.source.MethodBasedSourceSinkManager;
import soot.jimple.infoflow.source.SourceSinkManager;

/**
 * SourceManager implementation for AndroidSources
 * @author Steven Arzt
 *
 */
public class AndroidSourceSinkManager extends MethodBasedSourceSinkManager {

	private final List<AndroidMethod> sourceMethods;
	private final List<AndroidMethod> sinkMethods;
	private final boolean weakMatching;
	
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
		this.sourceMethods = sources;
		this.sinkMethods = sinks;
		this.weakMatching = false;
	}
	
	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with
	 * either strong or weak matching.
	 * @param sources The list of source methods
	 * @param sinks The list of sink methods
	 * @param weakMatching True for strong matching: The method signature in the
	 * code exactly match the one in the list. False for weak matching: If an
	 * entry in the list has no return type, it matches arbitrary return types
	 * if the rest of the method signature is compatible.
	 */
	public AndroidSourceSinkManager
			(List<AndroidMethod> sources,
			List<AndroidMethod> sinks,
			boolean weakMatching) {
		this.sourceMethods = sources;
		this.sinkMethods = sinks;
		this.weakMatching = weakMatching;
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

	/**
	 * Adds a list of methods as sinks
	 * @param sinks The methods to be added as sinks
	 */
	public void addSink(List<AndroidMethod> sinks) {
		this.sinkMethods.addAll(sinks);
	}

}

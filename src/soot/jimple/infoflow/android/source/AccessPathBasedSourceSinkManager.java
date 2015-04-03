package soot.jimple.infoflow.android.source;

import heros.InterproceduralCFG;
import heros.solver.IDESolver;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.IAccessPathMethodParser;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.source.AccessPathBundle;
import soot.jimple.infoflow.source.SourceInfo;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.corba.se.spi.ior.Identifiable;

public class AccessPathBasedSourceSinkManager extends AndroidSourceSinkManager {

	private final IAccessPathMethodParser sourceSinkParser;
	private HashMap<String, AccessPathBundle> accessPathCache = new HashMap<String, AccessPathBundle>();

	private final Map<String, AndroidMethod> sourceMethods;
	private final Map<String, AndroidMethod> sinkMethods;
	private final Map<String, AndroidMethod> callbackMethods;

	private static final SourceInfo sourceInfo = new SourceInfo(true);

	protected final LoadingCache<SootClass, Collection<SootClass>> interfacesOf = IDESolver.DEFAULT_CACHE_BUILDER
			.build(new CacheLoader<SootClass, Collection<SootClass>>() {
		@Override
		public Collection<SootClass> load(SootClass sc) throws Exception {
			Set<SootClass> set = new HashSet<SootClass>(sc.getInterfaceCount());
			for (SootClass i : sc.getInterfaces()) {
				set.add(i);
				set.addAll(interfacesOf.getUnchecked(i));
			}
			if (sc.hasSuperclass())
				set.addAll(interfacesOf.getUnchecked(sc.getSuperclass()));
			return set;
		}
	});

	/**
	 * Creates a new instance of the {@link AndroidSourceSinkManager} class with either strong or weak matching.
	 * 
	 * @param sources
	 *            The list of source methods
	 * @param sinks
	 *            The list of sink methods
	 */
	public AccessPathBasedSourceSinkManager(Set<AndroidMethod> sources, Set<AndroidMethod> sinks) {
		this(sources, sinks, new HashSet<AndroidMethod>(), LayoutMatchingMode.NoMatch, null, null);
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
	public AccessPathBasedSourceSinkManager(Set<AndroidMethod> sources, Set<AndroidMethod> sinks,
			Set<AndroidMethod> callbackMethods, LayoutMatchingMode layoutMatching,
			Map<Integer, LayoutControl> layoutControls, IAccessPathMethodParser sourceSinkParser) {
		super(sources, sinks, callbackMethods, layoutMatching, layoutControls);
		this.sourceMethods = new HashMap<String, AndroidMethod>();
		for (AndroidMethod am : sources)
			this.sourceMethods.put(am.getSignature(), am);

		this.sinkMethods = new HashMap<String, AndroidMethod>();
		for (AndroidMethod am : sinks)
			this.sinkMethods.put(am.getSignature(), am);

		this.callbackMethods = new HashMap<String, AndroidMethod>();
		for (AndroidMethod am : callbackMethods)
			this.callbackMethods.put(am.getSignature(), am);

		this.sourceSinkParser = sourceSinkParser;

		System.out.println("Created a SourceSinkManager with " + this.sourceMethods.size() + " sources, "
				+ this.sinkMethods.size() + " sinks, and " + this.callbackMethods.size() + " callback methods.");
	}

	/**
	 * @author Daniel Magin, Wittmann Andreas
	 * @return a source info which contains information about access path which will be tainted by this source
	 */
	@SuppressWarnings("deprecation")
	@Override
	public SourceInfo getSourceInfo(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg) {
		String methodSignature;
		SourceType type = getSourceType(sCallSite, cfg);

		switch (type) {
		case Callback:
			Value leftOp = ((IdentityStmt) sCallSite).getLeftOp();
			// taint return value
			AccessPath[] returnAP = { new AccessPath(leftOp, true) };
			AccessPathBundle bundle = new AccessPathBundle(returnAP);
			return new SourceInfo(sourceInfo.getTaintSubFields(), sourceInfo.getUserData(), bundle);
		case UISource:
		case MethodCall:
			methodSignature = sCallSite.getInvokeExpr().getMethod().getSignature();

			AccessPathBundle contextIndependentSourceAccessPathBundle = getAccessPathBundle(methodSignature);

			if (contextIndependentSourceAccessPathBundle == null) {
				return new SourceInfo(sourceInfo.getTaintSubFields());
			}

			// map base object
			AccessPath[] actualBaseAPs = null;
			if (sCallSite.containsInvokeExpr() && !(sCallSite.getInvokeExpr() instanceof StaticInvokeExpr)) {

				Value baseVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
				actualBaseAPs = mapAPtoActualValue(baseVal, contextIndependentSourceAccessPathBundle.getSourceBaseAPs());
			}

			// map return object
			AccessPath[] actualReturnAPs = null;
			if (sCallSite instanceof AssignStmt) {
				Value returnVal = ((AssignStmt) sCallSite).getLeftOp();
				actualReturnAPs = mapAPtoActualValue(returnVal,
						contextIndependentSourceAccessPathBundle.getSourceReturnAPs());
			}

			// map parameter objects
			AccessPath[][] actualParamAPs = new AccessPath[contextIndependentSourceAccessPathBundle
					.getSourceParameterCount()][];
			for (int i = 0; i < contextIndependentSourceAccessPathBundle.getSourceParameterCount(); i++) {
				Value paramVal = sCallSite.getInvokeExpr().getArg(i);
				// We only consider non constant params
				if (!(paramVal instanceof Constant))
					actualParamAPs[i] = mapAPtoActualValue(paramVal,
							contextIndependentSourceAccessPathBundle.getSourceParameterAPs(i));
				else
					actualParamAPs[i] = null;

			}
			AccessPathBundle actualMethodAPs = new AccessPathBundle(actualBaseAPs, null, actualParamAPs, null,
					actualReturnAPs);

			AndroidMethod source = this.sourceMethods.get(methodSignature);
			return new SourceInfo(sourceInfo.getTaintSubFields(), source, actualMethodAPs);
		case NoSource:
		default:
			return null;
		}
	}

	protected AccessPath[] mapAPtoActualValue(Value target, AccessPath[] APs) {
		if (APs != null) {
			AccessPath[] actualAPs = new AccessPath[APs.length];
			for (int i = 0; i < actualAPs.length; i++) {
				actualAPs[i] = APs[i].clone().copyWithNewValue(target);
			}
			return actualAPs;
		} else {
			return null;
		}
	}

	/**
	 * Adds a list of methods as sinks
	 * 
	 * @param sinks
	 *            The methods to be added as sinks
	 */
	public void addSink(Set<AndroidMethod> sinks) {
		for (AndroidMethod am : sinks)
			this.sinkMethods.put(am.getSignature(), am);
	}

	/**
	 * @author Daniel Magin, Wittmann Andreas
	 */
	@Override
	public boolean leaks(Stmt sCallSite, InterproceduralCFG<Unit, SootMethod> cfg, int index,
			AccessPath sourceAccessPath) {
		String methodSignature = sCallSite.getInvokeExpr().getMethod().getSignature();

		AccessPathBundle contextIndependentMethodAccessPathsBundle = getAccessPathBundle(methodSignature);

		// index < 0 => base, else Params
		AccessPath[] leakAccessPaths = (index < 0 ? contextIndependentMethodAccessPathsBundle.getSinkBaseAPs()
				: contextIndependentMethodAccessPathsBundle.getSinkParamterAPs(index));

		if (leakAccessPaths == null) {
			// no leaking access paths for the base value or the given parameter
			// => no leak
			return false;
		}

		// map abstract access path values to the concrete runtime values:
		Value actualVal = null;
		if (index < 0 && sCallSite.getInvokeExpr() instanceof InstanceInvokeExpr) {
			actualVal = ((InstanceInvokeExpr) sCallSite.getInvokeExpr()).getBase();
		} else {
			actualVal = sCallSite.getInvokeExpr().getArg(index);
		}
		leakAccessPaths = mapAPtoActualValue(actualVal, leakAccessPaths);

		for (AccessPath ap : leakAccessPaths) {
			// check if one of the leaking access paths entails the given access
			// path or vise versa
			if (ap.getFieldCount() <= sourceAccessPath.getFieldCount()) {
				return ap.entails(sourceAccessPath);
			} else {
				return sourceAccessPath.entails(ap);
			}
		}
		return false;
	}

	/**
	 * @author Wittmann Andreas, Daniel Magin
	 * 
	 *         The method only asks the parser for the AccessPaths if the AccessPaths are not in the cache yet
	 * @param methodSignature
	 *            is the method Signature from which we want to get the AccessPaths
	 * @return the MethodAccessPathBundle
	 */
	protected AccessPathBundle getAccessPathBundle(String methodSignature) {
		if (!accessPathCache.containsKey(methodSignature)) {
			AccessPathBundle methodAccessPaths = sourceSinkParser.getAccessPaths(methodSignature);
			accessPathCache.put(methodSignature, methodAccessPaths);
			return methodAccessPaths;
		} else
			return accessPathCache.get(methodSignature);
	}
}
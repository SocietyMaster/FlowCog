package soot.jimple.infoflow.android.callbacks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Default implementation of the callback analyzer class. This implementation
 * aims for precision. It tries to rule out callbacks registered in unreachable
 * code. The mapping between components and callbacks is as precise as possible.
 * 
 * @author Steven Arzt
 *
 */
public class DefaultCallbackAnalyzer extends AbstractCallbackAnalyzer {
	
	private final Map<String, Set<SootMethodAndClass>> callbackWorklist =
			new HashMap<String, Set<SootMethodAndClass>>();
	
	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses) throws IOException {
		super(config, entryPointClasses);
	}
	
	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses,
			String callbackFile) throws IOException {
		super(config, entryPointClasses, callbackFile);
	}

	public DefaultCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses,
			Set<String> androidCallbacks) throws IOException {
		super(config, entryPointClasses, androidCallbacks);
	}
	
	/**
	 * Collects the callback methods for all Android default handlers
	 * implemented in the source code.
	 * Note that this operation runs inside Soot, so this method only registers
	 * a new phase that will be executed when Soot is next run
	 */
	public void collectCallbackMethods() {
		logger.info("Collecting callbacks in DEFAULT mode...");
		
		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Find the mappings between classes and layouts
				findClassLayoutMappings();

				// Process the callback classes directly reachable from the
				// entry points
				for (String className : entryPointClasses) {
					SootClass sc = Scene.v().getSootClass(className);
					List<MethodOrMethodContext> methods = new ArrayList<MethodOrMethodContext>();
					methods.addAll(sc.getMethods());
					
					// Check for callbacks registered in the code
					analyzeRechableMethods(sc, methods);

					// Check for method overrides
					analyzeMethodOverrideCallbacks(sc);
				}
				System.out.println("Callback analysis done.");
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}
	
	/**
	 * Incrementally collects the callback methods for all Android default
	 * handlers implemented in the source code. This just processes the contents
	 * of the worklist.
	 * Note that this operation runs inside Soot, so this method only registers
	 * a new phase that will be executed when Soot is next run
	 */
	public void collectCallbackMethodsIncremental() {
		Transform transform = new Transform("wjtp.ajc", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				// Process the worklist from last time
				System.out.println("Running incremental callback analysis for " + callbackWorklist.size()
						+ " components...");
				MultiMap<String, SootMethodAndClass> workListCopy =
						new HashMultiMap<String, SootMethodAndClass>(callbackWorklist);
				for (String className : workListCopy.keySet()) {
					List<MethodOrMethodContext> entryClasses = new LinkedList<MethodOrMethodContext>();
					for (SootMethodAndClass am : workListCopy.get(className))
						entryClasses.add(Scene.v().getMethod(am.getSignature()));
					analyzeRechableMethods(Scene.v().getSootClass(className), entryClasses);
					callbackWorklist.remove(className);
				}
				System.out.println("Incremental callback analysis done.");
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}

	private void analyzeRechableMethods(SootClass lifecycleElement, List<MethodOrMethodContext> methods) {
		ReachableMethods rm = new ReachableMethods(Scene.v().getCallGraph(), methods);
		rm.update();

		// Scan for listeners in the class hierarchy
		Iterator<MethodOrMethodContext> reachableMethods = rm.listener();
		while (reachableMethods.hasNext()) {
			SootMethod method = reachableMethods.next().method();
			analyzeMethodForCallbackRegistrations(lifecycleElement, method);
			analyzeMethodForDynamicBroadcastReceiver(method);
		}
	}
	
	@Override
	protected boolean checkAndAddMethod(SootMethod method, SootClass baseClass) {
		if (super.checkAndAddMethod(method, baseClass)) {
			AndroidMethod am = new AndroidMethod(method);
			if (this.callbackWorklist.containsKey(baseClass.getName()))
					this.callbackWorklist.get(baseClass.getName()).add(am);
			else {
				Set<SootMethodAndClass> methods = new HashSet<SootMethodAndClass>();
				methods.add(am);
				this.callbackWorklist.put(baseClass.getName(), methods);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
		Iterator<MethodOrMethodContext> rmIterator = Scene.v().getReachableMethods().listener();
		while (rmIterator.hasNext()) {
			SootMethod sm = rmIterator.next().method();
			if (!sm.isConcrete())
				continue;
			
			for (Unit u : sm.retrieveActiveBody().getUnits())
				if (u instanceof Stmt) {
					Stmt stmt = (Stmt) u;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr inv = stmt.getInvokeExpr();
						if (invokesSetContentView(inv)) {
							for (Value val : inv.getArgs())
								if (val instanceof IntConstant) {
									IntConstant constVal = (IntConstant) val;
									Set<Integer> layoutIDs = this.layoutClasses.get(sm.getDeclaringClass().getName());
									if (layoutIDs == null) {
										layoutIDs = new HashSet<Integer>();
										this.layoutClasses.put(sm.getDeclaringClass().getName(), layoutIDs);
									}
									layoutIDs.add(constVal.value);
								}
						}
					}
				}
		}
	}
	
}

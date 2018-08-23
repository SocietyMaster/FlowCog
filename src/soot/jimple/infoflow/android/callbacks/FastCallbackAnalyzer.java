package soot.jimple.infoflow.android.callbacks;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;

/**
 * A callback analyzer that favors performance over precision.
 * 
 * @author Steven Arzt
 *
 */
public class FastCallbackAnalyzer extends AbstractCallbackAnalyzer {

	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses) throws IOException {
		super(config, entryPointClasses);
	}
	
	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses,
			String callbackFile) throws IOException {
		super(config, entryPointClasses, callbackFile);
	}

	public FastCallbackAnalyzer(InfoflowAndroidConfiguration config,
			Set<String> entryPointClasses,
			Set<String> androidCallbacks) throws IOException {
		super(config, entryPointClasses, androidCallbacks);
	}

	@Override
	public void collectCallbackMethods() {
		logger.info("Collecting callbacks in FAST mode...");
		
		// Find the mappings between classes and layouts
		findClassLayoutMappings();
		
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (sm.isConcrete()) {
						analyzeMethodForCallbackRegistrations(null, sm);
						analyzeMethodForDynamicBroadcastReceiver(sm);
					}
				}
				
				// Check for method overrides
				analyzeMethodOverrideCallbacks(sc);
			}
		}
	}

	@Override
	public void collectCallbackMethodsIncremental() {
		// Since this analyzer is not incremental, this method does nothing
	}

	/**
	 * Finds the mappings between classes and their respective layout files
	 */
	private void findClassLayoutMappings() {
		for (SootClass sc : Scene.v().getApplicationClasses()) {
			if (sc.isConcrete()) {
				for (SootMethod sm : sc.getMethods()) {
					if (!sm.isConcrete())
						continue;
					
					for (Unit u : sm.retrieveActiveBody().getUnits()) {
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
		}
	}

}

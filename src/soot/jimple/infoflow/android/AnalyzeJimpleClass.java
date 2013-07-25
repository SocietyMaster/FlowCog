package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointConstants;
import soot.jimple.toolkits.callgraph.ReachableMethods;

/**
 * Analyzes the classes in the APK file to find custom implementations of the
 * well-known Android callback and handler interfaces.
 * 
 * @author Steven Arzt
 *
 */
public class AnalyzeJimpleClass {

	private final Set<String> entryPointClasses;
	private final Set<String> androidCallbacks;
	private final Map<String, Set<AndroidMethod>> callbackMethods = new HashMap<String, Set<AndroidMethod>>();
	private final Map<String, Set<AndroidMethod>> callbackWorklist = new HashMap<String, Set<AndroidMethod>>();
	private final Map<SootClass, Set<Integer>> layoutClasses = new HashMap<SootClass, Set<Integer>>();

	public AnalyzeJimpleClass(Set<String> entryPointClasses) throws IOException {
		this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = loadAndroidCallbacks();
	}

	public AnalyzeJimpleClass(Set<String> entryPointClasses,
			Set<String> androidCallbacks) {
		this.entryPointClasses = entryPointClasses;
		this.androidCallbacks = new HashSet<String>();
	}

	/**
	 * Loads the set of interfaces that are used to implement Android callback
	 * handlers from a file on disk
	 * @return A set containing the names of the interfaces that are used to
	 * implement Android callback handlers
	 */
	private Set<String> loadAndroidCallbacks() throws IOException {
		Set<String> androidCallbacks = new HashSet<String>();
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader("AndroidCallbacks.txt"));
			String line;
			while ((line = rdr.readLine()) != null)
				if (!line.isEmpty())
					androidCallbacks.add(line);
		}
		finally {
			if (rdr != null)
				rdr.close();
		}
		return androidCallbacks;
	}

	/**
	 * Collects the callback methods for all Android default handlers
	 * implemented in the source code.
	 * Note that this operation runs inside Soot, so this method only registers
	 * a new phase that will be executed when Soot is next run
	 */
	public void collectCallbackMethods() {
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
				Map<String, Set<AndroidMethod>> workListCopy = new HashMap<String, Set<AndroidMethod>>
					(callbackWorklist);
				for (Entry<String, Set<AndroidMethod>> entry : workListCopy.entrySet()) {
					List<MethodOrMethodContext> entryClasses = new ArrayList<MethodOrMethodContext>();
					for (AndroidMethod am : entry.getValue()) {
						entryClasses.add(Scene.v().getMethod(am.getSignature()));
						callbackWorklist.get(entry.getKey()).remove(entry.getValue());
						if (callbackWorklist.get(entry.getKey()).isEmpty())
							callbackWorklist.remove(entry.getKey());
					}
					analyzeRechableMethods(Scene.v().getSootClass(entry.getKey()), entryClasses);
				}
				System.out.println("Incremental callback analysis done.");
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}

	private void analyzeRechableMethods(SootClass sc, List<MethodOrMethodContext> methods) {
		ReachableMethods rm = new ReachableMethods(Scene.v().getCallGraph(), methods);
		rm.update();

		// Scan for listeners in the class hierarchy
		Iterator<MethodOrMethodContext> reachableMethods = rm.listener();
		while (reachableMethods.hasNext()) {
			SootMethod method = reachableMethods.next().method();
			analyzeMethodForCallbackRegistrations(sc, method);
		}
	}

	/**
	 * Analyzes the given method and looks for callback registrations
	 * @param lifecycleElement The lifecycle element (activity, etc.) with which
	 * to associate the found callbacks
	 * @param method The method in which to look for callbacks
	 */
	private void analyzeMethodForCallbackRegistrations(SootClass lifecycleElement, SootMethod method) {
		// Do not analyze system classes
		if (method.getDeclaringClass().getName().startsWith("android.")
				|| method.getDeclaringClass().getName().startsWith("java."))
			return;
		if (!method.isConcrete())
			return;
		
		for (Unit u : method.retrieveActiveBody().getUnits()) {
			Stmt stmt = (Stmt) u;
			// Callback registrations are always instance invoke expressions
			if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
				InstanceInvokeExpr iinv = (InstanceInvokeExpr) stmt.getInvokeExpr();
				if (isCallbackRegistrationMethod(iinv.getMethod()))
					for (Value param : iinv.getArgs())
						if (param.getType() instanceof RefType) {
							SootClass callbackClass = ((RefType) param.getType()).getSootClass();
							for (SootClass c : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(callbackClass))
								analyzeClass(c, lifecycleElement);
						}
			}
		}
	}

	/**
	 * Checks whether the given method registers a new callback with the system
	 * @param method The method which is called
	 * @return True if the given method registers a new callback, otherwise false
	 */
	private boolean isCallbackRegistrationMethod(SootMethod method) {
		// Only calls to system APIs can register callbacks
		if (method.getDeclaringClass().getName().startsWith("android."))
			return false;
		
		for (Type paramType : method.getParameterTypes())
			if (paramType instanceof RefType)
				if (isCallbackInterface(((RefType) paramType).getSootClass()))
					return true;
				
		return false;
	}

	/**
	 * Checks whether the given Soot class is one of the well-known callback
	 * interfaces
	 * @param sootClass The Soot class to check.
	 * @return True if the given Soot class is one of the well-known callback
	 * interfaces, otherwise false
	 */
	private boolean isCallbackInterface(SootClass sootClass) {
		if (!sootClass.isInterface())
			return false;
		return androidCallbacks.contains(sootClass.getName());
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
						if (inv.getMethod().getName().equals("setContentView")
								&& inv.getMethod().getDeclaringClass().getName().equals("android.app.Activity")) {
							for (Value val : inv.getArgs())
								if (val instanceof IntConstant) {
									IntConstant constVal = (IntConstant) val;
									if (this.layoutClasses.containsKey(sm.getDeclaringClass()))
										this.layoutClasses.get(sm.getDeclaringClass()).add(constVal.value);
									else {
										Set<Integer> layoutIDs = new HashSet<Integer>();
										layoutIDs.add(constVal.value);
										this.layoutClasses.put(sm.getDeclaringClass(), layoutIDs);
									}
								}
						}
					}
				}
		}
	}
	
	/**
	 * Analyzes the given class to find callback methods
	 * @param sootClass The class to analyze
	 * @param lifecycleElement The lifecycle element (activity, service, etc.)
	 * to which the callback methods belong
	 */
	private void analyzeClass(SootClass sootClass, SootClass lifecycleElement) {
		// Do not analyze system classes
		if (sootClass.getName().startsWith("android.")
				|| sootClass.getName().startsWith("java."))
			return;
		
		// Check for callback handlers implemented via interfaces
		analyzeClassInterfaceCallbacks(sootClass, sootClass, lifecycleElement);
	}
	
	/**
	 * Enumeration for the types of classes we can have
	 */
	private enum ClassType {
		Activity,
		Service,
		BroadcastReceiver,
		ContentProvider,
		Plain
	}

	private void analyzeMethodOverrideCallbacks(SootClass sootClass) {
		if (!sootClass.isConcrete())
			return;
		
		// There are also some classes that implement interesting callback methods.
		// We model this as follows: Whenever the user overwrites a method in an
		// Android OS class that is not a well-known lifecycle method, we treat
		// it as a potential callback.
		ClassType classType = ClassType.Plain;
		Set<String> systemMethods = new HashSet<String>(10000);
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSuperclassesOf(sootClass)) {
			if (parentClass.getName().equals(AndroidEntryPointConstants.ACTIVITYCLASS))
				classType = ClassType.Activity; 
			else if (parentClass.getName().equals(AndroidEntryPointConstants.SERVICECLASS))
				classType = ClassType.Service;
			else if (parentClass.getName().equals(AndroidEntryPointConstants.BROADCASTRECEIVERCLASS))
				classType = ClassType.BroadcastReceiver;
			else if (parentClass.getName().equals(AndroidEntryPointConstants.CONTENTPROVIDERCLASS))
				classType = ClassType.ContentProvider;
			
			if (parentClass.getName().startsWith("android."))
				for (SootMethod sm : parentClass.getMethods())
					if (!sm.isConstructor())
						systemMethods.add(sm.getSubSignature());
		}
		
		// Iterate over all user-implemented methods. If they are inherited
		// from a system class, they are callback candidates.
		for (SootClass parentClass : Scene.v().getActiveHierarchy().getSubclassesOfIncluding(sootClass)) {
			if (parentClass.getName().startsWith("android."))
				continue;
			for (SootMethod method : parentClass.getMethods()) {
				if (!systemMethods.contains(method.getSubSignature()))
					continue;
				
				// This is an overridden system method. Check that we don't have
				// one of the lifecycle methods as they are treated separately.
				if (classType == ClassType.Activity
							&& AndroidEntryPointConstants.getActivityLifecycleMethods().contains(method.getSubSignature()))
						continue;
				if (classType == ClassType.Service
						&& AndroidEntryPointConstants.getServiceLifecycleMethods().contains(method.getSubSignature()))
					continue;
				if (classType == ClassType.BroadcastReceiver
						&& AndroidEntryPointConstants.getBroadcastLifecycleMethods().contains(method.getSubSignature()))
					continue;
				if (classType == ClassType.ContentProvider
						&& AndroidEntryPointConstants.getContentproviderLifecycleMethods().contains(method.getSubSignature()))
					continue;
				
				// This is a real callback method
				checkAndAddMethod(method, sootClass);
			}
		}
	}

	private SootMethod getMethodFromHierarchy(SootClass c, String methodName) {
		if (c.declaresMethodByName(methodName))
			return c.getMethodByName(methodName);
		if (c.hasSuperclass())
			return getMethodFromHierarchy(c.getSuperclass(), methodName);
		throw new RuntimeException("Could not find method");
	}
	
	private SootMethod getMethodFromHierarchyEx(SootClass c, String methodSignature) {
		if (c.declaresMethod(methodSignature))
			return c.getMethod(methodSignature);
		if (c.hasSuperclass())
			return getMethodFromHierarchyEx(c.getSuperclass(), methodSignature);
		throw new RuntimeException("Could not find method");
	}

	private void analyzeClassInterfaceCallbacks(SootClass baseClass, SootClass sootClass,
			SootClass lifecycleElement) {
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (!baseClass.isConcrete())
			return;
		
		// For a first take, we consider all classes in the android.* packages
		// to be part of the operating system
		if (baseClass.getName().startsWith("android."))
			return;
		
		// If we are a class, one of our superclasses might implement an Android
		// interface
		if (sootClass.hasSuperclass())
			analyzeClassInterfaceCallbacks(baseClass, sootClass.getSuperclass(), lifecycleElement);
		
		// Do we implement one of the well-known interfaces?
		for (SootClass i : collectAllInterfaces(sootClass)) {
			// android.accounts
			if (i.getName().equals("android.accounts.OnAccountsUpdateListener")) {
				if (i.declaresMethodByName("onAccountsUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccountsUpdated"), lifecycleElement);
			}
			// android.animation
			else if (i.getName().equals("android.animation.Animator$AnimatorListener")) {
				if (i.declaresMethodByName("onAnimationCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationCancel"), lifecycleElement);
				if (i.declaresMethodByName("onAnimationEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd"), lifecycleElement);
				if (i.declaresMethodByName("onAnimationRepeat"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat"), lifecycleElement);
				if (i.declaresMethodByName("onAnimationStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationStart"), lifecycleElement);
			}
			else if (i.getName().equals("android.animation.LayoutTransition$TransitionListener")) {
				if (i.declaresMethodByName("endTransition"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "endTransition"), lifecycleElement);
				if (i.declaresMethodByName("startTransition"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "startTransition"), lifecycleElement);
			}
			else if (i.getName().equals("android.animation.TimeAnimator$TimeListener")) {
				if (i.declaresMethodByName("onTimeUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeUpdate"), lifecycleElement);
			}
			else if (i.getName().equals("android.animation.ValueAnimator$AnimatorUpdateListener")) {
				if (i.declaresMethodByName("onAnimationUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationUpdate"), lifecycleElement);
			}
			// android.app
			else if (i.getName().equals("android.app.ActionBar$OnMenuVisibilityListener")) {
				if (i.declaresMethodByName("onMenuVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuVisibilityChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.ActionBar$OnNavigationListener")) {
				if (i.declaresMethodByName("onNavigationItemSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNavigationItemSelected"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.ActionBar$TabListener")) {
				if (i.declaresMethodByName("onTabReselected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabReselected"), lifecycleElement);
				if (i.declaresMethodByName("onTabSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabSelected"), lifecycleElement);
				if (i.declaresMethodByName("onTabUnselected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabUnselected"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.Application$ActivityLifecycleCallbacks")) {
				if (i.declaresMethodByName("onActivityCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityCreated"), lifecycleElement);
				if (i.declaresMethodByName("onActivityDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityDestroyed"), lifecycleElement);
				if (i.declaresMethodByName("onActivityPaused"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityPaused"), lifecycleElement);
				if (i.declaresMethodByName("onActivityResumed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityResumed"), lifecycleElement);
				if (i.declaresMethodByName("onActivitySaveInstanceState"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivitySaveInstanceState"), lifecycleElement);
				if (i.declaresMethodByName("onActivityStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStarted"), lifecycleElement);
				if (i.declaresMethodByName("onActivityStopped"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStopped"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.DatePickerDialog$OnDateSetListener")) {
				if (i.declaresMethodByName("onDateSet"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDateSet"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.FragmentBreadCrumbs$OnBreadCrumbClickListener")) {
				if (i.declaresMethodByName("onBreadCrumbClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBreadCrumbClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.FragmentManager$OnBackStackChangedListener")) {
				if (i.declaresMethodByName("onBackStackChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBackStackChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.KeyguardManager$OnKeyguardExitResult")) {
				if (i.declaresMethodByName("onKeyguardExitResult"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyguardExitResult"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.LoaderManager$LoaderCallbacks")) {
				if (i.declaresMethodByName("onCreateLoader"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCreateLoader"), lifecycleElement);
				if (i.declaresMethodByName("onLoadFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadFinished"), lifecycleElement);
				if (i.declaresMethodByName("onLoaderReset"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoaderReset"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.PendingIntent$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSendFinished"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.SearchManager$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.SearchManager$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"), lifecycleElement);
			}
			else if (i.getName().equals("android.app.TimePickerDialog$OnTimeSetListener")) {
				if (i.declaresMethodByName("onTimeSet"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeSet"), lifecycleElement);
			}
			// android.bluetooth
			else if (i.getName().equals("android.bluetooth.BluetoothProfile$ServiceListener")) {
				if (i.declaresMethodByName("onServiceConnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceConnected"), lifecycleElement);
				if (i.declaresMethodByName("onServiceDisconnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceDisconnected"), lifecycleElement);
			}
			// android.content
			else if (i.getName().equals("android.content.ClipboardManager$OnPrimaryClipChangedListener")) {
				if (i.declaresMethodByName("onPrimaryClipChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrimaryClipChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.ComponentCallbacks")) {
				if (i.declaresMethodByName("onConfigurationChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onConfigurationChanged"), lifecycleElement);
				if (i.declaresMethodByName("onLowMemory"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLowMemory"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.ComponentCallbacks2")) {
				if (i.declaresMethodByName("onTrimMemory"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTrimMemory"), lifecycleElement);
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.DialogInterface$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onClick(android.content.DialogInterface,int)"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.DialogInterface$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.DialogInterface$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.DialogInterface$OnMultiChoiceClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onClick(android.content.DialogInterface,int,boolean)"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.DialogInterface$OnShowListener")) {
				if (i.declaresMethodByName("onShow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShow"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.IntentSender$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSendFinished"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCanceledListener")) {
				if (i.declaresMethodByName("onLoadCanceled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadCanceled"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadComplete"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.SharedPreferences$OnSharedPreferenceChangeListener")) {
				if (i.declaresMethodByName("onSharedPreferenceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSharedPreferenceChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.content.SyncStatusObserver")) {
				if (i.declaresMethodByName("onStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStatusChanged"), lifecycleElement);
			}
			// android.database.sqlite
			else if (i.getName().equals("android.database.sqlite.SQLiteTransactionListener")) {
				if (i.declaresMethodByName("onBegin"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBegin"), lifecycleElement);
				if (i.declaresMethodByName("onCommit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCommit"), lifecycleElement);
				if (i.declaresMethodByName("onRollback"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRollback"), lifecycleElement);
			}
			// android.drm
			else if (i.getName().equals("android.drm.DrmManagerClient$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"), lifecycleElement);
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnEventListener")) {
				if (i.declaresMethodByName("onEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEvent"), lifecycleElement);
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"), lifecycleElement);
			}
			// android.gesture			
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGestureListener")) {
				if (i.declaresMethodByName("onGesture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesture"), lifecycleElement);
				if (i.declaresMethodByName("onGestureCancelled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureCancelled"), lifecycleElement);
				if (i.declaresMethodByName("onGestureEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureEnded"), lifecycleElement);
				if (i.declaresMethodByName("onGestureStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureStarted"), lifecycleElement);
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturePerformedListener")) {
				if (i.declaresMethodByName("onGesturePerformed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturePerformed"), lifecycleElement);
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturingListener")) {
				if (i.declaresMethodByName("onGesturingEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturingEnded"), lifecycleElement);
				if (i.declaresMethodByName("onGesturingStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturingStarted"), lifecycleElement);
			}
			// android.graphics
			else if (i.getName().equals("android.graphics.SurfaceTexture$OnFrameAvailableListener")) {
				if (i.declaresMethodByName("onFrameAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFrameAvailable"), lifecycleElement);
			}
			// android.hardware
			else if (i.getName().equals("android.hardware.Camera$AutoFocusCallback")) {
				if (i.declaresMethodByName("onAutoFocus"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAutoFocus"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$AutoFocusMoveCallback")) {
				if (i.declaresMethodByName("onAutoFocusMoving"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAutoFocusMoving"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$ErrorCallback")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$FaceDetectionListener")) {
				if (i.declaresMethodByName("onFaceDetection"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFaceDetection"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$OnZoomChangeListener")) {
				if (i.declaresMethodByName("onZoomChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onZoomChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$PictureCallback")) {
				if (i.declaresMethodByName("onPictureTaken"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPictureTaken"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$PreviewCallback")) {
				if (i.declaresMethodByName("onPreviewFrame"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreviewFrame"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.Camera$ShutterCallback")) {
				if (i.declaresMethodByName("onShutter"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShutter"), lifecycleElement);
			}
			else if (i.getName().equals("android.hardware.SensorEventListener")) {
				if (i.declaresMethodByName("onAccuracyChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccuracyChanged"), lifecycleElement);
				if (i.declaresMethodByName("onSensorChanged"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onSensorChanged(android.hardware.SensorEvent)"), lifecycleElement);
			}
			// android.hardware.display
			else if (i.getName().equals("android.hardware.display.DisplayManager$DisplayListener")) {
				if (i.declaresMethodByName("onDisplayAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayAdded"), lifecycleElement);
				if (i.declaresMethodByName("onDisplayChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayChanged"), lifecycleElement);
				if (i.declaresMethodByName("onDisplayRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayRemoved"), lifecycleElement);
			}
			// android.hardware.input
			else if (i.getName().equals("android.hardware.input.InputManager$InputDeviceListener")) {
				if (i.declaresMethodByName("onInputDeviceAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceAdded"), lifecycleElement);
				if (i.declaresMethodByName("onInputDeviceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceChanged"), lifecycleElement);
				if (i.declaresMethodByName("onInputDeviceRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceRemoved"), lifecycleElement);
			}
			// android.inputmethodservice
			else if (i.getName().equals("android.inputmethodservice.KeyboardView$OnKeyboardActionListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"), lifecycleElement);
				if (i.declaresMethodByName("onPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPress"), lifecycleElement);
				if (i.declaresMethodByName("onRelease"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRelease"), lifecycleElement);
				if (i.declaresMethodByName("onText"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onText"), lifecycleElement);
				if (i.declaresMethodByName("swipeDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeDown"), lifecycleElement);
				if (i.declaresMethodByName("swipeLeft"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeLeft"), lifecycleElement);
				if (i.declaresMethodByName("swipeRight"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeRight"), lifecycleElement);
				if (i.declaresMethodByName("swipeUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeUp"), lifecycleElement);
			}
			// android.location
			else if (i.getName().equals("android.location.GpsStatus$Listener")) {
				if (i.declaresMethodByName("onGpsStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGpsStatusChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.location.GpsStatus$NmeaListener")) {
				if (i.declaresMethodByName("onNmeaReceived"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNmeaReceived"), lifecycleElement);
			}
			else if (i.getName().equals("android.location.LocationListener")) {
				if (i.declaresMethodByName("onLocationChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLocationChanged"), lifecycleElement);
				if (i.declaresMethodByName("onProviderDisabled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProviderDisabled"), lifecycleElement);
				if (i.declaresMethodByName("onProviderEnabled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProviderEnabled"), lifecycleElement);
				if (i.declaresMethodByName("onStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStatusChanged"), lifecycleElement);
			}
			// android.media
			else if (i.getName().equals("android.media.AudioManager$OnAudioFocusChangeListener")) {
				if (i.declaresMethodByName("onAudioFocusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAudioFocusChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.AudioRecord$OnRecordPositionUpdateListener")
					|| i.getName().equals("android.media.AudioRecord$OnPlaybackPositionUpdateListener")) {
				if (i.declaresMethodByName("onMarkerReached"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMarkerReached"), lifecycleElement);
				if (i.declaresMethodByName("onPeriodicNotification"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPeriodicNotification"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.JetPlayer$OnJetEventListener")) {
				if (i.declaresMethodByName("onJetEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetEvent"), lifecycleElement);
				if (i.declaresMethodByName("onJetNumQueuedSegmentUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetNumQueuedSegmentUpdate"), lifecycleElement);
				if (i.declaresMethodByName("onJetPauseUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetPauseUpdate"), lifecycleElement);
				if (i.declaresMethodByName("onJetUserIdUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetUserIdUpdate"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnBufferingUpdateListener")) {
				if (i.declaresMethodByName("onBufferingUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBufferingUpdate"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnCompletionListener")) {
				if (i.declaresMethodByName("onCompletion"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCompletion"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnPreparedListener")) {
				if (i.declaresMethodByName("onPrepared"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrepared"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnSeekCompleteListener")) {
				if (i.declaresMethodByName("onSeekComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSeekComplete"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnTimedTextListener")) {
				if (i.declaresMethodByName("onTimedText"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimedText"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnVideoSizeChangedListener")) {
				if (i.declaresMethodByName("onVideoSizeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onVideoSizeChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$MediaScannerConnectionClient")) {
				if (i.declaresMethodByName("onMediaScannerConnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMediaScannerConnected"), lifecycleElement);
				if (i.declaresMethodByName("onScanCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScanCompleted"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$OnScanCompletedListener")) {
				if (i.declaresMethodByName("onScanCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScanCompleted"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.SoundPool$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadComplete"), lifecycleElement);
			}
			// android.media.audiofx
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnControlStatusChangeListener")) {
				if (i.declaresMethodByName("onControlStatusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onControlStatusChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnEnableStatusChangeListener")) {
				if (i.declaresMethodByName("onEnableStatusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEnableStatusChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.BassBoost$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.EnvironmentalReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.Equalizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.PresetReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.Virtualizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.media.audiofx.Visualizer$OnDataCaptureListener")) {
				if (i.declaresMethodByName("onFftDataCapture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFftDataCapture"), lifecycleElement);
				if (i.declaresMethodByName("onWaveFormDataCapture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onWaveFormDataCapture"), lifecycleElement);
			}
			// android.media.effect
			else if (i.getName().equals("android.media.effect$EffectUpdateListener")) {
				if (i.declaresMethodByName("onEffectUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEffectUpdated"), lifecycleElement);
			}
			// android.net.nsd
			else if (i.getName().equals("android.net.nsd.NsdManager$DiscoveryListener")) {
				if (i.declaresMethodByName("onDiscoveryStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStarted"), lifecycleElement);
				if (i.declaresMethodByName("onDiscoveryStopped"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStopped"), lifecycleElement);
				if (i.declaresMethodByName("onServiceFound"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceFound"), lifecycleElement);
				if (i.declaresMethodByName("onServiceLost"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceLost"), lifecycleElement);
				if (i.declaresMethodByName("onStartDiscoveryFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStartDiscoveryFailed"), lifecycleElement);
				if (i.declaresMethodByName("onStopDiscoveryFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStopDiscoveryFailed"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$RegistrationListener")) {
				if (i.declaresMethodByName("onRegistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed"), lifecycleElement);
				if (i.declaresMethodByName("onServiceRegistered"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceRegistered"), lifecycleElement);
				if (i.declaresMethodByName("onServiceUnregistered"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceUnregistered"), lifecycleElement);
				if (i.declaresMethodByName("onUnregistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUnregistrationFailed"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$ResolveListener")) {
				if (i.declaresMethodByName("onResolveFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onResolveFailed"), lifecycleElement);
				if (i.declaresMethodByName("onServiceResolved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceResolved"), lifecycleElement);
			}
			// android.net.sip
			else if (i.getName().equals("android.net.sip.SipRegistrationListener")) {
				if (i.declaresMethodByName("onRegistering"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistering"), lifecycleElement);
				if (i.declaresMethodByName("onRegistrationDone"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationDone"), lifecycleElement);
				if (i.declaresMethodByName("onRegistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed"), lifecycleElement);
			}
			// android.net.wifi.p2p
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ActionListener")) {
				if (i.declaresMethodByName("onFailure"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFailure"), lifecycleElement);
				if (i.declaresMethodByName("onSuccess"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuccess"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ChannelListener")) {
				if (i.declaresMethodByName("onChannelDisconnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChannelDisconnected"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ConnectionInfoListener")) {
				if (i.declaresMethodByName("onConnectionInfoAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onConnectionInfoAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdServiceResponseListener")) {
				if (i.declaresMethodByName("onDnsSdServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDnsSdServiceAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdTxtRecordListener")) {
				if (i.declaresMethodByName("onDnsSdTxtRecordAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDnsSdTxtRecordAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$GroupInfoListener")) {
				if (i.declaresMethodByName("onGroupInfoAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupInfoAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$PeerListListener")) {
				if (i.declaresMethodByName("onPeersAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPeersAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ServiceResponseListener")) {
				if (i.declaresMethodByName("onServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceAvailable"), lifecycleElement);
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$UpnpServiceResponseListener")) {
				if (i.declaresMethodByName("onUpnpServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUpnpServiceAvailable"), lifecycleElement);
			}
			// android.os
			else if (i.getName().equals("android.os.CancellationSignal$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"), lifecycleElement);
			}
			else if (i.getName().equals("android.os.IBinder$DeathRecipient")) {
				if (i.declaresMethodByName("binderDied"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "binderDied"), lifecycleElement);
			}
			else if (i.getName().equals("android.os.MessageQueue$IdleHandler")) {
				if (i.declaresMethodByName("queueIdle"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "queueIdle"), lifecycleElement);
			}
			else if (i.getName().equals("android.os.RecoverySystem$ProgressListener")) {
				if (i.declaresMethodByName("onProgress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProgress"), lifecycleElement);
			}
			// android.preference
			else if (i.getName().equals("android.preference.Preference$OnPreferenceChangeListener")) {
				if (i.declaresMethodByName("onPreferenceChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.preference.Preference$OnPreferenceClickListener")) {
				if (i.declaresMethodByName("onPreferenceClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.preference.PreferenceFragment$OnPreferenceStartFragmentCallback")) {
				if (i.declaresMethodByName("onPreferenceStartFragment"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceStartFragment"), lifecycleElement);
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityDestroyListener")) {
				if (i.declaresMethodByName("onActivityDestroy"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityDestroy"), lifecycleElement);
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityResultListener")) {
				if (i.declaresMethodByName("onActivityResult"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityResult"), lifecycleElement);
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityStopListener")) {
				if (i.declaresMethodByName("onActivityStop"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStop"), lifecycleElement);
			}
			// android.security
			else if (i.getName().equals("android.security.KeyChainAliasCallback")) {
				if (i.declaresMethodByName("alias"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "alias"), lifecycleElement);
			}
			// android.speech
			else if (i.getName().equals("android.speech.RecognitionListener")) {
				if (i.declaresMethodByName("onBeginningOfSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBeginningOfSpeech"), lifecycleElement);
				if (i.declaresMethodByName("onBufferReceived"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBufferReceived"), lifecycleElement);
				if (i.declaresMethodByName("onEndOfSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEndOfSpeech"), lifecycleElement);
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"), lifecycleElement);
				if (i.declaresMethodByName("onEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEvent"), lifecycleElement);
				if (i.declaresMethodByName("onPartialResults"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPartialResults"), lifecycleElement);
				if (i.declaresMethodByName("onReadyForSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onReadyForSpeech"), lifecycleElement);
				if (i.declaresMethodByName("onResults"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onResults"), lifecycleElement);
				if (i.declaresMethodByName("onRmsChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRmsChanged"), lifecycleElement);
			}
			// android.speech.tts
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnInitListener")) {
				if (i.declaresMethodByName("onInit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInit"), lifecycleElement);
			}			
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnUtteranceCompletedListener")) {
				if (i.declaresMethodByName("onUtteranceCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUtteranceCompleted"), lifecycleElement);
			}			
			// android.support - omitted
			// android.view
			else if (i.getName().equals("android.view.ActionMode$Callback")) {
				if (i.declaresMethodByName("onActionItemClicked"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActionItemClicked"), lifecycleElement);
				if (i.declaresMethodByName("onCreateActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCreateActionMode"), lifecycleElement);
				if (i.declaresMethodByName("onDestroyActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDestroyActionMode"), lifecycleElement);
				if (i.declaresMethodByName("onPrepareActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrepareActionMode"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ActionProvider$VisibilityListener")) {
				if (i.declaresMethodByName("onActionProviderVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActionProviderVisibilityChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.GestureDetector$OnDoubleTapListener")) {
				if (i.declaresMethodByName("onDoubleTap"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDoubleTap"), lifecycleElement);
				if (i.declaresMethodByName("onDoubleTapEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDoubleTapEvent"), lifecycleElement);
				if (i.declaresMethodByName("onSingleTapConfirmed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSingleTapConfirmed"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.GestureDetector$OnGestureListener")) {
				if (i.declaresMethodByName("onDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDown"), lifecycleElement);
				if (i.declaresMethodByName("onFling"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFling"), lifecycleElement);
				if (i.declaresMethodByName("onLongPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLongPress"), lifecycleElement);
				if (i.declaresMethodByName("onScroll"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScroll"), lifecycleElement);
				if (i.declaresMethodByName("onShowPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShowPress"), lifecycleElement);
				if (i.declaresMethodByName("onSingleTapUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSingleTapUp"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.InputQueue$Callback")) {
				if (i.declaresMethodByName("onInputQueueCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputQueueCreated"), lifecycleElement);
				if (i.declaresMethodByName("onInputQueueDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputQueueDestroyed"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.KeyEvent$Callback")) {
				if (i.declaresMethodByName("onKeyDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyDown"), lifecycleElement);
				if (i.declaresMethodByName("onKeyLongPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyLongPress"), lifecycleElement);
				if (i.declaresMethodByName("onKeyMultiple"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyMultiple"), lifecycleElement);
				if (i.declaresMethodByName("onKeyUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyUp"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.MenuItem$OnActionExpandListener")) {
				if (i.declaresMethodByName("onMenuItemActionCollapse"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionCollapse"), lifecycleElement);
				if (i.declaresMethodByName("onMenuItemActionExpand"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionExpand"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.MenuItem$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ScaleGestureDetector$OnScaleGestureListener")) {
				if (i.declaresMethodByName("onScale"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScale"), lifecycleElement);
				if (i.declaresMethodByName("onScaleBegin"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScaleBegin"), lifecycleElement);
				if (i.declaresMethodByName("onScaleEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScaleEnd"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback")) {
				if (i.declaresMethodByName("surfaceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceChanged"), lifecycleElement);
				if (i.declaresMethodByName("surfaceCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceCreated"), lifecycleElement);
				if (i.declaresMethodByName("surfaceDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceDestroyed"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback2")) {
				if (i.declaresMethodByName("surfaceRedrawNeeded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceRedrawNeeded"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.TextureView$SurfaceTextureListener")) {
				if (i.declaresMethodByName("onSurfaceTextureAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureAvailable"), lifecycleElement);
				if (i.declaresMethodByName("onSurfaceTextureDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureDestroyed"), lifecycleElement);
				if (i.declaresMethodByName("onSurfaceTextureSizeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureSizeChanged"), lifecycleElement);
				if (i.declaresMethodByName("onSurfaceTextureUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureUpdated"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnAttachStateChangeListener")) {
				if (i.declaresMethodByName("onViewAttachedToWindow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onViewAttachedToWindow"), lifecycleElement);
				if (i.declaresMethodByName("onViewDetachedFromWindow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onViewDetachedFromWindow"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onClick(android.view.View)"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnCreateContextMenuListener")) {
				if (i.declaresMethodByName("onCreateContextMenu"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onCreateContextMenu"
							+"(android.view.ContextMenu,android.view.View,android.view.ContextMenu$ContextMenuInfo)"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnDragListener")) {
				if (i.declaresMethodByName("onDrag"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDrag"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnFocusChangeListener")) {
				if (i.declaresMethodByName("onFocusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFocusChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnGenericMotionListener")) {
				if (i.declaresMethodByName("onGenericMotion"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGenericMotion"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnHoverListener")) {
				if (i.declaresMethodByName("onHover"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onHover"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnLayoutChangeListener")) {
				if (i.declaresMethodByName("onLayoutChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLayoutChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnLongClickListener")) {
				if (i.declaresMethodByName("onLongClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLongClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnSystemUiVisibilityChangeListener")) {
				if (i.declaresMethodByName("onSystemUiVisibilityChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSystemUiVisibilityChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.View$OnTouchListener")) {
				if (i.declaresMethodByName("onTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTouch"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewGroup$OnHierarchyChangeListener")) {
				if (i.declaresMethodByName("onChildViewAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildViewAdded"), lifecycleElement);
				if (i.declaresMethodByName("onChildViewRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildViewRemoved"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewStub$OnInflateListener")) {
				if (i.declaresMethodByName("onInflate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInflate"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnDrawListener")) {
				if (i.declaresMethodByName("onDraw"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDraw"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalFocusChangeListener")) {
				if (i.declaresMethodByName("onGlobalFocusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGlobalFocusChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalLayoutListener")) {
				if (i.declaresMethodByName("onGlobalLayout"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGlobalLayout"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnPreDrawListener")) {
				if (i.declaresMethodByName("onPreDraw"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreDraw"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnScrollChangedListener")) {
				if (i.declaresMethodByName("onScrollChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnTouchModeChangeListener")) {
				if (i.declaresMethodByName("onTouchModeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTouchModeChanged"), lifecycleElement);
			}
			// android.view.accessibility
			else if (i.getName().equals("android.view.accessibility.AccessibilityManager$AccessibilityStateChangeListener")) {
				if (i.declaresMethodByName("onAccessibilityStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccessibilityStateChanged"), lifecycleElement);
			}
			// android.view.animation
			else if (i.getName().equals("android.view.animation.Animation$AnimationListener")) {
				if (i.declaresMethodByName("onAnimationEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd"), lifecycleElement);
				if (i.declaresMethodByName("onAnimationRepeat"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat"), lifecycleElement);
				if (i.declaresMethodByName("onAnimationStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationStart"), lifecycleElement);
			}
			// android.view.inputmethod
			else if (i.getName().equals("android.view.inputmethod.InputMethod$SessionCallback")) {
				if (i.declaresMethodByName("sessionCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "sessionCreated"), lifecycleElement);
			}
			else if (i.getName().equals("android.view.inputmethod.InputMethodSession$EventCallback")) {
				if (i.declaresMethodByName("finishedEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "finishedEvent"), lifecycleElement);
			}
			// android.view.textservice
			else if (i.getName().equals("android.view.textservice.SpellCheckerSession$SpellCheckerSessionListener")) {
				if (i.declaresMethodByName("onGetSentenceSuggestions"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGetSentenceSuggestions"), lifecycleElement);
				if (i.declaresMethodByName("onGetSuggestions"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGetSuggestions"), lifecycleElement);
			}
			// android.webkit
			else if (i.getName().equals("android.webkit.DownloadListener")) {
				if (i.declaresMethodByName("onDownloadStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDownloadStart"), lifecycleElement);
			}
			// android.widget
			else if (i.getName().equals("android.widget.AbsListView$MultiChoiceModeListener")) {
				if (i.declaresMethodByName("onItemCheckedStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemCheckedStateChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AbsListView$OnScrollListener")) {
				if (i.declaresMethodByName("onScroll"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScroll"), lifecycleElement);
				if (i.declaresMethodByName("onScrollStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStateChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AbsListView$RecyclerListener")) {
				if (i.declaresMethodByName("onMovedToScrapHeap"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMovedToScrapHeap"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemClickListener")) {
				if (i.declaresMethodByName("onItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemLongClickListener")) {
				if (i.declaresMethodByName("onItemLongClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemLongClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AdapterView.OnItemSelectedListener")) {
				if (i.declaresMethodByName("onItemSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemSelected"), lifecycleElement);
				if (i.declaresMethodByName("onNothingSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNothingSelected"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.AutoCompleteTextView$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.CalendarView$OnDateChangeListener")) {
				if (i.declaresMethodByName("onSelectedDayChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSelectedDayChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.Chronometer$OnChronometerTickListener")) {
				if (i.declaresMethodByName("onChronometerTick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChronometerTick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.CompoundButton$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onCheckedChanged(android.widget.CompoundButton,boolean)"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.DatePicker$OnDateChangedListener")) {
				if (i.declaresMethodByName("onDateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDateChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnChildClickListener")) {
				if (i.declaresMethodByName("onChildClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupClickListener")) {
				if (i.declaresMethodByName("onGroupClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupCollapseListener")) {
				if (i.declaresMethodByName("onGroupCollapse"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupCollapse"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupExpandListener")) {
				if (i.declaresMethodByName("onGroupExpand"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupExpand"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.Filter$FilterListener")) {
				if (i.declaresMethodByName("onFilterComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFilterComplete"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnScrollListener")) {
				if (i.declaresMethodByName("onScrollStateChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStateChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnValueChangeListener")) {
				if (i.declaresMethodByName("onValueChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onValueChange"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.PopupMenu$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.PopupWindow$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.RadioGroup$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onCheckedChanged(android.widget.RadioGroup,int)"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.RatingBar$OnRatingBarChangeListener")) {
				if (i.declaresMethodByName("onRatingChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRatingChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SearchView$OnCloseListener")) {
				if (i.declaresMethodByName("onClose"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onClose"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SearchView$OnQueryTextListener")) {
				if (i.declaresMethodByName("onQueryTextChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onQueryTextChange"), lifecycleElement);
				if (i.declaresMethodByName("onQueryTextSubmit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onQueryTextSubmit"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SearchView$OnSuggestionListener")) {
				if (i.declaresMethodByName("onSuggestionClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuggestionClick"), lifecycleElement);
				if (i.declaresMethodByName("onSuggestionSelect"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuggestionSelect"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SeekBar$OnSeekBarChangeListener")) {
				if (i.declaresMethodByName("onProgressChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProgressChanged"), lifecycleElement);
				if (i.declaresMethodByName("onStartTrackingTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStartTrackingTouch"), lifecycleElement);
				if (i.declaresMethodByName("onStopTrackingTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStopTrackingTouch"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ShareActionProvider$OnShareTargetSelectedListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerCloseListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerOpenListener")) {
				if (i.declaresMethodByName("onDrawerOpened"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDrawerOpened"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerScrollListener")) {
				if (i.declaresMethodByName("onScrollEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollEnded"), lifecycleElement);
				if (i.declaresMethodByName("onScrollStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStarted"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.TabHost$OnTabChangeListener")) {
				if (i.declaresMethodByName("onTabChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.TextView$OnEditorActionListener")) {
				if (i.declaresMethodByName("onEditorAction"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEditorAction"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.TimePicker$OnTimeChangedListener")) {
				if (i.declaresMethodByName("onTimeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeChanged"), lifecycleElement);
			}
			else if (i.getName().equals("android.widget.ZoomButtonsController$OnZoomListener")) {
				if (i.declaresMethodByName("onVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onVisibilityChanged"), lifecycleElement);
				if (i.declaresMethodByName("onZoom"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onZoom"), lifecycleElement);
			}
		}
	}

	/**
	 * Checks whether the given Soot method comes from a system class. If not,
	 * it is added to the list of callback methods.
	 * @param method The method to check and add
	 * @param baseClass The base class (activity, service, etc.) to which this
	 * callback method belongs
	 */
	private void checkAndAddMethod(SootMethod method, SootClass baseClass) {
		AndroidMethod am = new AndroidMethod(method);
		if (!am.getClassName().startsWith("android.")
				&& !am.getClassName().startsWith("java.")) {
			boolean isNew;
			if (this.callbackMethods.containsKey(baseClass.getName()))
				isNew = this.callbackMethods.get(baseClass.getName()).add(am);
			else {
				Set<AndroidMethod> methods = new HashSet<AndroidMethod>();
				isNew = methods.add(am);
				this.callbackMethods.put(baseClass.getName(), methods);
			}
			
			if (isNew)
				if (this.callbackWorklist.containsKey(baseClass.getName()))
						this.callbackWorklist.get(baseClass.getName()).add(am);
				else {
					Set<AndroidMethod> methods = new HashSet<AndroidMethod>();
					isNew = methods.add(am);
					this.callbackWorklist.put(baseClass.getName(), methods);
				}
		}
	}

	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}
	
	public Map<String, Set<AndroidMethod>> getCallbackMethods() {
		return this.callbackMethods;
	}
	
	public Map<SootClass, Set<Integer>> getLayoutClasses() {
		return this.layoutClasses;
	}
		
}

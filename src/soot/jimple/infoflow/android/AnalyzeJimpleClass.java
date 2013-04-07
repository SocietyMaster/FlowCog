package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import soot.jimple.infoflow.android.data.AndroidMethod;

/**
 * Analyzes the classes in the APK file to find custom implementations of the
 * well-known Android callback and handler interfaces.
 * 
 * @author Steven Arzt
 *
 */
public class AnalyzeJimpleClass {

	private final List<AndroidMethod> callbackMethods = new ArrayList<AndroidMethod>();
	private final Map<SootClass, Set<Integer>> layoutClasses = new HashMap<SootClass, Set<Integer>>();

	public AnalyzeJimpleClass() {
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
				
				// Scan for listeners in the class hierarchy
				for (SootClass sc : Scene.v().getClasses())
					analyzeClass(sc);
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
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

	private void analyzeClass(SootClass sootClass) {
		analyzeClass(sootClass, sootClass);
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

	private void analyzeClass(SootClass baseClass, SootClass sootClass) {
		// There's no information we could use in an interface
		if (sootClass.isInterface())
			return;
		
		// We cannot create instances of abstract classes anyway, so there is no
		// reason to look for interface implementations
		if (baseClass.isAbstract())
			return;
		
		// For a first take, we consider all classes in the android.* packages
		// to be part of the operating system
		if (baseClass.getName().startsWith("android."))
			return;
		
		// If we are a class, one of our superclasses might implement an Android
		// interface
		if (sootClass.hasSuperclass())
			analyzeClass(baseClass, sootClass.getSuperclass());
		
		// Do we implement one of the well-known interfaces?
		for (SootClass i : collectAllInterfaces(sootClass)) {
			// android.accounts
			if (i.getName().equals("android.accounts.OnAccountsUpdateListener")) {
				if (i.declaresMethodByName("onAccountsUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccountsUpdated"));
			}
			// android.animation
			else if (i.getName().equals("android.animation.Animator$AnimatorListener")) {
				if (i.declaresMethodByName("onAnimationCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationCancel"));
				if (i.declaresMethodByName("onAnimationEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd"));
				if (i.declaresMethodByName("onAnimationRepeat"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat"));
				if (i.declaresMethodByName("onAnimationStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationStart"));
			}
			else if (i.getName().equals("android.animation.LayoutTransition$TransitionListener")) {
				if (i.declaresMethodByName("endTransition"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "endTransition"));
				if (i.declaresMethodByName("startTransition"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "startTransition"));
			}
			else if (i.getName().equals("android.animation.TimeAnimator$TimeListener")) {
				if (i.declaresMethodByName("onTimeUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeUpdate"));
			}
			else if (i.getName().equals("android.animation.ValueAnimator$AnimatorUpdateListener")) {
				if (i.declaresMethodByName("onAnimationUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationUpdate"));
			}
			// android.app
			else if (i.getName().equals("android.app.ActionBar$OnMenuVisibilityListener")) {
				if (i.declaresMethodByName("onMenuVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuVisibilityChanged"));
			}
			else if (i.getName().equals("android.app.ActionBar$OnNavigationListener")) {
				if (i.declaresMethodByName("onNavigationItemSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNavigationItemSelected"));
			}
			else if (i.getName().equals("android.app.ActionBar$TabListener")) {
				if (i.declaresMethodByName("onTabReselected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabReselected"));
				if (i.declaresMethodByName("onTabSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabSelected"));
				if (i.declaresMethodByName("onTabUnselected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabUnselected"));
			}
			else if (i.getName().equals("android.app.Application$ActivityLifecycleCallbacks")) {
				if (i.declaresMethodByName("onActivityCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityCreated"));
				if (i.declaresMethodByName("onActivityDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityDestroyed"));
				if (i.declaresMethodByName("onActivityPaused"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityPaused"));
				if (i.declaresMethodByName("onActivityResumed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityResumed"));
				if (i.declaresMethodByName("onActivitySaveInstanceState"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivitySaveInstanceState"));
				if (i.declaresMethodByName("onActivityStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStarted"));
				if (i.declaresMethodByName("onActivityStopped"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStopped"));
			}
			else if (i.getName().equals("android.app.DatePickerDialog$OnDateSetListener")) {
				if (i.declaresMethodByName("onDateSet"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDateSet"));
			}
			else if (i.getName().equals("android.app.FragmentBreadCrumbs$OnBreadCrumbClickListener")) {
				if (i.declaresMethodByName("onBreadCrumbClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBreadCrumbClick"));
			}
			else if (i.getName().equals("android.app.FragmentManager$OnBackStackChangedListener")) {
				if (i.declaresMethodByName("onBackStackChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBackStackChanged"));
			}
			else if (i.getName().equals("android.app.KeyguardManager$OnKeyguardExitResult")) {
				if (i.declaresMethodByName("onKeyguardExitResult"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyguardExitResult"));
			}
			else if (i.getName().equals("android.app.LoaderManager$LoaderCallbacks")) {
				if (i.declaresMethodByName("onCreateLoader"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCreateLoader"));
				if (i.declaresMethodByName("onLoadFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadFinished"));
				if (i.declaresMethodByName("onLoaderReset"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoaderReset"));
			}
			else if (i.getName().equals("android.app.PendingIntent$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSendFinished"));
			}
			else if (i.getName().equals("android.app.SearchManager$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"));
			}
			else if (i.getName().equals("android.app.SearchManager$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"));
			}
			else if (i.getName().equals("android.app.TimePickerDialog$OnTimeSetListener")) {
				if (i.declaresMethodByName("onTimeSet"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeSet"));
			}
			// android.bluetooth
			else if (i.getName().equals("android.bluetooth.BluetoothProfile$ServiceListener")) {
				if (i.declaresMethodByName("onServiceConnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceConnected"));
				if (i.declaresMethodByName("onServiceDisconnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceDisconnected"));
			}
			// android.content
			else if (i.getName().equals("android.content.ClipboardManager$OnPrimaryClipChangedListener")) {
				if (i.declaresMethodByName("onPrimaryClipChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrimaryClipChanged"));
			}
			else if (i.getName().equals("android.content.ComponentCallbacks")) {
				if (i.declaresMethodByName("onConfigurationChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onConfigurationChanged"));
				if (i.declaresMethodByName("onLowMemory"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLowMemory"));
			}
			else if (i.getName().equals("android.content.ComponentCallbacks2")) {
				if (i.declaresMethodByName("onTrimMemory"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTrimMemory"));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"));
			}
			else if (i.getName().equals("android.content.DialogInterface$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onClick(android.content.DialogInterface,int)"));
			}
			else if (i.getName().equals("android.content.DialogInterface$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"));
			}
			else if (i.getName().equals("android.content.DialogInterface$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"));
			}
			else if (i.getName().equals("android.content.DialogInterface$OnMultiChoiceClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onClick"));
			}
			else if (i.getName().equals("android.content.DialogInterface$OnShowListener")) {
				if (i.declaresMethodByName("onShow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShow"));
			}
			else if (i.getName().equals("android.content.IntentSender$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSendFinished"));
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCanceledListener")) {
				if (i.declaresMethodByName("onLoadCanceled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadCanceled"));
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadComplete"));
			}
			else if (i.getName().equals("android.content.SharedPreferences$OnSharedPreferenceChangeListener")) {
				if (i.declaresMethodByName("onSharedPreferenceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSharedPreferenceChanged"));
			}
			else if (i.getName().equals("android.content.SyncStatusObserver")) {
				if (i.declaresMethodByName("onStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStatusChanged"));
			}
			// android.database.sqlite
			else if (i.getName().equals("android.database.sqlite.SQLiteTransactionListener")) {
				if (i.declaresMethodByName("onBegin"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBegin"));
				if (i.declaresMethodByName("onCommit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCommit"));
				if (i.declaresMethodByName("onRollback"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRollback"));
			}
			// android.drm
			else if (i.getName().equals("android.drm.DrmManagerClient$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"));
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnEventListener")) {
				if (i.declaresMethodByName("onEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEvent"));
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"));
			}
			// android.gesture			
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGestureListener")) {
				if (i.declaresMethodByName("onGesture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesture"));
				if (i.declaresMethodByName("onGestureCancelled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureCancelled"));
				if (i.declaresMethodByName("onGestureEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureEnded"));
				if (i.declaresMethodByName("onGestureStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGestureStarted"));
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturePerformedListener")) {
				if (i.declaresMethodByName("onGesturePerformed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturePerformed"));
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturingListener")) {
				if (i.declaresMethodByName("onGesturingEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturingEnded"));
				if (i.declaresMethodByName("onGesturingStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGesturingStarted"));
			}
			// android.graphics
			else if (i.getName().equals("android.graphics.SurfaceTexture%OnFrameAvailableListener")) {
				if (i.declaresMethodByName("onFrameAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFrameAvailable"));
			}
			// android.hardware
			else if (i.getName().equals("android.hardware.Camera$AutoFocusCallback")) {
				if (i.declaresMethodByName("onAutoFocus"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAutoFocus"));
			}
			else if (i.getName().equals("android.hardware.Camera$AutoFocusMoveCallback")) {
				if (i.declaresMethodByName("onAutoFocusMoving"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAutoFocusMoving"));
			}
			else if (i.getName().equals("android.hardware.Camera$ErrorCallback")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"));
			}
			else if (i.getName().equals("android.hardware.Camera$FaceDetectionListener")) {
				if (i.declaresMethodByName("onFaceDetection"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFaceDetection"));
			}
			else if (i.getName().equals("android.hardware.Camera$OnZoomChangeListener")) {
				if (i.declaresMethodByName("onZoomChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onZoomChange"));
			}
			else if (i.getName().equals("android.hardware.Camera$PictureCallback")) {
				if (i.declaresMethodByName("onPictureTaken"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPictureTaken"));
			}
			else if (i.getName().equals("android.hardware.Camera$PreviewCallback")) {
				if (i.declaresMethodByName("onPreviewFrame"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreviewFrame"));
			}
			else if (i.getName().equals("android.hardware.Camera$ShutterCallback")) {
				if (i.declaresMethodByName("onShutter"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShutter"));
			}
			else if (i.getName().equals("android.hardware.SensorEventListener")) {
				if (i.declaresMethodByName("onAccuracyChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccuracyChanged"));
				if (i.declaresMethodByName("onSensorChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSensorChanged"));
			}
			// android.hardware.display
			else if (i.getName().equals("android.hardware.display.DisplayManager$DisplayListener")) {
				if (i.declaresMethodByName("onDisplayAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayAdded"));
				if (i.declaresMethodByName("onDisplayChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayChanged"));
				if (i.declaresMethodByName("onDisplayRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDisplayRemoved"));
			}
			// android.hardware.input
			else if (i.getName().equals("android.hardware.input.InputManager$InputDeviceListener")) {
				if (i.declaresMethodByName("onInputDeviceAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceAdded"));
				if (i.declaresMethodByName("onInputDeviceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceChanged"));
				if (i.declaresMethodByName("onInputDeviceRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputDeviceRemoved"));
			}
			// android.inputmethodservice
			else if (i.getName().equals("android.inputmethodservice.KeyboardView$OnKeyboardActionListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"));
				if (i.declaresMethodByName("onPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPress"));
				if (i.declaresMethodByName("onRelease"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRelease"));
				if (i.declaresMethodByName("onText"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onText"));
				if (i.declaresMethodByName("swipeDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeDown"));
				if (i.declaresMethodByName("swipeLeft"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeLeft"));
				if (i.declaresMethodByName("swipeRight"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeRight"));
				if (i.declaresMethodByName("swipeUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "swipeUp"));
			}
			// android.location
			else if (i.getName().equals("android.location.GpsStatus$Listener")) {
				if (i.declaresMethodByName("onGpsStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGpsStatusChanged"));
			}
			else if (i.getName().equals("android.location.GpsStatus$NmeaListener")) {
				if (i.declaresMethodByName("onNmeaReceived"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNmeaReceived"));
			}
			else if (i.getName().equals("android.location.LocationListener")) {
				if (i.declaresMethodByName("onLocationChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLocationChanged"));
				if (i.declaresMethodByName("onProviderDisabled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProviderDisabled"));
				if (i.declaresMethodByName("onProviderEnabled"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProviderEnabled"));
				if (i.declaresMethodByName("onStatusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStatusChanged"));
			}
			// android.media
			else if (i.getName().equals("android.media.AudioManager$OnAudioFocusChangeListener")) {
				if (i.declaresMethodByName("onAudioFocusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAudioFocusChange"));
			}
			else if (i.getName().equals("android.media.AudioRecord$OnRecordPositionUpdateListener")
					|| i.getName().equals("android.media.AudioRecord$OnPlaybackPositionUpdateListener")) {
				if (i.declaresMethodByName("onMarkerReached"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMarkerReached"));
				if (i.declaresMethodByName("onPeriodicNotification"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPeriodicNotification"));
			}
			else if (i.getName().equals("android.media.JetPlayer$OnJetEventListener")) {
				if (i.declaresMethodByName("onJetEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetEvent"));
				if (i.declaresMethodByName("onJetNumQueuedSegmentUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetNumQueuedSegmentUpdate"));
				if (i.declaresMethodByName("onJetPauseUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetPauseUpdate"));
				if (i.declaresMethodByName("onJetUserIdUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onJetUserIdUpdate"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnBufferingUpdateListener")) {
				if (i.declaresMethodByName("onBufferingUpdate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBufferingUpdate"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnCompletionListener")) {
				if (i.declaresMethodByName("onCompletion"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCompletion"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnPreparedListener")) {
				if (i.declaresMethodByName("onPrepared"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrepared"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnSeekCompleteListener")) {
				if (i.declaresMethodByName("onSeekComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSeekComplete"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnTimedTextListener")) {
				if (i.declaresMethodByName("onTimedText"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimedText"));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnVideoSizeChangedListener")) {
				if (i.declaresMethodByName("onVideoSizeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onVideoSizeChanged"));
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"));
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInfo"));
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$MediaScannerConnectionClient")) {
				if (i.declaresMethodByName("onMediaScannerConnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMediaScannerConnected"));
				if (i.declaresMethodByName("onScanCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScanCompleted"));
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$OnScanCompletedListener")) {
				if (i.declaresMethodByName("onScanCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScanCompleted"));
			}
			else if (i.getName().equals("android.media.SoundPool$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLoadComplete"));
			}
			// android.media.audiofx
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnControlStatusChangeListener")) {
				if (i.declaresMethodByName("onControlStatusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onControlStatusChange"));
			}
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnEnableStatusChangeListener")) {
				if (i.declaresMethodByName("onEnableStatusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEnableStatusChange"));
			}
			else if (i.getName().equals("android.media.audiofx.BassBoost$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"));
			}
			else if (i.getName().equals("android.media.audiofx.EnvironmentalReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"));
			}
			else if (i.getName().equals("android.media.audiofx.Equalizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"));
			}
			else if (i.getName().equals("android.media.audiofx.PresetReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"));
			}
			else if (i.getName().equals("android.media.audiofx.Virtualizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onParameterChange"));
			}
			else if (i.getName().equals("android.media.audiofx.Visualizer$OnDataCaptureListener")) {
				if (i.declaresMethodByName("onFftDataCapture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFftDataCapture"));
				if (i.declaresMethodByName("onWaveFormDataCapture"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onWaveFormDataCapture"));
			}
			// android.media.effect
			else if (i.getName().equals("android.media.effect$EffectUpdateListener")) {
				if (i.declaresMethodByName("onEffectUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEffectUpdated"));
			}
			// android.net.nsd
			else if (i.getName().equals("android.net.nsd.NsdManager$DiscoveryListener")) {
				if (i.declaresMethodByName("onDiscoveryStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStarted"));
				if (i.declaresMethodByName("onDiscoveryStopped"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStopped"));
				if (i.declaresMethodByName("onServiceFound"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceFound"));
				if (i.declaresMethodByName("onServiceLost"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceLost"));
				if (i.declaresMethodByName("onStartDiscoveryFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStartDiscoveryFailed"));
				if (i.declaresMethodByName("onStopDiscoveryFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStopDiscoveryFailed"));
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$RegistrationListener")) {
				if (i.declaresMethodByName("onRegistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed"));
				if (i.declaresMethodByName("onServiceRegistered"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceRegistered"));
				if (i.declaresMethodByName("onServiceUnregistered"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceUnregistered"));
				if (i.declaresMethodByName("onUnregistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUnregistrationFailed"));
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$ResolveListener")) {
				if (i.declaresMethodByName("onResolveFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onResolveFailed"));
				if (i.declaresMethodByName("onServiceResolved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceResolved"));
			}
			// android.net.sip
			else if (i.getName().equals("android.net.sip.SipRegistrationListener")) {
				if (i.declaresMethodByName("onRegistering"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistering"));
				if (i.declaresMethodByName("onRegistrationDone"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationDone"));
				if (i.declaresMethodByName("onRegistrationFailed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed"));
			}
			// android.net.wifi.p2p
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ActionListener")) {
				if (i.declaresMethodByName("onFailure"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFailure"));
				if (i.declaresMethodByName("onSuccess"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuccess"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ChannelListener")) {
				if (i.declaresMethodByName("onChannelDisconnected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChannelDisconnected"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ConnectionInfoListener")) {
				if (i.declaresMethodByName("onConnectionInfoAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onConnectionInfoAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdServiceResponseListener")) {
				if (i.declaresMethodByName("onDnsSdServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDnsSdServiceAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdTxtRecordListener")) {
				if (i.declaresMethodByName("onDnsSdTxtRecordAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDnsSdTxtRecordAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$GroupInfoListener")) {
				if (i.declaresMethodByName("onGroupInfoAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupInfoAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$PeerListListener")) {
				if (i.declaresMethodByName("onPeersAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPeersAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ServiceResponseListener")) {
				if (i.declaresMethodByName("onServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onServiceAvailable"));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$UpnpServiceResponseListener")) {
				if (i.declaresMethodByName("onUpnpServiceAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUpnpServiceAvailable"));
			}
			// android.os
			else if (i.getName().equals("android.os.CancellationSignal$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCancel"));
			}
			else if (i.getName().equals("android.os.IBinder$DeathRecipient")) {
				if (i.declaresMethodByName("binderDied"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "binderDied"));
			}
			else if (i.getName().equals("android.os.MessageQueue$IdleHandler")) {
				if (i.declaresMethodByName("queueIdle"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "queueIdle"));
			}
			else if (i.getName().equals("android.os.RecoverySystem$ProgressListener")) {
				if (i.declaresMethodByName("onProgress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProgress"));
			}
			// android.preference
			else if (i.getName().equals("android.preference.Preference$OnPreferenceChangeListener")) {
				if (i.declaresMethodByName("onPreferenceChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceChange"));
			}
			else if (i.getName().equals("android.preference.Preference$OnPreferenceClickListener")) {
				if (i.declaresMethodByName("onPreferenceClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceClick"));
			}
			else if (i.getName().equals("android.preference.PreferenceFragment$OnPreferenceStartFragmentCallback")) {
				if (i.declaresMethodByName("onPreferenceStartFragment"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreferenceStartFragment"));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityDestroyListener")) {
				if (i.declaresMethodByName("onActivityDestroy"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityDestroy"));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityResultListener")) {
				if (i.declaresMethodByName("onActivityResult"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityResult"));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityStopListener")) {
				if (i.declaresMethodByName("onActivityStop"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActivityStop"));
			}
			// android.security
			else if (i.getName().equals("android.security.KeyChainAliasCallback")) {
				if (i.declaresMethodByName("alias"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "alias"));
			}
			// android.speech
			else if (i.getName().equals("android.speech.RecognitionListener")) {
				if (i.declaresMethodByName("onBeginningOfSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBeginningOfSpeech"));
				if (i.declaresMethodByName("onBufferReceived"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onBufferReceived"));
				if (i.declaresMethodByName("onEndOfSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEndOfSpeech"));
				if (i.declaresMethodByName("onError"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onError"));
				if (i.declaresMethodByName("onEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEvent"));
				if (i.declaresMethodByName("onPartialResults"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPartialResults"));
				if (i.declaresMethodByName("onReadyForSpeech"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onReadyForSpeech"));
				if (i.declaresMethodByName("onResults"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onResults"));
				if (i.declaresMethodByName("onRmsChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRmsChanged"));
			}
			// android.speech.tts
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnInitListener")) {
				if (i.declaresMethodByName("onInit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInit"));
			}			
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnUtteranceCompletedListener")) {
				if (i.declaresMethodByName("onUtteranceCompleted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onUtteranceCompleted"));
			}			
			// android.support - omitted
			// android.view
			else if (i.getName().equals("android.view.ActionMode$Callback")) {
				if (i.declaresMethodByName("onActionItemClicked"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActionItemClicked"));
				if (i.declaresMethodByName("onCreateActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCreateActionMode"));
				if (i.declaresMethodByName("onDestroyActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDestroyActionMode"));
				if (i.declaresMethodByName("onPrepareActionMode"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPrepareActionMode"));
			}
			else if (i.getName().equals("android.view.ActionProvider$VisibilityListener")) {
				if (i.declaresMethodByName("onActionProviderVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onActionProviderVisibilityChanged"));
			}
			else if (i.getName().equals("android.view.GestureDetector$OnDoubleTapListener")) {
				if (i.declaresMethodByName("onDoubleTap"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDoubleTap"));
				if (i.declaresMethodByName("onDoubleTapEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDoubleTapEvent"));
				if (i.declaresMethodByName("onSingleTapConfirmed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSingleTapConfirmed"));
			}
			else if (i.getName().equals("android.view.GestureDetector$OnGestureListener")) {
				if (i.declaresMethodByName("onDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDown"));
				if (i.declaresMethodByName("onFling"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFling"));
				if (i.declaresMethodByName("onLongPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLongPress"));
				if (i.declaresMethodByName("onScroll"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScroll"));
				if (i.declaresMethodByName("onShowPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShowPress"));
				if (i.declaresMethodByName("onSingleTapUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSingleTapUp"));
			}
			else if (i.getName().equals("android.view.InputQueue$Callback")) {
				if (i.declaresMethodByName("onInputQueueCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputQueueCreated"));
				if (i.declaresMethodByName("onInputQueueDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInputQueueDestroyed"));
			}
			else if (i.getName().equals("android.view.KeyEvent$Callback")) {
				if (i.declaresMethodByName("onKeyDown"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyDown"));
				if (i.declaresMethodByName("onKeyLongPress"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyLongPress"));
				if (i.declaresMethodByName("onKeyMultiple"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyMultiple"));
				if (i.declaresMethodByName("onKeyUp"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKeyUp"));
			}
			else if (i.getName().equals("android.view.MenuItem$OnActionExpandListener")) {
				if (i.declaresMethodByName("onMenuItemActionCollapse"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionCollapse"));
				if (i.declaresMethodByName("onMenuItemActionExpand"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionExpand"));
			}
			else if (i.getName().equals("android.view.MenuItem$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick"));
			}
			else if (i.getName().equals("android.view.ScaleGestureDetector$OnScaleGestureListener")) {
				if (i.declaresMethodByName("onScale"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScale"));
				if (i.declaresMethodByName("onScaleBegin"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScaleBegin"));
				if (i.declaresMethodByName("onScaleEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScaleEnd"));
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback")) {
				if (i.declaresMethodByName("surfaceChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceChanged"));
				if (i.declaresMethodByName("surfaceCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceCreated"));
				if (i.declaresMethodByName("surfaceDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceDestroyed"));
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback2")) {
				if (i.declaresMethodByName("surfaceRedrawNeeded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "surfaceRedrawNeeded"));
			}
			else if (i.getName().equals("android.view.TextureView$SurfaceTextureListener")) {
				if (i.declaresMethodByName("onSurfaceTextureAvailable"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureAvailable"));
				if (i.declaresMethodByName("onSurfaceTextureDestroyed"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureDestroyed"));
				if (i.declaresMethodByName("onSurfaceTextureSizeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureSizeChanged"));
				if (i.declaresMethodByName("onSurfaceTextureUpdated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureUpdated"));
			}
			else if (i.getName().equals("android.view.View$OnAttachStateChangeListener")) {
				if (i.declaresMethodByName("onViewAttachedToWindow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onViewAttachedToWindow"));
				if (i.declaresMethodByName("onViewDetachedFromWindow"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onViewDetachedFromWindow"));
			}
			else if (i.getName().equals("android.view.View$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					checkAndAddMethod(getMethodFromHierarchyEx(baseClass, "void onClick(android.view.View)"));
			}
			else if (i.getName().equals("android.view.View$OnCreateContextMenuListener")) {
				if (i.declaresMethodByName("onCreateContextMenu"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCreateContextMenu"));
			}
			else if (i.getName().equals("android.view.View$OnDragListener")) {
				if (i.declaresMethodByName("onDrag"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDrag"));
			}
			else if (i.getName().equals("android.view.View$OnFocusChangeListener")) {
				if (i.declaresMethodByName("onFocusChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFocusChange"));
			}
			else if (i.getName().equals("android.view.View$OnGenericMotionListener")) {
				if (i.declaresMethodByName("onGenericMotion"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGenericMotion"));
			}
			else if (i.getName().equals("android.view.View$OnHoverListener")) {
				if (i.declaresMethodByName("onHover"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onHover"));
			}
			else if (i.getName().equals("android.view.View$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onKey"));
			}
			else if (i.getName().equals("android.view.View$OnLayoutChangeListener")) {
				if (i.declaresMethodByName("onLayoutChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLayoutChange"));
			}
			else if (i.getName().equals("android.view.View$OnLongClickListener")) {
				if (i.declaresMethodByName("onLongClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onLongClick"));
			}
			else if (i.getName().equals("android.view.View$OnSystemUiVisibilityChangeListener")) {
				if (i.declaresMethodByName("onSystemUiVisibilityChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSystemUiVisibilityChange"));
			}
			else if (i.getName().equals("android.view.View$OnTouchListener")) {
				if (i.declaresMethodByName("onTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTouch"));
			}
			else if (i.getName().equals("android.view.ViewGroup$OnHierarchyChangeListener")) {
				if (i.declaresMethodByName("onChildViewAdded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildViewAdded"));
				if (i.declaresMethodByName("onChildViewRemoved"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildViewRemoved"));
			}
			else if (i.getName().equals("android.view.ViewStub$OnInflateListener")) {
				if (i.declaresMethodByName("onInflate"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onInflate"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnDrawListener")) {
				if (i.declaresMethodByName("onDraw"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDraw"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalFocusChangeListener")) {
				if (i.declaresMethodByName("onGlobalFocusChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGlobalFocusChanged"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalLayoutListener")) {
				if (i.declaresMethodByName("onGlobalLayout"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGlobalLayout"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnPreDrawListener")) {
				if (i.declaresMethodByName("onPreDraw"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onPreDraw"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnScrollChangedListener")) {
				if (i.declaresMethodByName("onScrollChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollChanged"));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnTouchModeChangeListener")) {
				if (i.declaresMethodByName("onTouchModeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTouchModeChanged"));
			}
			// android.view.accessibility
			else if (i.getName().equals("android.view.accessibility.AccessibilityManager$AccessibilityStateChangeListener")) {
				if (i.declaresMethodByName("onAccessibilityStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAccessibilityStateChanged"));
			}
			// android.view.animation
			else if (i.getName().equals("android.view.animation.Animation$AnimationListener")) {
				if (i.declaresMethodByName("onAnimationEnd"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd"));
				if (i.declaresMethodByName("onAnimationRepeat"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat"));
				if (i.declaresMethodByName("onAnimationStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onAnimationStart"));
			}
			// android.view.inputmethod
			else if (i.getName().equals("android.view.inputmethod.InputMethod$SessionCallback")) {
				if (i.declaresMethodByName("sessionCreated"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "sessionCreated"));
			}
			else if (i.getName().equals("android.view.inputmethod.InputMethodSession$EventCallback")) {
				if (i.declaresMethodByName("finishedEvent"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "finishedEvent"));
			}
			// android.view.textservice
			else if (i.getName().equals("android.view.textservice.SpellCheckerSession$SpellCheckerSessionListener")) {
				if (i.declaresMethodByName("onGetSentenceSuggestions"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGetSentenceSuggestions"));
				if (i.declaresMethodByName("onGetSuggestions"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGetSuggestions"));
			}
			// android.webkit
			else if (i.getName().equals("android.webkit.DownloadListener")) {
				if (i.declaresMethodByName("onDownloadStart"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDownloadStart"));
			}
			// android.widget
			else if (i.getName().equals("android.widget.AbsListView$MultiChoiceModeListener")) {
				if (i.declaresMethodByName("onItemCheckedStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemCheckedStateChanged"));
			}
			else if (i.getName().equals("android.widget.AbsListView$OnScrollListener")) {
				if (i.declaresMethodByName("onScroll"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScroll"));
				if (i.declaresMethodByName("onScrollStateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStateChanged"));
			}
			else if (i.getName().equals("android.widget.AbsListView$RecyclerListener")) {
				if (i.declaresMethodByName("onMovedToScrapHeap"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMovedToScrapHeap"));
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemClickListener")) {
				if (i.declaresMethodByName("onItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemClick"));
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemLongClickListener")) {
				if (i.declaresMethodByName("onItemLongClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemLongClick"));
			}
			else if (i.getName().equals("android.widget.AdapterView.OnItemSelectedListener")) {
				if (i.declaresMethodByName("onItemSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onItemSelected"));
				if (i.declaresMethodByName("onNothingSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onNothingSelected"));
			}
			else if (i.getName().equals("android.widget.AutoCompleteTextView$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"));
			}
			else if (i.getName().equals("android.widget.CalendarView$OnDateChangeListener")) {
				if (i.declaresMethodByName("onSelectedDayChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSelectedDayChange"));
			}
			else if (i.getName().equals("android.widget.Chronometer$OnChronometerTickListener")) {
				if (i.declaresMethodByName("onChronometerTick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChronometerTick"));
			}
			else if (i.getName().equals("android.widget.CompoundButton$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCheckedChanged"));
			}
			else if (i.getName().equals("android.widget.DatePicker$OnDateChangedListener")) {
				if (i.declaresMethodByName("onDateChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDateChanged"));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnChildClickListener")) {
				if (i.declaresMethodByName("onChildClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onChildClick"));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupClickListener")) {
				if (i.declaresMethodByName("onGroupClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupClick"));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupCollapseListener")) {
				if (i.declaresMethodByName("onGroupCollapse"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupCollapse"));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupExpandListener")) {
				if (i.declaresMethodByName("onGroupExpand"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onGroupExpand"));
			}
			else if (i.getName().equals("android.widget.Filter$FilterListener")) {
				if (i.declaresMethodByName("onFilterComplete"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onFilterComplete"));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnScrollListener")) {
				if (i.declaresMethodByName("onScrollStateChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStateChange"));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnValueChangeListener")) {
				if (i.declaresMethodByName("onValueChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onValueChange"));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"));
			}
			else if (i.getName().equals("android.widget.PopupMenu$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick"));
			}
			else if (i.getName().equals("android.widget.PopupWindow$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDismiss"));
			}
			else if (i.getName().equals("android.widget.RadioGroup$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onCheckedChanged"));
			}
			else if (i.getName().equals("android.widget.RatingBar$OnRatingBarChangeListener")) {
				if (i.declaresMethodByName("onRatingChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onRatingChanged"));
			}
			else if (i.getName().equals("android.widget.SearchView$OnCloseListener")) {
				if (i.declaresMethodByName("onClose"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onClose"));
			}
			else if (i.getName().equals("android.widget.SearchView$OnQueryTextListener")) {
				if (i.declaresMethodByName("onQueryTextChange"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onQueryTextChange"));
				if (i.declaresMethodByName("onQueryTextSubmit"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onQueryTextSubmit"));
			}
			else if (i.getName().equals("android.widget.SearchView$OnSuggestionListener")) {
				if (i.declaresMethodByName("onSuggestionClick"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuggestionClick"));
				if (i.declaresMethodByName("onSuggestionSelect"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onSuggestionSelect"));
			}
			else if (i.getName().equals("android.widget.SeekBar$OnSeekBarChangeListener")) {
				if (i.declaresMethodByName("onProgressChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onProgressChanged"));
				if (i.declaresMethodByName("onStartTrackingTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStartTrackingTouch"));
				if (i.declaresMethodByName("onStopTrackingTouch"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onStopTrackingTouch"));
			}
			else if (i.getName().equals("android.widget.ShareActionProvider$OnShareTargetSelectedListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected"));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerCloseListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected"));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerOpenListener")) {
				if (i.declaresMethodByName("onDrawerOpened"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onDrawerOpened"));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerScrollListener")) {
				if (i.declaresMethodByName("onScrollEnded"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollEnded"));
				if (i.declaresMethodByName("onScrollStarted"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onScrollStarted"));
			}
			else if (i.getName().equals("android.widget.TabHost$OnTabChangeListener")) {
				if (i.declaresMethodByName("onTabChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTabChanged"));
			}
			else if (i.getName().equals("android.widget.TextView$OnEditorActionListener")) {
				if (i.declaresMethodByName("onEditorAction"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onEditorAction"));
			}
			else if (i.getName().equals("android.widget.TimePicker$OnTimeChangedListener")) {
				if (i.declaresMethodByName("onTimeChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onTimeChanged"));
			}
			else if (i.getName().equals("android.widget.ZoomButtonsController$OnZoomListener")) {
				if (i.declaresMethodByName("onVisibilityChanged"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onVisibilityChanged"));
				if (i.declaresMethodByName("onZoom"))
					checkAndAddMethod(getMethodFromHierarchy(baseClass, "onZoom"));
			}
		}
	}

	/**
	 * Checks whether the given Soot method comes from a system class. If not,
	 * it is added to the list of callback methods.
	 * @param method The method to check and add
	 */
	private void checkAndAddMethod(SootMethod method) {
		AndroidMethod am = new AndroidMethod(method);
		if (!am.getClassName().startsWith("android."))
			callbackMethods.add(am);
	}

	private Set<SootClass> collectAllInterfaces(SootClass sootClass) {
		Set<SootClass> interfaces = new HashSet<SootClass>(sootClass.getInterfaces());
		for (SootClass i : sootClass.getInterfaces())
			interfaces.addAll(collectAllInterfaces(i));
		return interfaces;
	}
	
	public List<AndroidMethod> getCallbackMethods() {
		return this.callbackMethods;
	}
	
	public Map<SootClass, Set<Integer>> getLayoutClasses() {
		return this.layoutClasses;
	}
		
}

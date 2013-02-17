package soot.jimple.infoflow.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.infoflow.util.AndroidEntryPointCreator;
import soot.options.Options;
import de.ecspride.sourcesinkfinder.data.AndroidMethod;

/**
 * Analyzes the classes in the APK file to find custom implementations of the
 * well-known Android callback and handler interfaces.
 * 
 * @author Steven Arzt
 *
 */
public class AnalyzeJimpleClass {

	private String androidJar;
	private String androidApk;
	private List<AndroidMethod> callbackMethods = new ArrayList<AndroidMethod>();

	public AnalyzeJimpleClass(String androidJar, String androidApk) {
		this.androidJar = androidJar;
		this.androidApk= androidApk;
	}

	public void collectCallbackMethods(Map<String, List<String>> baseEntryPoints) throws IOException {
		soot.G.reset();

		Transform transform = new Transform("wjtp.ifds", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {			
				for (SootClass sc : Scene.v().getClasses())
					analyzeClass(sc);
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
		
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_soot_classpath(androidApk + File.pathSeparator + Scene.v().getAndroidJarPath(androidJar, androidApk));
		Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Arrays.asList(baseEntryPoints.keySet().toArray()));
		Options.v().set_app(true);

		Scene.v().loadNecessaryClasses();
		
		for (String className : baseEntryPoints.keySet()) {
			SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
			c.setApplicationClass();
		}

		AndroidEntryPointCreator entryPointCreator = new AndroidEntryPointCreator();
		SootMethod entryPoint = entryPointCreator.createDummyMain(baseEntryPoints);
		
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		PackManager.v().runPacks();
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
	
	private void analyzeClass(SootClass baseClass, SootClass sootClass) {
		// There's no information we could use in an interface
		if (sootClass.isInterface())
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
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAccountsUpdated")));
			}
			// android.animation
			else if (i.getName().equals("android.animation.Animator$AnimatorListener")) {
				if (i.declaresMethodByName("onAnimationCancel"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationCancel")));
				if (i.declaresMethodByName("onAnimationEnd"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd")));
				if (i.declaresMethodByName("onAnimationRepeat"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat")));
				if (i.declaresMethodByName("onAnimationStart"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationStart")));
			}
			else if (i.getName().equals("android.animation.LayoutTransition$TransitionListener")) {
				if (i.declaresMethodByName("endTransition"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "endTransition")));
				if (i.declaresMethodByName("startTransition"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "startTransition")));
			}
			else if (i.getName().equals("android.animation.TimeAnimator$TimeListener")) {
				if (i.declaresMethodByName("onTimeUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTimeUpdate")));
			}
			else if (i.getName().equals("android.animation.ValueAnimator$AnimatorUpdateListener")) {
				if (i.declaresMethodByName("onAnimationUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationUpdate")));
			}
			// android.app
			else if (i.getName().equals("android.app.ActionBar$OnMenuVisibilityListener")) {
				if (i.declaresMethodByName("onMenuVisibilityChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMenuVisibilityChanged")));
			}
			else if (i.getName().equals("android.app.ActionBar$OnNavigationListener")) {
				if (i.declaresMethodByName("onNavigationItemSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onNavigationItemSelected")));
			}
			else if (i.getName().equals("android.app.ActionBar$TabListener")) {
				if (i.declaresMethodByName("onTabReselected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTabReselected")));
				if (i.declaresMethodByName("onTabSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTabSelected")));
				if (i.declaresMethodByName("onTabUnselected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTabUnselected")));
			}
			else if (i.getName().equals("android.app.Application$ActivityLifecycleCallbacks")) {
				if (i.declaresMethodByName("onActivityCreated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityCreated")));
				if (i.declaresMethodByName("onActivityDestroyed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityDestroyed")));
				if (i.declaresMethodByName("onActivityPaused"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityPaused")));
				if (i.declaresMethodByName("onActivityResumed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityResumed")));
				if (i.declaresMethodByName("onActivitySaveInstanceState"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivitySaveInstanceState")));
				if (i.declaresMethodByName("onActivityStarted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityStarted")));
				if (i.declaresMethodByName("onActivityStopped"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityStopped")));
			}
			else if (i.getName().equals("android.app.DatePickerDialog$OnDateSetListener")) {
				if (i.declaresMethodByName("onDateSet"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDateSet")));
			}
			else if (i.getName().equals("android.app.FragmentBreadCrumbs$OnBreadCrumbClickListener")) {
				if (i.declaresMethodByName("onBreadCrumbClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBreadCrumbClick")));
			}
			else if (i.getName().equals("android.app.FragmentManager$OnBackStackChangedListener")) {
				if (i.declaresMethodByName("onBackStackChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBackStackChanged")));
			}
			else if (i.getName().equals("android.app.KeyguardManager$OnKeyguardExitResult")) {
				if (i.declaresMethodByName("onKeyguardExitResult"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKeyguardExitResult")));
			}
			else if (i.getName().equals("android.app.LoaderManager$LoaderCallbacks")) {
				if (i.declaresMethodByName("onCreateLoader"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCreateLoader")));
				if (i.declaresMethodByName("onLoadFinished"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLoadFinished")));
				if (i.declaresMethodByName("onLoaderReset"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLoaderReset")));
			}
			else if (i.getName().equals("android.app.PendingIntent$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSendFinished")));
			}
			else if (i.getName().equals("android.app.SearchManager$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCancel")));
			}
			else if (i.getName().equals("android.app.SearchManager$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDismiss")));
			}
			else if (i.getName().equals("android.app.TimePickerDialog$OnTimeSetListener")) {
				if (i.declaresMethodByName("onTimeSet"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTimeSet")));
			}
			// android.bluetooth
			else if (i.getName().equals("android.bluetooth.BluetoothProfile$ServiceListener")) {
				if (i.declaresMethodByName("onServiceConnected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceConnected")));
				if (i.declaresMethodByName("onServiceDisconnected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceDisconnected")));
			}
			// android.content
			else if (i.getName().equals("android.content.ClipboardManager$OnPrimaryClipChangedListener")) {
				if (i.declaresMethodByName("onPrimaryClipChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPrimaryClipChanged")));
			}			
			else if (i.getName().equals("android.content.ComponentCallbacks")) {
				if (i.declaresMethodByName("onConfigurationChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onConfigurationChanged")));
				if (i.declaresMethodByName("onLowMemory"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLowMemory")));
			}
			else if (i.getName().equals("android.content.ComponentCallbacks2")) {
				if (i.declaresMethodByName("onTrimMemory"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTrimMemory")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCancel")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onClick")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDismiss")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKey")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnMultiChoiceClickListener")) {
				if (i.declaresMethodByName("onClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onClick")));
			}			
			else if (i.getName().equals("android.content.DialogInterface$OnShowListener")) {
				if (i.declaresMethodByName("onShow"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onShow")));
			}
			else if (i.getName().equals("android.content.IntentSender$OnFinished")) {
				if (i.declaresMethodByName("onSendFinished"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSendFinished")));
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCanceledListener")) {
				if (i.declaresMethodByName("onLoadCanceled"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLoadCanceled")));
			}
			else if (i.getName().equals("android.content.Loader$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLoadComplete")));
			}
			else if (i.getName().equals("android.content.SharedPreferences$OnSharedPreferenceChangeListener")) {
				if (i.declaresMethodByName("onSharedPreferenceChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSharedPreferenceChanged")));
			}
			else if (i.getName().equals("android.content.SyncStatusObserver")) {
				if (i.declaresMethodByName("onStatusChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStatusChanged")));
			}
			// android.database.sqlite
			else if (i.getName().equals("android.database.sqlite.SQLiteTransactionListener")) {
				if (i.declaresMethodByName("onBegin"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBegin")));
				if (i.declaresMethodByName("onCommit"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCommit")));
				if (i.declaresMethodByName("onRollback"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRollback")));
			}
			// android.drm
			else if (i.getName().equals("android.drm.DrmManagerClient$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onError")));
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnEventListener")) {
				if (i.declaresMethodByName("onEvent"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEvent")));
			}
			else if (i.getName().equals("android.drm.DrmManagerClient$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInfo")));
			}
			// android.gesture			
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGestureListener")) {
				if (i.declaresMethodByName("onGesture"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGesture")));
				if (i.declaresMethodByName("onGestureCancelled"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGestureCancelled")));
				if (i.declaresMethodByName("onGestureEnded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGestureEnded")));
				if (i.declaresMethodByName("onGestureStarted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGestureStarted")));
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturePerformedListener")) {
				if (i.declaresMethodByName("onGesturePerformed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGesturePerformed")));
			}
			else if (i.getName().equals("android.gesture.GestureOverlayView$OnGesturingListener")) {
				if (i.declaresMethodByName("onGesturingEnded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGesturingEnded")));
				if (i.declaresMethodByName("onGesturingStarted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGesturingStarted")));
			}
			// android.graphics
			else if (i.getName().equals("android.graphics.SurfaceTexture%OnFrameAvailableListener")) {
				if (i.declaresMethodByName("onFrameAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFrameAvailable")));
			}
			// android.hardware
			else if (i.getName().equals("android.hardware.Camera$AutoFocusCallback")) {
				if (i.declaresMethodByName("onAutoFocus"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAutoFocus")));
			}
			else if (i.getName().equals("android.hardware.Camera$AutoFocusMoveCallback")) {
				if (i.declaresMethodByName("onAutoFocusMoving"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAutoFocusMoving")));
			}
			else if (i.getName().equals("android.hardware.Camera$ErrorCallback")) {
				if (i.declaresMethodByName("onError"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onError")));
			}
			else if (i.getName().equals("android.hardware.Camera$FaceDetectionListener")) {
				if (i.declaresMethodByName("onFaceDetection"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFaceDetection")));
			}
			else if (i.getName().equals("android.hardware.Camera$OnZoomChangeListener")) {
				if (i.declaresMethodByName("onZoomChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onZoomChange")));
			}
			else if (i.getName().equals("android.hardware.Camera$PictureCallback")) {
				if (i.declaresMethodByName("onPictureTaken"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPictureTaken")));
			}
			else if (i.getName().equals("android.hardware.Camera$PreviewCallback")) {
				if (i.declaresMethodByName("onPreviewFrame"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPreviewFrame")));
			}
			else if (i.getName().equals("android.hardware.Camera$ShutterCallback")) {
				if (i.declaresMethodByName("onShutter"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onShutter")));
			}
			else if (i.getName().equals("android.hardware.SensorEventListener")) {
				if (i.declaresMethodByName("onAccuracyChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAccuracyChanged")));
				if (i.declaresMethodByName("onSensorChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSensorChanged")));
			}
			// android.hardware.display
			else if (i.getName().equals("android.hardware.display.DisplayManager$DisplayListener")) {
				if (i.declaresMethodByName("onDisplayAdded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDisplayAdded")));
				if (i.declaresMethodByName("onDisplayChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDisplayChanged")));
				if (i.declaresMethodByName("onDisplayRemoved"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDisplayRemoved")));
			}
			// android.hardware.input
			else if (i.getName().equals("android.hardware.input.InputManager$InputDeviceListener")) {
				if (i.declaresMethodByName("onInputDeviceAdded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInputDeviceAdded")));
				if (i.declaresMethodByName("onInputDeviceChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInputDeviceChanged")));
				if (i.declaresMethodByName("onInputDeviceRemoved"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInputDeviceRemoved")));
			}
			// android.inputmethodservice
			else if (i.getName().equals("android.inputmethodservice.KeyboardView$OnKeyboardActionListener")) {
				if (i.declaresMethodByName("onKey"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKey")));
				if (i.declaresMethodByName("onPress"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPress")));
				if (i.declaresMethodByName("onRelease"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRelease")));
				if (i.declaresMethodByName("onText"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onText")));
				if (i.declaresMethodByName("swipeDown"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "swipeDown")));
				if (i.declaresMethodByName("swipeLeft"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "swipeLeft")));
				if (i.declaresMethodByName("swipeRight"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "swipeRight")));
				if (i.declaresMethodByName("swipeUp"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "swipeUp")));
			}
			// android.location
			else if (i.getName().equals("android.location.GpsStatus$Listener")) {
				if (i.declaresMethodByName("onGpsStatusChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGpsStatusChanged")));
			}
			else if (i.getName().equals("android.location.GpsStatus$NmeaListener")) {
				if (i.declaresMethodByName("onNmeaReceived"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onNmeaReceived")));
			}
			else if (i.getName().equals("android.location.LocationListener")) {
				if (i.declaresMethodByName("onLocationChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLocationChanged")));
				if (i.declaresMethodByName("onProviderDisabled"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onProviderDisabled")));
				if (i.declaresMethodByName("onProviderEnabled"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onProviderEnabled")));
				if (i.declaresMethodByName("onStatusChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStatusChanged")));
			}
			// android.media
			else if (i.getName().equals("android.media.AudioManager$OnAudioFocusChangeListener")) {
				if (i.declaresMethodByName("onAudioFocusChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAudioFocusChange")));
			}
			else if (i.getName().equals("android.media.AudioRecord$OnRecordPositionUpdateListener")
					|| i.getName().equals("android.media.AudioRecord$OnPlaybackPositionUpdateListener")) {
				if (i.declaresMethodByName("onMarkerReached"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMarkerReached")));
				if (i.declaresMethodByName("onPeriodicNotification"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPeriodicNotification")));
			}
			else if (i.getName().equals("android.media.JetPlayer$OnJetEventListener")) {
				if (i.declaresMethodByName("onJetEvent"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onJetEvent")));
				if (i.declaresMethodByName("onJetNumQueuedSegmentUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onJetNumQueuedSegmentUpdate")));
				if (i.declaresMethodByName("onJetPauseUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onJetPauseUpdate")));
				if (i.declaresMethodByName("onJetUserIdUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onJetUserIdUpdate")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnBufferingUpdateListener")) {
				if (i.declaresMethodByName("onBufferingUpdate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBufferingUpdate")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnCompletionListener")) {
				if (i.declaresMethodByName("onCompletion"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCompletion")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onError")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInfo")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnPreparedListener")) {
				if (i.declaresMethodByName("onPrepared"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPrepared")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnSeekCompleteListener")) {
				if (i.declaresMethodByName("onSeekComplete"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSeekComplete")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnTimedTextListener")) {
				if (i.declaresMethodByName("onTimedText"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTimedText")));
			}
			else if (i.getName().equals("android.media.MediaPlayer$OnVideoSizeChangedListener")) {
				if (i.declaresMethodByName("onVideoSizeChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onVideoSizeChanged")));
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnErrorListener")) {
				if (i.declaresMethodByName("onError"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onError")));
			}
			else if (i.getName().equals("android.media.MediaRecorder$OnInfoListener")) {
				if (i.declaresMethodByName("onInfo"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInfo")));
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$MediaScannerConnectionClient")) {
				if (i.declaresMethodByName("onMediaScannerConnected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMediaScannerConnected")));
				if (i.declaresMethodByName("onScanCompleted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScanCompleted")));
			}
			else if (i.getName().equals("android.media.MediaScannerConnection$OnScanCompletedListener")) {
				if (i.declaresMethodByName("onScanCompleted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScanCompleted")));
			}
			else if (i.getName().equals("android.media.SoundPool$OnLoadCompleteListener")) {
				if (i.declaresMethodByName("onLoadComplete"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLoadComplete")));
			}
			// android.media.audiofx
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnControlStatusChangeListener")) {
				if (i.declaresMethodByName("onControlStatusChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onControlStatusChange")));
			}
			else if (i.getName().equals("android.media.audiofx.AudioEffect$OnEnableStatusChangeListener")) {
				if (i.declaresMethodByName("onEnableStatusChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEnableStatusChange")));
			}
			else if (i.getName().equals("android.media.audiofx.BassBoost$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onParameterChange")));
			}
			else if (i.getName().equals("android.media.audiofx.EnvironmentalReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onParameterChange")));
			}
			else if (i.getName().equals("android.media.audiofx.Equalizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onParameterChange")));
			}
			else if (i.getName().equals("android.media.audiofx.PresetReverb$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onParameterChange")));
			}
			else if (i.getName().equals("android.media.audiofx.Virtualizer$OnParameterChangeListener")) {
				if (i.declaresMethodByName("onParameterChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onParameterChange")));
			}
			else if (i.getName().equals("android.media.audiofx.Visualizer$OnDataCaptureListener")) {
				if (i.declaresMethodByName("onFftDataCapture"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFftDataCapture")));
				if (i.declaresMethodByName("onWaveFormDataCapture"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onWaveFormDataCapture")));
			}
			// android.media.effect
			else if (i.getName().equals("android.media.effect$EffectUpdateListener")) {
				if (i.declaresMethodByName("onEffectUpdated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEffectUpdated")));
			}
			// android.net.nsd
			else if (i.getName().equals("android.net.nsd.NsdManager$DiscoveryListener")) {
				if (i.declaresMethodByName("onDiscoveryStarted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStarted")));
				if (i.declaresMethodByName("onDiscoveryStopped"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDiscoveryStopped")));
				if (i.declaresMethodByName("onServiceFound"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceFound")));
				if (i.declaresMethodByName("onServiceLost"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceLost")));
				if (i.declaresMethodByName("onStartDiscoveryFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStartDiscoveryFailed")));
				if (i.declaresMethodByName("onStopDiscoveryFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStopDiscoveryFailed")));
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$RegistrationListener")) {
				if (i.declaresMethodByName("onRegistrationFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed")));
				if (i.declaresMethodByName("onServiceRegistered"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceRegistered")));
				if (i.declaresMethodByName("onServiceUnregistered"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceUnregistered")));
				if (i.declaresMethodByName("onUnregistrationFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onUnregistrationFailed")));
			}
			else if (i.getName().equals("android.net.nsd.NsdManager$ResolveListener")) {
				if (i.declaresMethodByName("onResolveFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onResolveFailed")));
				if (i.declaresMethodByName("onServiceResolved"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceResolved")));
			}
			// android.net.sip
			else if (i.getName().equals("android.net.sip.SipRegistrationListener")) {
				if (i.declaresMethodByName("onRegistering"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRegistering")));
				if (i.declaresMethodByName("onRegistrationDone"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRegistrationDone")));
				if (i.declaresMethodByName("onRegistrationFailed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRegistrationFailed")));
			}
			// android.net.wifi.p2p
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ActionListener")) {
				if (i.declaresMethodByName("onFailure"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFailure")));
				if (i.declaresMethodByName("onSuccess"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSuccess")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ChannelListener")) {
				if (i.declaresMethodByName("onChannelDisconnected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onChannelDisconnected")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ConnectionInfoListener")) {
				if (i.declaresMethodByName("onConnectionInfoAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onConnectionInfoAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdServiceResponseListener")) {
				if (i.declaresMethodByName("onDnsSdServiceAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDnsSdServiceAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$DnsSdTxtRecordListener")) {
				if (i.declaresMethodByName("onDnsSdTxtRecordAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDnsSdTxtRecordAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$GroupInfoListener")) {
				if (i.declaresMethodByName("onGroupInfoAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGroupInfoAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$PeerListListener")) {
				if (i.declaresMethodByName("onPeersAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPeersAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$ServiceResponseListener")) {
				if (i.declaresMethodByName("onServiceAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onServiceAvailable")));
			}
			else if (i.getName().equals("android.net.wifi.p2p.WifiP2pManager$UpnpServiceResponseListener")) {
				if (i.declaresMethodByName("onUpnpServiceAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onUpnpServiceAvailable")));
			}
			// android.os
			else if (i.getName().equals("android.os.CancellationSignal$OnCancelListener")) {
				if (i.declaresMethodByName("onCancel"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCancel")));
			}
			else if (i.getName().equals("android.os.IBinder$DeathRecipient")) {
				if (i.declaresMethodByName("binderDied"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "binderDied")));
			}
			else if (i.getName().equals("android.os.MessageQueue$IdleHandler")) {
				if (i.declaresMethodByName("queueIdle"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "queueIdle")));
			}
			else if (i.getName().equals("android.os.RecoverySystem$ProgressListener")) {
				if (i.declaresMethodByName("onProgress"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onProgress")));
			}
			// android.preference
			else if (i.getName().equals("android.preference.Preference$OnPreferenceChangeListener")) {
				if (i.declaresMethodByName("onPreferenceChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPreferenceChange")));
			}
			else if (i.getName().equals("android.preference.Preference$OnPreferenceClickListener")) {
				if (i.declaresMethodByName("onPreferenceClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPreferenceClick")));
			}
			else if (i.getName().equals("android.preference.PreferenceFragment$OnPreferenceStartFragmentCallback")) {
				if (i.declaresMethodByName("onPreferenceStartFragment"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPreferenceStartFragment")));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityDestroyListener")) {
				if (i.declaresMethodByName("onActivityDestroy"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityDestroy")));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityResultListener")) {
				if (i.declaresMethodByName("onActivityResult"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityResult")));
			}
			else if (i.getName().equals("android.preference.PreferenceManager$OnActivityStopListener")) {
				if (i.declaresMethodByName("onActivityStop"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActivityStop")));
			}
			// android.security
			else if (i.getName().equals("android.security.KeyChainAliasCallback")) {
				if (i.declaresMethodByName("alias"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "alias")));
			}
			// android.speech
			else if (i.getName().equals("android.speech.RecognitionListener")) {
				if (i.declaresMethodByName("onBeginningOfSpeech"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBeginningOfSpeech")));
				if (i.declaresMethodByName("onBufferReceived"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onBufferReceived")));
				if (i.declaresMethodByName("onEndOfSpeech"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEndOfSpeech")));
				if (i.declaresMethodByName("onError"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onError")));
				if (i.declaresMethodByName("onEvent"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEvent")));
				if (i.declaresMethodByName("onPartialResults"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPartialResults")));
				if (i.declaresMethodByName("onReadyForSpeech"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onReadyForSpeech")));
				if (i.declaresMethodByName("onResults"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onResults")));
				if (i.declaresMethodByName("onRmsChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRmsChanged")));
			}
			// android.speech.tts
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnInitListener")) {
				if (i.declaresMethodByName("onInit"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInit")));
			}			
			else if (i.getName().equals("android.speech.tts.TextToSpeech$OnUtteranceCompletedListener")) {
				if (i.declaresMethodByName("onUtteranceCompleted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onUtteranceCompleted")));
			}			
			// android.support - omitted
			// android.view
			else if (i.getName().equals("android.view.ActionMode$Callback")) {
				if (i.declaresMethodByName("onActionItemClicked"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActionItemClicked")));
				if (i.declaresMethodByName("onCreateActionMode"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCreateActionMode")));
				if (i.declaresMethodByName("onDestroyActionMode"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDestroyActionMode")));
				if (i.declaresMethodByName("onPrepareActionMode"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPrepareActionMode")));
			}
			else if (i.getName().equals("android.view.ActionProvider$VisibilityListener")) {
				if (i.declaresMethodByName("onActionProviderVisibilityChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onActionProviderVisibilityChanged")));
			}
			else if (i.getName().equals("android.view.GestureDetector$OnDoubleTapListener")) {
				if (i.declaresMethodByName("onDoubleTap"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDoubleTap")));
				if (i.declaresMethodByName("onDoubleTapEvent"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDoubleTapEvent")));
				if (i.declaresMethodByName("onSingleTapConfirmed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSingleTapConfirmed")));
			}
			else if (i.getName().equals("android.view.GestureDetector$OnGestureListener")) {
				if (i.declaresMethodByName("onDown"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDown")));
				if (i.declaresMethodByName("onFling"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFling")));
				if (i.declaresMethodByName("onLongPress"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLongPress")));
				if (i.declaresMethodByName("onScroll"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScroll")));
				if (i.declaresMethodByName("onShowPress"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onShowPress")));
				if (i.declaresMethodByName("onSingleTapUp"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSingleTapUp")));
			}
			else if (i.getName().equals("android.view.InputQueue$Callback")) {
				if (i.declaresMethodByName("onInputQueueCreated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInputQueueCreated")));
				if (i.declaresMethodByName("onInputQueueDestroyed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInputQueueDestroyed")));
			}
			else if (i.getName().equals("android.view.KeyEvent$Callback")) {
				if (i.declaresMethodByName("onKeyDown"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKeyDown")));
				if (i.declaresMethodByName("onKeyLongPress"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKeyLongPress")));
				if (i.declaresMethodByName("onKeyMultiple"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKeyMultiple")));
				if (i.declaresMethodByName("onKeyUp"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKeyUp")));
			}
			else if (i.getName().equals("android.view.MenuItem$OnActionExpandListener")) {
				if (i.declaresMethodByName("onMenuItemActionCollapse"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionCollapse")));
				if (i.declaresMethodByName("onMenuItemActionExpand"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMenuItemActionExpand")));
			}
			else if (i.getName().equals("android.view.MenuItem$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick")));
			}
			else if (i.getName().equals("android.view.ScaleGestureDetector$OnScaleGestureListener")) {
				if (i.declaresMethodByName("onScale"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScale")));
				if (i.declaresMethodByName("onScaleBegin"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScaleBegin")));
				if (i.declaresMethodByName("onScaleEnd"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScaleEnd")));
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback")) {
				if (i.declaresMethodByName("surfaceChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "surfaceChanged")));
				if (i.declaresMethodByName("surfaceCreated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "surfaceCreated")));
				if (i.declaresMethodByName("surfaceDestroyed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "surfaceDestroyed")));
			}
			else if (i.getName().equals("android.view.SurfaceHolder$Callback2")) {
				if (i.declaresMethodByName("surfaceRedrawNeeded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "surfaceRedrawNeeded")));
			}
			else if (i.getName().equals("android.view.TextureView$SurfaceTextureListener")) {
				if (i.declaresMethodByName("onSurfaceTextureAvailable"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureAvailable")));
				if (i.declaresMethodByName("onSurfaceTextureDestroyed"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureDestroyed")));
				if (i.declaresMethodByName("onSurfaceTextureSizeChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureSizeChanged")));
				if (i.declaresMethodByName("onSurfaceTextureUpdated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSurfaceTextureUpdated")));
			}
			else if (i.getName().equals("android.view.View$OnAttachStateChangeListener")) {
				if (i.declaresMethodByName("onViewAttachedToWindow"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onViewAttachedToWindow")));
				if (i.declaresMethodByName("onViewDetachedFromWindow"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onViewDetachedFromWindow")));
			}
			else if (i.getName().equals("android.view.View$OnClickListener")) {
				if (i.declaresMethodByName("onClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onClick")));
			}
			else if (i.getName().equals("android.view.View$OnCreateContextMenuListener")) {
				if (i.declaresMethodByName("onCreateContextMenu"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCreateContextMenu")));
			}
			else if (i.getName().equals("android.view.View$OnDragListener")) {
				if (i.declaresMethodByName("onDrag"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDrag")));
			}
			else if (i.getName().equals("android.view.View$OnFocusChangeListener")) {
				if (i.declaresMethodByName("onFocusChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFocusChange")));
			}
			else if (i.getName().equals("android.view.View$OnGenericMotionListener")) {
				if (i.declaresMethodByName("onGenericMotion"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGenericMotion")));
			}
			else if (i.getName().equals("android.view.View$OnHoverListener")) {
				if (i.declaresMethodByName("onHover"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onHover")));
			}
			else if (i.getName().equals("android.view.View$OnKeyListener")) {
				if (i.declaresMethodByName("onKey"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onKey")));
			}
			else if (i.getName().equals("android.view.View$OnLayoutChangeListener")) {
				if (i.declaresMethodByName("onLayoutChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLayoutChange")));
			}
			else if (i.getName().equals("android.view.View$OnLongClickListener")) {
				if (i.declaresMethodByName("onLongClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onLongClick")));
			}
			else if (i.getName().equals("android.view.View$OnSystemUiVisibilityChangeListener")) {
				if (i.declaresMethodByName("onSystemUiVisibilityChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSystemUiVisibilityChange")));
			}
			else if (i.getName().equals("android.view.View$OnTouchListener")) {
				if (i.declaresMethodByName("onTouch"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTouch")));
			}
			else if (i.getName().equals("android.view.ViewGroup$OnHierarchyChangeListener")) {
				if (i.declaresMethodByName("onChildViewAdded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onChildViewAdded")));
				if (i.declaresMethodByName("onChildViewRemoved"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onChildViewRemoved")));
			}
			else if (i.getName().equals("android.view.ViewStub$OnInflateListener")) {
				if (i.declaresMethodByName("onInflate"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onInflate")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnDrawListener")) {
				if (i.declaresMethodByName("onDraw"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDraw")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalFocusChangeListener")) {
				if (i.declaresMethodByName("onGlobalFocusChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGlobalFocusChanged")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnGlobalLayoutListener")) {
				if (i.declaresMethodByName("onGlobalLayout"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGlobalLayout")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnPreDrawListener")) {
				if (i.declaresMethodByName("onPreDraw"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onPreDraw")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnScrollChangedListener")) {
				if (i.declaresMethodByName("onScrollChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScrollChanged")));
			}
			else if (i.getName().equals("android.view.ViewTreeObserver$OnTouchModeChangeListener")) {
				if (i.declaresMethodByName("onTouchModeChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTouchModeChanged")));
			}
			// android.view.accessibility
			else if (i.getName().equals("android.view.accessibility.AccessibilityManager$AccessibilityStateChangeListener")) {
				if (i.declaresMethodByName("onAccessibilityStateChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAccessibilityStateChanged")));
			}
			// android.view.animation
			else if (i.getName().equals("android.view.animation.Animation$AnimationListener")) {
				if (i.declaresMethodByName("onAnimationEnd"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationEnd")));
				if (i.declaresMethodByName("onAnimationRepeat"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationRepeat")));
				if (i.declaresMethodByName("onAnimationStart"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onAnimationStart")));
			}
			// android.view.inputmethod
			else if (i.getName().equals("android.view.inputmethod.InputMethod$SessionCallback")) {
				if (i.declaresMethodByName("sessionCreated"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "sessionCreated")));
			}
			else if (i.getName().equals("android.view.inputmethod.InputMethodSession$EventCallback")) {
				if (i.declaresMethodByName("finishedEvent"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "finishedEvent")));
			}
			// android.view.textservice
			else if (i.getName().equals("android.view.textservice.SpellCheckerSession$SpellCheckerSessionListener")) {
				if (i.declaresMethodByName("onGetSentenceSuggestions"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGetSentenceSuggestions")));
				if (i.declaresMethodByName("onGetSuggestions"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGetSuggestions")));
			}
			// android.webkit
			else if (i.getName().equals("android.webkit.DownloadListener")) {
				if (i.declaresMethodByName("onDownloadStart"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDownloadStart")));
			}
			// android.widget
			else if (i.getName().equals("android.widget.AbsListView$MultiChoiceModeListener")) {
				if (i.declaresMethodByName("onItemCheckedStateChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onItemCheckedStateChanged")));
			}
			else if (i.getName().equals("android.widget.AbsListView$OnScrollListener")) {
				if (i.declaresMethodByName("onScroll"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScroll")));
				if (i.declaresMethodByName("onScrollStateChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScrollStateChanged")));
			}
			else if (i.getName().equals("android.widget.AbsListView$RecyclerListener")) {
				if (i.declaresMethodByName("onMovedToScrapHeap"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMovedToScrapHeap")));
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemClickListener")) {
				if (i.declaresMethodByName("onItemClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onItemClick")));
			}
			else if (i.getName().equals("android.widget.AdapterView$OnItemLongClickListener")) {
				if (i.declaresMethodByName("onItemLongClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onItemLongClick")));
			}
			else if (i.getName().equals("android.widget.AdapterView.OnItemSelectedListener")) {
				if (i.declaresMethodByName("onItemSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onItemSelected")));
				if (i.declaresMethodByName("onNothingSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onNothingSelected")));
			}
			else if (i.getName().equals("android.widget.AutoCompleteTextView$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDismiss")));
			}
			else if (i.getName().equals("android.widget.CalendarView$OnDateChangeListener")) {
				if (i.declaresMethodByName("onSelectedDayChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSelectedDayChange")));
			}
			else if (i.getName().equals("android.widget.Chronometer$OnChronometerTickListener")) {
				if (i.declaresMethodByName("onChronometerTick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onChronometerTick")));
			}
			else if (i.getName().equals("android.widget.CompoundButton$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCheckedChanged")));
			}
			else if (i.getName().equals("android.widget.DatePicker$OnDateChangedListener")) {
				if (i.declaresMethodByName("onDateChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDateChanged")));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnChildClickListener")) {
				if (i.declaresMethodByName("onChildClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onChildClick")));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupClickListener")) {
				if (i.declaresMethodByName("onGroupClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGroupClick")));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupCollapseListener")) {
				if (i.declaresMethodByName("onGroupCollapse"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGroupCollapse")));
			}
			else if (i.getName().equals("android.widget.ExpandableListView$OnGroupExpandListener")) {
				if (i.declaresMethodByName("onGroupExpand"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onGroupExpand")));
			}
			else if (i.getName().equals("android.widget.Filter$FilterListener")) {
				if (i.declaresMethodByName("onFilterComplete"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onFilterComplete")));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnScrollListener")) {
				if (i.declaresMethodByName("onScrollStateChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScrollStateChange")));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnValueChangeListener")) {
				if (i.declaresMethodByName("onValueChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onValueChange")));
			}
			else if (i.getName().equals("android.widget.NumberPicker$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDismiss")));
			}
			else if (i.getName().equals("android.widget.PopupMenu$OnMenuItemClickListener")) {
				if (i.declaresMethodByName("onMenuItemClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onMenuItemClick")));
			}
			else if (i.getName().equals("android.widget.PopupWindow$OnDismissListener")) {
				if (i.declaresMethodByName("onDismiss"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDismiss")));
			}
			else if (i.getName().equals("android.widget.RadioGroup$OnCheckedChangeListener")) {
				if (i.declaresMethodByName("onCheckedChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onCheckedChanged")));
			}
			else if (i.getName().equals("android.widget.RatingBar$OnRatingBarChangeListener")) {
				if (i.declaresMethodByName("onRatingChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onRatingChanged")));
			}
			else if (i.getName().equals("android.widget.SearchView$OnCloseListener")) {
				if (i.declaresMethodByName("onClose"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onClose")));
			}
			else if (i.getName().equals("android.widget.SearchView$OnQueryTextListener")) {
				if (i.declaresMethodByName("onQueryTextChange"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onQueryTextChange")));
				if (i.declaresMethodByName("onQueryTextSubmit"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onQueryTextSubmit")));
			}
			else if (i.getName().equals("android.widget.SearchView$OnSuggestionListener")) {
				if (i.declaresMethodByName("onSuggestionClick"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSuggestionClick")));
				if (i.declaresMethodByName("onSuggestionSelect"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onSuggestionSelect")));
			}
			else if (i.getName().equals("android.widget.SeekBar$OnSeekBarChangeListener")) {
				if (i.declaresMethodByName("onProgressChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onProgressChanged")));
				if (i.declaresMethodByName("onStartTrackingTouch"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStartTrackingTouch")));
				if (i.declaresMethodByName("onStopTrackingTouch"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onStopTrackingTouch")));
			}
			else if (i.getName().equals("android.widget.ShareActionProvider$OnShareTargetSelectedListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected")));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerCloseListener")) {
				if (i.declaresMethodByName("onShareTargetSelected"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onShareTargetSelected")));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerOpenListener")) {
				if (i.declaresMethodByName("onDrawerOpened"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onDrawerOpened")));
			}
			else if (i.getName().equals("android.widget.SlidingDrawer$OnDrawerScrollListener")) {
				if (i.declaresMethodByName("onScrollEnded"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScrollEnded")));
				if (i.declaresMethodByName("onScrollStarted"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onScrollStarted")));
			}
			else if (i.getName().equals("android.widget.TabHost$OnTabChangeListener")) {
				if (i.declaresMethodByName("onTabChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTabChanged")));
			}
			else if (i.getName().equals("android.widget.TextView$OnEditorActionListener")) {
				if (i.declaresMethodByName("onEditorAction"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onEditorAction")));
			}
			else if (i.getName().equals("android.widget.TimePicker$OnTimeChangedListener")) {
				if (i.declaresMethodByName("onTimeChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onTimeChanged")));
			}
			else if (i.getName().equals("android.widget.ZoomButtonsController$OnZoomListener")) {
				if (i.declaresMethodByName("onVisibilityChanged"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onVisibilityChanged")));
				if (i.declaresMethodByName("onZoom"))
					callbackMethods.add(new AndroidMethod(getMethodFromHierarchy(baseClass, "onZoom")));
			}
		}
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
		
}

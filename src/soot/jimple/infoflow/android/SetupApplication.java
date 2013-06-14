package soot.jimple.infoflow.android;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointCreator;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.options.Options;

public class SetupApplication {

	private Set<AndroidMethod> sinks = new HashSet<AndroidMethod>();
	private Set<AndroidMethod> sources = new HashSet<AndroidMethod>();
	private Map<String, Set<AndroidMethod>> callbackMethods = new HashMap<String, Set<AndroidMethod>>(10000);
	
	private Set<String> entrypoints = null;
	
	private Map<Integer, LayoutControl> layoutControls;
	private List<ARSCFileParser.ResPackage> resourcePackages = null;
	private String appPackageName = "";
	
	private String androidJar;
	private String apkFileLocation;
	private String taintWrapperFile;

	public SetupApplication(){
		
	}
	
	public SetupApplication(String androidJar, String apkFileLocation) {
		this.androidJar = androidJar;
		this.apkFileLocation = apkFileLocation;
	}

	public void printSinks(){
		System.out.println("Sinks:");
		for (AndroidMethod am : sinks) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sinks");
	}

	
	public void printSources(){
		System.out.println("Sources:");
		for (AndroidMethod am : sources) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sources");
	}

	public void printEntrypoints(){
		if (this.entrypoints == null)
			System.out.println("Entry points not initialized");
		else {
			System.out.println("Classes containing entry points:");
			for (String className : entrypoints)
				System.out.println("\t" + className);
			System.out.println("End of Entrypoints");
		}
	}

	public void setAndroidJar(String androidJar) {
		this.androidJar = androidJar;
	}

	public void setApkFileLocation(String apkFileLocation) {
		this.apkFileLocation = apkFileLocation;
	}
	
	public void setTaintWrapperFile(String taintWrapperFile) {
		this.taintWrapperFile = taintWrapperFile;
	}

	public void calculateSourcesSinksEntrypoints
			(String sourceSinkFile) throws IOException {
		ProcessManifest processMan = new ProcessManifest();

		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		processMan.loadManifestFile(apkFileLocation);
		this.appPackageName = processMan.getPackageName();
		this.entrypoints = processMan.getEntryPointClasses();

		// Parse the resource file
		ARSCFileParser resParser = new ARSCFileParser();
		resParser.parse(apkFileLocation);
		this.resourcePackages = resParser.getPackages();

		AnalyzeJimpleClass jimpleClass = null;
		LayoutFileParser lfp = new LayoutFileParser(this.appPackageName);
		
		boolean hasChanged = true;
		while (hasChanged) {
			hasChanged = false;
			soot.G.reset();
			initializeSoot();
	
			if (jimpleClass == null) {
				// Collect the callback interfaces implemented in the app's source code
				jimpleClass = new AnalyzeJimpleClass(entrypoints);
				jimpleClass.collectCallbackMethods();

				// Find the user-defined sources in the layout XML files
				lfp.parseLayoutFile(apkFileLocation, entrypoints);
			}
			else
				jimpleClass.collectCallbackMethodsIncremental();
			
			
			// Run the soot-based operations
			PackManager.v().runPacks();
			
			// Collect the results of the soot-based phases
			for (Entry<String, Set<AndroidMethod>> entry : jimpleClass.getCallbackMethods().entrySet()) {
				if (this.callbackMethods.containsKey(entry.getKey())) {
					if (this.callbackMethods.get(entry.getKey()).addAll(entry.getValue()))
						hasChanged = true;
				}
				else {
					this.callbackMethods.put(entry.getKey(), new HashSet<AndroidMethod>(entry.getValue()));
					hasChanged = true;
				}
			}
			this.layoutControls = lfp.getUserControls();
		}
		
		// Collect the XML-based callback methods
		for (Entry<SootClass, Set<Integer>> lcentry : jimpleClass.getLayoutClasses().entrySet())
			for (Integer classId : lcentry.getValue()) {
				AbstractResource resource = resParser.findResource(classId);
				if (resource instanceof StringResource) {
					StringResource strRes = (StringResource) resource;
					if (lfp.getCallbackMethods().containsKey(strRes.getValue()))
						for (String methodName : lfp.getCallbackMethods().get(strRes.getValue())) {
							Set<AndroidMethod> methods = this.callbackMethods.get(lcentry.getKey().getName());
							if (methods == null) {
								methods = new HashSet<AndroidMethod>();
								this.callbackMethods.put(lcentry.getKey().getName(), methods);
							}
							
							// The callback may be declared directly in the class
							// or in one of the superclasses
							SootMethod callbackMethod = null;
							SootClass callbackClass = lcentry.getKey();
							while (callbackMethod == null) {
								if (callbackClass.declaresMethodByName(methodName))
									callbackMethod = callbackClass.getMethodByName(methodName);
								if (callbackClass.hasSuperclass())
									callbackClass = callbackClass.getSuperclass();
								else
									break;
							}
							if (callbackMethod == null) {
								System.err.println("Callback method " + methodName + " not found in class "
										+ lcentry.getKey().getName());
								continue;
							}
							methods.add(new AndroidMethod(callbackMethod));
						}
				}
				else
					System.err.println("Unexpected resource type for layout class");
			}
		Set<AndroidMethod> callbacksPlain = new HashSet<AndroidMethod>();
		for (Set<AndroidMethod> set : this.callbackMethods.values())
			callbacksPlain.addAll(set);
		System.out.println("Found " + callbacksPlain.size() + " callback methods for "
				+ this.callbackMethods.size() + " components");

		PermissionMethodParser parser = PermissionMethodParser.fromFile(sourceSinkFile);
		for (AndroidMethod am : parser.parse()){
			if (am.isSource())
				sources.add(am);
			if(am.isSink())
				sinks.add(am);
		}
		
		//add sink for Intents:
		AndroidMethod setResult = new AndroidMethod(SootMethodRepresentationParser.v().parseSootMethodString
				("<android.app.Activity: void startActivity(android.content.Intent)>"));
		setResult.setSink(true);
		sinks.add(setResult);
		
		System.out.println("Entry point calculation done.");
	}

	/**
	 * Initializes soot for running the soot-based phases of the application
	 * metadata analysis
	 * @return The entry point used for running soot
	 */
	private SootMethod initializeSoot() {
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_soot_classpath(apkFileLocation + File.pathSeparator
				+ Scene.v().getAndroidJarPath(androidJar, apkFileLocation));
		Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Arrays.asList(this.entrypoints.toArray()));
		Options.v().set_app(true);
		Main.v().autoSetOptions();

		Scene.v().loadNecessaryClasses();
		
		for (String className : this.entrypoints) {
			SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
			c.setApplicationClass();	
		}

		SootMethod entryPoint = getEntryPointCreator().createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		return entryPoint;
	}

	/**
	 * Runs the data flow analysis
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow(){
		System.out.println("Running data flow analysis on " + apkFileLocation + " with "
				+ sources.size() + " sources and " + sinks.size() + " sinks...");
		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow(androidJar, false);
		String path = apkFileLocation + File.pathSeparator + Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		
		try {
			if (this.taintWrapperFile != null && !this.taintWrapperFile.isEmpty())
				info.setTaintWrapper(new EasyTaintWrapper(new File(this.taintWrapperFile)));
			info.setSootConfig(new SootConfigForAndroid());
			
			Set<AndroidMethod> callbacks = new HashSet<AndroidMethod>();
			for (Set<AndroidMethod> methods : this.callbackMethods.values())
				callbacks.addAll(methods);
			
			AndroidSourceSinkManager sourceSinkManager = new AndroidSourceSinkManager
				(sources, sinks, callbacks, false,
				LayoutMatchingMode.MatchSensitiveOnly, layoutControls);
			sourceSinkManager.setAppPackageName(this.appPackageName);
			sourceSinkManager.setResourcePackages(this.resourcePackages);
			
			AndroidEntryPointCreator entryPointCreator = getEntryPointCreator();
			
			System.out.println("Starting infoflow computation...");
			info.computeInfoflow(path, entryPointCreator, new ArrayList<String>(),
					sourceSinkManager);
			return info.getResults();
		}
		catch (IOException ex) {
			throw new RuntimeException("Error processing taint wrapper file", ex);
		}
	}

	private AndroidEntryPointCreator getEntryPointCreator() {
		AndroidEntryPointCreator entryPointCreator = new AndroidEntryPointCreator
			(new ArrayList<String>(this.entrypoints));
		Map<String, List<String>> callbackMethodSigs = new HashMap<String, List<String>>();
		for (String className : this.callbackMethods.keySet()) {
			List<String> methodSigs = new ArrayList<String>();
			callbackMethodSigs.put(className, methodSigs);
			for (AndroidMethod am : this.callbackMethods.get(className))
				methodSigs.add(am.getSignature());
		}
		entryPointCreator.setCallbackFunctions(callbackMethodSigs);
		return entryPointCreator;
	}
	
}

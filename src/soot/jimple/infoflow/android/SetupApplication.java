/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
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
import soot.jimple.infoflow.AbstractInfoflowProblem.PathTrackingMethod;
import soot.jimple.infoflow.Infoflow;
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
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.options.Options;

public class SetupApplication {

	private Set<AndroidMethod> sinks = null;
	private Set<AndroidMethod> sources = null;
	private final Map<String, Set<AndroidMethod>> callbackMethods = new HashMap<String, Set<AndroidMethod>>(10000);
	
	private PathTrackingMethod pathTracking = PathTrackingMethod.NoTracking;
	private boolean enableImplicitFlows = false;

	private Set<String> entrypoints = null;
	
	private Map<Integer, LayoutControl> layoutControls;
	private List<ARSCFileParser.ResPackage> resourcePackages = null;
	private String appPackageName = "";
	
	private final String androidJar;
	private final String apkFileLocation;
	private String taintWrapperFile;
	
	private AndroidSourceSinkManager sourceSinkManager = null;
	private AndroidEntryPointCreator entryPointCreator = null;
	
	public SetupApplication(String androidJar, String apkFileLocation) {
		this.androidJar = androidJar;
		this.apkFileLocation = apkFileLocation;
	}
	
	/**
	 * Gets the set of sinks loaded into FlowDroid
	 * @return The set of sinks loaded into FlowDroid
	 */
	public Set<AndroidMethod> getSinks() {
		return sinks;
	}

	/**
	 * Prints the list of sinks registered with FlowDroud to stdout
	 */
	public void printSinks(){
		if (sinks == null) {
			System.err.println("Sinks not calculated yet");
			return;
		}
		System.out.println("Sinks:");
		for (AndroidMethod am : sinks) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sinks");
	}
	
	/**
	 * Gets the set of sources loaded into FlowDroid
	 * @return The set of sources loaded into FlowDroid
	 */
	public Set<AndroidMethod> getSources() {
		return sources;
	}

	/**
	 * Prints the list of sources registered with FlowDroud to stdout
	 */
	public void printSources(){
		if (sources == null) {
			System.err.println("Sources not calculated yet");
			return;
		}
		System.out.println("Sources:");
		for (AndroidMethod am : sources) {
			System.out.println(am.toString());
		}
		System.out.println("End of Sources");
	}

	/**
	 * Prints list of classes containing entry points to stdout
	 */
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

	/**
	 * Sets the file from which to load taint wrapper information. If this value
	 * is null or empty, no taint wrapping is used.
	 * @param taintWrapperFile The taint wrapper file to use or null to disable
	 * taint wrapping
	 */
	public void setTaintWrapperFile(String taintWrapperFile) {
		this.taintWrapperFile = taintWrapperFile;
	}

	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods
	 * for the given APK file.
	 * @param sourceSinkFile The full path and file name of the file containing
	 * the sources and sinks
	 * @throws IOException Thrown if the given source/sink file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints
			(String sourceSinkFile) throws IOException {
		PermissionMethodParser parser = PermissionMethodParser.fromFile(sourceSinkFile);
		Set<AndroidMethod> sources = new HashSet<AndroidMethod>();
		Set<AndroidMethod> sinks = new HashSet<AndroidMethod>();
		for (AndroidMethod am : parser.parse()){
			if (am.isSource())
				sources.add(am);
			if(am.isSink())
				sinks.add(am);
		}
		calculateSourcesSinksEntrypoints(sources, sinks);
	}
	
	/**
	 * Calculates the sets of sources, sinks, entry points, and callbacks methods
	 * for the given APK file.
	 * @param sourceMethods The set of methods to be considered as sources
	 * @param sinkMethods The set of methods to be considered as sinks
	 * @throws IOException Thrown if the given source/sink file could not be read.
	 */
	public void calculateSourcesSinksEntrypoints
			(Set<AndroidMethod> sourceMethods,
			Set<AndroidMethod> sinkMethods) throws IOException {
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
		LayoutFileParser lfp = new LayoutFileParser(this.appPackageName, resParser);
		
		boolean hasChanged = true;
		while (hasChanged) {
			hasChanged = false;
			soot.G.reset();
			initializeSoot();
	
			if (jimpleClass == null) {
				// Collect the callback interfaces implemented in the app's source code
				jimpleClass = new AnalyzeJimpleClass(entrypoints);
				jimpleClass.collectCallbackMethods();

				// Find the user-defined sources in the layout XML files. This
				// only needs to be done once, but is a Soot phase.
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
								if (callbackClass.declaresMethodByName(methodName)) {
									String subSig = "void " + methodName + "(android.view.View)";
									for (SootMethod sm : callbackClass.getMethods())
										if (sm.getSubSignature().equals(subSig)) {
											callbackMethod = sm;
											break;
										}
								}
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
		
		// Add the callback methods as sources and sinks
		{
			Set<AndroidMethod> callbacksPlain = new HashSet<AndroidMethod>();
			for (Set<AndroidMethod> set : this.callbackMethods.values())
				callbacksPlain.addAll(set);
			System.out.println("Found " + callbacksPlain.size() + " callback methods for "
					+ this.callbackMethods.size() + " components");
	
			sources = new HashSet<AndroidMethod>(sourceMethods);
			sinks = new HashSet<AndroidMethod>(sinkMethods);
		}
		
		//add sink for Intents:
		{
			AndroidMethod setResult = new AndroidMethod(SootMethodRepresentationParser.v().parseSootMethodString
					("<android.app.Activity: void startActivity(android.content.Intent)>"));
			setResult.setSink(true);
			sinks.add(setResult);
		}
		
		System.out.println("Entry point calculation done.");
		
		// Clean up everything we no longer need
		soot.G.reset();
		
		// Create the SourceSinkManager
		{
			Set<AndroidMethod> callbacks = new HashSet<AndroidMethod>();
			for (Set<AndroidMethod> methods : this.callbackMethods.values())
				callbacks.addAll(methods);
			
			sourceSinkManager = new AndroidSourceSinkManager
					(sources, sinks, callbacks, false,
					LayoutMatchingMode.MatchSensitiveOnly, layoutControls);
			sourceSinkManager.setAppPackageName(this.appPackageName);
			sourceSinkManager.setResourcePackages(this.resourcePackages);
		}
		
		entryPointCreator = createEntryPointCreator();
	}
	
	/**
	 * Gets the source/sink manager constructed for FlowDroid. Make sure to call
	 * calculateSourcesSinksEntryPoints() first, or you will get a null result.
	 * @return FlowDroid's source/sink manager
	 */
	public AndroidSourceSinkManager getSourceSinkManager() {
		return sourceSinkManager;
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

		// Always update the entry point creator to reflect the newest set
		// of callback methods
		SootMethod entryPoint = createEntryPointCreator().createDummyMain();
		Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
		return entryPoint;
	}

	/**
	 * Runs the data flow analysis
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow(){
		return runInfoflow(null);
	}
	
	/**
	 * Runs the data flow analysis. Make sure to populate the sets of sources,
	 * sinks, and entry points first.
	 * @param onResultsAvailable The callback to be invoked when data flow
	 * results are available
	 * @return The results of the data flow analysis
	 */
	public InfoflowResults runInfoflow(ResultsAvailableHandler onResultsAvailable){
		if (sources == null || sinks == null)
			throw new RuntimeException("Sources and/or sinks not calculated yet");

		System.out.println("Running data flow analysis on " + apkFileLocation + " with "
				+ sources.size() + " sources and " + sinks.size() + " sinks...");
		Infoflow info = new Infoflow(androidJar, false);
		String path = apkFileLocation + File.pathSeparator + Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		
		try {
			if (this.taintWrapperFile != null && !this.taintWrapperFile.isEmpty())
				info.setTaintWrapper(new EasyTaintWrapper(new File(this.taintWrapperFile)));
			info.setSootConfig(new SootConfigForAndroid());
			if (onResultsAvailable != null)
				info.addResultsAvailableHandler(onResultsAvailable);
						
			System.out.println("Starting infoflow computation...");
			info.setPathTracking(pathTracking);
			info.setEnableImplicitFlows(enableImplicitFlows);
			info.setInspectSinks(false);
			info.computeInfoflow(path, entryPointCreator, new ArrayList<String>(),
					sourceSinkManager);
			return info.getResults();
		}
		catch (IOException ex) {
			throw new RuntimeException("Error processing taint wrapper file", ex);
		}
	}

	private AndroidEntryPointCreator createEntryPointCreator() {
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
	
	/**
	 * Gets the entry point creator used for generating the dummy main method
	 * emulating the Android lifecycle and the callbacks. Make sure to call
	 * calculateSourcesSinksEntryPoints() first, or you will get a null result.
	 * @return The entry point creator
	 */
	public AndroidEntryPointCreator getEntryPointCreator() {
		return entryPointCreator;
	}
	
	/**
	 * Sets whether and how data leakage paths through the application shall be
	 * tracked
	 * @param method The mode for tracking data leakage paths through the
	 * application
	 */
	public void setPathTracking(PathTrackingMethod method) {
		this.pathTracking = method;
	}
	
	/**
	 * Sets whether implicit flow tracking shall be enabled. While this allows
	 * control flow-based leaks to be found, it can severly affect performance
	 * and lead to an increased number of false positives.
	 * @param enableImplicitFlows True if implicit flow tracking shall be enabled,
	 * otherwise false
	 */
	public void setEnableImplicitFlows(boolean enableImplicitFlows) {
		this.enableImplicitFlows = enableImplicitFlows;
	}

}

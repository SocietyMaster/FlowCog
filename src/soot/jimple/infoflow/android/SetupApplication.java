package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import soot.jimple.infoflow.android.resources.LayoutControl;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.AndroidEntryPointCreator;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;
import soot.options.Options;

public class SetupApplication {

	private List<AndroidMethod> sinks = new ArrayList<AndroidMethod>();
	private List<AndroidMethod> sources = new ArrayList<AndroidMethod>();
	private List<String> entrypoints = new ArrayList<String>();
	private Map<Integer, LayoutControl> layoutControls;
	
	private String androidJar;
	private String apkFileLocation;
	private String matrixFileLocation;
	private String entryPointsFile;
	private String taintWrapperFile;

	public SetupApplication(){
		
	}
	
	public SetupApplication(String androidJar, String apkFileLocation,
			String matrixFileLocation, String entryPointsFile) {
		this.androidJar = androidJar;
		this.apkFileLocation = apkFileLocation;
		this.matrixFileLocation = matrixFileLocation;
		this.entryPointsFile = entryPointsFile;
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
		System.out.println("Entrypoints:");
		for (String s : entrypoints) {
			System.out.println(s);
		}
		System.out.println("End of Entrypoints");
	}

	public void setAndroidJar(String androidJar) {
		this.androidJar = androidJar;
	}

	public void setApkFileLocation(String apkFileLocation) {
		this.apkFileLocation = apkFileLocation;
	}

	public void setMatrixFileLocation(String matrixFileLocation) {
		this.matrixFileLocation = matrixFileLocation;
	}
	
	public void setEntryPointsFile(String entryPointsFile) {
		this.entryPointsFile = entryPointsFile;
	}
	
	public void setTaintWrapperFile(String taintWrapperFile) {
		this.taintWrapperFile = taintWrapperFile;
	}

	public void calculateSourcesSinksEntrypoints() throws IOException {
		ProcessManifest processMan = new ProcessManifest();

		// To look for callbacks, we need to start somewhere. We use the Android
		// lifecycle methods for this purpose.
		List<String> entryPointMethods = readTextFile(this.entryPointsFile);
		processMan.loadManifestFile(apkFileLocation);
		List<String> baseEntryPoints = processMan.getAndroidAppEntryPoints(entryPointMethods);
		entrypoints.addAll(baseEntryPoints);

		SootMethodRepresentationParser methodParser = new SootMethodRepresentationParser();
		HashMap<String, List<String>> entryPointClasses = methodParser.parseClassNames(baseEntryPoints, false);

		soot.G.reset();

		// Collect the callback interfaces implemented in the app's source code
		AnalyzeJimpleClass jimpleClass = new AnalyzeJimpleClass();
		jimpleClass.collectCallbackMethods();
		
		// Find the user-defined sources in the layout XML files
		LayoutFileParser lfp = new LayoutFileParser(processMan.getPackageName());
		lfp.parseLayoutFile(apkFileLocation, entryPointClasses.keySet());
		
		// Run the soot-based operations
		runSootBasedPhases(entryPointClasses);

		// Collect the results of the soot-based phases
		for (AndroidMethod am : jimpleClass.getCallbackMethods())
			entrypoints.add(am.getSignature());
		this.layoutControls = lfp.getUserControls();

		PermissionMethodParser parser = PermissionMethodParser.fromFile(matrixFileLocation);
		for (AndroidMethod am : parser.parse()){
			if (am.isSource())
				sources.add(am);
			if(am.isSink())
				sinks.add(am);
		}
	}
	
	/**
	 * Runs Soot and executes all analysis phases that have been registered so
	 * far.
	 * @param baseEntryPoints The basic entry points into the app
	 */
	private void runSootBasedPhases(HashMap<String, List<String>> baseEntryPoints) {
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_whole_program(true);
		Options.v().set_soot_classpath(apkFileLocation + File.pathSeparator
				+ Scene.v().getAndroidJarPath(androidJar, apkFileLocation));
		Options.v().set_android_jars(androidJar);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_process_dir(Arrays.asList(baseEntryPoints.keySet().toArray()));
		Options.v().set_app(true);
		Main.v().autoSetOptions();

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

	public InfoflowResults runInfoflow(){
		System.out.println("Running data flow analysis on " + apkFileLocation + " with "
				+ sources.size() + " sources and " + sinks.size() + " sinks...");
		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow(androidJar, false);
		String path = apkFileLocation + File.pathSeparator + Scene.v().getAndroidJarPath(androidJar, apkFileLocation);
		
		try {
			if (this.taintWrapperFile != null && !this.taintWrapperFile.isEmpty())
				info.setTaintWrapper(new EasyTaintWrapper(new File(this.taintWrapperFile)));
			info.setSootConfig(new SootConfigForAndroid());
			info.computeInfoflow(path, entrypoints, new AndroidSourceSinkManager
				(sources, sinks, false, LayoutMatchingMode.MatchSensitiveOnly, layoutControls));
			return info.getResults();
		}
		catch (IOException ex) {
			throw new RuntimeException("Error processing taint wrapper file", ex);
		}
	}
	
	private static List<String> readTextFile(String fileName) throws IOException {
		BufferedReader rdr = null;
		try {
			List<String> resList = new ArrayList<String>();
			rdr = new BufferedReader(new FileReader(fileName));
			String line;
			while ((line = rdr.readLine()) != null)
				resList.add(line);
			return resList;
		}
		finally {
			if (rdr != null)
				rdr.close();
		}
	}	

}

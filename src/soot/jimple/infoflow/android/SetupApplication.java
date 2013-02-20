package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

public class SetupApplication {

	private List<AndroidMethod> sinks = new ArrayList<AndroidMethod>();
	private List<AndroidMethod> sources = new ArrayList<AndroidMethod>();
	private List<String> entrypoints = new ArrayList<String>();
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
		processMan.getAndroidAppEntryPointsClassesList(apkFileLocation);
		List<String> baseEntryPoints = processMan.getAndroidAppEntryPoints(entryPointMethods);
		entrypoints.addAll(baseEntryPoints);

		SootMethodRepresentationParser methodParser = new SootMethodRepresentationParser();
		HashMap<String, List<String>> entryPointClasses = methodParser.parseClassNames(baseEntryPoints, false);

		// Collect the callback interfaces implemented in the app's source code
		AnalyzeJimpleClass jimpleClass = new AnalyzeJimpleClass(androidJar, apkFileLocation);
		jimpleClass.collectCallbackMethods(entryPointClasses);
		for (AndroidMethod am : jimpleClass.getCallbackMethods())
			entrypoints.add(am.getSignature());
		processMan.getAndroidAppEntryPointsClassesList(apkFileLocation);

		// 14.02.2013, SA: This does not work - conceptual and implementation issues
//		ReadXml readXml = new ReadXml();
//		readXml.generateAndroidAppPermissionMap(apkFileLocation);
//		entrypoints.addAll(readXml.getAdditionalEntryPoints(processMan.getAndroidClasses()));

		PermissionMethodParser parser = PermissionMethodParser.fromFile(matrixFileLocation);
		for (AndroidMethod am : parser.parse()){
			if (am.isSource())
				sources.add(am);
			if(am.isSink())
				sinks.add(am);
		}
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
			info.computeInfoflow(path, entrypoints, new AndroidSourceSinkManager(sources, sinks));
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

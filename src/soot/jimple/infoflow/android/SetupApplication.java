package soot.jimple.infoflow.android;

import java.io.IOException;
import java.util.List;

public class SetupApplication {

	private List<String> sinks, sources, entrypoints, mappedPermissionList;
	private String androidJar;
	private String jimpleFilesLocation;
	private String apkFileLocation;
	private String matrixFileLocation;
	private String sinkConfigFileLocation;
	private String sourceConfigFileLocation;
	
	public SetupApplication(){
		
	}
	
	public SetupApplication(String androidJar, String jimpleOutput,
			String apkFileLocation, String matrixFileLocation,
			String sinkConfigFileLocation, String sourceConfigFileLocation) {
		this.androidJar = androidJar;
		this.jimpleFilesLocation = jimpleOutput;
		this.apkFileLocation = apkFileLocation;
		this.matrixFileLocation = matrixFileLocation;
		this.sinkConfigFileLocation = sinkConfigFileLocation;
		this.sourceConfigFileLocation = sourceConfigFileLocation;
	}

	public void setSinkConfigFileLocation(String sinkConfigFileLocation) {
		this.sinkConfigFileLocation = sinkConfigFileLocation;
	}

	public void setSourceConfigFileLocation(String sourceConfigFileLocation) {
		this.sourceConfigFileLocation = sourceConfigFileLocation;
	}

	public List<String> getSinks() {
		return sinks;
	}
	
	public void printSinks(){
		System.out.println("Sinks:");
		for (String s : sinks) {
			System.out.println(s);
		}
		System.out.println("End of Sinks");
	}

	
	public void addSink(String sink){
		sinks.add(sink);
	}

	public List<String> getSources() {
		return sources;
	}

	
	public void addSource(String source){
		sources.add(source);
	}
	
	public void printSources(){
		System.out.println("Sources:");
		for (String s : sources) {
			System.out.println(s);
		}
		System.out.println("End of Sources");
	}

	public List<String> getEntrypoints() {
		return entrypoints;
	}

	public void setEntrypoints(List<String> entrypoints) {
		this.entrypoints = entrypoints;
	}
	
	public void addEntrypoint(String entrypoint){
		entrypoints.add(entrypoint);
	}
	
	public void printEntrypoints(){
		System.out.println("Entrypoints:");
		for (String s : entrypoints) {
			System.out.println(s);
		}
		System.out.println("End of Entrypoints");
	}

	public void printMappedPermissionList(){
		System.out.println("mappedPermissionList:");
		for (String s : mappedPermissionList) {
			System.out.println(s);
		}
		System.out.println("End of mappedPermissionList");
	}





	public void setJimpleOutput(String jimpleOutput) {
		this.jimpleFilesLocation = jimpleOutput;
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

	public void calculateSourcesSinksEntrypoints() throws IOException, wrongFormatedFileException{
		List<String> permissionList;
		
		ReadFile rf = new ReadFile();
		ProcessManifest processMan = new ProcessManifest();
		AnalyzeJimpleClass jimpleClass = new AnalyzeJimpleClass();
		AnalyzeConfigSourceSink analyzeConfig = new AnalyzeConfigSourceSink();
		
		jimpleClass.collectAndroidMethods(jimpleFilesLocation);
		entrypoints = jimpleClass.getEntryPoints();

		permissionList = processMan
				.getAndroidAppPermissionList(apkFileLocation);

		mappedPermissionList = rf.getMappedPermissionsOnlyComplete(
				matrixFileLocation, permissionList);
		
		sources = analyzeConfig.getReducedSourceSinkList(sourceConfigFileLocation,
				mappedPermissionList);

		sinks = analyzeConfig.getReducedSourceSinkList(
				sinkConfigFileLocation, mappedPermissionList);


		
	}
	
	public void runInfoflow(){
		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow();
		String path = jimpleFilesLocation + ";" + androidJar;

		info.computeInfoflow(path, entrypoints, sources, sinks);
		
	}

	

}

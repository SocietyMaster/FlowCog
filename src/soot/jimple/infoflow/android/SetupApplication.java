package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SetupApplication {

	private List<String> sinks, sources, entrypoints, mappedPermissionList;
	private String androidJar;
	private String jimpleFilesLocation;
	private String apkFileLocation;
	private String matrixFileLocation;
	@Deprecated
	private String sinkConfigFileLocation;
	@Deprecated
	private String sourceConfigFileLocation;
	
	public SetupApplication(){
		
	}
	
	public SetupApplication(String androidJar, String jimpleFilesLocation,
			String apkFileLocation, String matrixFileLocation) {
		this.androidJar = androidJar;
		this.jimpleFilesLocation = jimpleFilesLocation;
		this.apkFileLocation = apkFileLocation;
		this.matrixFileLocation = matrixFileLocation;
	}
	
	@Deprecated
	public SetupApplication(String androidJar, String jimpleFilesLocation,
			String apkFileLocation, String matrixFileLocation,
			String sinkConfigFileLocation, String sourceConfigFileLocation) {
		this.androidJar = androidJar;
		this.jimpleFilesLocation = jimpleFilesLocation;
		this.apkFileLocation = apkFileLocation;
		this.matrixFileLocation = matrixFileLocation;
		this.sinkConfigFileLocation = sinkConfigFileLocation;
		this.sourceConfigFileLocation = sourceConfigFileLocation;
	}
	
	@Deprecated
	public void setSinkConfigFileLocation(String sinkConfigFileLocation) {
		this.sinkConfigFileLocation = sinkConfigFileLocation;
	}
	
	@Deprecated
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





	public void setJimpleFilesLocation(String jimpleFilesLocation) {
		this.jimpleFilesLocation = jimpleFilesLocation;
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
		AnalyzeJimpleClass jimpleClass = new AnalyzeJimpleClass(jimpleFilesLocation, androidJar, apkFileLocation );
		
		
		jimpleClass.collectAndroidMethods();
		entrypoints = jimpleClass.getEntryPoints();
		ReadXml readXml = new ReadXml();
		readXml.generateAndroidAppPermissionMap(apkFileLocation);
		entrypoints.addAll(readXml.getAdditionalEntryPoints(processMan.getAndroidClasses()));

		permissionList = processMan
				.getAndroidAppPermissionList(apkFileLocation);

		mappedPermissionList = rf.getMappedPermissionsOnlyCompleteBayes(
				matrixFileLocation, permissionList);
//		System.out.println("MappedPermissionListBeginn");
		String sourceSinkNone;
		sources = new ArrayList<String>();
		sinks = new ArrayList<String>();
		
		boolean isDefined = false;
		if(mappedPermissionList.get(0).lastIndexOf("->")!=-1){
			isDefined = true;
		}
		
		if(isDefined){
			for (String m : mappedPermissionList){
				sourceSinkNone = m.substring(m.lastIndexOf("->") + 2).trim();
				if(sourceSinkNone.matches(".*_SOURCE_.*")){
					sources.add(m.substring(0,m.lastIndexOf("->")).trim());
				}
				if(sourceSinkNone.matches(".*_SINK_.*")){
					sinks.add(m.substring(0,m.lastIndexOf("->")).trim());
				}
				
				
			}
		}
		else{
			AnalyzeConfigSourceSink analyzeConfig = new AnalyzeConfigSourceSink();
			sources = analyzeConfig.getReducedSourceSinkList(sourceConfigFileLocation,
					mappedPermissionList);
	
			sinks = analyzeConfig.getReducedSourceSinkList(
					sinkConfigFileLocation, mappedPermissionList);
		}


		
	}
	
	public void runInfoflow(){
		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow();
		String path = jimpleFilesLocation + ";" + androidJar;

		info.computeInfoflow(path, entrypoints, sources, sinks);
		
	}
	
	public void generateJimpleFiles(String commandLine) throws IOException, InterruptedException{
		System.out.println("Generating the Jimple Files");
		Process proc = Runtime.getRuntime().exec(commandLine);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				proc.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null) {
			
			System.out.println(line);
		}
		proc.waitFor();
	}

	

}

package soot.jimple.infoflow.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AndroidMain {

	/**
	 * @param args
	 * @throws IOException
	 * @throws wrongFormatedFileException
	 */
	public static void main(String[] args) throws IOException,
			wrongFormatedFileException {
		List<String> sinks, sources, entrypoints, permissionList, mappedPermissionList;

		String path = "C:/Users/Spikey/Desktop/soot-networkusage;C:/Users/Spikey/Desktop/platforms/android-14/android.jar";
		String apkFileLocation = "C:/Users/Spikey/Downloads/com.example.android.networkusage.apk";
		String matrixFileLocation = "android-api9.matrix";
		String entrypointsFileLocation = "entrypoints-someLines.txt";
		ReadFile rf = new ReadFile();

		// TODO Read independently of order the arguments
		if (args.length > 0) {
			path = args[0];
		}
		if (args.length > 1) {
			apkFileLocation = args[1];
		}
		if (args.length > 2) {
			matrixFileLocation = args[2];
		}
		if (args.length > 3) {
			entrypointsFileLocation = args[3];
		}

		ProcessManifest processMan = new ProcessManifest();

		List<SootLine> sootLineList = new ArrayList<SootLine>();

		sootLineList = rf.readFileSootLine(entrypointsFileLocation);
		// for(int i =0; i<sootLineList.size();i++){
		// sootLineList.get(i).println();
		// }
		processMan.getAndroidAppEntryPointsClassesList(apkFileLocation);
		processMan.getAndroidAppEntryPointsWithClass(sootLineList);
		// processMan.printClasses();
		entrypoints = processMan.getEntryPoints();

		//Only for test purposes
		entrypoints
				.add("<com.example.android.networkusage.NetworkActivity$NetworkReceiver:  void onReceive(android.content.Context, android.content.Intent)>");
		//Only for test purposes
		entrypoints = rf.readFile("entryPointsNetworkUsage.txt", "");
		
		// entrypoints = rf.readFile(entrypointsFileLocation, "");
		// //Test
		// processMan.getAndroidAppEntryPointsClassesList(apkFileLocation);
		//
		//
		//
		// entrypoints = processMan.getAndroidAppEntryPoints(entrypoints);
		//
//		for (String s : entrypoints) {
//			System.out.println(s);
//		}

		permissionList = processMan
				.getAndroidAppPermissionList(apkFileLocation);

		// System.out.println("Permissions:");
		// for (String s : sources) {
		// System.out.println(s);
		// }

		mappedPermissionList = rf.getMappedPermissionsOnlyComplete(
				matrixFileLocation, permissionList);
		// sinks = rf.readFile(matrixFileLocation, ">");
		// sources = rf.readFileSources(matrixFileLocation);

		// TODO reduce sources and sinks
		AnalyzeConfigSourceSink analyzeConfig = new AnalyzeConfigSourceSink();

		sources = analyzeConfig.getReducedSourceSinkList("SourceConfig.txt",
				mappedPermissionList);

		sinks = analyzeConfig.getReducedSourceSinkList("SinkConfig.txt",
				mappedPermissionList);

		// System.out.println("sinks:");
		// for (String s : mappedPermissionList) {
		// System.out.println(s);
		// }
		// System.out.println("Sources:");
		// for (String s : sources) {
		// System.out.println(s);
		// }
		 System.out.println("Sinks:");
		 for (String s : sinks) {
		 System.out.println(s);
		 }

		// WriteProcessedMatrix wpm = new WriteProcessedMatrix();
		// wpm.toBeProccessedByExcel("android-api9.matrix",
		// "proccessed-android-api9.matrix");

		// If a new API is published, you have to update this file manually.
		// Check the Android reference like
		// "http://developer.android.com/reference/android/app/Activity.html"
		// TODO write it automatically

		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow();

		info.computeInfoflow(path, entrypoints, sources, sinks);

	}

}

package soot.jimple.infoflow.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AndroidMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<String> sinks, sources, entrypoints, permissionList, mappedPermissionList;

		String path = "C:/Users/Spikey/Desktop/test-dex";
		String apkFileLocation = "C:/Users/Spikey/Downloads/com.shazam.android-76101.apk";
		String matrixFileLocation = "android-api9.matrix";
		String entrypointsFileLocation = "entrypoints-oneLine.txt";

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
		permissionList = processMan
				.getAndroidAppPermissionList(apkFileLocation);

		// System.out.println("Permissions:");
		// for (String s : permissionList) {
		// System.out.println(s);
		// }

		ReadFile rf = new ReadFile();
		mappedPermissionList = rf.getMappedPermissionsOnlyComplete(
				matrixFileLocation, permissionList);
		// sinks = rf.readFile(matrixFileLocation, ">");
		// sources = rf.readFileSources(matrixFileLocation);

		// TODO reduce sources and sinks
		sinks = mappedPermissionList;
		sources = mappedPermissionList;

		// System.out.println("sinks:");
		// for (String s : mappedPermissionList) {
		// System.out.println(s);
		// }
		// System.out.println("Sources:");
		// for (String s : sources) {
		// System.out.println(s);
		// }
		// System.out.println("Sinks:");
		// for (String s : sinks) {
		// System.out.println(s);
		// }

		// WriteProcessedMatrix wpm = new WriteProcessedMatrix();
		// wpm.toBeProccessedByExcel("android-api9.matrix",
		// "proccessed-android-api9.matrix");

		// If a new API is published, you have to update this file manually.
		// Check the Android reference like
		// "http://developer.android.com/reference/android/app/Activity.html"
		// TODO write it automatically

		entrypoints = rf.readFile(entrypointsFileLocation, "");

		soot.jimple.infoflow.Infoflow info = new soot.jimple.infoflow.Infoflow();

		info.computeInfoflow(path, entrypoints, sources, sinks);

	}

}

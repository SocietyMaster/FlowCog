package soot.jimple.infoflow.android.TestApps;

import java.io.File;
import java.io.IOException;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.SetupApplication;

public class Test {
	
	static SetupApplication app = new SetupApplication();
	static String command;
	static boolean generate = false;
	
	private static boolean DEBUG = false;


	/**
	 * @param args[0] = path to apk-file
	 * @param args[1] = path to android-jar (path/android-platforms/android-XX/android.jar)
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length !=2) {
			System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");	
			return;
		}
		//start with cleanup:
		File outputDir = new File("JimpleOutput");
		if(outputDir.isDirectory()){
			boolean success = true;
			for(File f : outputDir.listFiles()){
				success = success && f.delete();
			}
			if(!success){
				System.err.println("Cleanup of output directory "+ outputDir + " failed!");
			}
		}
		app.setApkFileLocation(args[0]);
		app.setAndroidJar(args[1]);
		app.setMatrixFileLocation("SourcesAndSinks.txt");
		app.setEntryPointsFile("entrypoints-someLines.txt");
		
		app.calculateSourcesSinksEntrypoints();
		
		if (DEBUG) {
			app.printEntrypoints();
			app.printSinks();
			app.printSources();
		}
		
		InfoflowResults results = app.runInfoflow();
		if (results == null)
			System.out.println("No results found.");
		else
			results.printResults();
	}

}

package soot.jimple.infoflow.android.TestApps;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.SetupApplication;

public class Test {
	
	static SetupApplication app = new SetupApplication();
	static String command;
	static boolean generate = false;
	
	private static boolean DEBUG = false;


	/**
	 * @param args[0] = path to apk-file
	 * @param args[1] = path to android-dir (path/android-platforms/)
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length <2) {
			System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");	
			return;
		}
		//start with cleanup:
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()){
			boolean success = true;
			for(File f : outputDir.listFiles()){
				success = success && f.delete();
			}
			if(!success){
				System.err.println("Cleanup of output directory "+ outputDir + " failed!");
			}
		}
		
		List<String> apkFiles = new ArrayList<String>();
		File apkFile = new File(args[0]);
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {
					
				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}
				
			});
			for (String s : dirFiles)
				apkFiles.add(args[0] + File.separator + s);
		}
		else
			apkFiles.add(args[0]);

		for (String fileName : apkFiles) {
			long beforeRun = System.nanoTime();
			System.out.println("Analyzing file " + fileName + "...");
			app.setApkFileLocation(fileName);
			app.setAndroidJar(args[1]);
			app.setTaintWrapperFile("../soot-infoflow/EasyTaintWrapperSource.txt");
			
			app.calculateSourcesSinksEntrypoints("entrypoints-someLines.txt", "SourcesAndSinks.txt");
			
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
			System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
		}
	}

}

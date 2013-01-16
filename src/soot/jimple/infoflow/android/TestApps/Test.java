package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;

import soot.Scene;
import soot.jimple.infoflow.android.JimpleBuilder;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.wrongFormatedFileException;

public class Test {
	
	static SetupApplication app = new SetupApplication();
	static String command;
	static boolean generate = false;


	/**
	 * @param args[0] = path to apk-file
	 * @param args[1] = path to android-jar (path/android-platforms/android-XX/android.jar)
	 */
	public static void main(String[] args) throws IOException, wrongFormatedFileException, InterruptedException {
		if (args.length !=2) {
			System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");	
			return;
		}
		app.setApkFileLocation(args[0]);
		
		app.setAndroidJar(Scene.v().getAndroidJarPath(args[1], args[0]));
		app.setJimpleFilesLocation("JimpleOutput");
		app.setMatrixFileLocation("android-api9.matrix");
		app.setSinkConfigFileLocation("SinkConfig.txt");
		app.setSourceConfigFileLocation("SourceConfig.txt");
			
		JimpleBuilder jB = new JimpleBuilder();
		jB.buildJimple(args[1], args[0]);
		
		app.calculateSourcesSinksEntrypoints();
		
		app.printEntrypoints();
		app.printSinks();
		app.printSources();
		
		
		app.runInfoflow();

	}

}

package soot.jimple.infoflow.android.test.droidBench;

import java.io.File;
import java.io.IOException;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.SetupApplication;

public class JUnitTests {
	
	public InfoflowResults analyzeAPKFile(String fileName) throws IOException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);

		String droidBenchDir = System.getenv("DROIDBENCH");
		if (droidBenchDir == null)
			droidBenchDir = System.getProperty("DROIDBENCH");
		if (droidBenchDir == null)
			throw new RuntimeException("DroidBench dir not set");		
		System.out.println("Loading DroidBench from " + droidBenchDir);
		
		SetupApplication setupApplication = new SetupApplication(androidJars,
				droidBenchDir + File.separator + fileName);
		setupApplication.setTaintWrapperFile("EasyTaintWrapperSource.txt");
		setupApplication.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		return setupApplication.runInfoflow();
	}

}

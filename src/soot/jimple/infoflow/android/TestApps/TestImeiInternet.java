package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;

import soot.jimple.infoflow.android.ReadFile;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.wrongFormatedFileException;

public class TestImeiInternet {
	static SetupApplication app = new SetupApplication();
	/**
	 * @param args
	 * @throws wrongFormatedFileException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException, wrongFormatedFileException {
		if (args.length == 6) {
			app.setAndroidJar(args[0]);
			app.setJimpleOutput(args[1]);
			app.setApkFileLocation(args[2]);
			app.setMatrixFileLocation(args[3]);
			app.setSinkConfigFileLocation(args[4]);
			app.setSourceConfigFileLocation(args[5]);
		}
		else{
		
			app.setAndroidJar("C:/Users/Spikey/Desktop/platforms/android-14/android.jar");
			app.setApkFileLocation("C:/Users/Spikey/workspace/IMEI-sender/bin/IMEI-sender.apk");
			app.setJimpleOutput("C:/Users/Spikey/Desktop/soot-networkusage");
			app.setMatrixFileLocation("android-api9.matrix");
			app.setSinkConfigFileLocation("SinkConfig.txt");
			app.setSourceConfigFileLocation("SourceConfig.txt");
		}
		
		app.calculateSourcesSinksEntrypoints();
		
		app.printEntrypoints();
		app.printSinks();
		app.printSources();
		
		//only for test purposes
//		ReadFile rf= new ReadFile();
//		app.setEntrypoints(rf.readFile("entryPointsNetworkUsage.txt", ""));
		
		app.runInfoflow();
		
		
	}

}

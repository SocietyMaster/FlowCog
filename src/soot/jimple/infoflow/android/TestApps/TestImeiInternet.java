package soot.jimple.infoflow.android.TestApps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.wrongFormatedFileException;

public class TestImeiInternet {
	static SetupApplication app = new SetupApplication();
	static String command;
	static boolean generate = false;

	/**
	 * @param args
	 * @throws wrongFormatedFileException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			wrongFormatedFileException, InterruptedException {
		if (args.length == 6) {
			app.setAndroidJar(args[0]);
			app.setJimpleOutput(args[1]);
			app.setApkFileLocation(args[2]);
			app.setMatrixFileLocation(args[3]);
			app.setSinkConfigFileLocation(args[4]);
			app.setSourceConfigFileLocation(args[5]);
		} else if (args.length == 7) {
			app.setAndroidJar(args[0]);
			app.setJimpleOutput(args[1]);
			app.setApkFileLocation(args[2]);
			app.setMatrixFileLocation(args[3]);
			app.setSinkConfigFileLocation(args[4]);
			app.setSourceConfigFileLocation(args[5]);
			command = args[6];
			generate = true;
		} else {
			
			String jimpleOutput = "C:/Users/Spikey/Desktop/soot-networkusage";
			String apkFileLocation = "C:/Users/Spikey/workspace/IMEI-sender/bin/IMEI-sender.apk";
			app.setAndroidJar("C:/Users/Spikey/Desktop/platforms/android-14/android.jar");
			app.setApkFileLocation(apkFileLocation);
			app.setJimpleOutput(jimpleOutput);
			app.setMatrixFileLocation("android-api9.matrix");
			app.setSinkConfigFileLocation("SinkConfig.txt");
			app.setSourceConfigFileLocation("SourceConfig.txt");
			//The command line to execute soot-dex to transform apk file to jimple files till -d
			command = "cmd /C C:/glassfish3/jdk/jre/bin/javaw.exe -Xmx1024m -Dfile.encoding=Cp1252 -classpath C:/Users/Spikey/workspace/soot-dex/bin;C:/Users/Spikey/workspace/soot/classes;C:/Users/Spikey/workspace/jasmin/classes;C:/Users/Spikey/workspace/jasmin/libs/java_cup.jar;C:/Users/Spikey/workspace/soot/libs/polyglot.jar;C:/Users/Spikey/workspace/soot/libs/AXMLPrinter2.jar;C:/Users/Spikey/workspace/heros/bin;C:/Users/Spikey/workspace/heros/guava-13.0.jar;C:/Users/Spikey/workspace/soot-dex/baksmali-1.3.2.jar soot.Main -android-jars C:/Users/Spikey/Desktop/platforms -d";
			String middleLine = " -allow-phantom-refs -src-prec apk -process-dir ";
			String endLine = " -output-format jimple";
			command = command.concat(" ").concat(jimpleOutput).concat(middleLine).concat(apkFileLocation).concat(endLine);
			generate = false; //set to true after you have entered your correct command line
		}

		if (generate) {
			System.out.println("Generating the Jimple Files");
			Process proc = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			while ((reader.readLine()) != null) {
			}
			proc.waitFor();
		}

		app.calculateSourcesSinksEntrypoints();

		app.addSink("<java.net.HttpURLConnection: void setRequestMethod(java.lang.String)>");

		app.printEntrypoints();
		app.printSinks();
		app.printSources();

		// only for test purposes
		// ReadFile rf= new ReadFile();
		// app.setEntrypoints(rf.readFile("entryPointsNetworkUsage.txt", ""));

		app.runInfoflow();

	}

}

package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;

import soot.jimple.infoflow.android.SetupApplication;

public class TestImeiSmsApp {
	
	static SetupApplication app = new SetupApplication();
	static String command;
	static boolean generate = false;


	/**
	 * @param args
	 * @throws wrongFormatedFileException 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		/* TODO change to match refactoring, see Test.java
		if (args.length == 6) {
			app.setAndroidJar(args[0]);
			app.setJimpleFilesLocation(args[1]);
			app.setApkFileLocation(args[2]);
			app.setMatrixFileLocation(args[3]);
			app.setSinkConfigFileLocation(args[4]);
			app.setSourceConfigFileLocation(args[5]);
		}
		else if (args.length == 7) {
			app.setAndroidJar(args[0]);
			app.setJimpleFilesLocation(args[1]);
			app.setApkFileLocation(args[2]);
			app.setMatrixFileLocation(args[3]);
			app.setSinkConfigFileLocation(args[4]);
			app.setSourceConfigFileLocation(args[5]);
			command = args[6];
			generate = true;
		}
		else{
			String jimpleFilesLocation = "C:/Users/Spikey/Desktop/soot-imei-sms";
			String apkFileLocation = "C:/Users/Spikey/workspace/IMEI-SMS/bin/IMEI-SMS.apk";
			app.setAndroidJar("C:/Users/Spikey/Desktop/platforms/android-14/android.jar");
			app.setJimpleFilesLocation(jimpleFilesLocation);
			app.setApkFileLocation(apkFileLocation);
			app.setMatrixFileLocation("android-api9.matrix");
			app.setSinkConfigFileLocation("SinkConfig.txt");
			app.setSourceConfigFileLocation("SourceConfig.txt");
			//The command line to execute soot-dex to transform apk file to jimple files till -d
			command = "\"C:/Program Files/Java/jre7/bin/javaw.exe\" -Dfile.encoding=Cp1252 -classpath C:/Users/Spikey/workspace/soot-develop/classes;C:/Users/Spikey/workspace/jasmin/classes;C:/Users/Spikey/workspace/jasmin/libs/java_cup.jar;C:/Users/Spikey/workspace/soot-develop/libs/polyglot.jar;C:/Users/Spikey/workspace/soot-develop/libs/AXMLPrinter2.jar;C:/Users/Spikey/workspace/soot-develop/libs/baksmali-1.3.2.jar;C:/Users/Spikey/workspace/heros-develop/bin;C:/Users/Spikey/workspace/heros-develop/guava-13.0.jar soot.Main -android-jars C:/Users/Spikey/Desktop/platforms -d";
			String middleLine = " -allow-phantom-refs -src-prec apk -process-dir ";
			String endLine = " -output-format jimple";
			command = "cmd /C ".concat(command).concat(" ").concat(jimpleFilesLocation).concat(middleLine).concat(apkFileLocation).concat(endLine);
			generate = false; //set to true after you have entered your correct command line
		}
		
		if (generate) {
			app.generateJimpleFiles(command);
		}
		
		app.calculateSourcesSinksEntrypoints();
		
		app.printEntrypoints();
		app.printSinks();
		app.printSources();
		
		app.runInfoflow();
		*/
		
	}

}

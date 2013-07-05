package soot.jimple.infoflow.android.TestApps;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.AbstractInfoflowProblem.PathTrackingMethod;
import soot.jimple.infoflow.android.SetupApplication;

public class Test {
	
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
					if (name.equals("v2_com.fsck.k9_1_16024_K-9 Mail.apk")
							|| name.equals("v2_com.byril.battleship_1_8_Schiffeversenken.apk")
							|| name.equals("v2_mobi.mgeek.TunnyBrowser_1_203_Dolphin Browser.apk") // broken APK
							|| name.equals("v2_com.bigduckgames.flow_1_20_Flow Free.apk")
							|| name.equals("v2_com.starfinanz.smob.android.sfinanzstatus_1_20727_Sparkasse.apk") // dexpler fails
							|| name.equals("v2_com.zoosk.zoosk_1_85_Zoosk.apk") // apk looks broken, references non-existing field in system class
							|| name.equals("v2_com.zoosk.zoosk_1_85_Zoosk - Online-Dating.apk") // apk looks broken, references non-existing field in system class
							|| name.equals("v2_com.ebay.mobile_1_30_Offizielle eBay-App.apk") // dexpler fails
							|| name.equals("v2_com.streetspotr.streetspotr_1_30_Streetspotr.apk") // dexpler fails
							|| name.equals("v2_com.game.BMX_Boy_1_8_BMX Boy.apk") // out of memory
							|| name.equals("v2_com.hm_1_143_H&M.apk")		// Debug?
							|| name.equals("v2_com.trustgo.mobile.security_1_34_Antivirus & Mobile Security.apk") // out of memory
							|| name.equals("v2_com.bfs.papertoss_1_7005_Paper Toss.apk")	// exception in callgraph builder
							|| name.equals("v2_com.magmamobile.game.Smash_1_3_Smash.apk")	// exception in callgraph builder
							|| name.equals("v2_com.autoscout24_1_95_AutoScout24 - mobile Autosuche.apk") // exception in SPARK
							|| name.equals("v2_com.gameloft.android.ANMP.GloftMTHM_1_1090_World at Arms.apk")	// runs forever
							|| name.equals("v2_com.opera.browser_1_1301080958_Opera Mobile web browser.apk")	// exception in SPARK, missing API class
							|| name.equals("v2_com.google.android.music_1_914_Google Play Music.apk")	// exception in SPARK, missing API class
							|| name.equals("v2_com.disney.WMWLite_1_15_Where's My Water_ Free.apk")	// runs forever
							|| name.equals("v2_com.aldiko.android_1_200196_Aldiko Book Reader.apk") // dexpler fails
							|| name.equals("v2_com.andromo.dev10265.app194711_1_12_Berlin Tag & Nacht - Quiz.apk") // runs forever
							|| name.equals("v2_com.navigon.navigator_select_1_10804801_NAVIGON select.apk")	// exception in SPARK, missing API class
							|| name.equals("v2_com.kvadgroup.photostudio_1_20_Photo Studio.apk")	// broken APK (zip error)
							|| name.equals("v2_com.vectorunit.yellow_1_9_Beach Buggy Blitz.apk")	// runs forever
							|| name.equals("v2_com.iudesk.android.photo.editor_1_2013032310_Photo Editor.apk") // out of memory
							|| name.equals("v2_com.evernote_1_15020_Evernote.apk")	// exception in callgraph builder
							|| name.equals("v2_appinventor.ai_progetto2003.SCAN_1_9_QR BARCODE SCANNER.apk") // main method blowup
							|| name.equals("v2_com.game.SkaterBoy_1_8_Skater Boy.apk")	// runs forever
							|| name.equals("v2_com.teamlava.fashionstory21_1_2_Fashion Story_ Earth Day.apk") // exception in SPARK
							|| name.equals("v2_com.bestcoolfungames.antsmasher_1_80_Ameisen-Quetscher Kostenlos.apk")	// dexpler fails
							|| name.equals("v2_com.spotify.mobile.android.ui_1_51200052_Spotify.apk")	// dexpler fails
							|| name.equals("v2_com.adobe.air_1_3600609_Adobe AIR.apk")	// Dexpler missing superclass?
							|| name.equals("v2_com.gamehivecorp.kicktheboss2_1_15_Kick the Boss 2.apk")	// runs forever
							|| name.equals("v2_com.nqmobile.antivirus20_1_214_NQ Mobile Security& Antivirus.apk")	// Dexpler missing superclass?
							|| name.equals("v2_tunein.player_1_47_TuneIn Radio.apk") // dexpler fails
							|| name.equals("v2_com.lmaosoft.hangmanDE_1_24_Hangman - Deutsch-Spiel.apk")	// runs forever
							|| name.equals("v2_tv.dailyme.android_1_105_dailyme TV, Serien & Fernsehen.apk")	// broken apk? field ref
							|| name.equals("v2_com.twitter.android_1_400_Twitter.apk")	// dexpler fails
							|| name.equals("v2_de.radio.android_1_39_radio.de.apk")	// runs forever
							|| name.equals("v2_com.magmamobile.game.Burger_1_8_Burger.apk")	// broken apk? field ref
							|| name.equals("v2_com.rovio.angrybirdsspace.ads_1_1510_Angry Birds Space.apk")	// dexpler fails
							|| name.equals("v2_de.barcoo.android_1_50_Barcode Scanner barcoo.apk")	// runs forever
							|| name.equals("v2_www.agathasmaze.com.slenderman_1_26_SlenderMan.apk") // out of memory
							|| name.equals("v2_com.netbiscuits.kicker_1_11_kicker online.apk")	// field missing
							|| name.equals("v2_com.appturbo.appturboDE_1_2_App des Tages - 100% Gratis.apk")	// runs forever
							|| name.equals("v2_de.msg_1_37_mehr-tanken.apk")	// Spark fails
							|| name.equals("v2_com.wetter.androidclient_1_26_wetter.com.apk")	// StackOverflowException in dexpler
							|| name.equals("v2_mobi.infolife.taskmanager_1_84_Advanced Task Manager Deutsch.apk")	// runs forever
							|| name.equals("v2_com.feelingtouch.strikeforce2_1_7_SWAT_End War.apk")		// dexpler fails
							|| name.equals("v2_com.ebay.kleinanzeigen_1_294_eBay Kleinanzeigen.apk")	// field missing
							|| name.equals("v2_com.gravitylabs.photobooth_1_5_Photo Effects Pro.apk")	// out of memory
							|| name.equals("v2_com.rcd.radio90elf_1_488_90elf Fussball Bundesliga Live.apk")	// runs forever
							|| name.equals("v2_logo.quiz.game.category_1_20_Ultimate Logo Quiz.apk")	// missing field
							|| name.equals("v2_de.avm.android.fritzapp_1_1538_FRITZ!App Fon.apk")	// out of memory
							|| name.equals("v2_com.droidhen.game.poker_1_35_DH Texas Poker.apk")	// broken apk? field ref
							|| name.equals("v2_com.avast.android.mobilesecurity_1_4304_avast! Mobile Security.apk")	// dexpler bug
							|| name.equals("v2_de.rtl.video_1_5_RTL INSIDE.apk")		// broken apk? field ref
							|| name.equals("v2_jp.naver.line.android_1_107_LINE_ Gratis-Anrufe.apk")	// broken apk? field ref
							|| name.equals("v2_com.zeptolab.ctr.ads_1_20_Cut the Rope FULL FREE.apk")	// Dexpler issue
							|| name.equals("v2_com.advancedprocessmanager_1_59_Android Assistant(18 features).apk")		// Dexpler issue
							|| name.equals("v2_com.rcflightsim.cvplane2_1_42_Absolute RC Plane Sim.apk")	// out of memory
							|| name.equals("v2_com.zynga.livepoker_1_77_Zynga Poker.apk")	// dexpler fails
							|| name.equals("v2_com.baudeineapp.mcdcoupon_1_4_McDonalds Gutscheine App.apk")		// broken apk? field ref
							|| name.equals("v2_de.sellfisch.android.wwr_1_56_Wer Wird Reich (Quiz).apk")		// dexpler fails
							)
						return false;
					return (name.endsWith(".apk"));
				}
				
			});
			for (String s : dirFiles)
				apkFiles.add(s);
		}
		else
			apkFiles.add(args[0]);

		for (String fileName : apkFiles) {
			long beforeRun = System.nanoTime();
			String fullFilePath = fileName;
			
			// Directory handling
			if (apkFiles.size() > 1) {
				fullFilePath = args[0] + File.separator + fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
				File flagFile = new File("_Run" + fileName);
				if (flagFile.exists())
					continue;
				flagFile.createNewFile();
			}
			
			final SetupApplication app = new SetupApplication();
			app.setApkFileLocation(fullFilePath);
			app.setAndroidJar(args[1]);
			if (new File("../soot-infoflow/EasyTaintWrapperSource.txt").exists())
				app.setTaintWrapperFile("../soot-infoflow/EasyTaintWrapperSource.txt");
			else
				app.setTaintWrapperFile("EasyTaintWrapperSource.txt");
			app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
//			app.setPathTracking(PathTrackingMethod.ForwardTracking);
			
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
			
			// Make sure to remove leftovers
			System.gc();
		}
	}

}

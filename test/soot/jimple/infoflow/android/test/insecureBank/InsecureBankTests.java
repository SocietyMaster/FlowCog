/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.test.insecureBank;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class InsecureBankTests {
	
	private final static String sharedPrefs_putString =
			"<android.content.SharedPreferences$Editor: android.content.SharedPreferences$Editor putString(java.lang.String,java.lang.String)>";
	private final static String loginScreen_findViewById =
			"<android.app.Activity: android.view.View findViewById(int)>";
	private final static String loginScreen_startActivity =
			"<android.app.Activity: void startActivity(android.content.Intent)>";
	private final static String url_init =
			"<java.net.URL: void <init>(java.lang.String)>";
	private final static String intent_getExtras =
			"<android.content.Intent: android.os.Bundle getExtras()>";
	private final static String bundle_getString =
			"<android.os.Bundle: java.lang.String getString(java.lang.String)>";
	private final static String log_e =
			"<android.util.Log: int e(java.lang.String,java.lang.String)>";
	private final static String urlConnection_openConnection =
			"<java.net.URL: java.net.URLConnection openConnection()>";
	
	/**
	 * Analyzes the given APK file for data flows
	 * @param enableImplicitFlows True if implicit flows shall be tracked,
	 * otherwise false
	 * @return The data leaks found in the given APK file
	 * @throws IOException Thrown if the given APK file or any other required
	 * file could not be found
	 */
	private InfoflowResults analyzeAPKFile(boolean enableImplicitFlows) throws IOException {
		String androidJars = System.getenv("ANDROID_JARS");
		if (androidJars == null)
			androidJars = System.getProperty("ANDROID_JARS");
		if (androidJars == null)
			throw new RuntimeException("Android JAR dir not set");
		System.out.println("Loading Android.jar files from " + androidJars);
		
		SetupApplication setupApplication = new SetupApplication(androidJars,
				"insecureBank" + File.separator + "InsecureBank.apk");
		setupApplication.setTaintWrapper(new EasyTaintWrapper("EasyTaintWrapperSource.txt"));
		setupApplication.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
		setupApplication.setEnableImplicitFlows(enableImplicitFlows);
		return setupApplication.runInfoflow();
	}

	@Test
	public void runTestInsecureBank() throws IOException {
		InfoflowResults res = analyzeAPKFile(false);
		// 7 leaks + 1x inter-component communication (server ip going through an intent)
		Assert.assertEquals(8, res.size());
		
		Assert.assertTrue(res.isPathBetweenMethods(sharedPrefs_putString, loginScreen_findViewById));
		Assert.assertTrue(res.isPathBetweenMethods(loginScreen_startActivity, loginScreen_findViewById));
		Assert.assertTrue(res.isPathBetweenMethods(url_init, intent_getExtras));
		Assert.assertTrue(res.isPathBetweenMethods(url_init, bundle_getString));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, intent_getExtras));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, bundle_getString));
		Assert.assertTrue(res.isPathBetweenMethods(log_e, urlConnection_openConnection));
	}
	
}

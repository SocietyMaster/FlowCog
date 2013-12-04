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
	}
	
}

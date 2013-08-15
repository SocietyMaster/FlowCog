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
package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

public class AndroidSpecificTests extends JUnitTests {
	
	@Test
	public void runTestDirectLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_DirectLeak1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestInactiveActivity() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_InactiveActivity.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestLibrary2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_Library2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestLogNoLeak() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_LogNoLeak.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestObfuscation1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_Obfuscation1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestPrivateDataLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_PrivateDataLeak1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestPrivateDataLeak2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_PrivateDataLeak2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	@Ignore		// not supported, would require taint tracking via files
	public void runTestPrivateDataLeak3() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidSpecific_PrivateDataLeak3.apk");
		Assert.assertEquals(2, res.size());
	}

}

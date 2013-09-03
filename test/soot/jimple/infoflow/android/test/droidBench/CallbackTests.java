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

public class CallbackTests extends JUnitTests {
	
	@Test
	public void runTestAnonymousClass1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_AnonymousClass1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());	// loc + lat, but single parameter
	}

	@Test
	public void runTestButton1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestButton2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(4, res.size());		// 3 + (strong alias update not supported)
	}

	@Test
	public void runTestTestButton3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestButton4() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Button4.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestLocationLeak1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestLocationLeak2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestLocationLeak3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_LocationLeak3.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size()); // loc + lat, but single parameter
	}

	@Test
	public void runTestMethodOverride1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_MethodOverride1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestMultiHandlers1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_MultiHandlers1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

	@Test
	@Ignore		// Callback ordering is not supported
	public void runTestOrdering1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Ordering1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestRegisterGlobal1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_RegisterGlobal1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestRegisterGlobal2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_RegisterGlobal2.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}

	@Test
	@Ignore		// Unregistering callbacks is not supported
	public void runTestUnregister1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Callbacks_Unregister1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(0, res.size());
	}

}

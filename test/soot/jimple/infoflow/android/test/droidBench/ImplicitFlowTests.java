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

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class ImplicitFlowTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestImplicitFlow1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows/ImplicitFlow1.apk", true);
		Assert.assertEquals(1, res.size());		// same source and sink, gets collapsed into one leak
	}

	@Test(timeout=300000)
	public void runTestImplicitFlow2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows/ImplicitFlow2.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout=300000)
	public void runTestImplicitFlow3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows/ImplicitFlow3.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout=300000)
	public void runTestImplicitFlow4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows/ImplicitFlow4.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestImplicitFlow5() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows/ImplicitFlow5.apk", true);
		Assert.assertNotNull(res);
		Assert.assertEquals(1, res.size());
	}
}

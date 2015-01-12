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
import org.junit.Ignore;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.results.InfoflowResults;

public class GeneralJavaTests extends JUnitTests {
	
	@Test(timeout=300000)
	public void runTestLoop1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Loop1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestLoop2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Loop2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore("overwrites inside a buffer")		// not supported yet in FlowDroid
	public void runTestObfuscation1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Obfuscation1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestExceptions1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Exceptions1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestExceptions2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Exceptions2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore		// not supported yet, would require condition evaluation
	public void runTestExceptions3() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Exceptions3.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test(timeout=300000)
	public void runTestExceptions4() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/Exceptions4.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestFactoryMethods1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/FactoryMethods1.apk");
		Assert.assertNotNull(res);
		Assert.assertEquals(2, res.size());
	}

	@Test(timeout=300000)
	public void runTestSourceCodeSpecific1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/SourceCodeSpecific1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	@Ignore		// not supported yet in FlowDroid
	public void runTestStaticInitialization1() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/StaticInitialization1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test(timeout=300000)
	public void runTestStaticInitialization2() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/StaticInitialization2.apk");
		Assert.assertEquals(1, res.size());
	}
	
	@Test(timeout=300000)
	public void runTestUnreachableCode() throws IOException, XmlPullParserException {
		InfoflowResults res = analyzeAPKFile("GeneralJava/UnreachableCode.apk");
		if (res != null)
			Assert.assertEquals(0, res.size());
	}

}

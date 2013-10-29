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

import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

public class ImplicitFlowTests extends JUnitTests {
	
	@Test
	public void runTestImplicitFlow1() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow1.apk", true);
		Assert.assertEquals(1, res.size());		// same source and sink, gets collapsed into one leak
	}

	@Test
	public void runTestImplicitFlow2() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow2.apk", true);
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestImplicitFlow3() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow3.apk", true);
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestImplicitFlow4() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow4.apk", true);
		Assert.assertEquals(3, res.size());		// 2 + Exception
	}

}

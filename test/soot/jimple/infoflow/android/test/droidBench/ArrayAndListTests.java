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

@Ignore	// we do not support tainting of single array fields or map entries yet
public class ArrayAndListTests extends JUnitTests {
	
	@Test
	public void runTestArrayAccess1() throws IOException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists_ArrayAccess1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestArrayAccess2() throws IOException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists_ArrayAccess2.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestHashMapAccess1() throws IOException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists_HashMapAccess1.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestListAccess1() throws IOException {
		InfoflowResults res = analyzeAPKFile("ArraysAndLists_ListAccess1.apk");
		Assert.assertEquals(0, res.size());
	}

}

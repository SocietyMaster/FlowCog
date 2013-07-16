package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

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

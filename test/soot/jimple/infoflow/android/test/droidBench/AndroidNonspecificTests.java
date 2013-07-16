package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

public class AndroidNonspecificTests extends JUnitTests {
	
	@Test
	public void runTestLoop1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidNonspecific_Loop1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestLoop2() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidNonspecific_Loop2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestExceptions1() throws IOException {
		InfoflowResults res = analyzeAPKFile("GeneralJava_Exceptions1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestExceptions2() throws IOException {
		InfoflowResults res = analyzeAPKFile("GeneralJava_Exceptions2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestExceptions3() throws IOException {
		InfoflowResults res = analyzeAPKFile("GeneralJava_Exceptions3.apk");
		Assert.assertEquals(0, res.size());
	}

	@Test
	public void runTestExceptions4() throws IOException {
		InfoflowResults res = analyzeAPKFile("GeneralJava_Exceptions4.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestSourceCodeSpecific1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidNonspecific_SourceCodeSpecific1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestStaticInitialization1() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidNonspecific_StaticInitialization1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestUnreachableCode() throws IOException {
		InfoflowResults res = analyzeAPKFile("AndroidNonspecific_UnreachableCode.apk");
		Assert.assertEquals(1, res.size());
	}

}

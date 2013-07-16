package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

@Ignore
public class ImplicitFlowTests extends JUnitTests {
	
	@Test
	public void runTestImplicitFlow1() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow1.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestImplicitFlow2() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow2.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestImplicitFlow3() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow3.apk");
		Assert.assertEquals(2, res.size());
	}

	@Test
	public void runTestImplicitFlow4() throws IOException {
		InfoflowResults res = analyzeAPKFile("ImplicitFlows_ImplicitFlow4.apk");
		Assert.assertEquals(2, res.size());
	}

}

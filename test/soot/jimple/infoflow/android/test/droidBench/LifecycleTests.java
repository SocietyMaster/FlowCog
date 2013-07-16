package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

@Ignore
public class LifecycleTests extends JUnitTests {
	
	@Test
	public void runTestActivityLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ActivityLifecycle1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestActivityLifecycle2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ActivityLifecycle2.apk");
		Assert.assertEquals(1, res.size());
	}
	
	@Test
	public void runTestActivityLifecycle3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ActivityLifecycle3.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestActivityLifecycle4() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ActivityLifecycle4.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestApplicationLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ApplicationLifecycle1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestApplicationLifecycle2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ApplicationLifecycle2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestApplicationLifecycle3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ApplicationLifecycle3.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestServiceLifecycle1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_ServiceLifecycle1.apk");
		Assert.assertEquals(1, res.size());
	}

}

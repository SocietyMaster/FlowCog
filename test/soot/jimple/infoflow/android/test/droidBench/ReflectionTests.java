package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

@Ignore
public class ReflectionTests extends JUnitTests {
	
	@Test
	public void runTestReflection1() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_Reflection1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestReflection2() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_Reflection2.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestReflection3() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_Reflection3.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestReflection4() throws IOException {
		InfoflowResults res = analyzeAPKFile("Lifecycle_Reflection4.apk");
		Assert.assertEquals(1, res.size());
	}

}

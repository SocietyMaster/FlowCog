package soot.jimple.infoflow.android.test.droidBench;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import soot.jimple.infoflow.InfoflowResults;

@Ignore
public class InterAppCommunicationTests extends JUnitTests {
	
	@Test
	public void runTestActivityCommunication1() throws IOException {
		InfoflowResults res = analyzeAPKFile("InterAppCommunication_ActivityCommunication1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestIntentSink1() throws IOException {
		InfoflowResults res = analyzeAPKFile("InterAppCommunication_IntentSink1.apk");
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void runTestIntentSink2() throws IOException {
		InfoflowResults res = analyzeAPKFile("InterAppCommunication_IntentSink2.apk");
		Assert.assertEquals(1, res.size());
	}

}

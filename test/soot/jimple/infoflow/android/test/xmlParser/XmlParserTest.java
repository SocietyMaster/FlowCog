package soot.jimple.infoflow.android.test.xmlParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import soot.SootField;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethodAccessPathBundle;
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.data.parsers.XMLPermissionMethodParser;
import soot.jimple.infoflow.data.AccessPath;

/**
 * Testing the new xml-parser with the new xml format
 * 
 * @author Jannik Juergens
 *
 */
public class XmlParserTest {

	private static final int INITIAL_SET_SIZE = 10000;

	/**
	 * Compares the new and the old Parser for different xml files
	 * 
	 * @param xmlFile
	 *            in new format
	 * @param oldXmlFile
	 * @throws IOException
	 */
	private void compareParserResults(String xmlFile, String oldXmlFile) throws IOException {
		XMLPermissionMethodParser newParser = XMLPermissionMethodParser.fromFile(xmlFile);
		PermissionMethodParser oldParser = PermissionMethodParser.fromFile(oldXmlFile);
		// BadXmlParser newParser = new BadXmlParser(xmlFile);

		if (newParser != null && oldParser != null) {
			Assert.assertEquals(oldParser.parse(), newParser.parse());
		} else {
			Assert.assertTrue(false);
		}
	}

	/**
	 * Test with a empty xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void emptyXmlTest() throws IOException {
		String xmlFile = "testXmlParser/empty.xml";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeXmlTest() throws IOException {
		String xmlFile = "testXmlParser/complete.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a empty txt file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void emptyTxtTest() throws IOException {
		String xmlFile = "testXmlParser/empty.txt";
		compareParserResults(xmlFile, xmlFile);
	}

	/**
	 * Test with a complete txt file
	 * 
	 * @throws IOException
	 */
	@Test
	public void completeTxtTest() throws IOException {
		String xmlFile = "testXmlParser/complete.txt";
		String oldXmlFile = "testXmlParser/completeOld.txt";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test
	public void missingPartsXmlTest1() throws IOException {
		String xmlFile = "testXmlParser/missingParts.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * manual verification of the parser result
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test(timeout = 300000)
	public void verifyParserResultTest() throws IOException, XmlPullParserException {

		// Initialize soot because parser needs classes from soot
		SootInitializer.analyzeAPKFile("testXmlParser/AndroidTest.apk", "testXmlParser/SourcesAndSinksTest.xml");

		// parsing data from xml file
		String xmlFile = "testXmlParser/SourcesAndSinksTest.xml";
		XMLPermissionMethodParser newParser = XMLPermissionMethodParser.fromFile(xmlFile);
		Set<AndroidMethod> methodListParser = newParser.parse();

		// create two methods with reference data
		Set<AndroidMethod> methodListTest = new HashSet<AndroidMethod>(INITIAL_SET_SIZE);
		String methodName = "SourceTestFalsePositive";
		String returnType = "void";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		methodListTest.add(am1);

		methodParameters = new ArrayList<String>();
		methodParameters.add("double");
		AndroidMethod am2 = new AndroidMethod("sinkTestFalsePositive", methodParameters, "void",
				"com.example.androidtest.Sinks");
		methodListTest.add(am2);

		// both methods should be identical
		Assert.assertEquals(methodListTest, methodListParser);

		// check parts of the accessPath in detail

		AndroidMethodAccessPathBundle bundle1 = newParser.getAccessPaths(am1.getSignature());

		Assert.assertEquals(2, bundle1.getSourceParameterCount());
		Assert.assertEquals(1, bundle1.getSinkParameterCount());

		AccessPath[] sinkBAP = bundle1.getSinkBaseAPs();
		Assert.assertEquals("com.example.androidtest.Sources", sinkBAP[0].getBaseType().toString());

		AccessPath[] sinkPAP0 = bundle1.getSinkParamterAPs(0);
		Assert.assertEquals("c", sinkPAP0[0].getFirstField().getName());
		Assert.assertEquals("SomeClassA", sinkPAP0[0].getFirstFieldType().toString());
		SootField[] sf = sinkPAP0[0].getFields();
		Assert.assertEquals("c", sf[1].getName());
		Assert.assertEquals("java.lang.String", sf[1].getType().toString());

		// TODO
		AccessPath[] sourceBAP = bundle1.getSourceBaseAPs();
		AccessPath[] sourcePAP0 = bundle1.getSourceParameterAPs(0);
		AccessPath[] sourcePAP1 = bundle1.getSourceParameterAPs(1);

		AccessPath[] sourceRAP = bundle1.getSourceReturnAPs();
		Assert.assertEquals("java.lang.String", sourceRAP[0].getBaseType().toString());

		AndroidMethodAccessPathBundle bundle2 = newParser.getAccessPaths(am2.getSignature());

		Assert.assertEquals(0, bundle2.getSourceParameterCount());
		Assert.assertEquals(1, bundle2.getSinkParameterCount());

		sinkPAP0 = bundle2.getSinkParamterAPs(0);
		Assert.assertEquals("double", sinkPAP0[0].getBaseType().toString());

	}
}

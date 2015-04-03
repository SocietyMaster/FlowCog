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
import soot.jimple.infoflow.android.data.parsers.PermissionMethodParser;
import soot.jimple.infoflow.android.data.parsers.XMLPermissionMethodParser;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.source.AccessPathBundle;

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
	public void missingPartsXmlTest() throws IOException {
		String xmlFile = "testXmlParser/missingParts.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}
	
	/**
	 * Test with a incomplete but valid xml file
	 * 
	 * @throws IOException
	 */
	@Test(expected=RuntimeException.class)
	public void notValidXmlTest() throws IOException {
		String xmlFile = "testXmlParser/notValid.xml";
		String oldXmlFile = "testXmlParser/completeOld.xml";
		compareParserResults(xmlFile, oldXmlFile);
	}

	/**
	 * manual verification of the parser result
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	@Test
	public void verifyParserResultTest() throws IOException, XmlPullParserException {

		// parsing data from xml file
		String xmlFile = "testXmlParser/complete.xml";
		XMLPermissionMethodParser newParser = XMLPermissionMethodParser.fromFile(xmlFile);
		Set<AndroidMethod> methodListParser = newParser.parse();

		// create two methods with reference data
		Set<AndroidMethod> methodListTest = new HashSet<AndroidMethod>(INITIAL_SET_SIZE);
		String methodName = "sourceTest";
		String returnType = "java.lang.String";
		String className = "com.example.androidtest.Sources";
		List<String> methodParameters = new ArrayList<String>();
		methodParameters.add("com.example.androidtest.MyTestObject");
		methodParameters.add("int");
		AndroidMethod am1 = new AndroidMethod(methodName, methodParameters, returnType, className);
		methodListTest.add(am1);

		methodParameters = new ArrayList<String>();
		methodParameters.add("double");
		methodParameters.add("double");
		AndroidMethod am2 = new AndroidMethod("sinkTest", methodParameters, "void",
				"com.example.androidtest.Sinks");
		methodListTest.add(am2);

		// both methodLists should be identical
		Assert.assertEquals(methodListTest, methodListParser);
	}
}

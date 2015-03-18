package soot.jimple.infoflow.android.data.parsers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;
import soot.jimple.infoflow.android.data.AndroidMethodAccessPathBundle;

/**
 * Parses informations from the new Dataformat (XML) with the help of SAX.
 * Returns only a Set of Android Method when calling the function parse. For the
 * AccessPath use class SaxHandler.
 * 
 * TODO think about refactoring so that it isn't any longer needed to call
 * SaxHandler
 * 
 * 
 * @author Anna-Katharina Wickert, Joern Tillmans
 */

public class XMLPermissionMethodParser extends DefaultHandler implements
		IPermissionMethodParser, IAccessPathMethodParser {

	public static XMLPermissionMethodParser fromFile(String fileName)
			throws IOException {
		if (!verifyXML(fileName)) {
			throw new RuntimeException("The XML-File isn't valid");
		}
		XMLPermissionMethodParser pmp = new XMLPermissionMethodParser(fileName);

		return pmp;
	}

	public XMLPermissionMethodParser() {
		methodList = new HashSet<AndroidMethod>();
	}

	@Override
	/**
	 * Create a new instance of the SAX Parser, which will parse the xml 
	 */
	public Set<AndroidMethod> parse() throws IOException {
		methodList = new HashSet<AndroidMethod>();
		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			SAXParser parser = pf.newSAXParser();

			parser.parse(fileName, this);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		printData();
		return methodList;
	}

	/**
	 * Event Handler for the starting element for SAX. Possible start elements
	 * for filling AndroidMethod objects with the new data format: - method:
	 * Setting booleans to false and get the signature and category, -
	 * accessPath: To get the information whether the AndroidMethod is a sink or
	 * source, - and the other elements doesn't care for creating the
	 * AndroidMethod object.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {
		case "method":
			isSource = false;
			isSink = false;
			methodSignature = null;
			methodCategory = null;

			if (attributes != null) {
				methodSignature = attributes.getValue("signature");
				methodCategory = attributes.getValue("category");
			}
			break;

		case "accesspath":
			if (attributes != null) {
				if (attributes.getValue("isSource") != null) {
					isSource = (attributes.getValue("isSource")
							.equalsIgnoreCase("true")) ? true
							: isSource || false;
				}
				if (attributes.getValue("isSink") != null) {
					isSink = (attributes.getValue("isSink")
							.equalsIgnoreCase("true")) ? true : isSink || false;
				}
			}
			break;
		}

	}

	/**
	 * PathElement is the only element having values inside -> for this SAX
	 * Parser nothing to do.
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	/**
	 * EventHandler for the End of an element. -> Putting the values into the
	 * objects. For additional information: startElement description. Starting
	 * with the innerst elements and switching up to the outer elements
	 * 
	 * - pathElement -> means field sensitive, adding SootFields
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {

		case "method":
			if (methodSignature != null) {
				tempMeth = AndroidMethod.createfromSignature(methodSignature);
				if (methodCategory != null) {
					tempMeth.setCategory(CATEGORY.valueOf(methodCategory));
				}
				tempMeth.setSink(isSink);
				tempMeth.setSource(isSource);
				if (tempMeth != null) {
					methodList.add(tempMeth);
				}
			}
		}
	}

	/**
	 * Return the a AndroidMethodAccessPathBundle object to a given signature.
	 * 
	 * @return a AndroidMethodAccesPathBundle with the AccessPaths belonging to
	 *         the signature
	 * @param signature
	 *            The signature to which the AccessPaths should be retourned.
	 * @throws InvalidXML will be thrown if the signature is null or empty
	 */
	public AndroidMethodAccessPathBundle getAccessPaths(String signature) throws IOException{
		SaxHandler saxHandler = new SaxHandler(fileName);
		if (signature.isEmpty() || signature==null){
			throw new IOException("Signature is null or empty.");
		}
		AndroidMethodAccessPathBundle amapb = saxHandler
				.getAccessPaths(signature);
		assert (amapb != null);
		return amapb;
	}

	private XMLPermissionMethodParser(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data
	 * format.
	 * 
	 * @param fileName
	 *            of the XML
	 * @return true = valid XML false = invalid XML
	 */
	private static boolean verifyXML(String fileName) {

		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		Source xsdFile = new StreamSource(new File(XSD_FILE_PATH));
		Source xmlFile = new StreamSource(new File(fileName));
		boolean validXML = false;
		try {
			Schema schema = sf.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			try {
				validator.validate(xmlFile);
				validXML = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!validXML) {
				new IOException("File isn't  valid against the xsd");
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return validXML;
	}

	private void printData() {
		System.out.println("No of AndroidMethods: " + methodList.size() + ".");

		for (Iterator<AndroidMethod> it = methodList.iterator(); it.hasNext();) {
			System.out.println(it.next().toString());
		}
	}

	// Sett for collecting the return AndroidMethods.
	private Set<AndroidMethod> methodList;
	// Holding temporary values for handling with SAX
	private AndroidMethod tempMeth;
	private String methodSignature;
	private String methodCategory;
	private boolean isSource, isSink;

	// XML stuff incl. Verification against XSD
	private String fileName;

	private File XMLFile;
	private static final String XSD_FILE_PATH = "../../exchangeFormat.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final int INITIAL_SET_SIZE = 10000;
}

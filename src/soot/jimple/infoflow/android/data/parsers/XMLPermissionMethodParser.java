package soot.jimple.infoflow.android.data.parsers;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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
import soot.jimple.infoflow.source.AccessPathBundle;

/**
 * Parses informations from the new Dataformat (XML) with the help of SAX. Returns only a Set of Android Method when
 * calling the function parse. For the AccessPath the class SaxHandler is used. 
 * 
 * @author Anna-Katharina Wickert, Joern Tillmans
 */

public class XMLPermissionMethodParser extends DefaultHandler implements IPermissionMethodParser,
		IAccessPathMethodParser {

	public static final String METHOD_TAG = "method";
	public static final String BASE_TAG = "base";
	public static final String RETURN_TAG = "return";
	public static final String PARAM_TAG = "param";
	public static final String ACCESSPATH_TAG = "accesspath";
	public static final String PATHELEMENT_TAG = "pathelement";

	public static final String SIGNATURE_ATTRIBUTE = "signature";
	public static final String CATEGORY_ATTRIBUTE = "category";
	public static final String TYPE_ATTRIBUTE = "type";
	public static final String INDEX_ATTRIBUTE = "index";
	public static final String IS_SOURCE_ATTRIBUTE = "isSource";
	public static final String IS_SINK_ATTRIBUTE = "isSink";
	public static final String LENGTH_ATTRIBUTE = "length";
	
	public static final String TRUE = "true";
	
	// Sett for collecting the return AndroidMethods.
	private Set<AndroidMethod> methodList;

	// Holding temporary values for handling with SAX
	private AndroidMethod tempMeth;
	private String methodSignature;
	private String methodCategory;
	private boolean isSource, isSink;

	// XML stuff incl. Verification against XSD
	private String fileName;

	private static final String XSD_FILE_PATH = "../../exchangeFormat.xsd";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static XMLPermissionMethodParser fromFile(String fileName) throws IOException {
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
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return methodList;
	}

	/**
	 * Event Handler for the starting element for SAX. Possible start elements
	 * for filling AndroidMethod objects with the new data format: - method:
	 * Setting parsingvalues to false or null and get and set the signature and
	 * category, - accessPath: To get the information whether the AndroidMethod
	 * is a sink or source, - and the other elements doesn't care for creating
	 * the AndroidMethod object. At these element we will look, if calling the
	 * method getAccessPath (using an new SAX Handler)
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {
		case METHOD_TAG:
			isSource = false;
			isSink = false;
			methodSignature = null;
			methodCategory = null;

			if (attributes != null) {
				methodSignature = attributes.getValue(SIGNATURE_ATTRIBUTE).trim();
				methodCategory = attributes.getValue(CATEGORY_ATTRIBUTE).trim();
			}
			break;

		case ACCESSPATH_TAG:
			if (attributes != null) {
				if (attributes.getValue(IS_SOURCE_ATTRIBUTE) != null) {
					String sIsSource = attributes.getValue(IS_SOURCE_ATTRIBUTE);
					sIsSource = sIsSource.trim().toUpperCase();
					isSource = sIsSource.equalsIgnoreCase(TRUE) ? true : isSource || false;
				}
				
				if (attributes.getValue(IS_SINK_ATTRIBUTE) != null) {
					String sIsSink = attributes.getValue(IS_SINK_ATTRIBUTE);
					sIsSink = sIsSink.trim().toLowerCase();
					isSink = sIsSink.equals(TRUE) ? true : isSink || false;
				}
			}
			break;
		}

	}

	/**
	 * PathElement is the only element having values inside -> nothing to do
	 * here. Doesn't care at the current state of parsing.
	**/
	public void characters(char[] ch, int start, int length) throws SAXException {
	}

	/**
	 * EventHandler for the End of an element. -> Putting the values into the objects. For additional information:
	 * startElement description. Starting with the innerst elements and switching up to the outer elements
	 * 
	 * - pathElement -> means field sensitive, adding SootFields
	 */
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {

		case METHOD_TAG:
			if (methodSignature != null) {
				tempMeth = AndroidMethod.createfromSignature(methodSignature);
				if (methodCategory != null) {
					String methodCategoryUpper = methodCategory.toUpperCase().trim();
					tempMeth.setCategory(CATEGORY.valueOf(methodCategoryUpper));
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
	 * @return a AndroidMethodAccesPathBundle with the AccessPaths belonging to the signature
	 * @param signature
	 *            The signature to which the AccessPaths should be retourned.
	 * @throws InvalidXML
	 *             will be thrown if the signature is null or empty
	 */
	public AccessPathBundle getAccessPaths(String signature) {
		SaxHandler saxHandler = new SaxHandler(fileName);
		if (signature.isEmpty() || signature == null)
			throw new RuntimeException("Signature is null or empty.");

		AccessPathBundle amapb = saxHandler.getAccessPaths(signature);
		return amapb;
	}

	private XMLPermissionMethodParser(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Checks whether the given XML is valid against the XSD for the new data format.
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
				e.printStackTrace();
			}
			if (!validXML) {
				new IOException("File isn't  valid against the xsd");
			}
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return validXML;
	}
}

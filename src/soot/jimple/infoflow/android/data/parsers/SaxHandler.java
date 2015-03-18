package soot.jimple.infoflow.android.data.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import soot.Local;
import soot.Scene;
import soot.SootField;
import soot.jimple.Jimple;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethodAccessPathBundle;
import soot.jimple.infoflow.data.AccessPath;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class converts with the help of SAX the new data format (XML) into an
 * AndroidMethodPathBundle object. Therefore the class implements the
 * IAccesPAthMethodParser. To convert an XML file, the file itself and an String
 * with an signature is needed. This is because only those paths should be
 * extracted which are equal with the signature.
 * 
 * @author Anna-Katharina Wickert, Jörn Tillmanns
 * 
 */
public class SaxHandler extends DefaultHandler implements
		IAccessPathMethodParser {
	/**
	 * Generates a new SaxHandler object for converting the XML file.
	 * 
	 * @param xml
	 *            XML file which should be converted.
	 */
	public SaxHandler(String xml) {
		this.xml = xml;
	}

	/**
	 * Converts the XML (Constructor) to AccessPath which belongs to a
	 * signature.
	 * 
	 * @param signature
	 *            A string with the signature to which the Parser should deliver
	 *            the corresponding AndroidMethodAccessPathBundle.
	 * @return the AndroidMethodAccessPathBundle object with the accessPaths
	 *         belonging to the signature
	 */
	public AndroidMethodAccessPathBundle getAccessPaths(String signature)
			throws IOException {
		// TODO Auto-generated method stub
		this.signature = signature;
		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			SAXParser parser = pf.newSAXParser();

			parser.parse(xml, this);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return amapbReturn;
	}

	/**
	 * SAX-Handler Method for the startElements -> Setting booleans and
	 * comparing with the signature for the final creating of the object.
	 * 
	 * Index with no content (which shouldn't happen if the XML is valid) will
	 * return -1.
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		String qNameLower = qName.toLowerCase(); // needed fo case insentive
													// comparision (switch)
		switch (qNameLower) {
		case "method":
			methodSignature = null;
			if (attributes != null) {
				methodSignature = attributes.getValue("signature");
			}
			baseSet = null;
			returnSet = null;
			paramSet = new HashMap<Integer, HashSet<AccessPathTuple>>();
			paramType = new ArrayList<>();
//			pathElementType = new ArrayList<>();
			bBase = false;
			bReturn = false;
			bParam = false;
			fields = new ArrayList<>();

			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					bBase = false;
					bReturn = false;
					bParam = false;

				}
			}
			break;

		case "base":
			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					bBase = true;
					bReturn = false;
					bParam = false;

					if (attributes != null) {
						baseType = attributes.getValue("type");
						localBase = Jimple.v().newLocal(BASENAME,
								Scene.v().getSootClass(baseType).getType());
					}
				}
			}
			break;

		case "return":
			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					bBase = false;
					bReturn = true;
					bParam = false;

					if (attributes != null) {
						returnType = attributes.getValue("type");
						localBase = Jimple.v().newLocal(BASENAME,
								Scene.v().getSootClass(returnType).getType());
					}
				}
			}
			break;

		case "param":
			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					bBase = false;
					bReturn = false;
					bParam = true;

					if (attributes != null) {
						String index = attributes.getValue("index");

						paramIndex = (index != null && !index.isEmpty()) ? Integer
								.parseInt(index) : -1;
						assert (paramType != null);
						paramType.add(paramIndex, attributes.getValue("type"));

						localBase = Jimple
								.v()
								.newLocal(
										BASENAME,
										Scene.v()
												.getSootClass(
														paramType
																.get(paramIndex))
												.getType());
						assert (paramSet != null);
						paramSet.put(paramIndex, new HashSet<AccessPathTuple>());

					}
				}
			}

			break;

		case "accesspath":
			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					if (attributes != null) {
						isSource = (attributes.getValue("isSource")
								.equalsIgnoreCase("true")) ? true : false;
						isSink = (attributes.getValue("isSink")
								.equalsIgnoreCase("true")) ? true : false;
						String length = attributes.getValue("length");
						accessPathLength = (length != null & !length.isEmpty()) ? Integer
								.parseInt(length) : -1;
						if(accessPathLength!=-1){
							pathElementType = new String[accessPathLength];
						}
					}
				}
			}
			break;

		case "pathelement":
			if (methodSignature != null && signature != null) {
				if (methodSignature.equals(signature)) {
					if (attributes != null) {
						String index = attributes.getValue("index");
						pathElementIndex = (index != null && !index.isEmpty()) ? Integer
								.parseInt(index) : -1;
						assert (pathElementType != null);
						pathElementType[pathElementIndex-1] = attributes.getValue("type");
					}
				}
			}

		default:
			break;
		}

	}

	/**
	 * PathElement is the only element having values inside -> the only thing
	 * this Handler had to do.
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		pathElementContent = new String(ch, start, length);
		pathElementContent = pathElementContent.trim();
	}

	/**
	 * SAX-Handler for the endElement. -> Building the final
	 * AndroidMethodAccessPathBundle
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String qNameLower = qName.toLowerCase();
		switch (qNameLower) {
		case "method":
			int paramLength = AndroidMethod.createfromSignature(signature)
					.getParameters().size();
			AccessPath[] sourceBaseAPs = createAccessPathArray(baseSet,
					SinkSource.Source);
			AccessPath[] sinkBaseAPs = createAccessPathArray(baseSet,
					SinkSource.Sink);

			AccessPath[] sourceReturnAPs = createAccessPathArray(returnSet,
					SinkSource.Source);
			AccessPath[][] sourceParameterAPs = new AccessPath[paramLength][];
			AccessPath[][] sinkParameterAPs = new AccessPath[paramLength][];

			for (int i = 0; i < paramLength; i++) {
				assert (paramSet != null);
				if (paramSet.containsKey(i)) {
					sourceParameterAPs[i] = createAccessPathArray(
							paramSet.get(i), SinkSource.Source);
				} else {
					sourceParameterAPs[i] = null;
				}
			}

			for (int i = 0; i < paramLength; i++) {
				assert (paramSet != null);
				if (paramSet.containsKey(i)) {
					sinkParameterAPs[i] = createAccessPathArray(
							paramSet.get(i), SinkSource.Sink);
				} else {
					sourceParameterAPs[i] = null;
				}

			}

			amapbReturn = new AndroidMethodAccessPathBundle(sourceBaseAPs,
					sinkBaseAPs, sourceParameterAPs, sinkParameterAPs,
					sourceReturnAPs);

			break;
		case "accesspath":
			// could only be true if signatures are equal

			AccessPath acPath = null;
			if (accessPathLength == 0) {
				assert (localBase != null);
				if (localBase != null) {
					acPath = new AccessPath(localBase, false);
				}
			} else {
				// SootField[] fields
				assert (fields.size() == accessPathLength);
				SootField[] fieldsArray = fields.toArray(new SootField[fields.size()]);
				if (localBase != null) {
					acPath = new AccessPath(localBase, fieldsArray, false);
				}
			}

			SinkSource sinkSource = SinkSource.None;
			if (isSink && !isSource) {
				sinkSource = SinkSource.Sink;
			}
			if (!isSink && isSource) {
				sinkSource = SinkSource.Source;
			}
			if (!isSink && !isSource) {
				sinkSource = SinkSource.None;
			}
			if (isSink && isSource) {
				sinkSource = SinkSource.Both;
			}
			if (bBase) {
				baseSet.add(new AccessPathTuple(acPath, sinkSource));
			}
			if (bReturn) {
				returnSet.add(new AccessPathTuple(acPath, sinkSource));
			}
			if (bParam) {
				AccessPathTuple help = new AccessPathTuple(acPath, sinkSource);
				paramSet.get(paramIndex).add(help);
			}

			break;
		case "pathelement":
			// doesn't matter what it is
			if (bBase || bReturn || bParam) {
				assert (pathElementType != null);
				String subsignature = pathElementType[pathElementIndex-1]
						+ " " + pathElementContent;
				String superType;
				if (bParam) {
					assert (paramType != null);
					superType = paramType.get(paramIndex);
				} else {
					if (bReturn) {
						superType = returnType;
					} else {
						superType = baseType;
					}
				}
				SootField f = Scene.v().getSootClass(superType)
						.getField(subsignature);
				assert (fields != null);
				fields.add(f);
			}
		default:
			break;
		}
	}

	private AccessPath[] createAccessPathArray(
			HashSet<AccessPathTuple> pathSet, SinkSource sinkSource) {
		HashSet<AccessPath> tempHashSet = new HashSet<AccessPath>();
		if (pathSet != null) {
			for (AccessPathTuple item : pathSet) {
				if (item.sinkSource == sinkSource
						|| item.sinkSource == SinkSource.Both) {
					tempHashSet.add(item.accessPath);
				}
			}
		}
		return tempHashSet.toArray(new AccessPath[tempHashSet.size()]);
	}

	/**
	 * Helper for sinks and sources.
	 * 
	 * @author Jörn Tillmanns
	 */
	private enum SinkSource {
		Sink, Source, Both, None
	}

	/**
	 * Helper to save an AccessPath with the information about sink and sources.
	 * 
	 * @author Jörn Tillmanns
	 * 
	 */
	private class AccessPathTuple {
		public AccessPath accessPath;
		public SinkSource sinkSource;

		public AccessPathTuple(AccessPath accessPath, SinkSource sinkSource) {
			this.accessPath = accessPath;
			this.sinkSource = sinkSource;
		}
	}

	// AccessPathTuble Helper
	private HashSet<AccessPathTuple> baseSet;
	private HashSet<AccessPathTuple> returnSet;
	private HashMap<Integer, HashSet<AccessPathTuple>> paramSet;

	// Variables for Handling the XML Parsing
	private boolean bBase, bReturn, bParam, isSink, isSource;
	private String baseType, returnType;
	private ArrayList<String> paramType;
	private String[] pathElementType;
	private int paramIndex, pathElementIndex, accessPathLength;
	private String methodSignature;
	private String pathElementContent;

	// Other variables for generating the final return
	// AndroidMethodAccessPathBundle
	private Local localBase;
	private ArrayList<SootField> fields;

	// Variables get as input
	private String signature;
	private String xml;
	// Variable for the final return for the AndroidMethodAccessPathBundle
	private AndroidMethodAccessPathBundle amapbReturn;

	private static final String BASENAME = "tempBase";

}

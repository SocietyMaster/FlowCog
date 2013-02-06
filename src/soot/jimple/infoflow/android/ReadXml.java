package soot.jimple.infoflow.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import soot.G;
import test.AXMLPrinter;
import android.content.res.AXmlResourceParser;

public class ReadXml {
	private Map<String, List<String>> additionalEntryPoints = new HashMap<String, List<String>>();
	
	public void generateAndroidAppPermissionMap(String apk) {
		
		
		File apkF = new File(apk);
		

		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!"); 

		// get xmlFiles
		InputStream xmlFile = null;
		try {
			ZipFile archive = new ZipFile(apkF);
			for (@SuppressWarnings("rawtypes")
			Enumeration entries = archive.entries(); entries.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String entryName = entry.getName();
				// We are dealing with a XML file
				if (entryName.matches(".*\\.xml") && !entryName.equals("AndroidManifest.xml")) {
//					System.out.println("Entry: " + entryName.substring(entryName.lastIndexOf("/")+1,entryName.lastIndexOf(".xml")));
					String fileIdentifier = entryName.substring(entryName.lastIndexOf("/")+1,entryName.lastIndexOf(".xml"));
					xmlFile = archive.getInputStream(entry);
					List<String> additionalEntryPointsMethodName = handleXmlFile(xmlFile);
					xmlFile.close();

					additionalEntryPoints.put(fileIdentifier, additionalEntryPointsMethodName);

				}
			}
		} catch (Exception e) {
			throw new RuntimeException(
					"Error when looking for xml file in apk: " + e);
		}

		if (xmlFile == null) {
			G.v().out.println("Could not find any XML file");
		}

	}
	
	/**
	 *
	 * @param classes all classes that exist in the Manifest file
	 * @return
	 */
	public List<String> getAdditionalEntryPoints(Set<String> classes){
		List<String> returnList = new ArrayList<String>();
		
		for(String set : classes){
			for ( Map.Entry<String, List<String>> elem : additionalEntryPoints.entrySet() ){
				for(String method : elem.getValue()){
					returnList.add("<"+set+": "+method + "()>");
				}
			}
		}
			  
		
		
		return returnList;
	}

	private List<String> handleXmlFile(InputStream xmlFile) throws XmlPullParserException, IOException {
		List<String> additionalEntryPointsMethodName = new ArrayList<String>();
		// process xml file

		AXmlResourceParser parser = new AXmlResourceParser();
		parser.open(xmlFile);
		
		

		String attribute;
		while (true) {
			int type = parser.next();
			if (type == XmlPullParser.END_DOCUMENT) {
				break;
			}
			switch (type) {
			case XmlPullParser.START_DOCUMENT: {
				break;
			}
			case XmlPullParser.START_TAG: {

				String tagName = parser.getName();
				
				//more listeners can be added likewise
				if (tagName.equals("Button")) {
					
					for(int i=0; i < parser.getAttributeCount(); i++){
						attribute = parser.getAttributeName(i);
						if(attribute.equals("onClick")){
							additionalEntryPointsMethodName.add(AXMLPrinter.getAttributeValue(parser,	i));
//							System.out.println(AXMLPrinter.getAttributeValue(parser,i));
						}
					}

				}

				break;
			}
			case XmlPullParser.END_TAG:
				break;
			case XmlPullParser.TEXT:
				break;
			}
		}
		parser.close();
		
		return additionalEntryPointsMethodName;

		
	}

}

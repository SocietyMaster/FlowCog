package soot.jimple.infoflow.android.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;

import test.AXMLPrinter;
import android.content.res.AXmlResourceParser;

public class ProcessManifest {

	private Set<String> entryPointsClasses = new HashSet<String>();
	private String packageName = "";
	private Set<String> permissions = new HashSet<String>();
	
	/**
	 * Opens the given apk file and provides the given handler with a stream for
	 * accessing the contained android manifest file
	 * @param apk The apk file to process
	 * @param handler The handler for processing the apk file
	 * 
	 * @author Steven Arzt
	 */
	private void handleAndroidManifestFile(String apk, IManifestHandler handler) {
		File apkF = new File(apk);
		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");

		boolean found = false;
		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(apkF);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();
					// We are dealing with the Android manifest
					if (entryName.equals("AndroidManifest.xml")) {
						found = true;
						handler.handleManifest(archive.getInputStream(entry));
						break;
					}
				}
			}
			finally {
				if (archive != null)
					archive.close();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Error when looking for manifest in apk: " + e);
		}
		if (!found)
			throw new RuntimeException("No manifest file found in apk");
	}
	
	public void loadManifestFile(String apk) {
		handleAndroidManifestFile(apk, new IManifestHandler() {
			
			@Override
			public void handleManifest(InputStream stream) {
				loadClassesFromBinaryManifest(stream);
			}
			
		});
	}
	
	protected void loadClassesFromBinaryManifest(InputStream manifestIS) {
		try {
			AXmlResourceParser parser = new AXmlResourceParser();
			parser.open(manifestIS);

			int type = -1;
			while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
				switch (type) {
					case XmlPullParser.START_DOCUMENT:
						break;
					case XmlPullParser.START_TAG:
						String tagName = parser.getName();
						if (tagName.equals("manifest"))
							this.packageName = getAttributeValue(parser, "package");
						else if (tagName.equals("activity")
								|| tagName.equals("receiver")
								|| tagName.equals("service")
								|| tagName.equals("provider")) {
							String attrValue = getAttributeValue(parser, "name");
							if (attrValue.startsWith("."))
								entryPointsClasses.add(this.packageName + attrValue);
							else if (attrValue.substring(0, 1).equals(attrValue.substring(0, 1).toUpperCase()))
								entryPointsClasses.add(this.packageName + "." + attrValue);
							else
								entryPointsClasses.add(attrValue);
						}
						else if (tagName.equals("uses-permission")) {
							String permissionName = getAttributeValue(parser, "android:name");
							// We probably don't want to do this in some cases, so leave it
							// to the user
							// permissionName = permissionName.substring(permissionName.lastIndexOf(".") + 1);
							this.permissions.add(permissionName);
						}
						break;
					case XmlPullParser.END_TAG:
						break;
					case XmlPullParser.TEXT:
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getAttributeValue(AXmlResourceParser parser, String attributeName) {
		for (int i = 0; i < parser.getAttributeCount(); i++)
			if (parser.getAttributeName(i).equals(attributeName))
				return AXMLPrinter.getAttributeValue(parser, i);
		return "";
	}

	protected void loadClassesFromTextManifest(InputStream manifestIS) {
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.parse(manifestIS);
			
			Element rootElement = doc.getDocumentElement();
			this.packageName = rootElement.getAttribute("package");
			
			NodeList appsElement = rootElement.getElementsByTagName("application");
			for (int appIdx = 0; appIdx < appsElement.getLength(); appIdx++) {
				Element appElement = (Element) appsElement.item(appIdx);

				NodeList activities = appElement.getElementsByTagName("activity");
				NodeList receivers = appElement.getElementsByTagName("receiver");
				NodeList services  = appElement.getElementsByTagName("service");
				
				for (int i = 0; i < activities.getLength(); i++) {
					Element activity = (Element) activities.item(i);
					loadManifestEntry(activity, "android.app.Activity", this.packageName);
				}
				for (int i = 0; i < receivers.getLength(); i++) {
					Element receiver = (Element) receivers.item(i);
					loadManifestEntry(receiver, "android.content.BroadcastReceiver", this.packageName);
				}
				for (int i = 0; i < services.getLength(); i++) {
					Element service = (Element) services.item(i);
					loadManifestEntry(service, "android.app.Service", this.packageName);
				}
				
				NodeList permissions = appElement.getElementsByTagName("uses-permission");
				for (int i = 0; i < permissions.getLength(); i++) {
					Element permission = (Element) permissions.item(i);
					this.permissions.add(permission.getAttribute("android:name"));
				}
			}			
		}
		catch (IOException ex) {
			System.err.println("Could not parse manifest: " + ex.getMessage());
			ex.printStackTrace();
		} catch (ParserConfigurationException ex) {
			System.err.println("Could not parse manifest: " + ex.getMessage());
			ex.printStackTrace();
		} catch (SAXException ex) {
			System.err.println("Could not parse manifest: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private void loadManifestEntry(Element activity, String baseClass, String packageName) {
		String className = activity.getAttribute("android:name");		
		if (className.startsWith("."))
			entryPointsClasses.add(packageName + className);
		else if (className.substring(0, 1).equals(className.substring(0, 1).toUpperCase()))
			entryPointsClasses.add(packageName + "." + className);
		else
			entryPointsClasses.add(className);
	}

	public Set<String> getEntryPointClasses() {
		return this.entryPointsClasses;
	}
	
	public Set<String> getPermissions() {
		return this.permissions;
	}

	public String getPackageName() {
		return this.packageName;
	}
	
}

package soot.jimple.infoflow.android;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;

import soot.G;
import test.AXMLPrinter;
import android.content.res.AXmlResourceParser;

public class ProcessManifest {

	public List<String> getAndroidAppPermissionList(String apk) {

		File apkF = new File(apk);
		List<String> permissionArray = new ArrayList<String>();


		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");

		// get AndroidManifest
		InputStream manifestIS = null;
		try {
			ZipFile archive = new ZipFile(apkF);
			for (@SuppressWarnings("rawtypes")
			Enumeration entries = archive.entries(); entries.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String entryName = entry.getName();
				// We are dealing with the Android manifest
				if (entryName.equals("AndroidManifest.xml")) {
					manifestIS = archive.getInputStream(entry);
					break;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(
					"Error when looking for manifest in apk: " + e);
		}

		if (manifestIS == null) {
			G.v().out.println("Could not find  Android manifest!");

		} else {

			// process AndroidManifest.xml
			try {
				AXmlResourceParser parser = new AXmlResourceParser();
				parser.open(manifestIS);

				String permission;
				while (true) {
					int type = parser.next();
					if (type == XmlPullParser.END_DOCUMENT) {
						// throw new RuntimeException
						// ("target sdk version not found in Android manifest ("+
						// apkF +")");
						break;
					}
					switch (type) {
					case XmlPullParser.START_DOCUMENT: {
						break;
					}
					case XmlPullParser.START_TAG: {

						String tagName = parser.getName();
						if (tagName.equals("uses-permission")) {
							permission = AXMLPrinter.getAttributeValue(parser,
									0);
							
//							System.out.println(permission.substring(0,
//									permission.lastIndexOf(".")));
							if (permission.substring(0,
									permission.lastIndexOf(".")).equals(
									"android.permission")){
								
								permission = permission.substring(permission
										.lastIndexOf(".") + 1);
								permissionArray.add(permission);
//								System.out.println("Permission gefunden:"
//										+ permission);
							}
							// personal permissions
							else{
								permissionArray.add(permission);
							}

						}

						break;
					}
					case XmlPullParser.END_TAG:
						// depth--;
						break;
					case XmlPullParser.TEXT:
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}


		}


		return permissionArray;

	}

}

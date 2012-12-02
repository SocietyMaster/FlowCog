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

	private List<String> entryPointsClasses = new ArrayList<String>();
	private List<Class> classes = new ArrayList<Class>();

	private String createSootEntrypoints(String className, String methodName) {
		String completeName = "<" + className + ": " + methodName + ">";

		return completeName;
	}
	
	public List<String> getEntryPoints(){
		List<String> entryPoints = new ArrayList<String>();
		for(int i=0;i<classes.size();i++){
			entryPoints.addAll(classes.get(i).getMethods());
		}
		
		return entryPoints;
	}
	
	public List<String> getAndroidAppEntryPointsWithClass(List<SootLine> methodNames) {

		List<String> entrypoints = new ArrayList<String>();
		
		for(int i = 0; i<classes.size();i++){
			
			for (int j = 0; j < methodNames.size(); j++) {
			
				if (classes.get(i).getExtendsClass()
						.equals(methodNames.get(j).getClassName()) ||
						classes.get(i).getClassName().equals(methodNames.get(j).getClassName())) {

					classes.get(i).addMethod(methodNames.get(j).getMethod());

				}
			}
		}

		return entrypoints;
	}
	
	public void printClasses(){
		
		for(int i=0;i<classes.size();i++){
			
			classes.get(i).printClass();
		}
	}

	public List<String> getAndroidAppEntryPoints(List<String> methodNames) {

		List<String> entrypoints = new ArrayList<String>();

		for (int i = 0; i < entryPointsClasses.size(); i++) {
			int indexI = entryPointsClasses.get(i).indexOf(";");
			for (int j = 0; j < methodNames.size(); j++) {
				int indexJ = methodNames.get(j).indexOf(";");
				if (entryPointsClasses.get(i).substring(0, indexI)
						.equals(methodNames.get(j).substring(0, indexJ))) {
					entrypoints.add(createSootEntrypoints(entryPointsClasses
							.get(i).substring(indexI + 1), methodNames.get(j)
							.substring(indexJ + 1)));

				}
			}
		}

		return entrypoints;
	}

	public List<String> getAndroidAppEntryPointsClassesList(String apk) {
		File apkF = new File(apk);

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

				String packagename = "";

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

						if (tagName.equals("manifest")) {

							for (int i = 0; i < parser.getAttributeCount(); i++) {
								if (parser.getAttributeName(i)
										.equals("package")) {
									packagename = AXMLPrinter
											.getAttributeValue(parser, i);

								}
							}

						}
						if (tagName.equals("activity")
								|| tagName.equals("receiver")
								|| tagName.equals("service")) {
							
							String extentClass;
							if(tagName.equals("activity")){
								extentClass = "android.app.Activity";
							}
							else if(tagName.equals("receiver")){
								extentClass = "android.content.BroadcastReceiver";
							}
							else{
								extentClass = "android.app.Service";
							}

							

							for (int i = 0; i < parser.getAttributeCount(); i++) {
								if (parser.getAttributeName(i).equals("name")) {
									String attrValue = AXMLPrinter
											.getAttributeValue(parser, i);
									if (attrValue.startsWith(".")) {
										classes.add(new Class(extentClass, packagename + attrValue));

										entryPointsClasses.add(tagName + ";"
												+ packagename + attrValue);
									} else if (attrValue.substring(0, 1)
											.equals(attrValue.substring(0, 1)
													.toUpperCase())) {
										classes.add(new Class(extentClass, packagename + "."
												+ attrValue));

										entryPointsClasses
												.add(tagName + ";"
														+ packagename + "."
														+ attrValue);
									} else {
										classes.add(new Class(extentClass, attrValue));

										entryPointsClasses.add(tagName + ";"
												+ attrValue);

									}

								}
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
		return entryPointsClasses;
	}

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

							// System.out.println(permission.substring(0,
							// permission.lastIndexOf(".")));
							if (permission.substring(0,
									permission.lastIndexOf(".")).equals(
									"android.permission")) {

								permission = permission.substring(permission
										.lastIndexOf(".") + 1);
								permissionArray.add(permission);
								// System.out.println("Permission gefunden:"
								// + permission);
							}
							// personal permissions
							else {
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

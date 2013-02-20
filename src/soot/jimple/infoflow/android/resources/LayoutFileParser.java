package soot.jimple.infoflow.android.resources;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;

import test.AXMLPrinter;
import android.content.res.AXmlResourceParser;

public class LayoutFileParser {
	
	private final Map<String, String> userControls = new HashMap<String, String>();
	
	/**
	 * Opens the given apk file and provides the given handler with a stream for
	 * accessing the contained resource manifest files
	 * @param apk The apk file to process
	 * @param handler The handler for processing the apk file
	 * 
	 * @author Steven Arzt
	 */
	private void handleAndroidResourceFiles(String apk, IResourceHandler handler) {
		File apkF = new File(apk);
		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");

		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(apkF);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();

					// We are dealing with resource files
					if (entryName.startsWith("res/layout"))
						handler.handleResourceFile(archive.getInputStream(entry));
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
	}

	/**
	 * Parses all layout XML files in the given APK file and loads the IDs of
	 * the user controls in it.
	 * @param fileName The APK file in which to look for user controls
	 */
	public void parseLayoutFile(String fileName) {
		handleAndroidResourceFiles(fileName, new IResourceHandler() {
			
			@Override
			public void handleResourceFile(InputStream stream) {
				try {
					AXmlResourceParser parser = new AXmlResourceParser();
					parser.open(stream);

					boolean inLayout = false;
					int type = -1;
					while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
						switch (type) {
							case XmlPullParser.START_DOCUMENT:
								break;
							case XmlPullParser.START_TAG:
								String tagName = parser.getName();
								if (tagName.equals("AbsoluteLayout")
										|| tagName.equals("FrameLayout")
										|| tagName.equals("GridLayout")
										|| tagName.equals("LinearLayout")
										|| tagName.equals("RelativeLayout")
										|| tagName.equals("SlidingDrawer")
										)
									inLayout = true;
								if (inLayout && (tagName.equals("EditText")
										|| tagName.equals("CheckBox"))) {
									userControls.put(tagName, getAttributeValue(parser, "android:id"));
								}
								break;
							case XmlPullParser.END_TAG:
								inLayout = false;
								break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private String getAttributeValue(AXmlResourceParser parser, String attributeName) {
		for (int i = 0; i < parser.getAttributeCount(); i++)
			if (parser.getAttributeName(i).equals(attributeName))
				return AXMLPrinter.getAttributeValue(parser, i);
		return "";
	}
	
	public Map<String, String> getUserControls() {
		return this.userControls;
	}

}

package soot.jimple.infoflow.android.resources;

import java.io.InputStream;
import java.util.Set;

/**
 * Common interface for handlers working on Android resource XML files
 * 
 * @author Steven Arzt
 *
 */
public interface IResourceHandler {
	
	/**
	 * Called when the contents of an Android resource file shall be processed
	 * @param fileName The name of the file in the APK being processed
	 * @param fileNameFilter A list of names to be used for filtering the files
	 * in the APK that actually get processed.
	 * @param stream The stream through which the resource file can be accesses
	 */
	public void handleResourceFile(String fileName, Set<String> fileNameFilter, InputStream stream);

}

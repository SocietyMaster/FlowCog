package soot.jimple.infoflow.android.resources;

import java.io.InputStream;

/**
 * Common interface for handlers working on Android resource XML files
 * 
 * @author Steven Arzt
 *
 */
public interface IResourceHandler {
	
	/**
	 * Called when the contents of an Android resource file shall be processed
	 * @param stream The stream through which the resource file can be accesses
	 */
	public void handleResourceFile(InputStream stream);

}

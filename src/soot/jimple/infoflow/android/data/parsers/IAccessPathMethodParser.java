package soot.jimple.infoflow.android.data.parsers;

import java.io.IOException;

import soot.jimple.infoflow.android.data.AndroidMethodAccessPathBundle;

/**
 * Only Source/Sink Parsers implementing this interface can be used by the SourceSinkManager for access paths.
 * @author Daniel Magin
 */

public interface IAccessPathMethodParser {
	
	/**
	 * Reads the access paths of the given method from the Source and Sinks file.
	 * @param signature of the method as returned by SootMethod.getSignature().
	 * @return all access paths (either sources or sinks) of the given method.
	 */
	AndroidMethodAccessPathBundle getAccessPaths(String signature) throws IOException;
}

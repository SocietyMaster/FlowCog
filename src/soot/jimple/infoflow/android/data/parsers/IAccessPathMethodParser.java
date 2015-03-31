package soot.jimple.infoflow.android.data.parsers;

import soot.jimple.infoflow.source.AccessPathBundle;

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
	AccessPathBundle getAccessPaths(String signature);
}

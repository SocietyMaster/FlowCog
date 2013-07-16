package soot.jimple.infoflow.android.data.parsers;

import java.io.IOException;
import java.util.Set;

import soot.jimple.infoflow.android.data.AndroidMethod;

/**
 * Common interface for all parsers that are able to read in files with Android
 * methods and permissions
 *
 * @author Steven Arzt
 *
 */
public interface IPermissionMethodParser {
	
	Set<AndroidMethod> parse() throws IOException;

}

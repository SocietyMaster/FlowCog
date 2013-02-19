package soot.jimple.infoflow.android.data.parsers;

import java.io.IOException;
import java.util.List;

import soot.jimple.infoflow.android.data.AndroidMethod;

/**
 * Common interface for all parsers that are able to read in files with Android
 * methods and permissions
 *
 * @author Steven Arzt
 *
 */
public interface IPermissionMethodParser {
	
	List<AndroidMethod> parse() throws IOException;

}

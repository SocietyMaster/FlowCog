/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
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

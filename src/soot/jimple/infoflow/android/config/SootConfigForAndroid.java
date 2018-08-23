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
package soot.jimple.infoflow.android.config;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.options.Options;

public class SootConfigForAndroid implements IInfoflowConfig{

	@Override
	public void setSootOptions(Options options) {
		// explicitly include packages for shorter runtime:
		List<String> excludeList = new LinkedList<String>();
		excludeList.add("java.*");
		excludeList.add("sun.misc.*");
		excludeList.add("android.*");
		excludeList.add("org.apache.*");
		excludeList.add("soot.*");
		excludeList.add("javax.servlet.*");
		options.set_exclude(excludeList);
		Options.v().set_no_bodies_for_excluded(true);
		options.set_output_format(Options.output_format_none);
	}

}

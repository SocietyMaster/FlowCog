package soot.jimple.infoflow.android;

import java.util.LinkedList;
import java.util.List;

import soot.jimple.infoflow.config.IInfoflowSootConfig;
import soot.options.Options;

public class SootConfigForAndroid implements IInfoflowSootConfig{

	@Override
	public void setSootOptions(Options options) {
		// explicitly include packages for shorter runtime:
		List<String> includeList = new LinkedList<String>();
		includeList.add("java.lang.");
		includeList.add("java.util.");
		includeList.add("java.io.");
		includeList.add("sun.misc.");
		includeList.add("android.");
		includeList.add("org.apache.http.");
		includeList.add("de.test.");
		includeList.add("soot.");
		includeList.add("com.example.");
		includeList.add("com.jakobkontor.");
		includeList.add("java.net.");
		includeList.add("libcore.icu.");
		includeList.add("securibench.");
		includeList.add("javax.servlet.");
		options.set_include(includeList);
		Options.v().set_no_bodies_for_excluded(false);
		options.set_output_format(Options.output_format_none);
	}

}

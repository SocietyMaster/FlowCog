package soot.jimple.infoflow.android;

import java.util.LinkedList;
import java.util.List;

import soot.PackManager;
import soot.Scene;
import soot.options.Options;

public class JimpleBuilder {
	

	public void buildJimple(String androidjarPath, String apkFileLocation){
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_src_prec(Options.src_prec_apk);
		List<String> list = new LinkedList<String>();
		list.add(apkFileLocation);
		Options.v().set_process_dir(list);
		Options.v().set_android_jars(androidjarPath);
		Options.v().set_output_format(Options.output_format_jimple);
		Options.v().set_output_dir("JimpleOutput");
		soot.Main.v().autoSetOptions();
		
		System.out.println("Generating the Jimple Files");
		 Scene.v().loadNecessaryClasses();
		 PackManager.v().runPacks();
         PackManager.v().writeOutput();
         soot.G.reset();

	}

}

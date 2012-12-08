package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyzeJimpleClass {

	private List<String> methodList = new ArrayList<String>();
//	private Set<Class> classes = new HashSet<Class>();
	private Map<String, HashSet<String>> classes = new HashMap<String, HashSet<String>>();
	private Set<String> extendsImplementsClasses = new HashSet<String>();

	public void collectAndroidMethods(String jimpleDir) throws IOException {
		//search directory and process every file
		File f = new File(jimpleDir);
		File[] files = f.listFiles();
		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				if (!files[i].isDirectory()) {
					analyzeClass(files[i].getAbsolutePath());
				} else {
					System.err.println("unexpected directory in output folder");
				}

			}
		}

	}

	private void analyzeClass(String filename) throws IOException {

		if (checkFilename(filename)) {
			FileReader fReader = new FileReader(filename);
			BufferedReader bReader = new BufferedReader(fReader);
			// System.out.println(filename);
			String zeile;
			String extendsClass;
			String className;
			List<String> implementsClass = new ArrayList<String>();

			//only in the first line there could be class name, extends and implements
			if ((zeile = bReader.readLine()) != null) {
				// find class name
				if (zeile.matches(".*(\\s|.{0})(class|interface)\\s.*")) {
					String[] splitClassname = zeile.split("(\\s|.{0})(class|interface)\\s");

					splitClassname[1] = splitClassname[1].trim();
					int spaceIndex = splitClassname[1].indexOf(" ");
					if (spaceIndex != -1) {
						className = splitClassname[1].substring(0, spaceIndex);
					} else {
						className = splitClassname[1];
					}


					
				}
				else{
					throw new IOException("unexpected file" + zeile);
				}
				// find extend class
				if (zeile.matches(".*\\sextends\\s.*")) {
					String[] splitExtends = zeile.split("\\sextends\\s");
					splitExtends[1] = splitExtends[1].trim();
					
					int spaceIndex = splitExtends[1].indexOf(" ");
					if (spaceIndex != -1) {
						extendsClass = splitExtends[1].substring(0, spaceIndex);

					} else {
						extendsClass = splitExtends[1];
					}
					if (!extendsClass.matches("java.lang.Object")){
						extendsImplementsClasses.add(extendsClass);
					}

				}

				// find implement class
				if (zeile.matches(".*\\simplements\\s.*")) {
					String[] splitImplements = zeile.split("\\simplements\\s");
					splitImplements[1] = splitImplements[1].trim();
					
					for (String item : splitImplements[1].split(",")) {
						implementsClass.add(item.trim());
						
					}
					extendsImplementsClasses.addAll(implementsClass);

				}
				
				boolean looksLikeMethod = false;
				String savedLine = null;
				//analyze rest of the class
				while ((zeile = bReader.readLine()) != null) {
					if(looksLikeMethod){
						//it looks for {
						if(zeile.matches("\\s*\\{.*")){
		
							newEntry(className, handleLine(savedLine.trim()));
//							Class toBeAddedClass = readClass(className, savedLine.trim());
//							classes.add(toBeAddedClass);

						}
						else{
							looksLikeMethod = false;
						}
					}
					//every method that starts with on
					if(zeile.matches(".*\\son.*(.*)")){
						looksLikeMethod = true;
						savedLine = zeile;
					}


				}

			}

			bReader.close();
			fReader.close();
		}
	}
	
	private String handleLine(String line) {
		if(line.matches(".*(\\s|.{0})(public|private|protected)\\s.*")){
			
			String[] splitMethodName = line.split("(\\s|.{0})(public|private|protected)\\s");
			
			line = splitMethodName[1].trim();
//			System.out.println(line);
		}
		if(line.matches(".*(\\s|.{0})(volatile)\\s.*")){
//			System.out.println("ho");
			String[] splitMethodName = line.split("(\\s|.{0})(volatile)\\s");

			line = splitMethodName[1].trim();
//			line = line.substring(line.indexOf("volatile") + "volatile".length());
		}
		return line;
	}

	private void newEntry(String className, String method) {
		//does the class already exist?
		if(classes.containsKey(className)){
			classes.get(className).add(method);
		}
		else{
			HashSet<String> newEntry = new HashSet<String>();
			newEntry.add(method);
			
			classes.put(className, newEntry);
		}
		
	}
	
	public void printStrings(){
		for ( Map.Entry<String, HashSet<String>> e : classes.entrySet() ){
			System.out.println( e.getKey() + "="+ e.getValue() );
		}
	}
	
	public List<String> getEntryPoints(){
		List<String> entryPoints = new ArrayList<String>();
		for ( Map.Entry<String, HashSet<String>> e : classes.entrySet() ){
			for(String i : e.getValue()){
				entryPoints.add("<".concat(e.getKey()).concat(": ").concat(i).concat(">"));
			}
		}
		
		return entryPoints;
	}
	
	public void searchSources(String sourceFolder) throws IOException{
		for(String i : extendsImplementsClasses){
			String fileName = i.replace(".", "/");
			
			int index = fileName.indexOf("$");
			if(index!=-1){
				fileName = fileName.substring(0, index);
			}
			fileName = sourceFolder + fileName +".java";
//			System.out.println(fileName);
//			analyzeClass(fileName);
//			System.out.println(fileName);
//		FileReader fReader = new FileReader(filename);
//		BufferedReader bReader = new BufferedReader(fReader);
		}
	}

//	private Class readClass(String className, String line) throws IOException{
//		Class classElem = new Class(className);
////		int posSpace = line.indexOf(" ");
////		if(posSpace==-1){
////			throw new IOException("interpreting line error (1)");
////		}
////		else{
//			classElem.addMethod(line);
//			
////		}
//		
//		return classElem;
//	}
	
	/**checks if the file is a .jimple
	 * 
	 * @param filename
	 * @return true if it is a .jimple file
	 */
	private boolean checkFilename(String filename) {

		return filename.endsWith(".jimple");
	}

}

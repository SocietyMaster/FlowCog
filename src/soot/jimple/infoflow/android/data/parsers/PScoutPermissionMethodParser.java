package soot.jimple.infoflow.android.data.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.jimple.infoflow.android.data.AndroidMethod;

/**
 * Parser of the permissions to method map from the University of Toronto (PScout)
 * 
 * @author Siegfried Rasthofer
 */
public class PScoutPermissionMethodParser implements IPermissionMethodParser {
	private final String fileName;
	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>.+?(->.+)?$";
	
	public PScoutPermissionMethodParser(String filename){
		this.fileName = filename;
	}
	
	@Override
	public List<AndroidMethod> parse() throws IOException {
		List<AndroidMethod> methodList = new ArrayList<AndroidMethod>();
		BufferedReader rdr = readFile();
		
		String line = null;
		Pattern p = Pattern.compile(regex);
		String currentPermission = null;
		
		while ((line = rdr.readLine()) != null) {
			if(line.startsWith("Permission:"))
				currentPermission = line.substring(11);
			else{
				Matcher m = p.matcher(line);
				if(m.find()) {
					AndroidMethod singleMethod = parseMethod(m, currentPermission);
					if(methodList.contains(singleMethod)){
						int methodIndex = methodList.lastIndexOf(singleMethod);
						methodList.get(methodIndex).addPermission(currentPermission);
					}
					else	
						methodList.add(singleMethod);
				}
			}
		}
		
		try {
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return methodList;
	}
	
	private BufferedReader readFile(){
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		} 
		
		return br;
	}
	
	private AndroidMethod parseMethod(Matcher m, String currentPermission) {
		assert(m.group(1) != null && m.group(2) != null && m.group(3) != null 
				&& m.group(4) != null);
		AndroidMethod singleMethod;
		int groupIdx = 1;
		
		//class name
		String className = m.group(groupIdx++).trim();
		
		//return type
		String returnType = m.group(groupIdx++).trim();

		
		//method name
		String methodName = m.group(groupIdx++).trim();
		
		//method parameter
		List<String> methodParameters = new ArrayList<String>();
		String params = m.group(groupIdx++).trim();
		if (!params.isEmpty())
			for (String parameter : params.split(","))
				methodParameters.add(parameter.trim());
		
		//permissions
		Set<String> permissions = new HashSet<String>();
		permissions.add(currentPermission);
		
		//create method signature
		singleMethod = new AndroidMethod(methodName, methodParameters, returnType, className, permissions);
		
		if(m.group(5) != null){
			String targets = m.group(5).substring(3);
			
			for(String target : targets.split(" "))
				if(target.equals("_SOURCE_"))
					singleMethod.setSource(true);
				else if(target.equals("_SINK_"))
					singleMethod.setSink(true);
				else if(target.equals("_NONE_"))
					singleMethod.setNeitherNor(true);
				else
					throw new RuntimeException("error in target definition");
		}
		
		
		return singleMethod;
	}
}

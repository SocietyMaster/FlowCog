package soot.jimple.infoflow.android;

import java.util.ArrayList;
import java.util.List;

public class Class {
	
	private String className;
	private List<String> methods = new ArrayList<String>();
	
	public Class(String className){
		this.className = className;
	}

	
	public void addMethod(String method){
		methods.add(method);
	}
	
	public void printClass(){
		
		for(int i=0;i<methods.size();i++){
			
			System.out.println("<" + className + ": " + methods.get(i) + ">");
		}
	}
	
	public List<String> getMethods(){
		List<String> returnList = new ArrayList<String>();
		for(int i = 0; i<methods.size();i++){
			returnList.add("<" + className + ": " + methods.get(i) + ">");
		}
		
		
	
		return returnList;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	

}

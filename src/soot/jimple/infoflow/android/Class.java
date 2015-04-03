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

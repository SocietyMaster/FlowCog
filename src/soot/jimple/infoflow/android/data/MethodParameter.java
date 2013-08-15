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
package soot.jimple.infoflow.android.data;

/**
 * Class representing a single parameter of an Android method
 *
 * @author Steven Arzt
 *
 */
public class MethodParameter {
	
	private final String parameterName;
	private final String parameterType;
	
	public MethodParameter(String parameterName, String parameterType) {
		this.parameterName = parameterName;
		this.parameterType = parameterType;
	}
	
	public String getParameterName() {
		return this.parameterName;
	}

	public String getParameterType() {
		return this.parameterType;
	}

}

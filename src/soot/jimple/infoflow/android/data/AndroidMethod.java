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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.infoflow.util.SootMethodRepresentationParser;

/**
 * Class representing a single method in the Android SDK
 *
 * @author Steven Arzt, Siegfried Rasthofer, Daniel Magin, Joern Tillmanns
 *
 */
public class AndroidMethod extends SootMethodAndClass {

	public enum CATEGORY {
		// all categories
		ALL,

		// SOURCES
		NO_CATEGORY, HARDWARE_INFO, UNIQUE_IDENTIFIER, LOCATION_INFORMATION, NETWORK_INFORMATION, ACCOUNT_INFORMATION, EMAIL_INFORMATION, FILE_INFORMATION, BLUETOOTH_INFORMATION, VOIP_INFORMATION, DATABASE_INFORMATION, PHONE_INFORMATION,

		// SINKS
		PHONE_CONNECTION, INTER_APP_COMMUNICATION, VOIP, PHONE_STATE, EMAIL, BLUETOOTH, ACCOUNT_SETTINGS, VIDEO, SYNCHRONIZATION_DATA, NETWORK, EMAIL_SETTINGS, FILE, LOG,

		// SHARED
		AUDIO, SMS_MMS, CONTACT_INFORMATION, CALENDAR_INFORMATION, SYSTEM_SETTINGS, IMAGE, BROWSER_INFORMATION, NFC
	}

	private Set<String> permissions;

	private boolean isSource = false;
	private boolean isSink = false;
	private boolean isNeitherNor = false;

	private CATEGORY category = null;
	
	public AndroidMethod(String methodName, String returnType, String className) {
		super(methodName, className, returnType, new ArrayList<String>());
		this.permissions = null;
	}

	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className) {
		super(methodName, className, returnType, parameters);
		this.permissions = null;
	}

	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className,
			Set<String> permissions) {
		super(methodName, className, returnType, parameters);
		this.permissions = permissions;
	}

	public AndroidMethod(SootMethod sm) {
		super(sm);
		this.permissions = null;
	}

	public AndroidMethod(SootMethodAndClass methodAndClass) {
		super(methodAndClass);
		this.permissions = null;
	}

	public Set<String> getPermissions() {
		return this.permissions == null ? Collections.<String>emptySet()
				: this.permissions;
	}

	public boolean isSource() {
		return isSource;
	}

	public void setSource(boolean isSource) {
		this.isSource = isSource;
	}

	public void addPermission(String permission) {
		if (this.permissions == null)
			this.permissions = new HashSet<>();
		this.permissions.add(permission);
	}

	public boolean isSink() {
		return isSink;
	}

	public void setSink(boolean isSink) {
		this.isSink = isSink;
	}

	public boolean isNeitherNor() {
		return isNeitherNor;
	}

	public void setNeitherNor(boolean isNeitherNor) {
		this.isNeitherNor = isNeitherNor;
	}

	public void setCategory(CATEGORY category) {
		this.category = category;
	}

	public CATEGORY getCategory() {
		return this.category;
	}

	@Override
	public String toString() {
		String s = getSignature();
		if (permissions != null)
			for (String perm : permissions)
				s += " " + perm;

		if (this.isSource || this.isSink || this.isNeitherNor)
			s += " ->";
		if (this.isSource)
			s += " _SOURCE_";
		if (this.isSink)
			s += " _SINK_ ";
		if (this.isNeitherNor)
			s += " _NONE_";

		if (this.category != null)
			s += "|" + category;

		return s;
	}

	public String getSignatureAndPermissions() {
		String s = getSignature();
		if (permissions != null)
			for (String perm : permissions)
				s += " " + perm;
		return s;
	}

	/**
	 * Gets whether this method has been annotated as a source, sink or neither
	 * nor.
	 * 
	 * @return True if there is an annotations for this method, otherwise false.
	 */
	public boolean isAnnotated() {
		return isSource || isSink || isNeitherNor;
	}
	
	/***
	 * Static method to create AndroidMethod from Soot method signature
	 * 
	 * @param signature The Soot method signature
	 * @return The new AndroidMethod object
	 */
	public static AndroidMethod createFromSignature(String signature) {
		if (!signature.startsWith("<"))
			signature = "<" + signature;
		if (!signature.endsWith(">"))
			signature = signature + ">";
		
		SootMethodAndClass smac = SootMethodRepresentationParser.v()
				.parseSootMethodString(signature);
		return new AndroidMethod(smac.getMethodName(), smac.getParameters(),
				smac.getReturnType(), smac.getClassName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((category == null) ? 0 : category.hashCode());
		result = prime * result + (isNeitherNor ? 1231 : 1237);
		result = prime * result + (isSink ? 1231 : 1237);
		result = prime * result + (isSource ? 1231 : 1237);
		result = prime * result
				+ ((permissions == null) ? 0 : permissions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AndroidMethod other = (AndroidMethod) obj;
		if (category != other.category)
			return false;
		if (isNeitherNor != other.isNeitherNor)
			return false;
		if (isSink != other.isSink)
			return false;
		if (isSource != other.isSource)
			return false;
		if (permissions == null) {
			if (other.permissions != null)
				return false;
		} else if (!permissions.equals(other.permissions))
			return false;
		return true;
	}
	
}

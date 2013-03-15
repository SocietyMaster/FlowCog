package soot.jimple.infoflow.android.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.Type;

/**
 * Class representing a single method in the Android SDK
 *
 * @author Steven Arzt
 *
 */
public class AndroidMethod {

	private final String methodName;
	private final List<String> parameters;
	private final String returnType;
	
	private final String className;
	
	private final Set<String> permissions;
	
	private boolean isSource = false;
	private boolean isSink = false;
	private boolean isNeitherNor = false;
	
	public AndroidMethod(String signature) {

		String afterDoublePoint = signature.substring(signature.indexOf(":")+1).trim();
		this.methodName = afterDoublePoint.substring(afterDoublePoint.indexOf(" ") + 1, afterDoublePoint.indexOf("(")).trim();
		String allParams = afterDoublePoint.substring(afterDoublePoint.indexOf("(") + 1, afterDoublePoint.indexOf(")")).trim();
		this.parameters = new ArrayList<String>();
		for (String parameter : allParams.split(","))
			this.parameters.add(parameter.trim());
		this.returnType = afterDoublePoint.substring(0,afterDoublePoint.indexOf(" "));
		this.className = signature.substring(1,signature.indexOf(":"));
		this.permissions = Collections.emptySet();
	}
	
	public AndroidMethod(String methodName, String returnType, String className) {
		this.methodName = methodName;
		this.parameters = Collections.emptyList();
		this.returnType = returnType;
		this.className = className;
		this.permissions = Collections.emptySet();
	}
	
	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className) {
		this.methodName = methodName;
		this.parameters = parameters;
		this.returnType = returnType;
		this.className = className;
		this.permissions = Collections.emptySet();
	}

	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className, Set<String> permissions) {
		this.methodName = methodName;
		this.parameters = parameters;
		this.returnType = returnType;
		this.className = className;
		this.permissions = permissions;
	}
	
	public AndroidMethod(SootMethod sm) {
		this.methodName = sm.getName();
		this.returnType = sm.getReturnType().toString();
		this.className = sm.getDeclaringClass().getName();
		this.parameters = new ArrayList<String>();
		for (Type p: sm.getParameterTypes()) {
			this.parameters.add(p.toString());
		}
		this.permissions = Collections.emptySet();
	}

	public String getMethodName() {
		return this.methodName;
	}
	
	public List<String> getParameters() {
		return this.parameters;
	}
	
	public String getReturnType() {
		return this.returnType;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	public Set<String> getPermissions() {
		return this.permissions;
	}

	public boolean isSource() {
		return isSource;
	}

	public void setSource(boolean isSource) {
		this.isSource = isSource;
	}
	
	public void addPermission(String permission){
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
	
	@Override
	public String toString() {
		String s = "<" + this.className + ": " + (this.returnType.length() == 0 ? "" : this.returnType + " ")
				+ this.methodName + "(";
		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				s += ",";
			s += this.parameters.get(i).trim();
		}
		s += ")>";

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
		return s;
	}
	
	public String getSignature() {
		String s = "<" + this.className + ": " + (this.returnType.length() == 0 ? "" : this.returnType + " ")
				+ this.methodName + "(";
		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				s += ",";
			s += this.parameters.get(i).trim();
		}
		s += ")>";
		return s;
	}

	public String getSubSignature() {
		String s = (this.returnType.length() == 0 ? "" : this.returnType + " ") + this.methodName + "(";
		for (int i = 0; i < this.parameters.size(); i++) {
			if (i > 0)
				s += ",";
			s += this.parameters.get(i).trim();
		}
		s += ")";
		return s;
	}

	@Override
	public boolean equals(Object another) {
		if (super.equals(another))
			return true;
		if (!(another instanceof AndroidMethod))
			return false;
		AndroidMethod otherMethod = (AndroidMethod) another;
		
		if (!this.methodName.equals(otherMethod.methodName))
			return false;
		if (!this.parameters.equals(otherMethod.parameters))
			return false;
		if (!this.className.equals(otherMethod.className))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return this.methodName.hashCode()
				+ this.parameters.hashCode() * 5
//				+ this.returnType.hashCode() * 7
				+ this.className.hashCode() * 11;
	}

	/**
	 * Gets whether this method has been annotated as a source, sink or
	 * neither nor.
	 * @return True if there is an annotations for this method, otherwise
	 * false.
	 */
	public boolean isAnnotated() {
		return isSource || isSink || isNeitherNor;
	}

}

package soot.jimple.infoflow.android.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.infoflow.data.SootMethodAndClass;

/**
 * Class representing a single method in the Android SDK
 *
 * @author Steven Arzt, Siegfried Rasthofer
 *
 */
public class AndroidMethod extends SootMethodAndClass {

	public enum CATEGORY {NO_CATEGORY, HARDWARE_INFO, NFC, PHONE_CONNECTION, INTER_APP_COMMUNICATION, VOIP, CONTACT_INFORMATION, UNIQUE_IDENTIFIER, PHONE_STATE, SYSTEM_SETTINGS, 
		LOCATION_INFORMATION, NETWORK_INFORMATION, EMAIL, SMS_MMS, CALENDAR_INFORMATION, ACCOUNT_INFORMATION, BLUETOOTH, MUSIC, CONNECTION_INFORMATION, ACCOUNT_SETTINGS, VIDEO, AUDIO}

	private final Set<String> permissions;
	
	private boolean isSource = false;
	private boolean isSink = false;
	private boolean isNeitherNor = false;
	
    private CATEGORY category;
		
	public AndroidMethod(String methodName, String returnType, String className) {
		super(methodName, className, returnType, new ArrayList<String>());
		this.permissions = Collections.emptySet();
	}
	
	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className) {
		super(methodName, className, returnType, parameters);
		this.permissions = Collections.emptySet();
	}

	public AndroidMethod(String methodName, List<String> parameters, String returnType, String className, Set<String> permissions) {
		super(methodName, className, returnType, parameters);
		this.permissions = permissions;
	}
	
	public AndroidMethod(SootMethod sm) {
		super(sm);
		this.permissions = Collections.emptySet();
	}
	
	public AndroidMethod(SootMethodAndClass methodAndClass) {
		super (methodAndClass);
		this.permissions = Collections.emptySet();
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
	
	public void setCategory(CATEGORY category){
		this.category = category;
	}
	
	@Override
	public String toString() {
		String s = getSignature();
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
	
	@Override
	public boolean equals(Object another) {
		if (super.equals(another))
			return true;
		if (!(another instanceof AndroidMethod))
			return false;
		AndroidMethod otherMethod = (AndroidMethod) another;
		
		if (!this.getMethodName().equals(otherMethod.getMethodName()))
			return false;
		if (!this.getParameters().equals(otherMethod.getParameters()))
			return false;
		if (!this.getClassName().equals(otherMethod.getClassName()))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return this.getMethodName().hashCode()
				+ this.getParameters().hashCode() * 5
//				+ this.returnType.hashCode() * 7
				+ this.getClassName().hashCode() * 11;
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

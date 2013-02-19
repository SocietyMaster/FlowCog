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

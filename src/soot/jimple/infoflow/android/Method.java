package soot.jimple.infoflow.android;

public class Method {
	
	String method;
	
	public Method(String method){
		this.method = method;
	}
	
//	public String getSootLine(){
//		return "<" + className + ": " + returnValue + " " + methodName + ">";
//	}
	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	

}

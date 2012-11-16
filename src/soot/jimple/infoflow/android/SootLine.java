package soot.jimple.infoflow.android;

public class SootLine {
	
	String className, method;
	
	public SootLine(String className, String method){
		this.className = className;
		this.method = method;
	}
	
	public String getSootLine(){
		return "<" + className + ": " + method + ">";
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	public void println(){
		System.out.println("<" + className + ": " + method + ">");
	}


}

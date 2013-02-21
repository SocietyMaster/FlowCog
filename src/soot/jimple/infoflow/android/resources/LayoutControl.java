package soot.jimple.infoflow.android.resources;

import soot.SootClass;

/**
 * Data class representing a layout control on the android screen
 * 
 * @author Steven Arzt
 *
 */
public class LayoutControl {
	
	private final int id;
	private final SootClass viewClass;
	private boolean isSensitive;
	
	public LayoutControl(int id, SootClass viewClass) {
		this.id = id;
		this.viewClass = viewClass;
	}
	
	public LayoutControl(int id, SootClass viewClass, boolean isSensitive) {
		this(id, viewClass);
		this.isSensitive = isSensitive;
	}
	
	public int getID() {
		return this.id;
	}
	
	public SootClass getViewClass() {
		return this.viewClass;
	}
	
	public void setIsSensitive(boolean isSensitive) {
		this.isSensitive = isSensitive;
	}
	
	public boolean isSensitive() {
		return this.isSensitive;
	}

}

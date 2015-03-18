/**
 * 
 */
package soot.jimple.infoflow.android.source;

import soot.jimple.infoflow.android.data.AndroidMethodAccessPathBundle;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.source.SourceInfo;

/**
 * Extends the SourceInfo method with information about access paths which will be tainted by this source.
 * @author Daniel Magin
 *
 */

//TODO comment all stuff
public class SourceInfoWithAP extends SourceInfo {
	
	private final AndroidMethodAccessPathBundle bundle;

	public SourceInfoWithAP(boolean taintSubFields) {
		super(taintSubFields);
		this.bundle = null;
	}
	
	public SourceInfoWithAP(boolean taintSubFields, Object userData){
		super(taintSubFields, userData);
		this.bundle = null;
	}
	
	public SourceInfoWithAP(boolean taintSubFields, Object userData, AndroidMethodAccessPathBundle bundle){
		super(taintSubFields, userData);
		this.bundle = bundle;
	}

	public AccessPath[] getBaseAPs(){
		return this.bundle.getSourceBaseAPs();
	}
	
	public AccessPath[] getReturnAPs(){
		return this.bundle.getSourceReturnAPs();
	}
	
	public AccessPath[] getParameterAPs(int index){
		return this.bundle.getSourceParameterAPs(index);
	}
	
}

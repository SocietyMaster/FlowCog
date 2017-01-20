package soot.jimple.infoflow.android.nu;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class StmtPosTag implements Tag {
	final static public String TAG_NAME = "StmtPosTag";
	final String pos;
	public StmtPosTag(String pos){
		this.pos = pos;
	}
	@Override
	public String getName() {
		return TAG_NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		// TODO Auto-generated method stub
		return this.pos.getBytes();
	}
	
	public String getPos(){
		return this.pos;
	}
	
	public String toString(){
		return TAG_NAME+":"+pos;
	}

}

package soot.jimple.infoflow.android.nu;

import java.util.HashSet;
import java.util.Set;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class ResolvedConstantTag implements Tag {
	final static public String TAG_NAME = "ResolvedConstantTag";
	final Set<Integer> integerConstants = new HashSet<Integer>();
	final Set<String> stringConstants = new HashSet<String>();
	@Override
	public String getName() {
		
		return TAG_NAME;
	}

	@Override
	public byte[] getValue() throws AttributeValueException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void addIntegerConstant(Integer val){
		integerConstants.add(val);
	}
	
	public void addStringConstant(String val){
		stringConstants.add(val);
	}
	
	public Set<Integer> getIntegerConstants(){
		return integerConstants;
	}
	
	public Set<String> getStringConstants(){
		return stringConstants;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(TAG_NAME);
		sb.append(": IntConstCnt:"+integerConstants.size()+",StrConstCnt:"+stringConstants.size());
		return sb.toString();
	}
}

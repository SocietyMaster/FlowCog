package soot.jimple.infoflow.android.data;

import soot.jimple.infoflow.data.AccessPath;

/**
 * A class to handle all access paths of sources and sinks for a certain method.
 * @author Daniel Magin
 *
 */
public class AndroidMethodAccessPathBundle {

	private final AccessPath[] baseSource, baseSink, retSource;
	private final AccessPath[][] parameterSource, parameterSink;
	
	/**
	 * Sets all but the sourceReturnAPs to null. Useful for the most source methods.
	 * @param sourceReturnAPs all access paths of the return object which will be tainted by this source.
	 */
	public AndroidMethodAccessPathBundle(AccessPath[] sourceReturnAPs){
		this(null, null, null, null, sourceReturnAPs);
	}
	
	/**
	 * Sets all but the sinkParameterAPs to null. Useful for the most sink methods.
	 * @param sinkParamterAPs all access paths of all parameters which will be leaked by this sink.
	 * The index of the array represents the index of the parameter. The first parameter has the index 0.
	 */
	public AndroidMethodAccessPathBundle(AccessPath[][] sinkParamterAPs){
		this(null, null, null, sinkParamterAPs, null);
	}
	
	/**
	 * If there are no source or sink access paths for the base, return or any parameter, just set it null.
	 * @param sourceBaseAPs all access paths of the base object which will be tainted by this source.
	 * @param sinkBaseAPs all access paths  of the base object which will be leaked by this sink.
	 * @param sourceParameterAPs all access paths of all parameters which will be tainted by this source.
	 * The index of the array represents the index of the parameter. The first parameter has the index 0. 							 
	 * @param sinkParameterAPs all access paths of all parameters which will be leaked by this sink.
	 * The index of the array represents the index of the parameter. The first parameter has the index 0.
	 * @param sourceReturnAPs all access paths of the return object which will be tainted by this source.
	 * @param sinkReturnAPs all access paths  of the return object which will be leaked by this sink.
	 */
	public AndroidMethodAccessPathBundle(AccessPath[] sourceBaseAPs, AccessPath[] sinkBaseAPs, AccessPath[][] sourceParameterAPs, AccessPath[][] sinkParameterAPs, AccessPath[] sourceReturnAPs){
		this.baseSource = sourceBaseAPs;
		this.baseSink = sinkBaseAPs;
		this.retSource = sourceReturnAPs;
		this.parameterSource = sourceParameterAPs;
		this.parameterSink = sinkParameterAPs;
	}
	
	public AccessPath[] getSourceBaseAPs(){
		return this.baseSource;
	}
	
	public AccessPath[] getSourceReturnAPs(){
		return this.retSource;
	}
	
	public AccessPath[] getSourceParameterAPs(int index){
		return this.parameterSource[index];
	}
	
	public AccessPath[] getSinkBaseAPs(){
		return this.baseSink;
	}
	
	public AccessPath[] getSinkParamterAPs(int index){
		return this.parameterSink[index];
	}
	
	public int getSourceParameterCount(){
		return this.parameterSource.length;
	}
	
	public int getSinkParameterCount(){
		return this.parameterSink.length;
	}
}

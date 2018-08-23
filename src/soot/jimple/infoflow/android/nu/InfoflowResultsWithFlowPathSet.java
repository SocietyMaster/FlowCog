package soot.jimple.infoflow.android.nu;

import java.util.HashMap;
import java.util.Map;

import soot.jimple.infoflow.nu.FlowPath;
import soot.jimple.infoflow.nu.FlowPathSet;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.util.MultiMap;

public class InfoflowResultsWithFlowPathSet extends InfoflowResults {
	public static String getFlowTag(ResultSourceInfo src, ResultSinkInfo sink){
		//TODO: think about how to generate Flow tag;
		//Note this tag is used for the key of flowPathSetMap
		return src.getSource().toString()+"_"+sink.getSink().toString();
	}
	
	private FlowPathSet flowPathSet;
	
	public FlowPathSet getFlowPathSet() {
		return flowPathSet;
	}

	public void setFlowPathSet(FlowPathSet flowPathSet) {
		this.flowPathSet = flowPathSet;
	}

	public InfoflowResultsWithFlowPathSet(InfoflowResults ifr){
		super();
		MultiMap<ResultSinkInfo, ResultSourceInfo> mm = ifr.getResults();
		for(ResultSinkInfo sink : mm.keySet()){
			for(ResultSourceInfo source : mm.get(sink)){
				this.addResult(sink, source);
			}
		}
		flowPathSet = new FlowPathSet();
	}
}

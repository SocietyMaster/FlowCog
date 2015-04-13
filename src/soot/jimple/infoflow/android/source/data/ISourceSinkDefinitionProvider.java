package soot.jimple.infoflow.android.source.data;

import java.util.Set;

/**
 * Common interface for all clases that support loading source and sink
 * definitions
 * 
 * @author Steven Arzt
 *
 */
public interface ISourceSinkDefinitionProvider {
	
	/**
	 * Gets a set of all sources registered in the provider
	 * @return A set of all sources registered in the provider
	 */
	public Set<SourceSinkDefinition> getSources();
	
	/**
	 * Gets a set of all sinks registered in the provider
	 * @return A set of all sinks registered in the provider
	 */
	public Set<SourceSinkDefinition> getSinks();
	
}

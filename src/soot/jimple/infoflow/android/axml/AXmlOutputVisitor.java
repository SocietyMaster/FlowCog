package soot.jimple.infoflow.android.axml;

import pxb.android.axml.AxmlVisitor;

/**
 * Extends the {@link AxmlVisitor} by applying changes done to the root node and it's children to the output.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class AXmlOutputVisitor extends AxmlVisitor {
	/**
	 * Node containing the changes done to the XML document.
	 */
	protected AXmlNode root;
	
	/**
	 * Creates a new {@link AXmlOutputVisitor}. 
	 * 
	 * @param av
	 * @param root
	 */
    public AXmlOutputVisitor(AxmlVisitor av, AXmlNode root) {
        super(av);
        this.root = root;
    };
    
	@Override
	public NodeVisitor first(String ns, String name) {
		return new OutputVisitor(super.first(ns, name), this.root);
	}
}

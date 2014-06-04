package soot.jimple.infoflow.android.axml;

import java.util.HashSet;
import java.util.Set;

import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;

/**
 * Extends the {@link NodeVisitor} by applying changes done to the node this visitor visits.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class OutputVisitor extends AxmlVisitor {
	/**
	 * Node object which represents the visited node
	 */
	protected AXmlNode node;
	
	/**
	 * Internal counter which indicates the already visited children.
	 */
	protected int childCounter = 0;
	
	/**
	 * Set of nodes already visited
	 */
	protected Set<AXmlNode> visitedNodes = new HashSet<AXmlNode>();
	
	/**
	 * Creates a new {@link OutputVisitor} which applies the changes done to the given node.
	 * 
	 * @param	nv
	 * @param	node	node object which represents the visited node
	 */
	public OutputVisitor(NodeVisitor nv, AXmlNode node) {
		super(nv);
		this.node = node;
		
		// add all attributes which were added and are included
		for(AXmlAttribute<?> attr : this.node.getAttributes().values()) {
			if(attr.isAdded() && attr.isIncluded()) {
				// get resource id
				int resourceId = OutputVisitor.getAttributeResourceId(attr.getName());
				if(resourceId == -1)
					System.out.println("Warning: Attribute '"+attr.getName()+"' is not available on Android. This may lead to an malformed XML!");
				
				this.attr(attr.getNamespace(),
						attr.getName(),
						resourceId,
						attr.getType(),
						attr.getValue());
			}
		}
		
		// if this node is added manually call end method
		if(this.node.isAdded()) this.end();
	}
	
	@Override
	public void attr(String ns, String name, int resourceId, int type, Object obj) {
		AXmlAttribute<?> attr = this.node.getAttributes().get(name);
		
		// check if attribute is available in node's attribute list
		if(attr == null)
			super.attr(ns, name, resourceId, type, obj);
		// only add the attribute if it is included
		else
			if(attr.isIncluded())
				super.attr(attr.getNamespace(), attr.getName(), resourceId, attr.getType(), attr.getValue());
	}
	
	@Override
	public NodeVisitor child(String ns, String name) {
		// Find the next child that has been added
		while (this.childCounter < this.node.getChildren().size()) {
			AXmlNode child = this.node.getChildren().get(this.childCounter++);
			
			// only add the child if it is included
			if(child.isIncluded() && child.isAdded())
				new OutputVisitor(super.child(child.getNamespace(),
						child.getTag()), child);
		}
		
		// Find the correct child in which to continue
		for (AXmlNode child : this.node.getChildren())
			if (child.getTag().equals(name)
					&& safeEquals(child.getNamespace(), ns)
					&& visitedNodes.add(child))
				return new OutputVisitor(super.child(ns, name), child);
		
		// We did not find any further included nodes
		return null;
	}
	
	/**
	 * Checks whether the two values are equal
	 * @param val1
	 * @param val2
	 * @return True if the two values are equal, otherwise false
	 */
	private boolean safeEquals(String val1, String val2) {
		if (val1 == null)
			return val2 == null;
		if (val2 == null)
			return val1 == null;
		return val1.equals(val2);
	}

	@Override
	public void end() {
		// call child method for children not visited yet
		for (AXmlNode child : this.node.getChildren())
			if (child.isAdded() && visitedNodes.add(child))
				this.child(child.getNamespace(), child.getTag());
		
		super.end();
	}
	
	private static final int resId_maxSdkVersion = 16843377;
	private static final int resId_minSdkVersion = 16843276;
	private static final int resId_name = 16842755;
	private static final int resId_onClick = 16843375;
	
	/**
	 * Returns the Android resource Id of the attribute which has the given name.
	 * 
	 * @param	name	the attribute's name.
	 * @return	the resource Id defined by Android or -1 if the attribute does not exist.
	 * @see		android.R.attr
	 */
	public static int getAttributeResourceId(String name) {
		// try to get attribute's resource Id from Androids R class. Since we
		// don't want a hard-coded reference to the Android classes, we maintain
		// our own list.
		if (name.equals("name"))
			return resId_name;
		else if (name.equals("maxSdkVersion"))
			return resId_maxSdkVersion;
		else if (name.equals("minSdkVersion"))
			return resId_minSdkVersion;
		else if (name.equals("onClick"))
			return resId_onClick;
		
		// If we couldn't find the vlaue, try to find Android's R class in Soot
		SootClass rClass = Scene.v().forceResolve("android.R$attr", SootClass.BODIES);
		if (!rClass.declaresFieldByName(name))
			return -1;
		SootField idField = rClass.getFieldByName(name);
		for (Tag t : idField.getTags())
			if (t instanceof IntegerConstantValueTag) {
				IntegerConstantValueTag cvt = (IntegerConstantValueTag) t;
				return cvt.getIntValue();
			}
		return -1;
	}
}

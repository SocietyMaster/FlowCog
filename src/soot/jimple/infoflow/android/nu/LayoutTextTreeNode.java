package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LayoutTextTreeNode {
	public String nodeType;
	public int nodeID = 0;
	public String text = "";
	public LayoutTextTreeNode parent = null;
	public List<LayoutTextTreeNode> children = null;
	
	public LayoutTextTreeNode(String type, LayoutTextTreeNode parent){
		this.nodeType = type;
		this.parent =parent;
	}
	public void addChildNode(LayoutTextTreeNode cn){
		if (children == null)
			children = new LinkedList<LayoutTextTreeNode>();
		
		children.add(cn);
	}
	
	public String toString(){
		//return "<"+nodeType+", id:"+nodeID+", childrenNum:"+(children==null? "0" : children.size())+", Text:"+text+" >";
		return "<"+nodeType+", id:"+nodeID+", Text:"+text+" >";
	}
	
	public String toStringTree(){
		StringBuilder sb = new StringBuilder();
		traverseTextTreeHelper(this, 0, sb);
		return sb.toString();
	}
	
	private void traverseTextTreeHelper(LayoutTextTreeNode node, int level, StringBuilder sb){
		String space = new String(new char[level*2]).replace('\0', ' ');
		sb.append(space+node.toString()+"\n");
		if(node.children != null){
			for(LayoutTextTreeNode child : node.children)
				traverseTextTreeHelper(child, level+1, sb);
		}
	}
}
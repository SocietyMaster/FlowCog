package soot.jimple.infoflow.android.nu;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LayoutTextTreeNode {

	public enum ViewTextType {
		VIEW_TEXT,   /*Texts extracted from the View's text attribute.*/
		PARENT_TEXT, /*Texts extracted from all the Views sharing the same parent (grandparent) View. */
		LAYOUT_TEXT,  /*Texts extracted from all the Views in current layout.*/
		NO_TEXT
	}
	
	public class ViewText {
		public ViewTextType textType;
		public String viewType;
		public String texts; //seperated by ||
		public ViewText(ViewTextType textType, String viewType, String texts){
			this.textType = textType;
			this.viewType = viewType;
			this.texts = texts;
		}
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append("TextType:"+textType+",");
			sb.append("ViewType:"+viewType+",");
			sb.append("ID:"+nodeID+",");
			sb.append("Text:"+texts);
			return sb.toString();
		}
	}
	
	public String nodeType;
	public int nodeID = 0;
	public String text = "";
	
	public String allTexts = "";
	public ViewText textObj = null;
	
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
	
	public String toStringTree(int initSpace, String logo){
		StringBuilder sb = new StringBuilder();
		traverseTextTreeHelper(this, initSpace, sb, logo);
		return sb.toString();
	}
	
	public String extractTexts(String delimiter ){
		StringBuilder sb = new StringBuilder();
		extractTextsHelper(this, delimiter, sb);
		return sb.toString();
	}
	
	private void extractTextsHelper(LayoutTextTreeNode node, String delimiter, StringBuilder sb ){
		if(sb.length()>0) sb.append(" "+delimiter+" ");
		sb.append(node.textObj.toString());	
		
		if(node.children != null){
			for(LayoutTextTreeNode child : node.children)
				extractTextsHelper(child, delimiter, sb);
		}
	}
	
	private void traverseTextTreeHelper(LayoutTextTreeNode node, int level, StringBuilder sb, String logo){
		String space = new String(new char[level*2]).replace('\0', ' ');
		sb.append(logo+space+node.textObj.toString()+"\n");
		if(node.children != null){
			for(LayoutTextTreeNode child : node.children)
				traverseTextTreeHelper(child, level+1, sb, logo);
		}
	}
}
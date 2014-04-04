package soot.jimple.infoflow.android.axml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.AxmlWriter;
import android.content.res.AXmlResourceParser;

/**
 * {@link AXmlHandler} provides functionality to parse a byte compressed android xml file and access all nodes.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class AXmlHandler {
	public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
	
	/**
	 * Contains the byte compressed xml which was parsed by this {@link AXmlHandler}.
	 */
	protected byte[] xml;
	
	/**
	 * The xml document's root node.
	 */
	protected AXmlNode root;
	
	/**
	 * Map containing lists of nodes sharing the same <code>tag</code>.
	 * The <code>tag</code> is the key to access the list.
	 */
	protected HashMap<String, ArrayList<AXmlNode>> nodesWithTag = new HashMap<String, ArrayList<AXmlNode>>();
	
	/**
	 * Creates a new {@link AXmlHandler} which parses the {@link InputStream}.
	 * 
	 * @param	aXmlIs					InputStream reading a byte compressed android xml file
	 * @throws	IOException				if an I/O error occurs.
	 * @throws	XmlPullParserException	can occur due to a malformed Android XML.
	 */
	public AXmlHandler(InputStream aXmlIs) throws IOException, XmlPullParserException {
		// wrap the InputStream within a BufferedInputStream
		// to have mark() and reset() methods
		BufferedInputStream buffer;
		if(aXmlIs instanceof BufferedInputStream) buffer = (BufferedInputStream) aXmlIs;
		else buffer = new BufferedInputStream(aXmlIs);
		buffer.mark(0);

		// read xml one time for writing the output later on
		this.xml = new byte[aXmlIs.available()];
		buffer.read(this.xml);
		buffer.reset();
		
		// init
		AXmlNode node = null;
		AXmlNode parent = null;
		
		// create parser and parse the xml's contents
		AXmlResourceParser parser = new AXmlResourceParser();
		parser.open(buffer);
		
		int type = -1;
		String tag;
		while ((type = parser.next()) != AXmlResourceParser.END_DOCUMENT) {
			switch (type) {
				// Currently nothing to do at the document's start
				case AXmlResourceParser.START_DOCUMENT:
					break;

				// To handle an opening tag we create a new node
				// and fetch the namespace and all attributes
				case AXmlResourceParser.START_TAG:
					tag = parser.getName();
					parent = node;
					node = new AXmlNode(tag, parser.getNamespace(), parent, false);
					this.addPointer(tag, node);
					
					// add attributes to node object
					for(int i = 0; i < parser.getAttributeCount(); i++) {
						String name = parser.getAttributeName(i);
						String ns = parser.getAttributeNamespace(i);
						AXmlAttribute<?> attr = null;
						
						// we only parse attribute of types string, boolean and integer
						switch(parser.getAttributeValueType(i)) {
							case AxmlVisitor.TYPE_STRING:
								attr = new AXmlAttribute<String>(name, parser.getAttributeValue(i), ns, false);
								break;
							case AxmlVisitor.TYPE_INT_BOOLEAN:
								attr = new AXmlAttribute<Boolean>(name, parser.getAttributeBooleanValue(i, false), ns, false);
								break;
							case AxmlVisitor.TYPE_FIRST_INT:
							case AxmlVisitor.TYPE_INT_HEX:
								attr = new AXmlAttribute<Integer>(name, parser.getAttributeIntValue(i, 0), ns, false);
								break;
						}
						
						// if we can't handle the attributes type we simply ignore it
						if(attr != null) node.addAttribute(attr);
					}
					break;
					
				// A closing tag indicates we must move
				// one level upwards in the xml tree
				case AXmlResourceParser.END_TAG:
					this.root = node;
					node = parent;
					parent = (parent == null ? null : parent.getParent());
					break;
					
				// Android XML documents do not contain text
				case AXmlResourceParser.TEXT:
					break;
				
				// Currently nothing to do at the document's end, see loop condition
				case AXmlResourceParser.END_DOCUMENT:
					break;
			}
		}
	}
	
	/**
	 * Adds a pointer to the given <code>node</code> with the key <code>tag</code>.  
	 * 
	 * @param	tag		the node's tag
	 * @param	node	the node being pointed to
	 */
	protected void addPointer(String tag, AXmlNode node) {
		if(!this.nodesWithTag.containsKey(tag)) this.nodesWithTag.put(tag, new ArrayList<AXmlNode>());
		this.nodesWithTag.get(tag).add(node);
	}
	
	/**
	 * Returns the root node.
	 * 
	 * @return	the root node of the xml document
	 */
	public AXmlNode getRoot() {
		return this.root;
	}
	
	/**
	 * Returns a list containing all nodes of the xml document which have the given tag.
	 * 
	 * @param	tag		the tag being search for
	 * @return	list pointing on all nodes which have the given tag.
	 */
	public List<AXmlNode> getNodesWithTag(String tag) {
		if(this.nodesWithTag.containsKey(tag))
			return new ArrayList<AXmlNode>(this.nodesWithTag.get(tag));
		else
			return Collections.emptyList();
	}
	
	/**
	 * Returns the xml document as a compressed android xml byte array.
	 * This will consider all changes made to the root node and it's children.
	 * 
	 * @return	android byte compressed xml
	 */
	public byte[] toByteArray() {
		try {
			AxmlReader ar = new AxmlReader(this.xml);
			AxmlWriter aw = new AxmlWriter();
			
			ar.accept(new AXmlOutputVisitor(aw, this.root));
			
			return aw.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return this.toString(this.root, 0);
	}
	
	/**
	 * Returns a textual representation of the given node and it's data.
	 * 
	 * @param	node	current node which will be printed
	 * @param	depth	for the padding (indent)
	 * @return	String representation of the given node, it's attributes and children
	 */
	protected String toString(AXmlNode node, int depth) {
		StringBuilder sb = new StringBuilder();
		
		// construct indent for pretty console printing
		StringBuilder padding = new StringBuilder();
		for(int i = 0; i < depth; i++) padding.append("	");
		
		// append this nodes tag
		sb.append(padding).append(node.getTag());
		
		// add attributes
		for(AXmlAttribute<?> attr : node.getAttributes().values()) {
			sb.append("\n").append(padding).append("- ").append(attr.getName()).append(": ").append(attr.getValue());
		}
		
		// recursivly append children
		for(AXmlNode n : node.getChildren()) {
			sb.append("\n").append(this.toString(n, depth+1));
		}
		
		return sb.toString();
	}
}

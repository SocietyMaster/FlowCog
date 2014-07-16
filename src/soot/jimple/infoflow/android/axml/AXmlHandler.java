package soot.jimple.infoflow.android.axml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlWriter;
import soot.jimple.infoflow.android.axml.parsers.AXMLPrinter2Parser;
import soot.jimple.infoflow.android.axml.parsers.IBinaryXMLFileParser;

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
	 * The parser used for actually reading out the binary XML file
	 */
	protected final IBinaryXMLFileParser parser;
	
	/**
	 * Creates a new {@link AXmlHandler} which parses the {@link InputStream}.
	 * 
	 * @param	aXmlIs					InputStream reading a byte compressed android xml file
	 * @throws	IOException				if an I/O error occurs.
	 */
	public AXmlHandler(InputStream aXmlIs) throws IOException {
		this(aXmlIs, new AXMLPrinter2Parser());
	}
	
	/**
	 * Creates a new {@link AXmlHandler} which parses the {@link InputStream}.
	 * 
	 * @param	aXmlIs					InputStream reading a byte compressed android xml file
	 * @param	parser					The parser implementation to be used
	 * @throws	IOException				if an I/O error occurs.
	 */
	public AXmlHandler(InputStream aXmlIs, IBinaryXMLFileParser parser) throws IOException {
		if (aXmlIs == null)
			throw new RuntimeException("NULL input stream for AXmlHandler");

		// wrap the InputStream within a BufferedInputStream
		// to have mark() and reset() methods
		BufferedInputStream buffer = new BufferedInputStream(aXmlIs);
		
		// read xml one time for writing the output later on
		{
			List<byte[]> chunks = new ArrayList<byte[]>();
			int bytesRead = 0;
			while (aXmlIs.available() > 0) {
				byte[] nextChunk = new byte[aXmlIs.available()];
				int chunkSize = buffer.read(nextChunk);
				if (chunkSize < 0)
					break;
				chunks.add(nextChunk);
				bytesRead += chunkSize;
			}
			
			// Create the full array
			this.xml = new byte[bytesRead];
			int bytesCopied = 0;
			for (byte[] chunk : chunks) {
				int toCopy = Math.min(chunk.length, bytesRead - bytesCopied);
				System.arraycopy(chunk, 0, this.xml, bytesCopied, toCopy);
				bytesCopied += toCopy;
			}
		}
		
		parser.parseFile(this.xml);
		this.parser = parser;
	}
	
	/**
	 * Returns the root node.
	 * 
	 * @return	the root node of the xml document
	 */
	public AXmlNode getRoot() {
		return parser.getRoot();
	}
	
	/**
	 * Returns a list containing all nodes of the xml document which have the given tag.
	 * 
	 * @param	tag		the tag being search for
	 * @return	list pointing on all nodes which have the given tag.
	 */
	public List<AXmlNode> getNodesWithTag(String tag) {
		return parser.getNodesWithTag(tag);
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
			
			AXmlNode rootNode = new AXmlNode("rootNode", "", null);
			rootNode.addChild(this.getRoot());
			ar.accept(new OutputVisitor(aw, rootNode));
			
			return aw.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return this.toString(this.getRoot(), 0);
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

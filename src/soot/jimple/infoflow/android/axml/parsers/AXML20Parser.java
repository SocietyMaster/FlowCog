package soot.jimple.infoflow.android.axml.parsers;

import java.io.IOException;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import pxb.android.axml.ValueWrapper;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;

/**
 * Class for parsing Android binary XML files using the AXMLPrinter2 library 
 * 
 * @author Steven Arzt
 */
public class AXML20Parser extends AbstractBinaryXMLFileParser {
	
	private class MyNodeVisitor extends AxmlVisitor {
		
		public final AXmlNode node;
		
		public MyNodeVisitor() {
			this.node = new AXmlNode("dummy", "", null);
		}
		
		public MyNodeVisitor(AXmlNode node) {
			this.node = node;
		}
		
		@Override
		public void attr(String ns, String name, int resourceId, int type, Object obj) {
			if (this.node == null)
				throw new RuntimeException("NULL nodes cannot have attributes");
			
    		// Read out the field data
    		String tname = name.trim();
    		if (type == AxmlVisitor.TYPE_REFERENCE
    				|| type == AxmlVisitor.TYPE_INT_HEX
    				|| type == AxmlVisitor.TYPE_FIRST_INT) {
    			if (obj instanceof Integer)
    				this.node.addAttribute(new AXmlAttribute<Integer>(tname, (Integer) obj, ns));
    			else if (obj instanceof ValueWrapper) {
    				ValueWrapper wrapper = (ValueWrapper) obj;
    				this.node.addAttribute(new AXmlAttribute<Integer>(tname, Integer.valueOf(wrapper.raw), ns));
    			}
    			else
    				throw new RuntimeException("Unsupported value type");
    		}
    		else if (type == AxmlVisitor.TYPE_STRING) {
    			if (obj instanceof String)
    				this.node.addAttribute(new AXmlAttribute<String>(tname, (String) obj, ns));
    			else if (obj instanceof ValueWrapper) {
    				ValueWrapper wrapper = (ValueWrapper) obj;
    				this.node.addAttribute(new AXmlAttribute<String>(tname, wrapper.raw, ns));
    			}
    			else
    				throw new RuntimeException("Unsupported value type");
    		}
    		else if (type == AxmlVisitor.TYPE_INT_BOOLEAN)
    			this.node.addAttribute(new AXmlAttribute<Boolean>(tname, (Boolean) obj, ns));
    		
    		super.attr(ns, name, resourceId, type, obj);
			
		}
		
    	@Override
       	public NodeVisitor child(String ns, String name) {
    		AXmlNode childNode = new AXmlNode(name == null ? null : name.trim(),
    				ns == null ? null : ns.trim(), node);
    		return new MyNodeVisitor(childNode);
    	}
    	
    	@Override
    	public void end() {
    		root = node;
    	}

	}

	@Override
	public void parseFile(byte[] buffer) throws IOException {
		AxmlReader rdr = new AxmlReader(buffer);
		rdr.accept(new MyNodeVisitor());
	}

}

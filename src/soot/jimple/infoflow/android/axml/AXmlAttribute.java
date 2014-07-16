package soot.jimple.infoflow.android.axml;

import static pxb.android.axml.AxmlVisitor.TYPE_INT_BOOLEAN;
import static pxb.android.axml.AxmlVisitor.TYPE_INT_HEX;
import static pxb.android.axml.AxmlVisitor.TYPE_STRING;

/**
 * Represents an attribute of an Android XML node.
 * 
 * @param	<T> determines the attribute's type. Currently {@link Integer}, {@link Boolean} and {@link String} are supported.
 * @author	Stefan Haas, Mario Schlipf
 */
public class AXmlAttribute<T> extends AXmlElement {
	/**
	 * The attribute's name.
	 */
	protected String name;
	
	/**
	 * The attribute's value.
	 */
	protected T value;
	
	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>, <code>value</code> and <code>namespace</code>.<br />
	 * The <code>addded</code> flag is defaulted to true (see {@link AXmlElement#added}).
	 * 
	 * @param	name	the attribute's name.
	 * @param	value	the attribute's value.
	 * @param	ns		the attribute's namespace.
	 */
	public AXmlAttribute(String name, T value, String ns) {
		this(name, value, ns, true);
	}
	
	/**
	 * Creates a new {@link AXmlAttribute} object with the given <code>name</code>, <code>value</code> and <code>namespace</code>.
	 * 
	 * @param	name	the attribute's name.
	 * @param	value	the attribute's value.
	 * @param	ns		the attribute's namespace.
	 * @param	added	wheter this attribute was part of a parsed xml file or added afterwards.
	 */
	public AXmlAttribute(String name, T value, String ns, boolean added) {
		super(ns, added);
		this.name = name;
		this.value = value;
	}
	
	/**
	 * Returns the name of this attribute.
	 * 
	 * @return	the attribute's name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value for this attribute.
	 * 
	 * @param	value	the new value.
	 */
	public void setValue(T value) {
		this.value = value;
	}
	
	/**
	 * Returns the value of this attribute.
	 * 
	 * @return	the attribute's value.
	 */
	public T getValue() {
		return this.value;
	}

	/**
	 * Returns an integer which identifies this attribute's type.
	 * Currently if AXmlAttribute is typed as {@link Integer} this will return {@link AxmlVisitor#TYPE_INT_HEX},
	 * typed {@link Boolean} will result in {@link AxmlVisitor#TYPE_INT_BOOLEAN} and otherwise (even if not
	 * typed as {@link String}) this returns {@link AxmlVisitor#TYPE_STRING}.
	 * 
	 * @return	integer representing the attribute's type
	 * @see		AxmlVisitor#TYPE_INT_HEX
	 * @see		AxmlVisitor#TYPE_INT_BOOLEAN
	 * @see		AxmlVisitor#TYPE_STRING
	 */
	public int getType() {
		if(this.value instanceof Integer) return TYPE_INT_HEX;
		else if(this.value instanceof Boolean) return TYPE_INT_BOOLEAN;
		else return TYPE_STRING;
	}
	
	@Override
	public String toString() {
		return this.name + "=\"" + this.value + "\"";
	}
	
}

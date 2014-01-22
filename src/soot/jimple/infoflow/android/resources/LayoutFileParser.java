/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.resources;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.AxmlVisitor.NodeVisitor;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.Transform;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;

/**
 * Parser for analyzing the layout XML files inside an android application
 * 
 * @author Steven Arzt
 *
 */
public class LayoutFileParser extends AbstractResourceParser {
	
	private static final boolean DEBUG = true;
	
	private final Map<Integer, LayoutControl> userControls = new HashMap<Integer, LayoutControl>();
	private final Map<String, Set<String>> callbackMethods = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> includeDependencies = new HashMap<String, Set<String>>();
	private final String packageName;
	private final ARSCFileParser resParser;
	
	private final static int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
	private final static int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
	private final static int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
	private final static int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;
	
	public LayoutFileParser(String packageName, ARSCFileParser resParser) {
		this.packageName = packageName;
		this.resParser = resParser;
	}
	
	private SootClass getLayoutClass(String className) {
		// Cut off some junk returned by the parser
		if (className.startsWith(";"))
			className = className.substring(1);
		
		if (className.contains("(") || className.contains("<") || className.contains("/")) {
			System.err.println("Invalid class name " + className);
			return null;
		}
		
		SootClass sc = Scene.v().forceResolve(className, SootClass.BODIES);
		if ((sc == null || sc.isPhantom()) && !packageName.isEmpty())
			sc = Scene.v().forceResolve(packageName + "." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.view." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.widget." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.webkit." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom()) {
   			System.err.println("Could not find layout class " + className);
   			return null;
		}
		return sc;		
	}
	
	private boolean isLayoutClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
   		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOf(theClass))
   			if (parent.getName().equals("android.view.ViewGroup")) {
   				found = true;
   				break;
   			}
   		return found;
	}
	
	private boolean isViewClass(SootClass theClass) {
		if (theClass == null)
			return false;
		
		// To make sure that nothing all wonky is going on here, we
   		// check the hierarchy to find the android view class
   		boolean found = false;
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOfIncluding(theClass))
   			if (parent.getName().equals("android.view.View")
   					|| parent.getName().equals("android.webkit.WebView")) {
   				found = true;
   				break;
   			}
   		if (!found) {
   			System.err.println("Layout class " + theClass.getName() + " is not derived from "
   					+ "android.view.View");
   			return false;
   		}
   		return true;
	}
	
	/**
	 * Checks whether the given namespace belongs to the Android operating system
	 * @param ns The namespace to check
	 * @return True if the namespace belongs to Android, otherwise false
	 */
	private boolean isAndroidNamespace(String ns) {
		if (ns == null)
			return false;
		ns = ns.trim();
		if (ns.startsWith("*"))
			ns = ns.substring(1);
		if (!ns.equals("http://schemas.android.com/apk/res/android"))
			return false;
		return true;
	}
	
	private <X,Y> void addToMapSet(Map<X, Set<Y>> target, X layoutFile, Y callback) {
		if (target.containsKey(layoutFile))
			target.get(layoutFile).add(callback);
		else {
			Set<Y> callbackSet = new HashSet<Y>();
			callbackSet.add(callback);
			target.put(layoutFile, callbackSet);
		}
	}

	/**
	 * Adds a callback method found in an XML file to the result set
	 * @param layoutFile The XML file in which the callback has been found
	 * @param callback The callback found in the given XML file
	 */
	private void addCallbackMethod(String layoutFile, String callback) {
		addToMapSet(callbackMethods, layoutFile, callback);
		
		// Recursively process any dependencies we might have collected before
		// we have processed the target
		if (includeDependencies.containsKey(layoutFile))
			for (String target : includeDependencies.get(layoutFile))
				addCallbackMethod(target, callback);
	}
	
	/**
	 * Parser for "include" directives in layout XML files
	 */
	private class IncludeParser extends NodeVisitor {
		
		private final String layoutFile;

    	public IncludeParser(String layoutFile) {
    		this.layoutFile = layoutFile;
    	}
    	
    	@Override
    	public void attr(String ns, String name, int resourceId, int type, Object obj) {
    		// Is this the target file attribute?
    		String tname = name.trim();
    		if (tname.equals("layout")) {
    			if (type == AxmlVisitor.TYPE_REFERENCE && obj instanceof Integer) {
    				// We need to get the target XML file from the binary manifest
    				AbstractResource targetRes = resParser.findResource((Integer) obj);
    				if (targetRes == null) {
    					System.err.println("Target resource " + obj + " for layout include not found");
    					return;
    				}
    				if (!(targetRes instanceof StringResource)) {
    					System.err.println("Invalid target node for include tag in layout XML, was "
    							+ targetRes.getClass().getName());
    					return;
    				}
    				String targetFile = ((StringResource) targetRes).getValue();
    				
    				// If we have already processed the target file, we can
    				// simply copy the callbacks we have found there
        			if (callbackMethods.containsKey(targetFile))
        				for (String callback : callbackMethods.get(targetFile))
        					addCallbackMethod(layoutFile, callback);
        			else {
        				// We need to record a dependency to resolve later
        				addToMapSet(includeDependencies, targetFile, layoutFile);
        			}
    			}
    		}
    		
    		super.attr(ns, name, resourceId, type, obj);
    	}
    	
	}
	
	/**
	 * Parser for layout components defined in XML files
	 */
	private class LayoutParser extends NodeVisitor {

		private final String layoutFile;
		private final SootClass theClass;
    	private Integer id = -1;
    	private boolean isSensitive = false;
    	
    	public LayoutParser(String layoutFile, SootClass theClass) {
    		this.layoutFile = layoutFile;
    		this.theClass = theClass;
    	}

    	@Override
       	public NodeVisitor child(String ns, String name) {
    		if (name == null || name.isEmpty()) {
    			System.err.println("Encountered a null or empty node name "
    					+ "in file " + layoutFile + ", skipping node...");
    			return null;
    		}
    		
    		// Check for inclusions
    		String tname = name.trim();
    		if (tname.equals("include"))
    			return new IncludeParser(layoutFile);
    		
    		// The "merge" tag merges the next hierarchy level into the current
    		// one for flattening hierarchies.
    		if (tname.equals("merge"))
       			return new LayoutParser(layoutFile, theClass);
    		
			final SootClass childClass = getLayoutClass(tname);
			if (childClass != null && (isLayoutClass(childClass) || isViewClass(childClass)))
       			return new LayoutParser(layoutFile, childClass);
			else
				return super.child(ns, name);
       	}
		
    	@Override
    	public void attr(String ns, String name, int resourceId, int type, Object obj) {
    		// Check that we're actually working on an android attribute
    		if (!isAndroidNamespace(ns))
    			return;

    		// Read out the field data
    		String tname = name.trim();
    		if (tname.equals("id") && type == AxmlVisitor.TYPE_REFERENCE)
    			this.id = (Integer) obj;
    		else if (tname.equals("password") && type == AxmlVisitor.TYPE_INT_BOOLEAN)
    			isSensitive = ((Integer) obj) != 0; // -1 for true, 0 for false
    		else if (!isSensitive && tname.equals("inputType") && type == AxmlVisitor.TYPE_INT_HEX) {
    			int tp = (Integer) obj;
    			isSensitive = ((tp & TYPE_NUMBER_VARIATION_PASSWORD) == TYPE_NUMBER_VARIATION_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_PASSWORD) == TYPE_TEXT_VARIATION_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_WEB_PASSWORD) == TYPE_TEXT_VARIATION_WEB_PASSWORD);
    		}
    		else if (isActionListener(tname) && type == AxmlVisitor.TYPE_STRING && obj instanceof String) {
    			String strData = ((String) obj).trim();
    			addCallbackMethod(layoutFile, strData);
    		}
    		else {
    			if (DEBUG && type == AxmlVisitor.TYPE_STRING)
    				System.out.println("Found unrecognized XML attribute:  " + tname);
    		}
    		
    		super.attr(ns, name, resourceId, type, obj);
    	}
    	
		/**
    	 * Checks whether this name is the name of a well-known Android listener
    	 * attribute. This is a function to allow for future extension.
    	 * @param name The attribute name to check. This name is guaranteed to
    	 * be in the android namespace.
    	 * @return True if the given attribute name corresponds to a listener,
    	 * otherwise false.
    	 */
    	private boolean isActionListener(String name) {
    		return name.equals("onClick");
    	}

		@Override
    	public void end() {
    		if (id > 0)
    			userControls.put(id, new LayoutControl(id, theClass, isSensitive));
    	}
	}
	
	/**
	 * Parses all layout XML files in the given APK file and loads the IDs of
	 * the user controls in it.
	 * @param fileName The APK file in which to look for user controls
	 */
	public void parseLayoutFile(final String fileName, final Set<String> classes) {
		Transform transform = new Transform("wjtp.lfp", new SceneTransformer() {
			protected void internalTransform(String phaseName, @SuppressWarnings("rawtypes") Map options) {
				handleAndroidResourceFiles(fileName, /*classes,*/ null, new IResourceHandler() {
					
					@Override
					public void handleResourceFile(final String fileName, Set<String> fileNameFilter, InputStream stream) {
						// We only process valid layout XML files
						if (!fileName.startsWith("res/layout"))
							return;
						if (!fileName.endsWith(".xml")) {
							System.err.println("Skipping file " + fileName + " in layout folder...");
							return;
						}
						
						// Get the fully-qualified class name
						String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
						if (!packageName.isEmpty())
							entryClass = packageName + "." + entryClass;
						
						// We are dealing with resource files
						if (!fileName.startsWith("res/layout"))
							return;
						if (fileNameFilter != null) {
							boolean found = false;
							for (String s : fileNameFilter)
								if (s.equalsIgnoreCase(entryClass)) {
									found = true;
									break;
								}
							if (!found)
								return;
						}

						try {
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							int in;
							while ((in = stream.read()) >= 0)
								bos.write(in);
							bos.flush();
							byte[] data = bos.toByteArray();
							if (data == null || data.length == 0)	// File empty?
								return;
							
							AxmlReader rdr = new AxmlReader(data);
							rdr.accept(new AxmlVisitor() {
								
								@Override
								public NodeVisitor first(String ns, String name) {
									if (name == null)
										return new LayoutParser(fileName, null);
									
									final String tname = name.trim();
									final SootClass theClass = tname.isEmpty() || tname.equals("merge")
											|| tname.equals("include") ? null : getLayoutClass(name.trim());
									if (theClass == null || isLayoutClass(theClass))
										return new LayoutParser(fileName, theClass);
									else
										return super.first(ns, name);
								}
							});
							
							System.out.println("Found " + userControls.size() + " layout controls in file "
									+ fileName);
						}
						catch (Exception ex) {
							System.err.println("Could not read binary XML file: " + ex.getMessage());
							ex.printStackTrace();
						}
					}
				});
			}
		});
		PackManager.v().getPack("wjtp").add(transform);
	}
	
	/**
	 * Gets the user controls found in the layout XML file. The result is a
	 * mapping from the id to the respective layout control.
	 * @return The layout controls found in the XML file.
	 */
	public Map<Integer, LayoutControl> getUserControls() {
		return this.userControls;
	}

	/**
	 * Gets the callback methods found in the layout XML file. The result is a
	 * mapping from the file name to the set of found callback methods.
	 * @return The callback methods found in the XML file.
	 */
	public Map<String, Set<String>> getCallbackMethods() {
		return this.callbackMethods;
	}
	
}

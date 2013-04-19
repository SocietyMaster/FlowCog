package soot.jimple.infoflow.android.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	private final String packageName;
	
	private final static int TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010;
	private final static int TYPE_TEXT_VARIATION_PASSWORD = 0x00000080;
	private final static int TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090;
	private final static int TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0;
	
	public LayoutFileParser(String packageName) {
		this.packageName = packageName;
	}
	
	private SootClass getLayoutClass(String className) {
		SootClass sc = Scene.v().forceResolve(className, SootClass.BODIES);
		if ((sc == null || sc.isPhantom()) && !packageName.isEmpty())
			sc = Scene.v().forceResolve(packageName + "." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.widget." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
			sc = Scene.v().forceResolve("android.webkit." + className, SootClass.BODIES);
		if (sc == null || sc.isPhantom())
   			System.err.println("Could not find layout class " + className);
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
   		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOf(theClass))
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
			final SootClass childClass = getLayoutClass(name.trim());
			if (isLayoutClass(childClass) || isViewClass(childClass))
       			return new LayoutParser(layoutFile, childClass);
			else
				return super.child(ns, name);
       	}
		        	
    	@Override
    	public void attr(String ns, String name, int resourceId, int type, Object obj) {
    		// Check that we're actually working on an android attribute
    		if (ns == null)
    			return;
    		ns = ns.trim();
    		if (ns.startsWith("*"))
    			ns = ns.substring(1);
    		if (!ns.equals("http://schemas.android.com/apk/res/android"))
    			return;

    		// Read out the field data
    		name = name.trim();
    		if (name.equals("id") && type == AxmlVisitor.TYPE_REFERENCE)
    			this.id = (Integer) obj;
    		else if (name.equals("password") && type == AxmlVisitor.TYPE_INT_BOOLEAN)
    			isSensitive = ((Integer) obj) != 0; // -1 for true, 0 for false
    		else if (!isSensitive && name.equals("inputType") && type == AxmlVisitor.TYPE_INT_HEX) {
    			int tp = (Integer) obj;
    			isSensitive = ((tp & TYPE_NUMBER_VARIATION_PASSWORD) == TYPE_NUMBER_VARIATION_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_PASSWORD) == TYPE_TEXT_VARIATION_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
    					|| ((tp & TYPE_TEXT_VARIATION_WEB_PASSWORD) == TYPE_TEXT_VARIATION_WEB_PASSWORD);
    		}
    		else if (isActionListener(name) && type == AxmlVisitor.TYPE_STRING && obj instanceof String) {
    			String strData = ((String) obj).trim();
    			if (callbackMethods.containsKey(layoutFile))
    				callbackMethods.get(layoutFile).add(strData);
    			else {
    				Set<String> callbackSet = new HashSet<String>();
    				callbackSet.add(strData);
    				callbackMethods.put(layoutFile, callbackSet);
    			}
    		}
    		else {
    			if (DEBUG && type == AxmlVisitor.TYPE_STRING)
    				System.out.println("Found unrecognized XML attribute:  " + name);
    		}
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
						if (!fileName.startsWith("res/layout/"))
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
							
							AxmlReader rdr = new AxmlReader(data);
							rdr.accept(new AxmlVisitor() {
								
								@Override
								public NodeVisitor first(String ns, String name) {
									final SootClass theClass = name == null ? null : getLayoutClass(name.trim());
									if (theClass == null || isLayoutClass(theClass))
										return new LayoutParser(fileName, theClass);
									else
										return super.first(ns, name);
								}
							});
							
							System.out.println("Found " + userControls.size() + " layout controls in file "
									+ fileName);
						}
						catch (IOException ex) {
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

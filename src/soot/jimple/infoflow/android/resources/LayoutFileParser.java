package soot.jimple.infoflow.android.resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
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
public class LayoutFileParser {
	
	private final Map<Integer, LayoutControl> userControls = new HashMap<Integer, LayoutControl>();
	private final String packageName;
	
	public LayoutFileParser(String packageName) {
		this.packageName = packageName;
	}
	
	/**
	 * Opens the given apk file and provides the given handler with a stream for
	 * accessing the contained resource manifest files
	 * @param apk The apk file to process
	 * @param fileNameFilter If this parameter is non-null, only files with a
	 * name (excluding extension) in this set will be analyzed.
	 * @param handler The handler for processing the apk file
	 * 
	 * @author Steven Arzt
	 */
	private void handleAndroidResourceFiles(String apk, Set<String> fileNameFilter,
			IResourceHandler handler) {
		File apkF = new File(apk);
		if (!apkF.exists())
			throw new RuntimeException("file '" + apk + "' does not exist!");

		try {
			ZipFile archive = null;
			try {
				archive = new ZipFile(apkF);
				Enumeration<?> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = (ZipEntry) entries.nextElement();
					String entryName = entry.getName();
					
					File f = new File(entryName);
					String entryClass = f.getName();
					entryClass = entryClass.substring(0, entryClass.lastIndexOf("."));
					if (!this.packageName.isEmpty())
						entryClass = this.packageName + "." + entryClass;
					
					// We are dealing with resource files
					if (!entryName.startsWith("res/layout"))
						continue;
					if (fileNameFilter != null) {
						boolean found = false;
						for (String s : fileNameFilter)
							if (s.equalsIgnoreCase(entryClass)) {
								found = true;
								break;
							}
						if (!found)
							continue;
					}
					
					handler.handleResourceFile(archive.getInputStream(entry));
				}
			}
			finally {
				if (archive != null)
					archive.close();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Error when looking for XML resource files in apk: " + e);
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
				handleAndroidResourceFiles(fileName, classes, new IResourceHandler() {
					
					@Override
					public void handleResourceFile(InputStream stream) {
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
							        NodeVisitor nv = super.first(ns, name);
							        return new NodeVisitor(nv) {
							        
							        	@Override
							        	public NodeVisitor child(String ns, String name) {
							        		// Try to find the class indicated by the XML file
							        		String className = name.trim();
							        		final SootClass layoutClass;
							        		if (Scene.v().containsClass(className))
							        			layoutClass = Scene.v().getSootClass(className);
							        		else if (!packageName.isEmpty() && Scene.v().containsClass
							        					(packageName + "." + className))
							        			layoutClass = Scene.v().getSootClass(packageName + "." + className);
							        		else if (Scene.v().containsClass("android.widget." + className))
							        			layoutClass = Scene.v().getSootClass("android.widget." + className);
							        		else {
							        			System.out.println("Could not find layout class " + className);
							        			return super.child(ns, name);
							        		}
							        		assert layoutClass != null;
							        		
							        		// To make sure that nothing all wonky is going on here, we
							        		// check the hierarchy to find the android view class
							        		boolean found = false;
							        		for (SootClass parent : Scene.v().getActiveHierarchy().getSuperclassesOf(layoutClass))
							        			if (parent.getName().equals("android.view.View")) {
							        				found = true;
							        				break;
							        			}
							        		if (!found) {
							        			System.out.println("Layout class " + className + " is not derived from "
							        					+ "android.view.View");
							        			return super.child(ns, name);
							        		}
							        		
							        		// Ok, this is really a layout control
									        return new NodeVisitor(nv) {
									        	
									        	private Integer id = -1;
									        	private boolean isSensitive = false;
									        	
									        	@Override
									        	public void attr(String ns, String name, int resourceId, int type, Object obj) {
									        		// Check that we're actually working on an android attribute
									        		ns = ns.trim();
									        		if (ns.startsWith("*"))
									        			ns = ns.substring(1);
									        		if (!ns.equals("http://schemas.android.com/apk/res/android"))
									        			return;

									        		name = name.trim();
									        		if (name.equals("id") && type == AxmlVisitor.TYPE_REFERENCE)
									        			this.id = (Integer) obj;
									        		else if (name.equals("password") && type == AxmlVisitor.TYPE_INT_BOOLEAN)
									        			isSensitive = ((Integer) obj) != 0; // -1 for true, 0 for false
									        	}
									        	
									        	@Override
									        	public void end() {
									        		userControls.put(id, new LayoutControl(id, layoutClass, isSensitive));
									        	}
									        	
									        };
							        	}
							        };
								}
							});
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
	
	public Map<Integer, LayoutControl> getUserControls() {
		return this.userControls;
	}

}

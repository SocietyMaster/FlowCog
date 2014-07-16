package soot.jimple.infoflow.android.manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;

import pxb.android.axml.AxmlVisitor;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.ApkHandler;

/**
 * This class provides easy access to all data of an AppManifest.<br />
 * Nodes and attributes of a parsed manifest can be changed. A new byte compressed
 * manifest considering the changes can be generated.
 * 
 * @author Steven Arzt
 * @author Stefan Haas, Mario Schlipf
 * @see <a href="http://developer.android.com/guide/topics/manifest/manifest-intro.html">App Manifest</a>
 */
public class ProcessManifest {
	
	/**
	 * Enumeration containing the various component types supported in Android
	 */
	public enum ComponentType {
		Activity,
		Service,
		ContentProvider,
		BroadcastReceiver
	}
		
	/**
	 * Handler for zip-like apk files
	 */
	protected ApkHandler apk = null;
	
	/**
	 * Handler for android xml files
	 */
	protected AXmlHandler axml;
	
	// android manifest data
	protected AXmlNode manifest;
	protected AXmlNode application;
	
	// Components in the manifest file
	protected List<AXmlNode> providers = null;
	protected List<AXmlNode> services = null;
	protected List<AXmlNode> activities = null;
	protected List<AXmlNode> receivers = null;
	
	/**
	 * Processes an AppManifest which is within the file identified by the given path.
	 * 
	 * @param	apkPath					file path to an APK.
	 * @throws	IOException				if an I/O error occurs.
	 * @throws	XmlPullParserException	can occur due to a malformed manifest.
	 */
	public ProcessManifest(String apkPath) throws IOException, XmlPullParserException {
		this(new File(apkPath));
	}
	
	/**
	 * Processes an AppManifest which is within the given {@link File}.
	 * 
	 * @param	apkFile					the AppManifest within the given APK will be parsed.
	 * @throws	IOException				if an I/O error occurs.
	 * @throws	XmlPullParserException	can occur due to a malformed manifest.
	 * @see		{@link ProcessManifest#ProcessManifest(InputStream)}
	 */
	public ProcessManifest(File apkFile) throws IOException, XmlPullParserException {
		this.apk = new ApkHandler(apkFile);
		this.handle(this.apk.getInputStream("AndroidManifest.xml"));
	}
		
	/**
	 * Processes an AppManifest which is provided by the given {@link InputStream}.
	 * 
	 * @param	manifestIS				InputStream for an AppManifest.
	 * @throws	IOException				if an I/O error occurs.
	 * @throws	XmlPullParserException	can occur due to a malformed manifest.
	 */
	public ProcessManifest(InputStream manifestIS) throws IOException, XmlPullParserException {
		this.handle(manifestIS);
	}
	
	/**
	 * Initialises the {@link ProcessManifest} by parsing the manifest provided by the given {@link InputStream}.
	 * 
	 * @param	manifestIS				InputStream for an AppManifest.
	 * @throws	IOException				if an I/O error occurs.
	 * @throws	XmlPullParserException	can occur due to a malformed manifest.
	 */
	protected void handle(InputStream manifestIS) throws IOException, XmlPullParserException {
		this.axml = new AXmlHandler(manifestIS);
		
		// get manifest node
		List<AXmlNode> manifests = this.axml.getNodesWithTag("manifest");
		if(manifests.isEmpty()) throw new RuntimeException("Manifest contains no manifest node");
		else if(manifests.size() > 1) throw new RuntimeException("Manifest contains more than one manifest node");
		this.manifest = manifests.get(0);
		
		// get application node
		List<AXmlNode> applications = this.manifest.getChildrenWithTag("application");
		if(applications.isEmpty()) throw new RuntimeException("Manifest contains no application node");
		else if(applications.size() > 1) throw new RuntimeException("Manifest contains more than one application node");
		this.application = applications.get(0);
				
		// Get components
		this.providers = this.axml.getNodesWithTag("provider");
		this.services = this.axml.getNodesWithTag("service");
		this.activities = this.axml.getNodesWithTag("activity");
		this.receivers = this.axml.getNodesWithTag("receiver");
	}
	
	/**
	 * Generates a full class name from a short class name by appending the
	 * globally-defined package when necessary
	 * @param className The class name to expand
	 * @return The expanded class name for the given short name
	 */
	private String expandClassName(String className) {
		String packageName = getPackageName();
		if (className.startsWith("."))
			return packageName + className;
		else if (className.substring(0, 1).equals(className.substring(0, 1).toUpperCase()))
			return packageName + "." + className;
		else
			return className;
	}
	
	/**
	 * Returns the handler which parsed and holds the manifest's data.
	 * 
	 * @return Android XML handler
	 */
	public AXmlHandler getAXml() {
		return this.axml;
	}
	
	/**
	 * Returns the handler which opened the APK file. If {@link ProcessManifest} was
	 * instanciated directly with an {@link InputStream} this will return <code>null</code>.
	 * 
	 * @return APK Handler
	 */
	public ApkHandler getApk() {
		return this.apk;
	}
	
	/**
	 * The unique <code>manifest</code> node of the AppManifest.
	 * 
	 * @return manifest node
	 */
	public AXmlNode getManifest() {
		return this.manifest;
	}
	
	/**
	 * The unique <code>application</code> node of the AppManifest.
	 * 
	 * @return application node
	 */
	public AXmlNode getApplication() {
		return this.application;
	}
	
	/**
	 * Returns a list containing all nodes with tag <code>provider</code>.
	 * 
	 * @return list with all providers
	 */
	public ArrayList<AXmlNode> getProviders() {
		return new ArrayList<AXmlNode>(this.providers);
	}
	
	/**
	 * Returns a list containing all nodes with tag <code>service</code>.
	 * 
	 * @return list with all services
	 */
	public ArrayList<AXmlNode> getServices() {
		return new ArrayList<AXmlNode>(this.services);
	}
	
	/**
	 * Gets all classes the contain entry points in this applications
	 * @return All classes the contain entry points in this applications
	 */
	public Set<String> getEntryPointClasses() {
		// If the application is not enabled, there are no entry points
		if (!isApplicationEnabled())
			return Collections.emptySet();
		
		// Collect the components
		Set<String> entryPoints = new HashSet<String>();
		for (AXmlNode node : this.activities)
			checkAndAddComponent(entryPoints, node);
		for (AXmlNode node : this.providers)
			checkAndAddComponent(entryPoints, node);
		for (AXmlNode node : this.services)
			checkAndAddComponent(entryPoints, node);
		for (AXmlNode node : this.receivers)
			checkAndAddComponent(entryPoints, node);
		
		String appName = getApplicationName();
		if (appName != null && !appName.isEmpty())
			entryPoints.add(appName);
		
		return entryPoints;
	}
	
	private void checkAndAddComponent(Set<String> entryPoints, AXmlNode node) {
		AXmlAttribute<?> attrEnabled = node.getAttribute("enabled");
		if (attrEnabled == null || !attrEnabled.getValue().equals(Boolean.FALSE)) {
			AXmlAttribute<?> attr = node.getAttribute("name");
			if (attr != null)
				entryPoints.add(expandClassName((String) attr.getValue()));
			else {
				// This component does not have a name, so this might be obfuscated
				// malware. We apply a heuristic.
				for (Entry<String, AXmlAttribute<?>> a : node.getAttributes().entrySet())
					if (a.getValue().getName().isEmpty()
							&& a.getValue().getType() == AxmlVisitor.TYPE_STRING) {
						String name = (String) a.getValue().getValue();
						if (isValidComponentName(name))
							entryPoints.add(expandClassName(name));
					}
			}
		}
	}
	
	/**
	 * Checks if the specified name is a valid Android component name
	 * @param name The Android component name to check
	 * @return True if the given name is a valid Android component name,
	 * otherwise false
	 */
	private boolean isValidComponentName(String name) {
		if (name.isEmpty())
			return false;
		if (name.equals("true") || name.equals("false"))
			return false;
		if (Character.isDigit(name.charAt(0)))
			return false;
		
		if (name.startsWith("."))
			return true;
		
		// Be conservative
		return false;
	}

	/**
	 * Gets the type of the component identified by the given class name
	 * @param className The class name for which to get the component type
	 * @return The component type of the given class if this class has been
	 * registered as a component in the manifest file, otherwise null
	 */
	public ComponentType getComponentType(String className) {
		for (AXmlNode node : this.activities)
			if (node.getAttribute("name").getValue().equals(className))
				return ComponentType.Activity;
		for (AXmlNode node : this.services)
			if (node.getAttribute("name").getValue().equals(className))
				return ComponentType.Service;
		for (AXmlNode node : this.receivers)
			if (node.getAttribute("name").getValue().equals(className))
				return ComponentType.BroadcastReceiver;
		for (AXmlNode node : this.providers)
			if (node.getAttribute("name").getValue().equals(className))
				return ComponentType.ContentProvider;
		return null;
	}
	
	/**
	 * Returns a list containing all nodes with tag <code>activity</code>.
	 * 
	 * @return list with all activities
	 */
	public ArrayList<AXmlNode> getActivities() {
		return new ArrayList<AXmlNode>(this.activities);
	}
	
	/**
	 * Returns a list containing all nodes with tag <code>receiver</code>.
	 * 
	 * @return list with all receivers
	 */
	public ArrayList<AXmlNode> getReceivers() {
		return new ArrayList<AXmlNode>(this.receivers);
	}
	
	/**
	 * Returns the <code>provider</code> which has the given <code>name</code>.
	 * 
	 * @param	name	the provider's name
	 * @return	provider with <code>name</code>
	 */
	public AXmlNode getProvider(String name) {
		return this.getNodeWithName(this.providers, name);
	}
	
	/**
	 * Returns the <code>service</code> which has the given <code>name</code>.
	 * 
	 * @param	name	the service's name
	 * @return	service with <code>name</code>
	 */
	public AXmlNode getService(String name) {
		return this.getNodeWithName(this.services, name);
	}
	
	/**
	 * Returns the <code>activity</code> which has the given <code>name</code>.
	 * 
	 * @param	name	the activitie's name
	 * @return	activitiy with <code>name</code>
	 */
	public AXmlNode getActivity(String name) {
		return this.getNodeWithName(this.activities, name);
	}
	
	/**
	 * Returns the <code>receiver</code> which has the given <code>name</code>.
	 * 
	 * @param	name	the receiver's name
	 * @return	receiver with <code>name</code>
	 */
	public AXmlNode getReceiver(String name) {
		return this.getNodeWithName(this.receivers, name);
	}
	
	/**
	 * Iterates over <code>list</code> and checks which node has the given <code>name</code>.
	 * 
	 * @param	list	contains nodes.
	 * @param	name	the node's name.
	 * @return	node with <code>name</code>.
	 */
	protected AXmlNode getNodeWithName(List<AXmlNode> list, String name) {
		for (AXmlNode node : list) {
			Object attr = node.getAttributes().get("name");
			if(attr != null && attr.equals(name))
				return node;
		}
		
		return null;
	}
	
	/**
	 * Returns the Manifest as a compressed android xml byte array.
	 * This will consider all changes made to the manifest and
	 * application nodes respectively to their child nodes.
	 * 
	 * @return	byte compressed AppManifest
	 * @see		AXmlHandler#toByteArray()
	 */
	public byte[] getOutput() {
		return this.axml.toByteArray();
	}

	/**
	 * Gets the application's package name
	 * @return The package name of the application
	 */
	private String cache_PackageName = null;
	public String getPackageName() {
		if (cache_PackageName == null) {
			AXmlAttribute<?> attr = this.manifest.getAttribute("package");
			if (attr != null)
				cache_PackageName = (String) attr.getValue();
		}
		return cache_PackageName;
	}

	/**
	 * Gets the version code of the application. This code is used to compare
	 * versions for updates.
	 * @return The version code of the application
	 */
	public int getVersionCode() {
		AXmlAttribute<?> attr = this.manifest.getAttribute("versionCode");
		return attr == null ? -1 : Integer.getInteger((String) attr.getValue());
	}
	
	/**
	 * Gets the application's version name as it is displayed to the user
	 * @return The application#s version name as in pretty print
	 */
	public String getVersionName() {
		AXmlAttribute<?> attr = this.manifest.getAttribute("versionName");
		return attr == null ? null : (String) attr.getValue();
	}
	
	/**
	 * Gets the name of the Android application class
	 * @return The name of the Android application class
	 */
	public String getApplicationName() {
		AXmlAttribute<?> attr = this.application.getAttribute("name");
		return attr == null ? null : expandClassName((String) attr.getValue());		
	}
	
	/**
	 * Gets whether this Android application is enabled
	 * @return True if this application is enabled, otherwise false
	 */
	public boolean isApplicationEnabled() {
		AXmlAttribute<?> attr = this.application.getAttribute("enabled");
		return attr == null || !attr.getValue().equals(Boolean.FALSE);
	}
	
	/**
	 * Gets the minimum SDK version on which this application is supposed to run
	 * @return The minimum SDK version on which this application is supposed to run
	 */
	public int getMinSdkVersion() {
		List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
		if (usesSdk == null || usesSdk.isEmpty())
			return -1;
		AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("minSdkVersion");
		return attr == null ? -1 : Integer.getInteger((String) attr.getValue());		
	}
	
	/**
	 * Gets the target SDK version for which this application was developed
	 * @return The target SDK version for which this application was developed
	 */
	public int targetSdkVersion() {
		List<AXmlNode> usesSdk = this.manifest.getChildrenWithTag("uses-sdk");
		if (usesSdk == null || usesSdk.isEmpty())
			return -1;
		AXmlAttribute<?> attr = usesSdk.get(0).getAttribute("targetSdkVersion");
		return attr == null ? -1 : Integer.getInteger((String) attr.getValue());
	}
	
	/**
	 * Gets the permissions this application requests
	 * @return The permissions requested by this application
	 * @return
	 */
	public Set<String> getPermissions() {
		List<AXmlNode> usesPerms = this.manifest.getChildrenWithTag("uses-permission");
		Set<String> permissions = new HashSet<String>();
		for (AXmlNode perm : usesPerms) {
			AXmlAttribute<?> attr = perm.getAttribute("name");
			if (attr != null)
				permissions.add((String) attr.getValue());
			else {
				// The required "name" attribute is missing, so we collect all empty
				// attributes as a best-effort solution for broken malware apps
				for (AXmlAttribute<?> a : perm.getAttributes().values())
					if (a.getType() == AxmlVisitor.TYPE_STRING && a.getName().isEmpty())
						permissions.add((String) a.getValue());
			}
		}
		return permissions;
	}
	
}
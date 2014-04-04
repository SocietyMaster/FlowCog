package soot.jimple.infoflow.android.axml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Provides access to the files within an APK and can add and replace files.
 * 
 * @author Stefan Haas, Mario Schlipf
 */
public class ApkHandler {
	/**
	 * The handled APK file.
	 */
	protected File apk;
	
	/**
	 * Pointer to the ZipFile. If an InputStream for a file within
	 * the ZipFile is returned by {@link ApkHandler#getInputStream(String)} the
	 * ZipFile object has to remain available in order to read the InputStream.
	 */
	protected ZipFile zip;
	
	/**
	 * @param	path			the APK's path
	 * @throws	ZipException	occurs if the APK is no a valid zip file.
	 * @throws	IOException		if an I/O error occurs.
	 * @see		ApkHandler#ApkHandler(File)
	 */
	public ApkHandler(String path) throws ZipException, IOException {
		this(new File(path));
	}
	
	/**
	 * Creates a new {@link ApkHandler} which handles the given APK file.
	 * 
	 * @param	apk				the APK's path
	 * @throws	ZipException	occurs if the APK is no a valid zip file.
	 * @throws	IOException		if an I/O error occurs.
	 */
	public ApkHandler(File apk) throws ZipException, IOException {
		this.apk = apk;
	}
	
	/**
	 * Returns the absolute path of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see File#getAbsolutePath()
	 */
	public String getAbsolutePath() {
		return this.apk.getAbsolutePath();
	}
	
	/**
	 * Returns the path of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see		File#getPath()
	 */
	public String getPath() {
		return this.apk.getPath();
	}
	
	/**
	 * Returns the filename of the APK which is held by the {@link ApkHandler}.
	 * 
	 * @see		File#getName()
	 */
	public String getFilename() {
		return this.apk.getName();
	}
	
	/**
	 * Returns an {@link InputStream} for a file within the APK.<br />
	 * The given filename has to be the relative path within the APK, e.g. <code>res/menu/main.xml</code> 
	 * 
	 * @param	filename		the file's path
	 * @return	{@link InputStream} for the searched file, if not found null
	 * @throws	IOException		if an I/O error occurs.
	 */
	public InputStream getInputStream(String filename) throws IOException {
		InputStream is = null;
		
		// check if zip file is already opened
		if(this.zip == null) this.zip = new ZipFile(this.apk);
		
		// search for file with given filename
		Enumeration<?> entries = this.zip.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String entryName = entry.getName();
			if (entryName.equals(filename)) {
				is = this.zip.getInputStream(entry);
				break;
			}
		}
		
		return is;
	}
	
	/**
	 * @param	files			array with File objects to be added to the APK.
	 * @throws	IOException		if an I/O error occurs.
	 * @see		{@link ApkHandler#addFilesToApk(List, Map)}
	 */
	public void addFilesToApk(List<File> files) throws IOException {
		this.addFilesToApk(files, new HashMap<String, String>());
	}
	
	/**
	 * Adds the files to the APK which is handled by this {@link ApkHandler}. 
	 * 
	 * @param	files			Array with File objects to be added to the APK.
	 * @param	paths			Map containing paths where to put the files. The Map's keys are the file's paths: <code>paths.get(file.getPath())</code>
	 * @throws	IOException		if an I/O error occurs.
	 */
	public void addFilesToApk(List<File> files, Map<String, String> paths) throws IOException {
		// close zip file to rename apk
		if(this.zip != null) {
			this.zip.close();
			this.zip = null;
		}
		
		// add missing paths to directories parameter
		for(File file : files) {
			if(!paths.containsKey(file.getPath())) paths.put(file.getPath(), file.getName());
		}
		
		// get a temp file
		File tempFile = File.createTempFile(this.apk.getName(), null);
		
		// delete it, otherwise we cannot rename the existing zip to it
		tempFile.delete();

		boolean renameOk = this.apk.renameTo(tempFile);
		if (!renameOk) {
			throw new RuntimeException("could not rename the file "
					+ this.apk.getAbsolutePath() + " to "
					+ tempFile.getAbsolutePath());
		}
		
		byte[] buf = new byte[1024];
		ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
		
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(this.apk));
		ZipEntry entry;
		
		nextEntry:
		while ((entry = zin.getNextEntry()) != null) {
			// skip replaced entries
			for(String path : paths.values()) if(entry.getName().equals(path)) continue nextEntry;
			
			// if not replaced add the zip entry to the output stream
			out.putNextEntry(entry);
			
			// transfer bytes from the zip file to the output file
			int len;
			while ((len = zin.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			
			// close entries
			zin.closeEntry();
			out.closeEntry();
		}
		
		// close stream
		zin.close();
		
		// add files
		for(File file : files) {
			InputStream in = new FileInputStream(file);
			out.putNextEntry(new ZipEntry(paths.get(file.getPath())));
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
			in.close();			
		}

		// flush and close the zip file
		out.flush();
		out.close();
	}
}

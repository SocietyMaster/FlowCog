package soot.jimple.infoflow.android.nu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ValueResourceParser {
	private final Map<String, Integer> decompiledValuesNameIDMap = new HashMap<String, Integer>();
	private final String tmpDirPath;
	private final String apkToolPath;
	private final String apkFileLocation;
	
	public ValueResourceParser(String apkFileLocation, String apkToolPath, String tmpDirPath){
		this.apkToolPath = apkToolPath;
		this.tmpDirPath = tmpDirPath;
		this.apkFileLocation = apkFileLocation;
		extractNameIDPairsFromCompiledValueResources(this.apkFileLocation);
	}
	
	public Integer getResourceIDFromValueResourceFile(String key){
		return decompiledValuesNameIDMap.get(key);
	}
	
	public void displayDecompiledValueIDPairs(){
		for(String key : decompiledValuesNameIDMap.keySet())
			System.out.println("  ValueNameIDPair:"+key+" => "+decompiledValuesNameIDMap.get(key));
	}
	
	private void extractNameIDPairsFromCompiledValueResources(String filename){
		File apkF = new File(filename);
		if (!apkF.exists())
			throw new RuntimeException("file '" + filename + "' does not exist!");
		try {
			String fname = filename.toLowerCase();
			if(fname.contains(File.separator)){
				int idx = fname.lastIndexOf(File.separator);
				fname = fname.substring(idx+1, fname.length());
			}
			if(fname.endsWith(".apk"))
				fname = fname.substring(0, fname.length()-4);
			String path = tmpDirPath+fname;
			String cmd = apkToolPath+" d "+filename +" -o "+path;
			System.out.println("Execute cmd: "+cmd);
			Process p = Runtime.getRuntime().exec(cmd);
		    p.waitFor(); 
		    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line = "";
		    while((line = reader.readLine()) != null)
		    	System.out.print("Decompiling APK: "+line + "\n");

		    path = path + "/res/values/";
		    File f = new File(path);
		    if(!f.isDirectory()){
		    	System.err.println("Error compiling: value folder doesn't exist");
		    	return ;
		    }
		    
		    SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = getValueResouceHandler();
		    for(String xmlFile : f.list()){
		    	xmlFile = f.getAbsolutePath()+"/"+xmlFile;
		    	if(!xmlFile.toLowerCase().endsWith(".xml"))
		    		continue;
		    	InputStream is = new FileInputStream(xmlFile);
		    	System.out.println("analyzing file "+xmlFile);
		    	try{
		    		saxParser.parse(is, handler);
		    	}
		    	catch(Exception e){
		    		System.out.println("Error analyzing file."+e.toString());
		    	}
		    }

		}
		catch (Exception e) {
			System.err.println("Error extractNameIDPairsFromCompiledValueResources in apk "
					+ filename + ": " + e);
			e.printStackTrace();
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new RuntimeException(e);
		}
	}
	
	private DefaultHandler getValueResouceHandler(){
		DefaultHandler handler = new DefaultHandler() {
			public void startElement(String uri, String localName,String qName,
		                Attributes attributes) throws SAXException {
				String name = attributes.getValue("name");
				String id = attributes.getValue("id");
				if(name!=null && id!=null){
					try{
						name = name.trim();
						Integer idInt = null;
						if(id.toLowerCase().startsWith("0x")){
							Long tt = Long.parseLong(id.substring(2, id.length()), 16);
							idInt = tt.intValue();
						}
						else
							idInt = Integer.valueOf(id);
						
						decompiledValuesNameIDMap.put(name, idInt);
					}
					catch(Exception e){
						System.err.println("Error in converting integer: "+name+" "+id+" "+e.toString());
					}
				}
				//System.out.println("Start Element :" + qName+" N:"+name+" ID:"+id);
			}

			public void endElement(String uri, String localName,
				String qName) throws SAXException {

			}

			public void characters(char ch[], int start, int length) throws SAXException {

			}
		};
		   return handler;
	}
}

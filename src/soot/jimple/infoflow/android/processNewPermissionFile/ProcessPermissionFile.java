package soot.jimple.infoflow.android.processNewPermissionFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProcessPermissionFile {
	
	private Map<String, List<String>> permissionMap;
	
	public List<String> getPermissionList(){
		
		List<String> permissionList = new ArrayList<String>();
		
		Iterator<String> iter = permissionMap.keySet().iterator();
		while(iter.hasNext()){
			String signature = iter.next();
			List<String> permissions = permissionMap.get(signature);
			String allPermissions = "";
			for(String s : permissions){
				allPermissions += (" " + s);
			}
			permissionList.add(signature + allPermissions);
		}
		
		return permissionList;
	}
	
	/**
	 * 
	 * @param filenameToBeSaved where it should be saved
	 */
	public void writePermissionList(String filenameToBeSaved){

		try {
			File file = new File(filenameToBeSaved);
			file.delete();
			file = new File(filenameToBeSaved);
			FileWriter writer = new FileWriter(file ,true);

			Iterator<String> iter = permissionMap.keySet().iterator();
			while(iter.hasNext()){
				String signature = iter.next();
				List<String> permissions = permissionMap.get(signature);
				String allPermissions = "";
				for(String s : permissions){
					allPermissions += (" " + s);
				}
				writer.write(signature + allPermissions);
				writer.write(System.getProperty("line.separator"));
			}
			  
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void createPermissionMap(String filename){
		
		try {
			FileReader fReader = new FileReader(filename);

			BufferedReader bReader = new BufferedReader(fReader);
			
			permissionMap = new HashMap<String, List<String>>();
	
			String zeile;
			
			String permissionName = "";
			while ((zeile = bReader.readLine()) != null) {
				if(zeile.startsWith("Permission:")){
					permissionName = zeile.substring(zeile.lastIndexOf(".") + 1);
				}
				else if(!zeile.endsWith("Callers:")){
					String signature = zeile.substring(0, zeile.lastIndexOf("(")-1);
					if(!permissionMap.containsKey(signature)){
						List<String> permissionList = new ArrayList<String>();
						permissionList.add(permissionName);
						permissionMap.put(signature, permissionList);
					}
					else{
						permissionMap.get(signature).add(permissionName);
					}
				}
				
				
	
			}
			bReader.close();
			fReader.close();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

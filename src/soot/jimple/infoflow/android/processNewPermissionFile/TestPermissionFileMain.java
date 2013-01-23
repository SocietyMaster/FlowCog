package soot.jimple.infoflow.android.processNewPermissionFile;

import java.util.List;

public class TestPermissionFileMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ProcessPermissionFile ppf = new ProcessPermissionFile();
		
		//the files can be found on http://pscout.csl.toronto.edu/
		ppf.createPermissionMap("jellybean_allmappings.txt");
		
		List<String> permissionList = ppf.getPermissionList();
		
		for(String s: permissionList){
			System.out.println(s);
		}
		
		ppf.writePermissionList("android-4_1_1-permission-list.txt");

	}

}

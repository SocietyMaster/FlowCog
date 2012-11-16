package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadFile {

	public List<String> readFile(String filename, String separator)
			throws IOException {
		List<String> list = new ArrayList<String>();

		FileReader fReader = new FileReader(filename);
		BufferedReader bReader = new BufferedReader(fReader);

		String zeile;
		int position;
		while ((zeile = bReader.readLine()) != null) {
			if (separator != "") {
				position = zeile.lastIndexOf(separator);
				list.add(zeile.substring(0, position + 1));
			} else {
				list.add(zeile);
			}

		}
		bReader.close();
		fReader.close();
		// System.out.println(list.get(0));
		// System.out.println(list.get(1));
		// System.out.println(list.size());
		return list;
	}

	public List<SootLine> readFileSootLine(String filename) throws IOException {
		List<SootLine> list = new ArrayList<SootLine>();

		FileReader fReader = new FileReader(filename);
		BufferedReader bReader = new BufferedReader(fReader);

		String zeile, argument;
		int position;
		while ((zeile = bReader.readLine()) != null) {

//			position = zeile.lastIndexOf(">");
			

			position = zeile.lastIndexOf(":");
			argument = zeile.substring(1, position);
			
			zeile = zeile.substring(position + 1, zeile.length() - 1).trim();
//			zeile = zeile.substring(0, zeile.indexOf(" "));

			list.add(new SootLine(argument, zeile));


		}
		bReader.close();
		fReader.close();
		// System.out.println(list.get(0));
		// System.out.println(list.get(1));
		// System.out.println(list.size());
		return list;
	}
	public List<String> readFileSources(String filename) throws IOException {
		List<String> list = new ArrayList<String>();

		FileReader fReader = new FileReader(filename);
		BufferedReader bReader = new BufferedReader(fReader);

		String zeile, argument;
		int position;
		while ((zeile = bReader.readLine()) != null) {

			position = zeile.lastIndexOf(">");
			argument = zeile.substring(0, position + 1);

			position = zeile.lastIndexOf(":");
			zeile = zeile.substring(position + 1).trim();
			zeile = zeile.substring(0, zeile.indexOf(" "));

			if (zeile.compareTo("void") != 0) {
				list.add(argument);
			}

		}
		bReader.close();
		fReader.close();
		// System.out.println(list.get(0));
		// System.out.println(list.get(1));
		// System.out.println(list.size());
		return list;
	}

	public List<String> getMappedPermissions(String permissionMatrixFilename,
			List<String> permissionApp) throws IOException {
		List<String> mappedPermissions = new ArrayList<String>();

		FileReader fReader = new FileReader(permissionMatrixFilename);
		BufferedReader bReader = new BufferedReader(fReader);

		String zeile, permissions;
		int position;
		while ((zeile = bReader.readLine()) != null) {

			position = zeile.lastIndexOf(">");
			permissions = zeile.substring(position + 1).trim();
			String[] permissionArrayMatrix = permissions.split(" ");
			
			boolean found = false;
			for (int i = 0; i < permissionArrayMatrix.length; i++) {

				for (String t : permissionApp) {
					if (permissionArrayMatrix[i].equals(t)) {
						found = true;
						 mappedPermissions.add(zeile.substring(0, position +
						 1));
//						 System.out.println(t);
						break;
					}
				}
				if (found){
					break;
				}


			}
			//
/*			boolean foundAll = true;
			for (int i = 0; i < permissionArray.length; i++) {
				boolean found = false;
				for (String t : permissionApp) {
					if (permissionArray[i].equals(t)) {
						found = true;
						// mappedPermissions.add(zeile.substring(0, position +
						// 1));
//						 System.out.println(zeile.substring(0, position + 1));
						break;
					}
				}
				if (!found) {
					foundAll = false;
					break;
				}

			}
			if (foundAll) {
				mappedPermissions.add(zeile.substring(0, position + 1));
//				System.out.println(zeile.substring(0, position + 1));
			}*/

			// position = zeile.lastIndexOf(":");
			// zeile = zeile.substring(position + 1).trim();
			// zeile = zeile.substring(0, zeile.indexOf(" "));
			//
			// if (zeile.compareTo("void") != 0) {
			// mappedPermissions.add(permissions);
			// }

		}
		bReader.close();
		fReader.close();

		return mappedPermissions;

	}
	

	public List<String> getMappedPermissionsOnlyComplete(String permissionMatrixFilename,
			List<String> permissionApp) throws IOException {
		List<String> mappedPermissions = new ArrayList<String>();

		FileReader fReader = new FileReader(permissionMatrixFilename);
		BufferedReader bReader = new BufferedReader(fReader);

		String zeile, permissions;
		int position;
		

		while ((zeile = bReader.readLine()) != null) {

			position = zeile.lastIndexOf(">");
			permissions = zeile.substring(position + 1).trim();
			String[] permissionArrayMatrix = permissions.split(" ");
			
//			boolean found = false;
//			for (int i = 0; i < permissionArrayMatrix.length; i++) {
//
//				for (String t : permissionApp) {
//					if (permissionArrayMatrix[i].equals(t)) {
//						found = true;
//						 mappedPermissions.add(zeile.substring(0, position +
//						 1));
////						 System.out.println(t);
//						break;
//					}
//				}
//				if (found){
//					break;
//				}
//
//
//			}
			//

			boolean foundAll = true;
			for (int i = 0; i < permissionArrayMatrix.length; i++) {
				boolean found = false;
				
				for (String t : permissionApp) {
					if (permissionArrayMatrix[i].equals(t)) {
						found = true;
						// mappedPermissions.add(zeile.substring(0, position +
						// 1));
//						 System.out.println(zeile.substring(0, position + 1));
						break;
					}
				}
				if (!found) {
					foundAll = false;
					break;
				}

			}
			if (foundAll) {
				mappedPermissions.add(zeile.substring(0, position + 1));
//				System.out.println(zeile);
			}

			// position = zeile.lastIndexOf(":");
			// zeile = zeile.substring(position + 1).trim();
			// zeile = zeile.substring(0, zeile.indexOf(" "));
			//
			// if (zeile.compareTo("void") != 0) {
			// mappedPermissions.add(permissions);
			// }

		}
		bReader.close();
		fReader.close();

		return mappedPermissions;

	}
	


}

package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WriteProcessedMatrix {
	public void toBeProccessedByExcel(String filenameToBeRead, String filenameToBeSaved)
			throws IOException {


		FileReader fReader = new FileReader(filenameToBeRead);
		BufferedReader bReader = new BufferedReader(fReader);

		FileWriter writer;
		  File file = new File(filenameToBeSaved);
		  file.delete();
		  file = new File(filenameToBeSaved);
		  writer = new FileWriter(file ,true);
		 
		  
		
		String zeile, newZeile, permissions;
		int position;
		while ((zeile = bReader.readLine()) != null) {
			position = zeile.lastIndexOf(">");
			newZeile = zeile.substring(0, position + 1);
			
			permissions = zeile.substring(position + 1);
			permissions = permissions.trim();
			
			String[] permissionArrayMatrix = permissions.split(" ");
			for(String t : permissionArrayMatrix){
				newZeile = newZeile + ";"  + t;
				System.out.println(newZeile);
			}
			writer.write(newZeile);
			writer.write(System.getProperty("line.separator"));

		}
		bReader.close();
		fReader.close();
		  writer.flush();
		  writer.close();
		// System.out.println(list.get(0));
		// System.out.println(list.get(1));
		// System.out.println(list.size());

	}

}

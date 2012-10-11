package soot.jimple.infoflow.android;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeConfigSourceSink {

	public List<String> getReducedSourceSinkList(String configFilename, List<String> SourceSinkList)
			throws IOException, wrongFormatedFileException {
		List<String> returnList = new ArrayList<String>();

		FileReader fReader = new FileReader(configFilename);
		BufferedReader bReader = new BufferedReader(fReader);

		String configLine;

		while ((configLine = bReader.readLine()) != null) {
			if (configLine.substring(0, 1).equals("+")) {
				if(configLine.substring(1).toUpperCase().equals("ALL")) {
					
					returnList = SourceSinkList;
				}
				else{
					for(String elem : SourceSinkList){
						String[] sourceSinkListLinePart = elem.split(" ");
						if(sourceSinkListLinePart[2].matches(configLine.substring(1))){
							returnList.add(elem);
						}
					}
				}

			} else if (configLine.substring(0, 1).equals("-")) {
				
				for(int i=0; i<returnList.size();i++){
					String[] returnListLinePart = returnList.get(i).split(" ");
					
					
					
					if(returnListLinePart[2].matches(configLine.substring(1))){
						returnList.remove(i);
						i--;
					}
				}

			} else {
				throw new wrongFormatedFileException(
						"Each line has to begin with + or -");
			}


		}
		bReader.close();
		fReader.close();

		return returnList;

	}

}

package nu.dataflow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64.Decoder;

public class MainClass {
	final Pattern METHOD_CALL_PATTERN = Pattern.compile("\\<(.)+?\\>");
	final Set<String> tlds = new HashSet<String>();
	final Map<String, Integer> fieldCount = new HashMap<String, Integer>();
	final Map<String, Integer> methodCount = new HashMap<String, Integer>();
	final Pattern BASE64_PATTERN = Pattern.compile("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$");
	
	public static void main(String[] args) {
		MainClass m = new MainClass();
		m.installTLDs("../tlds.txt");
		m.readURLFile("../url.log");
		
		String a="abcde sdef,fnnn";
		String[] tmp = a.split("[ ,]");
		for(String t : tmp)
			System.out.println(t);
	}
	
	
	private Map<String, List<String> > readURLFile(String filename){
		Map<String, List<String>> rs= new HashMap<String, List<String>>();
		BufferedReader br = null;
		FileReader fr = null;
		try {

			fr = new FileReader(filename);
			br = new BufferedReader(fr);

			String line;
			int cnt = 0;
			int bad = 0;
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				String[] tmp = line.split(":");
				
				if(tmp==null || tmp.length<3) continue;
				String fname = tmp[0].trim();	
				Set<String> vals = null;
				String target = null;
				if(tmp.length > 3){
					StringBuilder sb = new StringBuilder();
					for(int i=2; i<tmp.length; i++)
						sb.append(tmp[i]);
					target = sb.toString();
					vals = processURL(target);
				}
				else{
					target = tmp[2];
					vals = processURL(tmp[2]);
				}
				if(vals.size() > 0){
					cnt++;
					System.out.println(fname);
					for(String v : vals)
						System.out.println("  "+v);
				}
				else {
					bad++;
					//System.out.println(target);
				}
			}
			System.out.println("Count: "+cnt+" vs "+bad);
			

		} catch (IOException e) {

			e.printStackTrace();

		} 	
		for(String key : fieldCount.keySet()){
			System.out.println("FC:"+key+" : "+fieldCount.get(key));
		}
		for(String key : methodCount.keySet()){
			System.out.println("MC:"+key+" : "+methodCount.get(key));
		}
		return rs;
	}
	
	private Set<String> processURL(String line){
		Set<String> rs = new HashSet<String>();
		String[] elems = line.split(",");
		
		if(elems==null || elems.length==0)
			return rs;
		for(String elem : elems){
			String[] cells = elem.split(" ");
			for(String cell : cells){
				cell = cell.trim().toLowerCase();
				if(cell.length()==0) continue;
//				System.out.println(cell);
				if(cell.startsWith("'")|| cell.startsWith("\""))
					cell = cell.substring(1);
				if(cell.startsWith("http://") || cell.startsWith("https://") ||
						cell.startsWith("http//") || cell.startsWith("https//") )
					rs.add(cell);
				else {
					if(cell.contains("<methodcall>") || cell.contains("<classfield>"))
						continue;
					String[] tmp = cell.split("\\.");
					if(tmp==null || tmp.length<=1) continue;
					if(tlds.contains(tmp[tmp.length-1].trim()) ){
						//System.out.println("found a host:"+cell+" "+cell.length());
						rs.add(cell);
					}
				}
			}
		}
		
		//process [NUTAG]
		for(String elem : elems){
			String[] tmp = elem.split("\\[NUTAG\\]");
			if(tmp==null || tmp.length<1) continue;
		
			for(String cell : tmp){
				cell = cell.trim();
				if(cell.startsWith("<METHODCALL>")){
					cell = cell.substring(12).trim();
					Matcher matcher = METHOD_CALL_PATTERN.matcher(cell);
//					System.out.println("M:"+cell+" ");
					if(matcher.find()){
						incrementKeyVal(methodCount, matcher.group(0).trim());
						rs.add(matcher.group(0).trim());
					}
				}
				else if(cell.startsWith("<CLASSFIELD>")){
					cell = cell.substring(12);
					String[] tt = cell.split("[, ]");
	
					if(tt==null || tt.length<=0)
						continue;
					String key = tt[0].trim();
					incrementKeyVal(fieldCount, key);
					rs.add(key);
				}
			}
		}
		
		if(rs.size() == 0){
			for(String elem : elems){
				String[] tmp = elem.split(" ");
				for(String t : tmp){
					Matcher m = BASE64_PATTERN.matcher(t);
					if(m.find()){
						String x = new String(Base64.getDecoder().decode(t)).toLowerCase();
						if(x.startsWith("http"))
							rs.add(x);
					}
				}
			}
		}
		//System.out.println("found "+rs.size()+" urls");
		return rs;
	}
	
	private void installTLDs(String filename){
		BufferedReader br = null;
		FileReader fr = null;
		try {
			fr = new FileReader(filename);
			br = new BufferedReader(fr);
			String line;
			br = new BufferedReader(new FileReader(filename));
			while ((line = br.readLine()) != null) {
				if(line.startsWith("//")) continue;
				line = line.trim();
				tlds.add(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 	
		System.out.println("has read "+tlds.size()+" tlds.");
	}
	
	private <K,V> void addToMapList(Map<K,List<V>> map, K key, V val) {
		if(map.containsKey(key))
			map.get(key).add(val);
		else{
			List<V> vals = new ArrayList<V>();
			vals.add(val);
			map.put(key, vals);
		}
	}
	private <K> void incrementKeyVal(Map<K,Integer> map, K key) {
		int val = 1;
		if(map.containsKey(key))
			val = ((Integer)map.get(key)) + 1 ;
	
		map.put(key, val);
		
	}
	
	
}

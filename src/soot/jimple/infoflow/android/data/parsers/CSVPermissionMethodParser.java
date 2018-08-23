/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.data.parsers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.source.data.SourceSinkDefinition;

/**
 * Parser for Android method / permission maps in the format defined by Port Felt
 *
 * @author Steven Arzt
 *
 */
public class CSVPermissionMethodParser implements ISourceSinkDefinitionProvider {

	private Set<SourceSinkDefinition> sourceList = null;
	private Set<SourceSinkDefinition> sinkList = null;
	private Set<SourceSinkDefinition> neitherList = null;

	private static final int INITIAL_SET_SIZE = 10000;

	private final String fileName;
	
	public CSVPermissionMethodParser(String fileName) {
		this.fileName = fileName;
	}
	
	public void parse() {
		sourceList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		sinkList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		neitherList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(this.fileName));
			String line = null;
			boolean firstLine = true;
			while ((line = rdr.readLine()) != null) {
				// Ignore the first line which is a header
				if (firstLine) {
					firstLine = false;
					continue;
				}
				firstLine = false;
				
				// Get the CSV fields
				String[] fields = line.split("\t");
				if (fields.length < 1) {
					System.err.println("Found invalid line: " + line);
					continue;
				}
				
				// Parse the method signature
				String methodName;
				String className;
				List<String> methodParams = new ArrayList<String>();
				Set<String> permissions = new HashSet<String>();
				try {
					if (fields[0].contains(")"))
						methodName = fields[0].substring(0, fields[0].indexOf("("));
					else
						methodName = fields[0];
					className = methodName.substring(0, methodName.lastIndexOf("."));
					methodName = methodName.substring(methodName.lastIndexOf(".") + 1);
					
					// Parse the parameters
					if (fields[0].contains("(")) {
						String parameters = fields[0].substring(fields[0].indexOf("(") + 1);
						parameters = parameters.substring(0, parameters.indexOf(")"));
						for (String p : parameters.split(","))
							methodParams.add(p);
					}
					
					String perm = (fields.length > 1) ? fields[1] : "";
					perm = perm.replaceAll(" and ", " ");
					perm = perm.replaceAll(" or ", " ");
					if (perm.contains("."))
						perm = perm.substring(perm.lastIndexOf(".") + 1);
					for (String p : perm.split(" "))
						permissions.add(p);
				}
				catch (StringIndexOutOfBoundsException ex) {
					System.err.println("Could not parse line: " + line);
					ex.printStackTrace();
					continue;
				}
				
				AndroidMethod method = new AndroidMethod(methodName,
						methodParams, "", className, permissions);
				if (method.isSource())
					sourceList.add(new SourceSinkDefinition(method));
				else if (method.isSink())
					sinkList.add(new SourceSinkDefinition(method));
				else if (method.isNeitherNor())
					neitherList.add(new SourceSinkDefinition(method));
			}
			
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		finally {
			if (rdr != null)
				try {
					rdr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	@Override
	public Set<SourceSinkDefinition> getSources() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sourceList;
	}

	@Override
	public Set<SourceSinkDefinition> getSinks() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sinkList;
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		if (sourceList == null || sinkList == null)
			parse();
		
		Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sourceList.size()
				+ sinkList.size() + neitherList.size());
		sourcesSinks.addAll(sourceList);
		sourcesSinks.addAll(sinkList);
		sourcesSinks.addAll(neitherList);
		return sourcesSinks;
	}

}

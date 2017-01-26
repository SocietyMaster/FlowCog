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
package soot.jimple.infoflow.android.TestApps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.stream.XMLStreamException;

import org.xmlpull.v1.XmlPullParserException;

import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.UnopExpr;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackAnalyzer;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.nu.FlowClassifier;
import soot.jimple.infoflow.android.nu.InfoflowResultsWithFlowPathSet;
import soot.jimple.infoflow.android.nu.LayoutFileParserForTextExtraction;
import soot.jimple.infoflow.android.nu.LayoutTextTreeNode;
import soot.jimple.infoflow.android.nu.ParameterSearch;
import soot.jimple.infoflow.android.nu.ResolvedConstantTag;
import soot.jimple.infoflow.android.nu.ResourceManager;
import soot.jimple.infoflow.android.nu.ValueResourceParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.SourceType;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.handlers.ResultsAvailableHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.nu.FlowPath;
import soot.jimple.infoflow.nu.FlowPathSet;
import soot.jimple.infoflow.nu.GraphTool;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.results.xml.InfoflowResultsSerializer;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;

public class Test {
	
	//XIANG: store the flow path set returned by the first SetupApplication.
	//not a good design, think about how to fix it.
	//private static FlowPathSet fps = null;
	
	private static final class MyResultsAvailableHandler implements
			ResultsAvailableHandler {
		private final BufferedWriter wr;

		private MyResultsAvailableHandler() {
			this.wr = null;
		}

		private MyResultsAvailableHandler(BufferedWriter wr) {
			this.wr = wr;
		}

		@Override
		public void onResultsAvailable(
				IInfoflowCFG cfg, InfoflowResults results) {
			// Dump the results
			if (results == null) {
				print("No results found.");
			}
			else {
				// Report the results
				for (ResultSinkInfo sink : results.getResults().keySet()) {
					print("Found a flow to sink " + sink + ", from the following sources:");
					for (ResultSourceInfo source : results.getResults().get(sink)) {
						print("\t- " + source.getSource() + " (in "
								+ cfg.getMethodOf(source.getSource()).getSignature()  + ")");
						if (source.getPath() != null)
							print("\t\ton Path " + Arrays.toString(source.getPath()));
					}
				}
				
				// Serialize the results if requested
				// Write the results into a file if requested
				if (resultFilePath != null && !resultFilePath.isEmpty()) {
					InfoflowResultsSerializer serializer = new InfoflowResultsSerializer(cfg);
					try {
						serializer.serialize(results, resultFilePath);
					} catch (FileNotFoundException ex) {
						System.err.println("Could not write data flow results to file: " + ex.getMessage());
						ex.printStackTrace();
						throw new RuntimeException(ex);
					} catch (XMLStreamException ex) {
						System.err.println("Could not write data flow results to file: " + ex.getMessage());
						ex.printStackTrace();
						throw new RuntimeException(ex);
					}
				}
			}
			
		}

		private void print(String string) {
			try {
				System.out.println(string);
				if (wr != null)
					wr.write(string + "\n");
			}
			catch (IOException ex) {
				// ignore
			}
		}
	}
	private static final class ConstantPropogationResultsHanlder implements ResultsAvailableHandler {
		private final BufferedWriter wr;
		private final Map<Stmt, Set> rs = new HashMap<Stmt, Set>();
		
		public Map<Stmt, Set> getRS() {
			return rs;
		}

		private ConstantPropogationResultsHanlder() {
			this.wr = null;
		}

		private ConstantPropogationResultsHanlder(BufferedWriter wr) {
			this.wr = wr;
		}

		@Override
		public void onResultsAvailable(
				IInfoflowCFG cfg, InfoflowResults results) {
			// Dump the results
			if (results == null) {
				print("No results found.");
			}
			else {
				// Report the results
				for (ResultSinkInfo sink : results.getResults().keySet()) {
					print("Found a flow to sink " + sink + ", from the following sources:"+sink.getSink());
					for (ResultSourceInfo source : results.getResults().get(sink)) {
						print("\tSource:" + source.getSource() + " (in "
								+ cfg.getMethodOf(source.getSource()).getSignature()  + ")");
						if (source.getPath() != null){
							for(Stmt stmt : source.getPath()){
								print("\t\t -" + stmt+" @"+cfg.getMethodOf(stmt));
							}
						}
						
						Stmt sourceStmt = source.getSource();
						
						Stmt sinkStmt = sink.getSink();
						Set set = rs.get(sinkStmt);
						if(set == null){
							set = new HashSet<Object>();
							rs.put(sinkStmt, set);
						}
						if(sinkStmt.getInvokeExpr().getMethod().getName().equals("findViewById")){
							if(sourceStmt instanceof IdentityStmt){
								IdentityStmt is = (IdentityStmt)sourceStmt;
								if(is.getRightOp() instanceof ParameterRef){
									ParameterRef right = (ParameterRef)(is.getRightOp());
									int idx = right.getIndex();
									Collection<Unit> callers = cfg.getCallersOf(cfg.getMethodOf(sourceStmt));
									if(callers != null && callers.size()>0){
										for(Unit caller : callers){
											InvokeExpr ie = ((Stmt)caller).getInvokeExpr();
											if(idx >= ie.getArgCount())
												continue;
											Value arg = ie.getArg(idx);
											if(arg instanceof IntConstant){
												Integer v = ((IntConstant)arg).value;
												set.add(v);
											}
												
										}
									}
								}
							}
							else if(sourceStmt instanceof AssignStmt){
								AssignStmt as = (AssignStmt)sourceStmt;
								Value right = as.getRightOp();
								if(right instanceof CastExpr){
									CastExpr ce = (CastExpr)right;
									Value v = ce.getOp();
									if(v instanceof IntConstant)
										set.add(((IntConstant)v).value);
								}
								else if(right instanceof UnopExpr){
									UnopExpr ue = (UnopExpr)right;
									
									Value v = ue.getOp();
									if(v instanceof IntConstant)
										set.add(Math.abs(((IntConstant)v).value));
								}
								else if(right instanceof BinopExpr ){
									BinopExpr be = (BinopExpr)right;
									Value v1 = be.getOp1();
									Value v2 = be.getOp2();
									if(v1 instanceof IntConstant && v2 instanceof IntConstant){
										Integer v1Int = ((IntConstant)v1).value;
										Integer v2Int = ((IntConstant)v2).value;
										if(be.getSymbol().equals("+"))
											set.add(v1Int+v2Int);
										else if(be.getSymbol().equals("*"))
											set.add(v1Int*v2Int);
										else if(be.getSymbol().equals("-"))
											set.add(v1Int-v2Int);
										else if(be.getSymbol().equals("/"))
											set.add(v1Int/v2Int);
									}
								}
								else if(right instanceof InvokeExpr){
									InvokeExpr ie = sourceStmt.getInvokeExpr();
									SootMethod sm = ie.getMethod();
									if(sm.getName().equals("valueOf") && sm.getDeclaringClass().getName().equals("java.lang.Integer")){
										Value arg = ie.getArg(0);
										if(arg instanceof IntConstant)
											set.add(((IntConstant)arg).value);
									}
									
								}
								else if(right instanceof IntConstant){
									set.add(((IntConstant)right).value);
								}
								else{
									System.out.println("UNKNOWN SOURCE: "+sourceStmt);
								}
							}
							
							//solve the case when initialize array with Integer[] arr = {id1,id2, id3, id4}
							//FlowDroid will only display the last 2 or 3 ids.
							//If this happens, we will find all assignments to arrayref
							//and then find potential def for the array
							if (source.getPath() != null){
								for(Stmt stmt : source.getPath()){
									if(stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp() instanceof ArrayRef){
										ArrayRef ar = (ArrayRef)((AssignStmt) stmt).getLeftOp();
										Value base = ar.getBase();
										System.out.println("Found array assign: "+stmt);
										if(base instanceof Local){
											Local lbase = (Local)base;
											SootMethod sm = cfg.getMethodOf(stmt);
											if(!sm.hasActiveBody()) continue;
											
											UnitGraph g = new ExceptionalUnitGraph(sm.getActiveBody());
										    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
										    for (Unit u : orderer.newList(g, false)) {
										    	Stmt s = (Stmt)u;
										    	if(! (s instanceof AssignStmt && 
										    		 ((AssignStmt) s).getLeftOp() instanceof ArrayRef))
										    		continue;
										    	Value b = ((ArrayRef)(((AssignStmt) s).getLeftOp())).getBase();
										    	if(lbase.equals(b)){
										    		System.out.println("  Found another array assign to this base: ");
										    		Value rv = ((AssignStmt) s).getRightOp();
										    		if(rv instanceof Constant){
										    			System.out.println("    Constant:"+rv);
										    		}
										    		else if(rv instanceof Local){
										    			IntConstant c = searchIntConstantDef(g, (Stmt)u, (Local)rv);
										    			if(c != null){
										    				System.out.println("    ConstantFound:"+c);
										    				set.add(c.value);
										    			}
										    			else{
										    				System.out.println("    Cannot find constant");
										    			}
										    		}
										    	}
										    }//for u in g
										}
									}
								}
							}
						}//findViewById case
						else {
							//TODO: add PreferenceValues
						}
						
						
					}//source
					Set set = rs.get(sink.getSink());
					ResolvedConstantTag rct = new ResolvedConstantTag();
					sink.getSink().addTag(rct);
					for(Object obj : set){
						if(obj instanceof Integer)
							rct.addIntegerConstant((Integer)obj);
						else if(obj instanceof String)
							rct.addStringConstant((String)obj);
					}
					
					
				}//per sink
				
			}
			
		}
		
		private IntConstant searchIntConstantDef(UnitGraph ug, Stmt stmt, Local target){
			Set<Stmt> visited = new HashSet<Stmt>();
			Queue<Stmt> queue = new LinkedList<Stmt>();
			queue.add(stmt);
			while(!queue.isEmpty()){
				stmt = queue.poll();
				if(stmt instanceof AssignStmt && ((AssignStmt) stmt).getLeftOp().equals(target)){
					Value rv = ((AssignStmt) stmt).getRightOp();
					if(rv instanceof IntConstant){
						return (IntConstant)rv;
					}
					else if(rv instanceof Local){
						target = (Local)rv;
					}
					else if(rv instanceof InvokeExpr){
						InvokeExpr ie = (InvokeExpr)rv;
						SootMethod sm = ie.getMethod();
						if(sm.getName().equals("valueOf") && sm.getDeclaringClass().getName().equals("java.lang.Integer")){
							Value arg = ie.getArg(0);
							if(arg instanceof IntConstant)
								return (IntConstant)arg;
						}
					}
				}
				visited.add(stmt);
				List<Unit> preds = ug.getPredsOf(stmt);
				if(preds != null){
					for(Unit pred : preds)
						if(!visited.contains((Stmt)pred)){
							queue.add((Stmt)pred);
						}
				}
			}
			return null;
		}
		
		private void print(String string) {
			try {
				System.out.println(string);
				if (wr != null)
					wr.write(string + "\n");
			}
			catch (IOException ex) {
				// ignore
			}
		}
		
	}
	
	private static InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
	
	private static int repeatCount = 1;
	private static int timeout = -1;
	private static int sysTimeout = -1;
	private static String apktoolpath = "apktool";

	private static String tmpDirPath = "/tmp/";
	
	private static boolean aggressiveTaintWrapper = false;
	private static boolean noTaintWrapper = false;
	private static String summaryPath = "";
	private static String resultFilePath = "";
	
	private static boolean DEBUG = false;

	private static IIPCManager ipcManager = null;
	public static void setIPCManager(IIPCManager ipcManager)
	{
		Test.ipcManager = ipcManager;
	}
	public static IIPCManager getIPCManager()
	{
		return Test.ipcManager;
	}
	public static String getApktoolpath() {
		return apktoolpath;
	}
	public static String getTmpDirPath() {
		return tmpDirPath;
	}
	
	/**
	 * @param args Program arguments. args[0] = path to apk-file,
	 * args[1] = path to android-dir (path/android-platforms/)
	 */
	public static void main(final String[] args) throws IOException, InterruptedException {
		if (args.length < 2) {
			printUsage();	
			return;
		}
		//start with cleanup:
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()){
			boolean success = true;
			for(File f : outputDir.listFiles()){
				success = success && f.delete();
			}
			if(!success){
				System.err.println("Cleanup of output directory "+ outputDir + " failed!");
			}
			outputDir.delete();
		}
		
		// Parse additional command-line arguments
		if (!parseAdditionalOptions(args))
			return;
		if (!validateAdditionalOptions())
			return;
		if (repeatCount <= 0)
			return;
		
		List<String> apkFiles = new ArrayList<String>();
		String apkName = "com.cssoft.eztrans.km01-25.apk";
		//String apkName = "air.com.eni.JudyChineseMaker2-1003000.apk";
		//String apkName = "com.app_greenjobs.layout-400.apk";
		if(!args[0].toLowerCase().endsWith("apk"))
			args[0] += apkName;
		
		File apkFile = new File(args[0]);
		
		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {
			
				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}
			
			});
			for (String s : dirFiles)
				apkFiles.add(s);
		} else {
			//apk is a file so grab the extension
			String extension = apkFile.getName().substring(apkFile.getName().lastIndexOf("."));
			if (extension.equalsIgnoreCase(".txt")) {
				BufferedReader rdr = new BufferedReader(new FileReader(apkFile));
				String line = null;
				while ((line = rdr.readLine()) != null)
					apkFiles.add(line);
				rdr.close();
			}
			else if (extension.equalsIgnoreCase(".apk"))
				apkFiles.add(args[0]);
			else {
				System.err.println("Invalid input file format: " + extension);
				return;
			}
		}
		
		int oldRepeatCount = repeatCount;
		for (final String fileName : apkFiles) {
			repeatCount = oldRepeatCount;
			final String fullFilePath;
			System.gc();
			
			// Directory handling
			if (apkFiles.size() > 1) {
				if (apkFile.isDirectory())
					fullFilePath = args[0] + File.separator + fileName;
				else
					fullFilePath = fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
				File flagFile = new File("_Run_" + new File(fileName).getName());
				if (flagFile.exists())
					continue;
				flagFile.createNewFile();
			}
			else
				fullFilePath = fileName;

			// Run the analysis
			while (repeatCount > 0) {
				System.gc();
				if (timeout > 0)
					runAnalysisTimeout(fullFilePath, args[1]);
				else if (sysTimeout > 0)
					runAnalysisSysTimeout(fullFilePath, args[1]);
				else{
					//analyze resource file to extract view info
					ValueResourceParser valResParser = new ValueResourceParser(fullFilePath, apktoolpath, tmpDirPath);
					valResParser.displayDecompiledValueIDPairs();
					runAnalysisForConstantPropogation(fullFilePath, args[1], null, valResParser,true);
					
					//Analysis
					runNUDataFlowAnalysis(fullFilePath, args[1]);					
					//GraphTool.displayAllMethodGraph();
				}
				repeatCount--;
			}
			
			System.gc();
		}
	}
	static private void runNUDataFlowAnalysis(String fullFilePath, String androidJar){
		//XIANG
		try{
			File f = new File(tmpDirPath);
			if(!f.exists() || !f.isDirectory()){
				System.err.println("tmp folder not exits: "+tmpDirPath);
				System.exit(1);
			}
		}
		catch(Exception e){
			System.err.println("tmp folder not exits:"+e.toString());
			System.exit(1);
		}
		//first round data flow analysis to find flows.
		FlowPathSet fps = runAnalysis(fullFilePath, androidJar);
		
		System.out.println("AAAAA Start second round.");
		ProcessManifest processMan = null;
		ResourceManager resMgr = null;
		try{
			processMan = new ProcessManifest(fullFilePath);
			String appPackageName = processMan.getPackageName();
			System.out.println("FillFilePath: "+fullFilePath);
			resMgr = new ResourceManager(fullFilePath, appPackageName, apktoolpath, tmpDirPath);
		}
		catch(Exception e){
			System.err.println("failed to run taint analysis on view-flow. "+e);
			e.printStackTrace();
		}
		
		System.out.println("Start correlating View and Flow.");
		//second round data flow analysis to correlate flows and views.
		//GraphTool.displayAllMethodGraph();
		soot.G.reset();
		runAnalysisForFlowViewCorrelation(fullFilePath, androidJar, fps);
				
		//correlate view and flow based on events defined in XML file
		fps.updateXMLEventListener(resMgr.getXMLEventHandler2ViewIds());
		//display for debug
		displayFlowViewInfo(fps, resMgr);
	}
	
	//XIANG
	public static void displayFlowViewInfo(FlowPathSet fps, ResourceManager resMgr){
			System.out.println("NULIST: Display Flow Index");
			for(int i=0; i<fps.getLst().size(); i++){
				System.out.println("NULIST: Flow:"+i+"  => SRC:"+fps.getLst().get(i).getSource().toString()+" SINK:"+fps.getLst().get(i).getSink().toString());
				System.out.println("  DEBUG:"+fps.getLst().get(i).toString());
			}
			System.out.println("NULIST: Done Display Flow Index");
			Map<Integer, Set<Integer>> map = fps.getViewFlowMap();
			System.out.println("NULIST: Display Flow View Info");
			//first go through the views with VIEWs found.
			for(Integer flowId : map.keySet()){
				Set<Integer> set = map.get(flowId);
				for(Integer viewId : set){
					LayoutTextTreeNode node = resMgr.getNodeById(viewId);
					String type = "unknown";
					if(node != null)  type = node.nodeType;
					System.out.println("NULIST:[BEGIN] Flow:"+flowId+" => "+viewId+" ("+
						type+") ["+fps.getLst().get(flowId).getTag()+"]");
					
					String texts = resMgr.getTextsById(viewId);
					
					System.out.println("NULIST:[TEXT]:"+texts);
					System.out.println("NULIST:[END]");
				}
			}
			//resMgr.getL
			//then go through the views triggered by life cycle events (e.g., onCreate)
			//display the layout information.
			Map<String, Set<Integer>> cls2LayoutIds = fps.getActivityLayoutMap();
			for(int i=0; i<fps.getLst().size(); i++){
//				FlowPath fp2 = fps.getLst().get(i);
//				Set<String> ss = fp2.getEventListenerClassSet();
//				for(String s : ss){
//					System.out.println("XPAN: "+s);
//				}
				//only display those flows without associated views
				if(!map.containsKey(i)){
					FlowPath fp = fps.getLst().get(i);
					Set<String> declaringCls = fp.getDeclaringClassSet();
					System.out.println("NULIST:[BEGIN] Flow:"+i+" => noview ["+fps.getLst().get(i).getTag()+"] "+declaringCls.size());
					for(String cls : declaringCls){
						Set<Integer> layouts = cls2LayoutIds.get(cls);
						System.out.println("NULIST:    Class:"+cls+" layouts:"+layouts);
						if(layouts == null) continue;
						for(Integer layoutId : layouts){
							//LayoutTextTreeNode node = id2Node.get(layoutId);
							LayoutTextTreeNode layout  = resMgr.getLayoutById(layoutId);
							if(layout == null){
								System.out.println("NULIST:  cannot find this layout "+layoutId);
								continue;
							}
							//System.out.println("NULIST:    Layout:\n"+layout.toStringTree(3, "NULIST:"));
							System.out.println("NULIST:[TEXT]:"+layout.extractTexts("||"));
						}
					}
					System.out.println("NULIST:[END]");
				}
			}
			System.out.println("NULIST: Done Displaying Flow View Info");
	}

	/**
	 * Parses the optional command-line arguments
	 * @param args The array of arguments to parse
	 * @return True if all arguments are valid and could be parsed, otherwise
	 * false
	 */
	private static boolean parseAdditionalOptions(String[] args) {
		int i = 2;
		while (i < args.length) {
			if(args[i].equalsIgnoreCase("--apktoolpath")) {
				apktoolpath = args[i+1];
				i += 2;
			}
			else if(args[i].equalsIgnoreCase("--tmppath")) {
				//output tmp files
				tmpDirPath = args[i+1];
				if(!tmpDirPath.endsWith(File.separator))
					tmpDirPath += File.separator;
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--timeout")) {
				timeout = Integer.valueOf(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--systimeout")) {
				sysTimeout = Integer.valueOf(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--singleflow")) {
				config.setStopAfterFirstFlow(true);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--implicit")) {
				config.setEnableImplicitFlows(true);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--nostatic")) {
				config.setEnableStaticFieldTracking(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--aplength")) {
				InfoflowAndroidConfiguration.setAccessPathLength(Integer.valueOf(args[i+1]));
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--cgalgo")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("AUTO"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.AutomaticSelection);
				else if (algo.equalsIgnoreCase("CHA"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.CHA);
				else if (algo.equalsIgnoreCase("VTA"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.VTA);
				else if (algo.equalsIgnoreCase("RTA"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.RTA);
				else if (algo.equalsIgnoreCase("SPARK"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.SPARK);
				else if (algo.equalsIgnoreCase("GEOM"))
					config.setCallgraphAlgorithm(CallgraphAlgorithm.GEOM);
				else {
					System.err.println("Invalid callgraph algorithm");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--nocallbacks")) {
				config.setEnableCallbacks(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--noexceptions")) {
				config.setEnableExceptionTracking(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--layoutmode")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("NONE"))
					config.setLayoutMatchingMode(LayoutMatchingMode.NoMatch);
				else if (algo.equalsIgnoreCase("PWD"))
					config.setLayoutMatchingMode(LayoutMatchingMode.MatchSensitiveOnly);
				else if (algo.equalsIgnoreCase("ALL"))
					config.setLayoutMatchingMode(LayoutMatchingMode.MatchAll);
				else {
					System.err.println("Invalid layout matching mode");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--aliasflowins")) {
				config.setFlowSensitiveAliasing(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--paths")) {
				config.setComputeResultPaths(true);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--nopaths")) {
				config.setComputeResultPaths(false);
				i++;
			}
			else if(args[i].equalsIgnoreCase("--graphoutpath")){
				GraphTool.setOutputFolder(args[i+1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--aggressivetw")) {
				aggressiveTaintWrapper = false;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--pathalgo")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("CONTEXTSENSITIVE"))
					config.setPathBuilder(PathBuilder.ContextSensitive);
				else if (algo.equalsIgnoreCase("CONTEXTINSENSITIVE"))
					config.setPathBuilder(PathBuilder.ContextInsensitive);
				else if (algo.equalsIgnoreCase("SOURCESONLY"))
					config.setPathBuilder(PathBuilder.ContextInsensitiveSourceFinder);
				else {
					System.err.println("Invalid path reconstruction algorithm");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--summarypath")) {
				summaryPath = args[i + 1];
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--saveresults")) {
				resultFilePath = args[i + 1];
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--sysflows")) {
				config.setIgnoreFlowsInSystemPackages(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--notaintwrapper")) {
				noTaintWrapper = true;
				i++;
			}
			else if (args[i].equalsIgnoreCase("--repeatcount")) {
				repeatCount = Integer.parseInt(args[i + 1]);
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--noarraysize")) {
				config.setEnableArraySizeTainting(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--arraysize")) {
				config.setEnableArraySizeTainting(true);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--notypetightening")) {
				InfoflowAndroidConfiguration.setUseTypeTightening(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--safemode")) {
				InfoflowAndroidConfiguration.setUseThisChainReduction(false);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--logsourcesandsinks")) {
				config.setLogSourcesAndSinks(true);
				i++;
			}
			else if (args[i].equalsIgnoreCase("--callbackanalyzer")) {
				String algo = args[i+1];
				if (algo.equalsIgnoreCase("DEFAULT"))
					config.setCallbackAnalyzer(CallbackAnalyzer.Default);
				else if (algo.equalsIgnoreCase("FAST"))
					config.setCallbackAnalyzer(CallbackAnalyzer.Fast);
				else {
					System.err.println("Invalid callback analysis algorithm");
					return false;
				}
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--maxthreadnum")){
				config.setMaxThreadNum(Integer.valueOf(args[i+1]));
				i += 2;
			}
			else if (args[i].equalsIgnoreCase("--arraysizetainting")) {
				config.setEnableArraySizeTainting(true);
				i++;
			}
			else
				i++;
		}
		return true;
	}
	
	private static boolean validateAdditionalOptions() {
		if (timeout > 0 && sysTimeout > 0) {
			return false;
		}
		if (!config.getFlowSensitiveAliasing()
				&& config.getCallgraphAlgorithm() != CallgraphAlgorithm.OnDemand
				&& config.getCallgraphAlgorithm() != CallgraphAlgorithm.AutomaticSelection) {
			System.err.println("Flow-insensitive aliasing can only be configured for callgraph "
					+ "algorithms that support this choice.");
			return false;
		}
		return true;
	}
	
	private static void runAnalysisTimeout(final String fileName, final String androidJar) {
		//XIANG
		//Invalid for now.
		//Fix return value if wanna use this method.
		/*
		FutureTask<InfoflowResults> task = new FutureTask<InfoflowResults>(new Callable<InfoflowResults>() {

			@Override
			public InfoflowResults call() throws Exception {
				
				final BufferedWriter wr = new BufferedWriter(new FileWriter("_out_" + new File(fileName).getName() + ".txt"));
				try {
					final long beforeRun = System.nanoTime();
					wr.write("Running data flow analysis...\n");
					final InfoflowResults res = runAnalysis(fileName, androidJar);
					wr.write("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds\n");
					
					wr.flush();
					return res;
				}
				finally {
					if (wr != null)
						wr.close();
				}
			}
			
		});
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(task);
		
		try {
			System.out.println("Running infoflow task...");
			task.get(timeout, TimeUnit.MINUTES);
		} catch (ExecutionException e) {
			System.err.println("Infoflow computation failed: " + e.getMessage());
			e.printStackTrace();
		} catch (TimeoutException e) {
			System.err.println("Infoflow computation timed out: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println("Infoflow computation interrupted: " + e.getMessage());
			e.printStackTrace();
		}
		
		// Make sure to remove leftovers
		executor.shutdown();		
		*/
	}

	private static void runAnalysisSysTimeout(final String fileName, final String androidJar) {
		String classpath = System.getProperty("java.class.path");
		String javaHome = System.getProperty("java.home");
		String executable = "/usr/bin/timeout";
		String[] command = new String[] { executable,
				"-s", "KILL",
				sysTimeout + "m",
				javaHome + "/bin/java",
				"-cp", classpath,
				"soot.jimple.infoflow.android.TestApps.Test",
				fileName,
				androidJar,
				config.getStopAfterFirstFlow() ? "--singleflow" : "--nosingleflow",
				config.getEnableImplicitFlows() ? "--implicit" : "--noimplicit",
				config.getEnableStaticFieldTracking() ? "--static" : "--nostatic", 
				"--aplength", Integer.toString(InfoflowAndroidConfiguration.getAccessPathLength()),
				"--cgalgo", callgraphAlgorithmToString(config.getCallgraphAlgorithm()),
				config.getEnableCallbacks() ? "--callbacks" : "--nocallbacks",
				config.getEnableExceptionTracking() ? "--exceptions" : "--noexceptions",
				"--layoutmode", layoutMatchingModeToString(config.getLayoutMatchingMode()),
				config.getFlowSensitiveAliasing() ? "--aliasflowsens" : "--aliasflowins",
				config.getComputeResultPaths() ? "--paths" : "--nopaths",
				aggressiveTaintWrapper ? "--aggressivetw" : "--nonaggressivetw",
				"--pathalgo", pathAlgorithmToString(config.getPathBuilder()),
				(summaryPath != null && !summaryPath.isEmpty()) ? "--summarypath" : "",
				(summaryPath != null && !summaryPath.isEmpty()) ? summaryPath : "",
				(resultFilePath != null && !resultFilePath.isEmpty()) ? "--saveresults" : "",
				noTaintWrapper ? "--notaintwrapper" : "",
//				"--repeatCount", Integer.toString(repeatCount),
				config.getEnableArraySizeTainting() ? "" : "--noarraysize",
				InfoflowAndroidConfiguration.getUseTypeTightening() ? "" : "--notypetightening",
				InfoflowAndroidConfiguration.getUseThisChainReduction() ? "" : "--safemode",
				config.getLogSourcesAndSinks() ? "--logsourcesandsinks" : "",
				"--callbackanalyzer", callbackAlgorithmToString(config.getCallbackAnalyzer()),
				"--maxthreadnum", Integer.toString(config.getMaxThreadNum()),
				config.getEnableArraySizeTainting() ? "--arraysizetainting" : ""
				};
		System.out.println("Running command: " + executable + " " + Arrays.toString(command));
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectOutput(new File("out_" + new File(fileName).getName() + "_" + repeatCount + ".txt"));
			pb.redirectError(new File("err_" + new File(fileName).getName() + "_" + repeatCount + ".txt"));
			Process proc = pb.start();
			proc.waitFor();
		} catch (IOException ex) {
			System.err.println("Could not execute timeout command: " + ex.getMessage());
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			System.err.println("Process was interrupted: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private static String callgraphAlgorithmToString(CallgraphAlgorithm algorihm) {
		switch (algorihm) {
			case AutomaticSelection:
				return "AUTO";
			case CHA:
				return "CHA";
			case VTA:
				return "VTA";
			case RTA:
				return "RTA";
			case SPARK:
				return "SPARK";
			case GEOM:
				return "GEOM";
			default:
				return "unknown";
		}
	}

	private static String layoutMatchingModeToString(LayoutMatchingMode mode) {
		switch (mode) {
			case NoMatch:
				return "NONE";
			case MatchSensitiveOnly:
				return "PWD";
			case MatchAll:
				return "ALL";
			default:
				return "unknown";
		}
	}
	
	private static String pathAlgorithmToString(PathBuilder pathBuilder) {
		switch (pathBuilder) {
			case ContextSensitive:
				return "CONTEXTSENSITIVE";
			case ContextInsensitive :
				return "CONTEXTINSENSITIVE";
			case ContextInsensitiveSourceFinder :
				return "SOURCESONLY";
			default :
				return "UNKNOWN";
		}
	}
	
	private static String callbackAlgorithmToString(CallbackAnalyzer analyzer) {
		switch (analyzer) {
			case Default:
				return "DEFAULT";
			case Fast:
				return "FAST";
			default :
				return "UNKNOWN";
		}
	}

	private static FlowPathSet runAnalysis(final String fileName, final String androidJar) {
		try {
			final long beforeRun = System.nanoTime();

			final SetupApplication app;
			if (null == ipcManager)
			{
				app = new SetupApplication(androidJar, fileName);
			}
			else
			{
				app = new SetupApplication(androidJar, fileName, ipcManager);
			}
			
			// Set configuration object
			app.setConfig(config);
			if (noTaintWrapper)
				app.setSootConfig(new IInfoflowConfig() {
					
					@Override
					public void setSootOptions(Options options) {
						options.set_include_all(true);
					}
					
				});
			
			final ITaintPropagationWrapper taintWrapper;
			if (noTaintWrapper)
				taintWrapper = null;
			else if (summaryPath != null && !summaryPath.isEmpty()) {
				System.out.println("Using the StubDroid taint wrapper");
				taintWrapper = createLibrarySummaryTW();
				if (taintWrapper == null) {
					System.err.println("Could not initialize StubDroid");
					return null;
				}
			}
			else {
				final EasyTaintWrapper easyTaintWrapper;
				File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
				if (twSourceFile.exists())
					easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
				else {
					twSourceFile = new File("EasyTaintWrapperSource.txt");
					if (twSourceFile.exists())
						easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
					else {
						System.err.println("Taint wrapper definition file not found at "
								+ twSourceFile.getAbsolutePath());
						return null;
					}
				}
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper = easyTaintWrapper;
			}
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
			
			if (DEBUG) {
				app.printEntrypoints();
				app.printSinks();
				app.printSources();
			}
			
			System.out.println("Running data flow analysis...");

			final InfoflowResults res = app.runInfoflow(new MyResultsAvailableHandler());

			InfoflowResultsWithFlowPathSet resFPS = (InfoflowResultsWithFlowPathSet)res;
			
			System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds");
			
			if (config.getLogSourcesAndSinks()) {
				if (!app.getCollectedSources().isEmpty()) {
					System.out.println("Collected sources:");
					for (Stmt s : app.getCollectedSources())
						System.out.println("\t" + s);
				}
				if (!app.getCollectedSinks().isEmpty()) {
					System.out.println("Collected sinks:");
					for (Stmt s : app.getCollectedSinks())
						System.out.println("\t" + s);
				}
			}
			
			return resFPS.getFlowPathSet();
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	//XIANG
	private static InfoflowResults runAnalysisForFlowViewCorrelation(final String fileName, final String androidJar, FlowPathSet fps) {
		try {
			final long beforeRun = System.nanoTime();

			final SetupApplication app;
			if (null == ipcManager)
			{
				app = new SetupApplication(androidJar, fileName);
			}
			else
			{
				app = new SetupApplication(androidJar, fileName, ipcManager);
			}
			//XIANG
			app.setFlowPathSet(fps);
			
			// Set configuration object
			app.setConfig(config);
			if (noTaintWrapper)
				app.setSootConfig(new IInfoflowConfig() {
					
					@Override
					public void setSootOptions(Options options) {
						options.set_include_all(true);
					}
					
				});
			
			final ITaintPropagationWrapper taintWrapper;
			if (noTaintWrapper)
				taintWrapper = null;
			else if (summaryPath != null && !summaryPath.isEmpty()) {
				System.out.println("Using the StubDroid taint wrapper");
				taintWrapper = createLibrarySummaryTW();
				if (taintWrapper == null) {
					System.err.println("Could not initialize StubDroid");
					return null;
				}
			}
			else {
				final EasyTaintWrapper easyTaintWrapper;
				File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
				
				if (twSourceFile.exists())
					easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
				else {
					twSourceFile = new File("EasyTaintWrapperSource.txt");
					if (twSourceFile.exists())
						easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
					else {
						System.err.println("Taint wrapper definition file not found at "
								+ twSourceFile.getAbsolutePath());
						return null;
					}
				}
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper = easyTaintWrapper;
			}
			app.setTaintWrapper(taintWrapper);
			//app.calculateSourcesSinksEntrypoints("SourceAndSinksForFlowViewCorrelation.txt");
			app.calculateSourcesSinksEntrypointsForViewFlowCorrelation("SourceAndSinksForFlowViewCorrelation.txt");
			//app.calculateSourcesSinksEntrypoints("Test.txt");
			app.printEntrypoints();
			app.printSinks();
			app.printSources();
			
			
			System.out.println("Running data flow analysis...");
			final InfoflowResults res = app.runInfoflow(new MyResultsAvailableHandler());
			
			System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds. Correlation TA");
			
			if (config.getLogSourcesAndSinks()) {
				if (!app.getCollectedSources().isEmpty()) {
					System.out.println("Collected sources:");
					for (Stmt s : app.getCollectedSources())
						System.out.println("\t" + s);
				}
				if (!app.getCollectedSinks().isEmpty()) {
					System.out.println("Collected sinks:");
					for (Stmt s : app.getCollectedSinks())
						System.out.println("\t" + s);
				}
			}
			
			return res;
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	
	//TODO: modify return value;
	private static InfoflowResults runAnalysisForConstantPropogation(final String fileName, final String androidJar, FlowPathSet fps, 
			ValueResourceParser valResMgr, boolean onlyDoFast) {
		try {
			final long beforeRun = System.nanoTime();
			SetupApplication app1;
			if (null == ipcManager) app1 = new SetupApplication(androidJar, fileName);
			else app1 = new SetupApplication(androidJar, fileName, ipcManager);
			
			app1.setConfig(config);
			if (noTaintWrapper)
				app1.setSootConfig(new IInfoflowConfig() {
					@Override
					public void setSootOptions(Options options) {
						options.set_include_all(true);
					}	
				});
			final ITaintPropagationWrapper taintWrapper1;
			if (noTaintWrapper) taintWrapper1 = null;
			else if (summaryPath != null && !summaryPath.isEmpty()) {
				System.out.println("Using the StubDroid taint wrapper");
				taintWrapper1 = createLibrarySummaryTW();
				if (taintWrapper1 == null) {
					System.err.println("Could not initialize StubDroid");
					return null;
				}
			}
			else {
				final EasyTaintWrapper easyTaintWrapper;
				File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
				if (twSourceFile.exists())
					easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
				else {
					twSourceFile = new File("EasyTaintWrapperSource.txt");
					if (twSourceFile.exists())
						easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
					else {
						System.err.println("Taint wrapper definition file not found at "
								+ twSourceFile.getAbsolutePath());
						return null;
					}
				}
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper1 = easyTaintWrapper;
			}
			app1.setTaintWrapper(taintWrapper1);
			app1.calculateSourcesSinksEntrypointsForConstantPropogation("Test.txt");
			Set<Stmt> findViewByIdStmts = app1.fastSearchKeyInvokeExprSearch(valResMgr);
			if(findViewByIdStmts==null || findViewByIdStmts.size()==0)
				return null;
			if(onlyDoFast){
				System.err.println("ALERT: exit because onlyDoFast");
				//GraphTool.displayAllMethodGraph();
				return null;
			}
			
			//run data flow analysis 
			//this could be very slow.
			System.out.println("NULIST: start data flow analysis for findViewById");
			final SetupApplication app;
			if (null == ipcManager)
			{
				app = new SetupApplication(androidJar, fileName);
			}
			else
			{
				app = new SetupApplication(androidJar, fileName, ipcManager);
			}
			app.setConfig(config);
			if (noTaintWrapper)
				app.setSootConfig(new IInfoflowConfig() {
					
					@Override
					public void setSootOptions(Options options) {
						options.set_include_all(true);
					}
					
				});
			
			final ITaintPropagationWrapper taintWrapper;
			if (noTaintWrapper)
				taintWrapper = null;
			else if (summaryPath != null && !summaryPath.isEmpty()) {
				System.out.println("Using the StubDroid taint wrapper");
				taintWrapper = createLibrarySummaryTW();
				if (taintWrapper == null) {
					System.err.println("Could not initialize StubDroid");
					return null;
				}
			}
			else {
				final EasyTaintWrapper easyTaintWrapper;
				//TODO: what's this.
				File twSourceFile = new File("../soot-infoflow/EasyTaintWrapperSource.txt");
				
				if (twSourceFile.exists())
					easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
				else {
					twSourceFile = new File("EasyTaintWrapperSource.txt");
					if (twSourceFile.exists())
						easyTaintWrapper = new EasyTaintWrapper(twSourceFile);
					else {
						System.err.println("Taint wrapper definition file not found at "
								+ twSourceFile.getAbsolutePath());
						return null;
					}
				}
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper = easyTaintWrapper;
			}
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypointsForConstantPropogation("Test.txt");
			app.printEntrypoints();
			app.printSinks();
			app.printSources();
			
			System.out.println("Running data flow analysis...");
			ConstantPropogationResultsHanlder rh = new ConstantPropogationResultsHanlder();
			final InfoflowResults res = app.runInfoflowForConstantPropogation(rh, valResMgr);
			
			System.out.println("Analysis has run for " + (System.nanoTime() - beforeRun) / 1E9 + " seconds. Correlation TA");
			Map<Stmt,Set> constantAnalysisMap = rh.getRS();
			for(Stmt sink : constantAnalysisMap.keySet()){
				Set vals = constantAnalysisMap.get(sink);
				for(Object obj : vals)
					System.out.println("FINDID: "+sink+" ==> "+obj);
			}
			
			
			if (config.getLogSourcesAndSinks()) {
				if (!app.getCollectedSources().isEmpty()) {
					System.out.println("Collected sources:");
					for (Stmt s : app.getCollectedSources())
						System.out.println("\t" + s);
				}
				if (!app.getCollectedSinks().isEmpty()) {
					System.out.println("Collected sinks:");
					for (Stmt s : app.getCollectedSinks())
						System.out.println("\t" + s);
				}
			}
			
			//Test
			ARSCFileParser resParser = new ARSCFileParser();
			resParser.parse(fileName);
			ProcessManifest processMan = new ProcessManifest(fileName);
			//this.appPackageName = processMan.getPackageName();
			ParameterSearch ps = new ParameterSearch(valResMgr,  resParser.getPackages(),  processMan.getPackageName(),null);
			ps.searchMethodCall("findViewById", null);
			
			return res;
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates the taint wrapper for using library summaries
	 * @return The taint wrapper for using library summaries
	 * @throws IOException Thrown if one of the required files could not be read
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ITaintPropagationWrapper createLibrarySummaryTW()
			throws IOException {
		try {
			Class clzLazySummary = Class.forName("soot.jimple.infoflow.methodSummary.data.provider.LazySummaryProvider");
			Class itfLazySummary = Class.forName("soot.jimple.infoflow.methodSummary.data.provider.IMethodSummaryProvider");
			
			Object lazySummary = clzLazySummary.getConstructor(File.class).newInstance(new File(summaryPath));
			
			ITaintPropagationWrapper summaryWrapper = (ITaintPropagationWrapper) Class.forName
					("soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper").getConstructor
					(itfLazySummary).newInstance(lazySummary);
			
			ITaintPropagationWrapper systemClassWrapper = new ITaintPropagationWrapper() {
				
				private ITaintPropagationWrapper wrapper = new EasyTaintWrapper("EasyTaintWrapperSource.txt");
				
				private boolean isSystemClass(Stmt stmt) {
					if (stmt.containsInvokeExpr())
						return SystemClassHandler.isClassInSystemPackage(
								stmt.getInvokeExpr().getMethod().getDeclaringClass().getName());
					return false;
				}
				
				@Override
				public boolean supportsCallee(Stmt callSite) {
					return isSystemClass(callSite) && wrapper.supportsCallee(callSite);
				}
				
				@Override
				public boolean supportsCallee(SootMethod method) {
					return SystemClassHandler.isClassInSystemPackage(method.getDeclaringClass().getName())
							&& wrapper.supportsCallee(method);
				}
				
				@Override
				public boolean isExclusive(Stmt stmt, Abstraction taintedPath) {
					return isSystemClass(stmt) && wrapper.isExclusive(stmt, taintedPath);
				}
				
				@Override
				public void initialize(InfoflowManager manager) {
					wrapper.initialize(manager);
				}
				
				@Override
				public int getWrapperMisses() {
					return 0;
				}
				
				@Override
				public int getWrapperHits() {
					return 0;
				}
				
				@Override
				public Set<Abstraction> getTaintsForMethod(Stmt stmt, Abstraction d1,
						Abstraction taintedPath) {
					if (!isSystemClass(stmt))
						return null;
					return wrapper.getTaintsForMethod(stmt, d1, taintedPath);
				}
				
				@Override
				public Set<Abstraction> getAliasesForMethod(Stmt stmt, Abstraction d1,
						Abstraction taintedPath) {
					if (!isSystemClass(stmt))
						return null;
					return wrapper.getAliasesForMethod(stmt, d1, taintedPath);
				}
				
			};
			
			Method setFallbackMethod = summaryWrapper.getClass().getMethod("setFallbackTaintWrapper",
					ITaintPropagationWrapper.class);
			setFallbackMethod.invoke(summaryWrapper, systemClassWrapper);
			
			return summaryWrapper;
		}
		catch (ClassNotFoundException | NoSuchMethodException ex) {
			System.err.println("Could not find library summary classes: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		catch (InvocationTargetException ex) {
			System.err.println("Could not initialize library summaries: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
		catch (IllegalAccessException | InstantiationException ex) {
			System.err.println("Internal error in library summary initialization: " + ex.getMessage());
			ex.printStackTrace();
			return null;
		}
	}

	private static void printUsage() {
		System.out.println("FlowDroid (c) Secure Software Engineering Group @ EC SPRIDE");
		System.out.println();
		System.out.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");
		System.out.println("Optional further parameters:");
		System.out.println("\t--TIMEOUT n Time out after n seconds");
		System.out.println("\t--SYSTIMEOUT n Hard time out (kill process) after n seconds, Unix only");
		System.out.println("\t--SINGLEFLOW Stop after finding first leak");
		System.out.println("\t--IMPLICIT Enable implicit flows");
		System.out.println("\t--NOSTATIC Disable static field tracking");
		System.out.println("\t--NOEXCEPTIONS Disable exception tracking");
		System.out.println("\t--APLENGTH n Set access path length to n");
		System.out.println("\t--CGALGO x Use callgraph algorithm x");
		System.out.println("\t--NOCALLBACKS Disable callback analysis");
		System.out.println("\t--LAYOUTMODE x Set UI control analysis mode to x");
		System.out.println("\t--ALIASFLOWINS Use a flow insensitive alias search");
		System.out.println("\t--NOPATHS Do not compute result paths");
		System.out.println("\t--AGGRESSIVETW Use taint wrapper in aggressive mode");
		System.out.println("\t--PATHALGO Use path reconstruction algorithm x");
		System.out.println("\t--LIBSUMTW Use library summary taint wrapper");
		System.out.println("\t--SUMMARYPATH Path to library summaries");
		System.out.println("\t--SYSFLOWS Also analyze classes in system packages");
		System.out.println("\t--NOTAINTWRAPPER Disables the use of taint wrappers");
		System.out.println("\t--NOTYPETIGHTENING Disables the use of taint wrappers");
		System.out.println("\t--LOGSOURCESANDSINKS Print out concrete source/sink instances");
		System.out.println("\t--CALLBACKANALYZER x Uses callback analysis algorithm x");
		System.out.println("\t--MAXTHREADNUM x Sets the maximum number of threads to be used by the analysis to x");
		System.out.println();
		System.out.println("Supported callgraph algorithms: AUTO, CHA, RTA, VTA, SPARK, GEOM");
		System.out.println("Supported layout mode algorithms: NONE, PWD, ALL");
		System.out.println("Supported path algorithms: CONTEXTSENSITIVE, CONTEXTINSENSITIVE, SOURCESONLY");
		System.out.println("Supported callback algorithms: DEFAULT, FAST");
	}

}

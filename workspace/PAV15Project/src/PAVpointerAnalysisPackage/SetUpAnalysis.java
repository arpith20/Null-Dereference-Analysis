package PAVpointerAnalysisPackage;

/**
 * @author Arpith K, Sridhar G
 * Program Analysis and Verification, 2015
 *
 */

// Do NOT import the pointer analysis package
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.viz.viewer.IrAndSourceViewer;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;

public class SetUpAnalysis {

	private String classpath;
	private String mainClass;
	private String analysisClass;
	private String analysisMethod;

	// CGNODE of the method which we are analyzing
	private CGNode target;

	// List of CGNODEs of all the methods which are present in the class
	private ArrayList<CGNode> globalMethods = new ArrayList<CGNode>();

	// HashMap which maps the names of the methods to its corresponding CGNodes
	private HashMap<String, CGNode> hashGlobalMethods = new HashMap<String, CGNode>();

	// Main DATA class to store the analysis results
	private Data d;

	// List of all the program points to analyze. End analysis when this list
	// becomes empty
	private ArrayList<String> workingList = new ArrayList<String>();

	// HashMap which contains the list of all program points corresponding to
	// each method in the program
	private HashMap<String, ArrayList<String>> programPoints = new HashMap<String, ArrayList<String>>();

	class callSiteData {
		public String pPoint;
		public int varNum;
		public HashMap<Integer, Integer> columnsOpened;

		public callSiteData(String succPPoint, int returnVar) {
			pPoint = succPPoint;
			varNum = returnVar;
			columnsOpened = new HashMap<Integer, Integer>();
			return;
		}

		public void display() {
			System.out.println("PPoint: " + pPoint);
			System.out.println("Variable: " + varNum);
			System.out.println("HashMap:\n" + columnsOpened);
			return;
		}
	};

	HashMap<String, ArrayList<callSiteData>> mapToCallSiteData = new HashMap<String, ArrayList<callSiteData>>();

	// START: NO CHANGE REGION
	private AnalysisScope scope; // scope defines the set of files to be
									// analyzed
	private ClassHierarchy ch; // Generate the class hierarchy for the scope
	private Iterable<Entrypoint> entrypoints; // In the call graph, these are
												// the entrypoint nodes
	private AnalysisOptions options; // Specify options for the call graph
										// builder
	private CallGraphBuilder builder; // Builder object for the call graph
	private CallGraph cg; // Call graph for the program

	public SetUpAnalysis(String classpath, String mainClass, String analysisClass, String analysisMethod) {
		this.classpath = classpath;
		this.mainClass = mainClass;
		this.analysisClass = analysisClass;
		this.analysisMethod = analysisMethod;

	}

	/**
	 * Defines the scope of analysis: identifies the classes for analysis
	 * 
	 * @throws Exception
	 */
	public void buildScope() throws Exception {
		FileProvider f = new FileProvider();
		scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath,
				f.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
	}

	/**
	 * Builds the hierarchy among the classes to be analyzed: if B extends A,
	 * then A is a superclass of B, etc
	 * 
	 * @throws Exception
	 */
	public void buildClassHierarchy() throws Exception {
		ch = ClassHierarchy.make(scope);
	}

	/**
	 * The nodes of a call graph are methods. This method defines the
	 * "entry points" in the call graph. Note: The entry point may not
	 * necessarily be the main method.
	 */
	public void buildEntryPoints() {
		entrypoints = Util.makeMainEntrypoints(scope, ch, mainClass);
	}

	/**
	 * Options to build the required call graph.
	 */
	public void setUpCallGraphConstruction() {
		options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
		builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), ch, scope);
	}

	/**
	 * Build the call graph.
	 * 
	 * @throws Exception
	 */
	public void generateCallGraph() throws Exception {
		cg = builder.makeCallGraph(options, null);
	}

	// END: NO CHANGE REGION

	// Method to initialize a few things before the analysis starts
	public void init() {
		if (!setTarget()) {
			System.out.println("Set Target Failed");
			System.exit(-1);
		}
		setGlobalMethods();

		d = new Data();
	}

	// Method to store the CGNODE of the method which we are analyzing in the
	// data member TARGET
	public boolean setTarget() {
		Iterator<CGNode> nodes = cg.iterator();
		CGNode target = null;
		while (nodes.hasNext()) {
			CGNode node = nodes.next();
			String nodeInfo = node.toString();

			// Check if the signature of the method contains the string. This is
			// sufficient to say that it is the method we are searching for
			if (nodeInfo.contains(analysisClass) && nodeInfo.contains(analysisMethod)) {
				target = node;
				break;
			}
		}
		// System.out.println(target);
		if (target != null) {
			this.target = target;
			return true;
		} else {
			this.target = null;
			return false;
		}
	}

	// Method to store all the methods present in the class in the data member
	// GLOBALMETHODS and also add a hash mapping from the shorthand name of the
	// method to their CGNode
	public void setGlobalMethods() {

		// Iterate over all the methods present in the CALLGRAPH and check if
		// the name of the method has APPLICATION as the third word in it.
		// This can be done better! TODO
		Iterator<CGNode> nodes = cg.iterator();
		while (nodes.hasNext()) {
			CGNode node = nodes.next();
			String nodeInfo = node.toString();
			if (nodeInfo.contains("Application")) {
				String[] s = nodeInfo.split("[ ]");
				if (s[2].contains("Application")) {

					// Add the CGNode to the GLOBALMETHODS
					// Do not add the Constructor i.e init to the array list or
					// to the hashMap
					if (!nodeInfo.contains("<init>()V")) {
						globalMethods.add(node);

						// Get shorthand name of the method and add it to the
						// hashMap
						String methodName = node.getMethod().toString().split("[,]")[2].split("[(]")[0].substring(1);
						String className = node.getMethod().toString().split("[,]")[1].substring(1);
						hashGlobalMethods.put(className + methodName, node);
					}
				}
			}
		}
		// System.out.println("AnalysisClass: " + analysisClass);
		// System.out.println("HashGlobal: \n" + hashGlobalMethods + "\n");
		return;
	}

	// Prints the program point names of the methods we are analyzing
	public void getProgramPoints() {

		// Get the list of all the methods reachable directly or transitively
		// from the method which we are analyzing
		ArrayList<CGNode> methods = getTransitiveCallSites();
		for (CGNode cgnode : methods) {

			// Get just the name of the method from the method signature. This
			// follows the structure of the WALA method signature
			String methodName = cgnode.getMethod().toString().split("[,]")[2].split("[(]")[0].substring(1);

			// Iterate over the basicBlocks present in the CFG
			SSACFG cfg = cgnode.getIR().getControlFlowGraph();
			Iterator<ISSABasicBlock> ibb = cfg.iterator();
			while (ibb.hasNext()) {
				ISSABasicBlock bb = ibb.next();

				// Get all the normal successors of the current basicBlock. This
				// will exclude the exception edges.
				Collection<ISSABasicBlock> succSet = cfg.getNormalSuccessors(bb);
				for (ISSABasicBlock succ : succSet) {
					// The unique number assigned to each basicBlock and method
					// name is used as a program point.
					// Example: main.1.2 foo.9.10 bar.10.23
					String pp = methodName + "." + bb.getNumber() + "." + succ.getNumber();

					// Add the program point to the WORKINGLIST
					// TODO
					// if (methodName.equals("startTest")) {
					// workingList.add(pp);
					// d.addProgramPoint(pp);
					// }
					workingList.add(pp);
					d.addProgramPoint(pp);

					// Add the program point to the hash map containing the set
					// of all program points present in a particular method
					//
					// Check if the method is already present in the
					// programPoints hashMap
					if (!programPoints.containsKey(methodName)) {
						// List of programPoints doesn't exist. Create a new one
						ArrayList<String> list = new ArrayList<String>();
						list.add(pp);
						programPoints.put(methodName, list);
					} else {
						// Add the programPoint to the existing arrayList
						ArrayList<String> list = programPoints.get(methodName);
						list.add(pp);
					}
				}
			}
		}
		// System.out.println("Program Points: " + programPoints + "\n");
		return;
	}

	// Returns the list of all the methods which are called directly or
	// transitively by the TARGET method
	public ArrayList<CGNode> getTransitiveCallSites() {
		ArrayList<CGNode> callSites = new ArrayList<CGNode>();
		ArrayList<CGNode> workingList = new ArrayList<CGNode>();

		// Add the method which we are currently analyzing to working list.
		// We want transitive callSites from the method which we are analyzing
		callSites.add(target);
		workingList.add(target);
		while (!workingList.isEmpty()) {
			CGNode cur = workingList.get(0);

			// Get the direct callSites from CUR
			ArrayList<CGNode> directSites = getDirectCallSites(cur);
			if (!directSites.isEmpty()) {
				for (CGNode add : directSites) {

					// Add the direct callSites to the workingList and callSites
					// if NOT already present
					if (!callSites.contains(add))
						callSites.add(add);
					if (!workingList.contains(add))
						workingList.add(add);
				}
			}
			// Reduce the size of the workingList
			workingList.remove(cur);
		}
		// System.out.println("Transitive sites are: " + callSites);
		return callSites;
	}

	// Returns the list of all the methods which are called directly by the ROOT
	// method
	public ArrayList<CGNode> getDirectCallSites(CGNode root) {
		ArrayList<CGNode> callSites = new ArrayList<CGNode>();

		String rootSignature = root.getMethod().getSignature();
		String[] rootSigString = rootSignature.split("[.]");
		String rootMethodName = rootSigString[2].split("[(]")[0];
		String rootClassName = "L" + rootSigString[0] + "/" + rootSigString[1];

		// Iterate over all the call sites in ROOT
		Iterator<CallSiteReference> icsr = root.getIR().iterateCallSites();
		while (icsr.hasNext()) {
			CallSiteReference csr = icsr.next();

			// invokeSpecial is for Constructors
			// invokeVirtual is for method invocation including library methods
			// invokeStatic is for invoking static methods
			// Considering only invokeVirtual and invokeStatic
			// TODO
			if (!csr.isSpecial()) {

				// Check if the Target of this is present in the CALL GRAPH
				// If not present, DO NOT ADD
				String signature = csr.getDeclaredTarget().getSignature();
				String[] sigString = signature.split("[.]");
				String targetMethodName = sigString[2].split("[(]")[0];
				String targetClassName = "L" + sigString[0] + "/" + sigString[1];
				if (hashGlobalMethods.containsKey(targetClassName + targetMethodName)) {
					Set<CGNode> nodes = cg.getPossibleTargets(root, csr);
					if (nodes.size() == 1) {
						Iterator<CGNode> i = nodes.iterator();
						CGNode temp = i.next();
						callSites.add(temp);

						// Add this callSite to the CallSiteData class
						// Check if targetMethodName already exists
						ArrayList<callSiteData> siteData = mapToCallSiteData.get(targetMethodName);
						if (siteData == null) {
							siteData = new ArrayList<callSiteData>();
							mapToCallSiteData.put(targetMethodName, siteData);
						}
					} else {
						System.out.println("size of possibletarget nodes not equal to 1");
					}
				} else {
					// System.out.println(className+methodName +"not present");
				}
			}
		}
		// System.out.println("CallSites for " + root + ":\n" + callSites);
		return callSites;
	}

	// Method to set the initial value for the analysis
	public void setD0() {
		// Get the name of the method we are analyzing
		String methodName = target.getMethod().toString().split("[,]")[2].split("[(]")[0].substring(1);

		// Get the initial program point
		String pp = methodName + ".0.1";

		// Open column 0 in all programPoints in target method
		openColumnsDriver(methodName, 0);

		// Add D0 to DATA
		// TODO. REMOVE THIS
		d.add(pp, 0, "", "");

		return;
	}

	public void openColumnsDriver(String methodName, int column) {
		ArrayList<String> pPoints = programPoints.get(methodName);
		for (String pp : pPoints) {
			d.openColumn(pp, column);
		}
	}

	// Main kildall algorithm. This will do the analysis and will return once
	// the WORKINGLIST is empty
	public void kildall() {
		// System.out.println(programPoints);
		// System.out.println(target.getIR());
		// System.out.println(" WOrking list is:" + workingList + "\n");
		// System.out.println(globalMethods);
		while (!workingList.isEmpty()) {
			String curPP = workingList.get(0);

			System.out.println("PP:" + curPP);

			// Check if all the columns in the program point are unmarked.
			// If true, continue
			if (d.checkAllColumnsUnmarked(curPP)) {
				workingList.remove(0);
				continue;
			}

			// System.out.println("kildall");
			// Call the transfer function driver
			transferFunctionDriver(curPP);

			workingList.remove(0);
		}
		// System.out.println("\n\n");
		// d.display();

		for (Map.Entry<String, ArrayList<String>> entry : programPoints.entrySet()) {
			String method = entry.getKey();
			ArrayList<String> iterate = entry.getValue();
			System.out.println("\n\n\n" + method + "");
			for (int i = 0; i < method.length(); i++)
				System.out.print("=");
			System.out.println("\n");
			for (String pPoint : iterate)
				d.displayProgramPoint(pPoint);
		}

		// System.out.println(mapToCallSiteData);
	}

	public void transferFunctionDriver(String pPoint) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int prevBBNum = Integer.parseInt(pPoint.split("[.]")[1]);
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock prevBB = cfg.getBasicBlock(prevBBNum);
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

		// Get the list of all the successor basicBlocks
		Collection<ISSABasicBlock> succBB = cfg.getNormalSuccessors(srcBB);

		// Get the markings present at the current program point
		HashMap<Integer, Boolean> markings = d.getColumnMarkings(pPoint);

		// System.out.println("TrasferFUnction driver");
		// Iterate ONLY over the set of columns which are marked
		for (Map.Entry<Integer, Boolean> entry : markings.entrySet()) {
			// System.out.println("TrasferFUnction Loop");
			Integer column = entry.getKey();
			Boolean mark = entry.getValue();

			// Unmarked
			if (mark == false)
				continue;

			// Un-mark the current column
			d.unmark(pPoint, column);

			// Create a new hashMap and initialize it to the value present at
			// the current program point. This value will be propagated to the
			// successors
			HashMap<String, ArrayList<String>> propagatedValue = new HashMap<String, ArrayList<String>>();
			propagatedValue = d.retrieve(pPoint, column);

			System.out.println("Before entring blocks");
			System.out.println(propagatedValue);
			System.out.println("\n");
			// Check if the value is BOT. If so, propagate BOT and continue
			if (d.isBOT(pPoint, column)) {
				// System.out.println("In BOT");
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					boolean changed = false;
					changed = d.propagate(succPP, column, propagatedValue);
					if (changed)
						workingList.add(succPP);
					// System.out.println(succPP + ":");
					// d.displayProgramPoint(succPP, column);
				}
				continue;
			}

			// Check if there are no instructions in the basic block. If so,
			// then propagate the same value to the successors
			if (srcBB.getAllInstructions().isEmpty()) {
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					boolean changed = false;
					changed = d.propagate(succPP, column, propagatedValue);
					if (changed)
						workingList.add(succPP);
				}
				continue;
			}

			// Apply transfer function to the data present in COLUMN for the
			// instructions in the basicBlock
			// The output will be a new HashMap<String,ArrayList<String>>. This
			// value will be propagated to the successors
			boolean propagate = true;
			Iterator<SSAInstruction> iSSA = srcBB.iterator();
			while (iSSA.hasNext()) {
				SSAInstruction inst = iSSA.next();

				if (inst instanceof SSANewInstruction) {
					newTransferFunction(pPoint, column, (SSANewInstruction) inst, propagatedValue);
					propagate = true;
				} else if (inst instanceof SSAPutInstruction) {
					putTransferFunction(pPoint, column, (SSAPutInstruction) inst, propagatedValue);
					propagate = true;
				} else if (inst instanceof SSAGetInstruction) {
					getTransferFunction(pPoint, column, (SSAGetInstruction) inst, propagatedValue);
					propagate = true;
				} else if (inst instanceof SSAInvokeInstruction) {

					// System.out.println("In Invoke instruction");
					if (((SSAInvokeInstruction) inst).isSpecial()) {
						propagate = true;
						continue;
					}
					// Check if library methods are being called
					CallSiteReference csr = ((SSAInvokeInstruction) inst).getCallSite();
					String signature = csr.getDeclaredTarget().getSignature();
					String[] sigString = signature.split("[.]");
					String targetMethodName = sigString[2].split("[(]")[0];
					String className = "L" + sigString[0] + "/" + sigString[1];
					if (!hashGlobalMethods.containsKey(className + targetMethodName)) {
						// System.out.println("LIBRARY");
						propagate = true;
						continue;
					}
					ISSABasicBlock succ = succBB.iterator().next();
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					callTransferFunction(pPoint, column, (SSAInvokeInstruction) inst, propagatedValue, targetMethodName,
							succPP);
					propagate = true;
					continue;
				} else if (inst instanceof SSAPhiInstruction) {
					phiTransferFunction(pPoint, column, (SSAPhiInstruction) inst, propagatedValue);
					propagate = true;
				} else if (inst instanceof SSAReturnInstruction) {
					returnTransferFunction(pPoint, column, (SSAReturnInstruction) inst, propagatedValue);
					propagate = true;
					break;
				} else if (inst instanceof SSAConditionalBranchInstruction) {
					branchTransferFunction(pPoint, column, (SSAConditionalBranchInstruction) inst, propagatedValue);
					propagate = false;
					break;
				}
			}
			if (propagate) {
				// Iterate over the successor basicBlocks to JOIN the
				// propagatedValue
				boolean changed = false;
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					changed = d.propagate(succPP, column, propagatedValue);
					if (changed)
						workingList.add(succPP);
				}
			}
		}
		return;
	}

	public void newTransferFunction(String pPoint, int column, SSANewInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		System.out.println("Inside New");
		System.out.println(inst);
		// System.out.println("Inside new: \n" + target + "\n" + inst);

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int prevBBNum = Integer.parseInt(pPoint.split("[.]")[1]);
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

		// System.out.println(node);
		String varNum = Integer.toString(inst.getDef());

		String allocationName = methodName + ".new" + inst.iindex;

		// Get the mapping for this variable in the map. If mapping not present,
		// create a mapping
		ArrayList<String> pointsTo = propagatedValue.get(varNum);
		if (pointsTo == null) {
			pointsTo = new ArrayList<String>();
			propagatedValue.put(varNum, pointsTo);
		}

		// DO a STRONG UPDATE to the points to set of the current variable
		pointsTo.add(allocationName);

		// Get the list of all the successor basicBlocks
		Collection<ISSABasicBlock> succBB = cfg.getNormalSuccessors(srcBB);

		System.out.println("After new");
		System.out.println(propagatedValue);
		return;
	}

	public void putTransferFunction(String pPoint, int column, SSAPutInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {
		// System.out.println("=====================");
		// System.out.println(inst.toString());

		String varRIGHT = Integer.toString(inst.getVal()); // xyz->{||THIS||}
		String varLEFT = Integer.toString(inst.getUse(0)); // ||THIS||->{xyz}

		// null constants are added here
		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);
		if (node.getIR().getSymbolTable().isNullConstant(inst.getVal())) {
			propagatedValue.put(Integer.toString(inst.getVal()), new ArrayList<String>(Arrays.asList("null")));
		}

		// xyz.THIS = abc
		// some sorcery to get the data member
		String dataMember = (inst.toString().split("[,]"))[2].substring(1, (inst.toString().split("[,]"))[2].length());

		ArrayList<String> pointsToLEFT = propagatedValue.get(varLEFT);
		ArrayList<String> pointsToRIGHT = new ArrayList<String>(propagatedValue.get(varRIGHT));
		if (pointsToLEFT != null && pointsToRIGHT != null) {
			for (String s : pointsToLEFT) {
				// System.out.print(s + "."+dataMember+"->");
				String point = s + "." + dataMember;

				// The following performs weak update
				if (propagatedValue.get(point) != null) {
					ArrayList<String> old_pointsto = new ArrayList<String>(propagatedValue.get(point));
					for (String s2 : old_pointsto) {
						pointsToRIGHT.add(s2);
					}
				}

				propagatedValue.put(point, pointsToRIGHT);
				// for (String s2 : pointsToRIGHT) {
				// //System.out.print(s2);
				// }
				// System.out.println("");
			}
		}

		// System.out.println("=====================");
	}

	public void getTransferFunction(String pPoint, int column, SSAGetInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {
		// System.out.println("=====================");
		// System.out.println(inst.toString());

		// null constants are added here
		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);
		if (node.getIR().getSymbolTable().isNullConstant(inst.getUse(0))) {
			propagatedValue.put(Integer.toString(inst.getUse(0)), new ArrayList<String>(Arrays.asList("null")));
		}

		// gets the data member
		String dataMember = (inst.toString().split("[,]"))[2].substring(1, (inst.toString().split("[,]"))[2].length());

		String varRIGHT = Integer.toString(inst.getUse(0)); // xyz->{||THIS||}
		String varLEFT = Integer.toString(inst.getDef()); // ||THIS||->{xyz}

		ArrayList<String> pointsToRIGHT = propagatedValue.get(varRIGHT);
		ArrayList<String> pointsToFinal = new ArrayList<String>();
		if (pointsToRIGHT != null) {
			for (String s : pointsToRIGHT) {
				String point = s + "." + dataMember;

				if (propagatedValue.get(point) != null) {
					ArrayList<String> point_pointsto = propagatedValue.get(point);
					for (String s2 : point_pointsto) {
						pointsToFinal.add(s2);
					}
				}
			}
			propagatedValue.put(varLEFT, pointsToFinal);
		}

		// System.out.println(varLEFT + " " + varRIGHT);
		// System.out.println("=====================");
	}

	public void callTransferFunction(String pPoint, Integer column, SSAInvokeInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue, String targetMethodName, String succPPoint) {

		// System.out.println("inside ");
		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);
		// System.out.println(inst.getNumberOfReturnValues());

		// // Get the variable numbers
		// int var1 = inst.getUse(0);
		// int var2 = inst.getUse(1);
		// String var1Str = Integer.toString(var1);
		// String var2Str = Integer.toString(var2);

		// Iterate over all the variables to check if NULL
		int numOfUses = inst.getNumberOfUses();
		for (int i = 0; i < numOfUses; i++) {
			int varNum = inst.getUse(i);
			String varStr = Integer.toString(varNum);

			if (node.getIR().getSymbolTable().isNullConstant(varNum)) {
				// Add this NULL Constant into the propagated value
				propagatedValue.put(varStr, new ArrayList<String>(Arrays.asList("null")));
			}
		}

		HashMap<String, ArrayList<String>> callSiteValue = new HashMap<String, ArrayList<String>>();

		// System.out.println("PointsTo:\n" + propagatedValue);
		int start;
		if (inst.isStatic()) {
			// Static, variables will be set from 0
			start = 0;
		} else
			start = 1;

		for (int i = start; i < inst.getNumberOfParameters(); i++) {
			int var = inst.getUse(i);
			String varStr = Integer.toString(var);
			ArrayList<String> pointsTo = propagatedValue.get(varStr);

			if (pointsTo == null)
				continue;

			ArrayList<String> callSitePointsTo = new ArrayList<String>();
			for (String point : pointsTo)
				callSitePointsTo.add(point);

			callSiteValue.put(Integer.toString(i + 1), callSitePointsTo);
		}
		// System.out.println("CallsiteValue:\n" + callSiteValue);

		// finds and adds the symbolic objects to callSiteValue
		for (int i = start; i < inst.getNumberOfParameters(); i++) {
			int var = inst.getUse(i);
			String varStr = Integer.toString(var);
			ArrayList<String> pointsTo = propagatedValue.get(varStr);

			if (pointsTo == null)
				continue;

			for (String point : pointsTo) {
				for (Map.Entry<String, ArrayList<String>> entry : propagatedValue.entrySet()) {
					String key = entry.getKey();
					if (entry.getValue() == null)
						continue;
					ArrayList<String> value = new ArrayList<String>(entry.getValue());
					if (key.contains(point)) {
						callSiteValue.put(key, value);
					}
				}
			}
		}

		String targetPP = targetMethodName + ".0.1";

		// Check if propagatedValue already exists in the TARGETMETHOD
		int newCol = d.columnMapExists(targetPP, callSiteValue);
		if (newCol == -1) {
			// Does not exist. Get the new column number
			newCol = d.getNewColumnNum(targetPP);
			openColumnsDriver(targetMethodName, newCol);
			d.copyEntireMap(targetPP, newCol, callSiteValue);
		}

		if (inst.getNumberOfReturnValues() == 0)
			return;
		else if (inst.getNumberOfReturnValues() > 1)
			throw new NullPointerException("CallStatement returned more than 1 value:\n" + inst + "\n");

		int returnVar = inst.getReturnValue(0);

		// Update the callSiteData to correspond to this new values
		ArrayList<callSiteData> listCallSites = mapToCallSiteData.get(targetMethodName);
		callSiteData thisCallSite = null;
		for (callSiteData iterateCallData : listCallSites) {
			if (iterateCallData.pPoint.equals(succPPoint)) {
				thisCallSite = iterateCallData;
				break;
			}
		}
		if (thisCallSite == null) {
			// System.out.println("inside NULL");
			// No entry for this callSite. Add a new one
			thisCallSite = new callSiteData(succPPoint, returnVar);
			thisCallSite.pPoint = new String(succPPoint);
			thisCallSite.varNum = returnVar;
			thisCallSite.columnsOpened = new HashMap<Integer, Integer>();
			thisCallSite.columnsOpened.put(newCol, column);
			listCallSites.add(thisCallSite);
			// System.out.println("Disp");
			// thisCallSite.display();
		} else {
			Integer columnMapped = thisCallSite.columnsOpened.get(newCol);
			if (columnMapped == null)
				thisCallSite.columnsOpened.put(newCol, column);
			else if (columnMapped != column)
				throw new NullPointerException("CallData with column mapping not correct:\n" + inst + "\n");

		}
		// System.out.println("CallSiteData after CallInstructionis:");
		// mapToCallSiteData.get(targetMethodName).get(0).display();
		// System.out.println("ThiCallSiteData:\n");
		// System.out.println("Disp");
		// thisCallSite.display();
		// d.display();
		return;
	}

	public void phiTransferFunction(String pPoint, Integer column, SSAPhiInstruction inst,
			HashMap<String, ArrayList<String>> toPropagate) {

		System.out.println("Before PHI:");
		// System.out.println(toPropagate);
		// System.out.println("\n");

		String methodName = pPoint.split("[.]")[0];
		Integer currentBlockNumber = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();

		// Iterate over all the variables to check if NULL
		int numOfUses = inst.getNumberOfUses();
		for (int i = 0; i < numOfUses; i++) {
			int varNum = inst.getUse(i);
			String varStr = Integer.toString(varNum);

			if (node.getIR().getSymbolTable().isNullConstant(varNum)) {
				// Add this NULL Constant into the propagated value
				if (toPropagate.containsKey(varStr) == false)
					toPropagate.put(varStr, new ArrayList<String>(Arrays.asList("null")));
			}
		}

		ISSABasicBlock bb = cfg.getBasicBlock(currentBlockNumber);

		Iterator<ISSABasicBlock> predNodes = cfg.getPredNodes(bb);

		// Collection<ISSABasicBlock> succNodes = cfg.getNormalSuccessors(bb);
		// if (succNodes.size() > 1)
		// throw new NullPointerException("The number of successors is assumed
		// to be one");

		ArrayList<Integer> predBBNumbers = new ArrayList<Integer>();
		while (predNodes.hasNext()) {
			ISSABasicBlock pred_bb = predNodes.next();
			predBBNumbers.add(pred_bb.getNumber());
		}
		// System.out.println("PredNodes are:");
		// System.out.println(predBBNumbers);

		int var_lhs = inst.getDef();
		ArrayList<String> al_var_lhs = toPropagate.get(Integer.toString(var_lhs));
		for (int i = 0; i < inst.getNumberOfUses(); i++) {
			String pp_pred = "";

			int var_rhs = inst.getUse(i);
			pp_pred = methodName + "." + predBBNumbers.get(i) + "." + currentBlockNumber;

			// System.out.println("Predesessor: " + pp_pred);
			ArrayList<String> valuesInVar_rhs;
			if (pp_pred.equals(pPoint))
				valuesInVar_rhs = toPropagate.get(Integer.toString(var_rhs));
			else
				valuesInVar_rhs = d.retrieve(pp_pred, column, Integer.toString(var_rhs));

			if (node.getIR().getSymbolTable().isNullConstant(var_rhs)) {
				// Add this NULL Constant into the propagated value
				// if (toPropagate.containsKey(varStr) == false)
				if (valuesInVar_rhs == null)
					valuesInVar_rhs = new ArrayList<String>();
				if (valuesInVar_rhs.contains("null") == false)
					valuesInVar_rhs.add("null");
			}

			if (valuesInVar_rhs == null)
				continue;

			// System.out.println("From predessor: " + pp_pred);
			// System.out.println(valuesInVar_rhs);

			if (valuesInVar_rhs != null) {
				if (al_var_lhs == null) {
					// System.out.println(var_lhs + " not present");
					al_var_lhs = new ArrayList<String>();
					// for (String value : temp)
					// // if ( )
					// temp.addAll(valuesInVar_rhs);
					toPropagate.put(Integer.toString(var_lhs), al_var_lhs);
				}
				for (String value : valuesInVar_rhs) {
					if (al_var_lhs.contains(value) == false)
						al_var_lhs.add(value);
					// if (al_var_lhs == null) {
					// ArrayList<String> temp = new ArrayList<String>();
					// temp.add(value);
					// toPropagate.put(var_lhs + "", temp);
					// } else {
					// if (!al_var_lhs.contains(value))
					// al_var_lhs.add(value);
					// }
				}

			} else
				throw new NullPointerException("ValuesInVar_RHS is NULL in phi transfer function");
		}

		System.out.println("propagated Value is:");
		System.out.println(toPropagate);
		// System.out.println("All_var_lhs is:");
		// System.out.println(al_var_lhs);
		// predBBNumbers.clear();
	}

	public void returnTransferFunction(String pPoint, Integer column, SSAReturnInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {
		if (inst.getNumberOfUses() == 0)
			return;

		String methodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + methodName);

		// Iterate over all the variables to check if NULL
		int numOfUses = inst.getNumberOfUses();
		for (int i = 0; i < numOfUses; i++) {
			int varNum = inst.getUse(i);
			String varStr = Integer.toString(varNum);

			if (node.getIR().getSymbolTable().isNullConstant(varNum)) {
				// Add this NULL Constant into the propagated value
				propagatedValue.put(varStr, new ArrayList<String>(Arrays.asList("null")));
			}
		}

		int var = inst.getUse(0);
		// System.out.println(pPoint);
		ArrayList<String> pointsTo = propagatedValue.get(Integer.toString(var));

		// Variable mapping is not there in the method. What will you propagate?
		// Just return.
		if (pointsTo == null)
			return;

		// System.out.println("callSiteData is:\n");
		// System.out.println(mapToCallSiteData);

		ArrayList<callSiteData> al_csd = mapToCallSiteData.get(methodName);
		if (al_csd == null)
			throw new NullPointerException("al_csd inside return is null");

		// System.out.println("Propagated Value is\n");
		// System.out.println(propagatedValue);
		// System.out.println("CallSiteData ");

		boolean changed = false;
		String returnPP = null;
		int returnColumn = -1;
		for (callSiteData csd : al_csd) {
			// System.out.println("Current callSiteData");
			// csd.display();
			if (csd.columnsOpened == null)
				System.out.println("CSD.columndsOPned is nULL");
			Integer col_original = csd.columnsOpened.get(column);
			if (col_original == null)
				// This column was not opened by this call. Continue searching
				continue;
			returnColumn = col_original;
			boolean check;
			returnPP = csd.pPoint;
			for (String v : pointsTo) {
				check = d.add(csd.pPoint, col_original, Integer.toString(csd.varNum), v);
				if (check == true)
					changed = true;
			}
			if (changed) {
				// System.out.println("added to working list");
				workingList.add(returnPP);
				d.mark(returnPP, returnColumn);
			}
			// System.out.println("Return Data:");
			// d.displayProgramPointUnderCol(returnPP, returnColumn);
		}

		// System.out.println("\npropagated value after return:");
		// System.out.println(propagatedValue);
		// System.out.println("Return site after return is:\n");
		return;
	}

	public <T> List<T> intersection(List<T> list1, List<T> list2) {
		List<T> list = new ArrayList<T>();

		for (T t : list1) {
			if (list2.contains(t)) {
				list.add(t);
			}
		}

		return list;
	}

	public void branchTransferFunction(String pPoint, int column, SSAConditionalBranchInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		System.out.println("inside branch");
		System.out.println(propagatedValue);
		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int prevBBNum = Integer.parseInt(pPoint.split("[.]")[1]);
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock prevBB = cfg.getBasicBlock(prevBBNum);
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

		// Get the list of all the successor basicBlocks
		Collection<ISSABasicBlock> succBB = cfg.getNormalSuccessors(srcBB);

		// Get the variable numbers
		int var1 = inst.getUse(0);
		int var2 = inst.getUse(1);
		String var1Str = Integer.toString(var1);
		String var2Str = Integer.toString(var2);

		// Operator used on the conditional
		String op = inst.getOperator().toString();

		// Iterate over all the variables to check if NULL
		int numOfUses = inst.getNumberOfUses();
		for (int i = 0; i < numOfUses; i++) {
			int varNum = inst.getUse(i);
			String varStr = Integer.toString(varNum);

			if (node.getIR().getSymbolTable().isNullConstant(varNum)) {
				// Add this NULL Constant into the propagated value
				propagatedValue.put(varStr, new ArrayList<String>(Arrays.asList("null")));
			}
		}
		// System.out.println(inst.toString());

		HashMap<String, ArrayList<String>> trueBranch = new HashMap<String, ArrayList<String>>(propagatedValue);
		HashMap<String, ArrayList<String>> falseBranch = new HashMap<String, ArrayList<String>>(propagatedValue);

		// Check if it is an object comparison. If TRUE, then Deterministic IF,
		// else Non-Deterministic IF
		if (inst.isObjectComparison()) {
			// System.out.println(propagatedValue);
			ArrayList<String> v1PointsTo = propagatedValue.get(var1Str);
			ArrayList<String> v2PointsTo = propagatedValue.get(var2Str);

			// if ( pPoint.equals("))
			// Get TRUE and FALSE basicBlocks
			int trueInst = inst.getTarget();
			BasicBlock trueBB = node.getIR().getControlFlowGraph().getBlockForInstruction(trueInst);
			int trueBBNum = trueBB.getNumber();
			ISSABasicBlock falseBB = null;

			// Only 2 Successors should be there for a basicBlock
			if (succBB.size() != 2) {
				System.out.println("Conditional statement has more than 2 successors\n" + inst);
				System.exit(-1);
			}

			// Get the False BasicBlock
			for (ISSABasicBlock succ : succBB) {
				if (succ != trueBB)
					falseBB = succ;
			}

			String trueSuccPP = methodName + "." + srcBB.getNumber() + "." + trueBB.getNumber();
			String falseSuccPP = methodName + "." + srcBB.getNumber() + "." + falseBB.getNumber();

			// System.out.println("Before singleton");
			// When the pointsTo set is singleton
			if (v1PointsTo.size() == 1 && v2PointsTo.size() == 1) {
				System.out.println("both are single");
				System.out.println(propagatedValue.get(var1Str));
				System.out.println(propagatedValue.get(var2Str));
				boolean contains = propagatedValue.get(var1Str).containsAll(propagatedValue.get(var2Str));
				System.out.println("Contains:" + contains);
				// Check if the condition is satisfied
				// System.out.println(contains);
				if (((op.equals("ne") && contains == false)) || (op.equals("eq") && contains == true)) {
					// Condition is satisfied
					// Set false branch to BOT
					// falseBranch.clear();
					// falseBranch.put("bot", new
					// ArrayList<String>(Arrays.asList("bot")));
					System.out.println("propagated BOT. Cond = TRUE");
					d.setToBOT(falseSuccPP, column);
					boolean changed = false;
					changed = d.propagate(trueSuccPP, column, trueBranch);
					if (changed)
						workingList.add(trueSuccPP);
				} else {
					// System.out.println("In true");
					// Condition failed
					// Set true branch to BOT
					// trueBranch.clear();
					// trueBranch.put("bot", new
					// ArrayList<String>(Arrays.asList("bot")));
					// System.out.println(trueBranch);
					System.out.println("propagated bot. cond = FALSE");
					d.setToBOT(trueSuccPP, column);
					boolean changed = false;
					changed = d.propagate(falseSuccPP, column, falseBranch);
					if (changed)
						workingList.add(falseSuccPP);
				}
			} else {

				// If not singleton, then send the INTERSECTION to the == branch
				// and ID to != branch
				System.out.println("Before Propagating:");
				System.out.println("True:");
				System.out.println(trueBranch);
				System.out.println("false");
				System.out.println(falseBranch);
				// falseBranch.put("sri", new
				// ArrayList<String>(Arrays.asList("sri")));
				// if (trueBranch.containsKey("9"))
				// trueBranch.remove("9");
				if (op.equals("ne")) {

					// // Intersection of v1 and v2 in FALSEBRANCH
					// ArrayList<String> work1 = falseBranch.get(var1Str);
					// ArrayList<String> work2 = falseBranch.get(var2Str);
					// for (int i = 0; i < work1.size(); i++) {
					// String inter = work1.get(i);
					// if (work2.contains(inter) == false) {
					// work1.remove(inter);
					// i--;
					// }
					// }
					// for (int i = 0; i < work2.size(); i++) {
					// String inter = work2.get(i);
					// if (work1.contains(inter) == false) {
					// work2.remove(inter);
					// i--;
					// }
					// }

					// falseBranch.get(var1 + "").retainAll(falseBranch.get(var1
					// + ""));
					// falseBranch.get(var2 + "").retainAll(falseBranch.get(var1
					// + ""));
					ArrayList<String> temp = new ArrayList<String>(
							intersection(falseBranch.get(var1 + ""), falseBranch.get(var2 + "")));

					// System.out.println("1:" + falseBranch.get(var1Str));
					// System.out.println("2:" + falseBranch.get(var2Str));
					falseBranch.put(var1 + "", new ArrayList<String>(temp));
					falseBranch.put(var2 + "", new ArrayList<String>(temp));

					System.out.println("After intersection:");
					System.out.println("True:");
					System.out.println(trueBranch);
					System.out.println("false");
					System.out.println(temp);
					// Check if the INTERSECTION is NULL. If TRUE, set it to BOT
					if (falseBranch.get(var1Str).size() == 0 && falseBranch.get(var2Str).size() == 0) {
						// falseBranch.clear();
						// falseBranch.put("bot", new
						// ArrayList<String>(Arrays.asList("bot")));
						d.setToBOT(falseSuccPP, column);
						boolean changed = false;
						changed = d.propagate(trueSuccPP, column, trueBranch);
						if (changed)
							workingList.add(trueSuccPP);
					} else {
						boolean changed = false;
						changed = d.propagate(falseSuccPP, column, falseBranch);
						if (changed)
							workingList.add(falseSuccPP);
						System.out.println("SUcc: " + falseSuccPP);
						d.displayProgramPointUnderCol(falseSuccPP, column);

						changed = false;
						changed = d.propagate(trueSuccPP, column, trueBranch);
						if (changed)
							workingList.add(trueSuccPP);
						System.out.println("SUcc: " + trueSuccPP);
						d.displayProgramPointUnderCol(trueSuccPP, column);
					}
					//
					// throw new NullPointerException(
					// "Intersection of ArrayList<> is not same. Branch transfer
					// function: NE");
				} else if (op.equals("eq")) {

					// Intersection of v1 and v2 in TRUEBRANCH
					// trueBranch.get(var1Str).retainAll(trueBranch.get(var2Str));
					// trueBranch.get(var2Str).retainAll(trueBranch.get(var1Str));
					ArrayList<String> temp = new ArrayList<String>(
							intersection(trueBranch.get(var1Str), trueBranch.get(var2Str)));

					// System.out.println("1:" + falseBranch.get(var1Str));
					// System.out.println("2:" + falseBranch.get(var2Str));
					trueBranch.put(var1Str, new ArrayList<String>(temp));
					trueBranch.put(var2Str, new ArrayList<String>(temp));

					// Check if the INTERSECTION is NULL. If TRUE, make it BOT
					if (trueBranch.get(var1Str).size() == 0 && trueBranch.get(var2Str).size() == 0) {
						// trueBranch.clear();
						// trueBranch.put("bot", new
						// ArrayList<String>(Arrays.asList("bot")));
						d.setToBOT(trueSuccPP, column);
						boolean changed = false;
						changed = d.propagate(falseSuccPP, column, falseBranch);
						if (changed)
							workingList.add(falseSuccPP);
					} else {
						boolean changed = false;
						changed = d.propagate(trueSuccPP, column, trueBranch);
						if (changed)
							workingList.add(trueSuccPP);

						changed = false;
						changed = d.propagate(falseSuccPP, column, falseBranch);
						if (changed)
							workingList.add(falseSuccPP);
						System.out.println("SUcc: " + falseSuccPP);
					}
					// throw new NullPointerException(
					// "Intersection of ArrayList<> is not same. Branch transfer
					// function: EQ");
				}
			}

			// // Propagate to TRUE and FALSE branches
			// String succPP = methodName + "." + srcBB.getNumber() + "." +
			// trueBB.getNumber();
			// d.propagate(succPP, column, trueBranch);
			// d.displayProgramPoint(succPP, column);
			//
			// succPP = methodName + "." + srcBB.getNumber() + "." +
			// falseBB.getNumber();
			// d.propagate(succPP, column, falseBranch);
		} else {
			for (ISSABasicBlock succ : succBB) {
				String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
				boolean changed = false;
				changed = d.propagate(succPP, column, propagatedValue);
				if (changed)
					workingList.add(succPP);
			}
		}

		return;
	}

	public void printNodes() {
		System.out.println("Displaying Application's Call Graph nodes: ");
		Iterator<CGNode> nodes = cg.iterator();

		// Printout the nodes in the call-graph
		while (nodes.hasNext()) {
			String nodeInfo = nodes.next().toString();
			if (nodeInfo.contains("Application"))
				System.out.println(nodeInfo);
		}
	}

	/**
	 * This method prints the IR of the analysisMethod
	 */
	public void printIR() {
		System.out.println("\n\n");
		Iterator<CGNode> nodes = cg.iterator();
		CGNode target = null;
		while (nodes.hasNext()) {
			CGNode node = nodes.next();
			String nodeInfo = node.toString();
			if (nodeInfo.contains(analysisClass) && nodeInfo.contains(analysisMethod)) {
				target = node;
				break;
			}
		}
		if (target != null) {
			System.out.println("The IR of method " + target.getMethod().getSignature() + " is:");
			System.out.println(target.getIR().toString());
		} else {
			System.out.println("The given method in the given class could not be found");
		}
	}
}
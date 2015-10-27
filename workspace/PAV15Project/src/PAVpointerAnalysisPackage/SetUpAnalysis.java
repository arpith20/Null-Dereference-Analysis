package PAVpointerAnalysisPackage;

/**
 * @author Arpith K, Sridhar G
 * Program Analysis and Verification, 2015
 *
 */

// Do NOT import the pointer analysis package
import java.io.*;
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
import com.ibm.wala.util.intset.IntSet;
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
						hashGlobalMethods.put(methodName, node);
					}
				}
			}
		}
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
					if (methodName.equals("loopTest")) {
						workingList.add(pp);
						d.addProgramPoint(pp);
					}

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
		return callSites;
	}

	// Returns the list of all the methods which are called directly by the ROOT
	// method
	public ArrayList<CGNode> getDirectCallSites(CGNode root) {
		ArrayList<CGNode> callSites = new ArrayList<CGNode>();

		// Iterate over all the call sites in ROOT
		Iterator<CallSiteReference> icsr = root.getIR().iterateCallSites();
		while (icsr.hasNext()) {
			CallSiteReference csr = icsr.next();
			if (!csr.isSpecial()) {

				// Get the CGNode of the target method. This is a singleton set
				Set<CGNode> nodes = cg.getPossibleTargets(root, csr);
				if (nodes.size() == 1) {
					Iterator<CGNode> i = nodes.iterator();
					CGNode temp = i.next();

					// Check if the method is a class method and not library
					// function
					if (globalMethods.contains(temp))
						callSites.add(temp);
				} else {
					// Currently exit the code if more than one possible target
					// is found for a callSite. Can do better!
					System.out.println("GetPossibleTargets returned more than 1 CGNode!!");
					System.exit(-1);
				}
			}
		}
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
		while (!workingList.isEmpty()) {
			String curPP = workingList.get(0);

			// System.out.println("PP:" + curPP);

			// Check if all the columns in the program point are unmarked.
			// If true, continue
			if (d.checkAllColumnsUnmarked(curPP)) {
				workingList.remove(0);
				continue;
			}

			// System.out.println("kildall");
			// Call the transfer function driver
			transferFunctionDriver(curPP);

			String methodName = curPP.split("[.]")[0];
			int srcBB = Integer.parseInt(curPP.split("[.]")[2]);
			CGNode node = hashGlobalMethods.get(methodName);

			workingList.remove(0);
		}
		// System.out.println("\n\n");
		d.display();
	}

	public void transferFunctionDriver(String pPoint) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int prevBBNum = Integer.parseInt(pPoint.split("[.]")[1]);
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(methodName);
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

			// Check if the value is BOT. If so, propagate BOT and continue
			if (propagatedValue.containsKey("bot")) {
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					d.setToBOT(succPP, column);
				}
				continue;
			}

			// Check if there are no instructions in the basic block. If so,
			// then propagate the same value to the successors
			if (srcBB.getAllInstructions().isEmpty()) {
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					d.propagate(succPP, column, propagatedValue);
				}
				continue;
			}

			// Apply transfer function to the data present in COLUMN for the
			// instructions in the basicBlock
			// The output will be a new HashMap<String,ArrayList<String>>. This
			// value will be propagated to the successors
			Iterator<SSAInstruction> iSSA = srcBB.iterator();
			while (iSSA.hasNext()) {
				SSAInstruction inst = iSSA.next();

				if (inst instanceof SSANewInstruction) {
					newTransferFunction(pPoint, column, (SSANewInstruction) inst, propagatedValue);
				} else if (inst instanceof SSAInvokeInstruction)
					callTransferFunction();
				else if (inst instanceof SSAPhiInstruction) {
					System.out.println("this: " + inst.toString() + "");
					phiTransferFunction(pPoint, column, (SSAPhiInstruction) inst, propagatedValue);
				}

				else if (inst instanceof SSAReturnInstruction)
					returnTransferFunction();
				else if (inst instanceof SSAConditionalBranchInstruction)
					branchTransferFunction(methodName, (SSAConditionalBranchInstruction) inst, propagatedValue);
			}
		}
	}

	public void newTransferFunction(String pPoint, int column, SSANewInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int prevBBNum = Integer.parseInt(pPoint.split("[.]")[1]);
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

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

		// Iterate over the successor basicBlocks to JOIN the
		// propagatedValue
		System.out.println("before propagate" + propagatedValue);
		for (ISSABasicBlock succ : succBB) {
			String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
			d.propagate(succPP, column, propagatedValue);
			d.displayProgramPoint(succPP, column);
		}

		return;
	}

	public void callTransferFunction() {

	}

	public void phiTransferFunction_old(String pPoint, int column, SSAPhiInstruction inst,
			HashMap<String, ArrayList<String>> toPropagate) {

		// used to keep track of the bot values of predecessors
		// this will remain false if all predecessors are bot
		Boolean flag = false;

		String methodName = pPoint.split("[.]")[0];
		Integer currentBlockNumber = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();

		ISSABasicBlock bb = cfg.getBasicBlock(currentBlockNumber);
		ISSABasicBlock bb_successor = cfg.getBasicBlock(currentBlockNumber);

		Iterator<ISSABasicBlock> predNodes = cfg.getPredNodes(bb);

		Collection<ISSABasicBlock> succNodes = cfg.getNormalSuccessors(bb);
		if (succNodes.size() > 1)
			throw new NullPointerException("The number of successors is assumed to be one");
		for (ISSABasicBlock b : succNodes) {
			bb_successor = b;
		}

		ArrayList<Integer> predBBNumbers = new ArrayList<Integer>();
		while (predNodes.hasNext()) {
			ISSABasicBlock pred_bb = predNodes.next();
			System.out.println(pred_bb.getNumber());
			predBBNumbers.add(pred_bb.getNumber());
		}

		System.out.println(methodName);
		System.out.println(inst.getNumberOfUses());
		for (int i = 0; i < inst.getNumberOfUses(); i++) {
			String pp_pred = "";
			String pp_succ = "";

			int var_rhs = inst.getUse(i);
			int var_lhs = inst.getDef();
			pp_pred = methodName + "." + predBBNumbers.get(i) + "." + currentBlockNumber;
			pp_succ = methodName + "." + currentBlockNumber + "." + bb_successor.getNumber();

			// Do not erase
			System.out.println("-----");
			System.out.println(pp_pred);
			System.out.println("v" + var_rhs + " of predBB" + predBBNumbers.get(i) + " goes into v" + var_lhs);
			System.out.println(pp_succ);
			System.out.println(">>>>>>");

			ArrayList<String> valuesInVar_rhs = d.retrieve(pp_pred, column, var_rhs + "");
			if (valuesInVar_rhs != null) {
				for (String value : valuesInVar_rhs) {
					d.add(pp_succ, column, var_lhs + "", value);
				}
			} else {
				String temp = "v" + var_lhs + ":#null";
				if (inst.toString().contains(temp))
					d.add(pp_succ, column, var_lhs + "", "null");
			}
			if ((!d.retrieve(pp_pred, column).containsKey("bot"))
					|| ((i == inst.getNumberOfUses() - 1) && (flag == false))) {
				d.join(pp_succ, column, pp_pred, column);
				flag = true;
			}
		}

		predBBNumbers.clear();
	}

	public void phiTransferFunction(String pPoint, int column, SSAPhiInstruction inst,
			HashMap<String, ArrayList<String>> toPropagate) {

		String methodName = pPoint.split("[.]")[0];
		Integer currentBlockNumber = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();

		ISSABasicBlock bb = cfg.getBasicBlock(currentBlockNumber);

		Iterator<ISSABasicBlock> predNodes = cfg.getPredNodes(bb);

		Collection<ISSABasicBlock> succNodes = cfg.getNormalSuccessors(bb);
		if (succNodes.size() > 1)
			throw new NullPointerException("The number of successors is assumed to be one");

		ArrayList<Integer> predBBNumbers = new ArrayList<Integer>();
		while (predNodes.hasNext()) {
			ISSABasicBlock pred_bb = predNodes.next();
			predBBNumbers.add(pred_bb.getNumber());
		}

		for (int i = 0; i < inst.getNumberOfUses(); i++) {
			String pp_pred = "";

			int var_rhs = inst.getUse(i);
			int var_lhs = inst.getDef();
			pp_pred = methodName + "." + predBBNumbers.get(i) + "." + currentBlockNumber;

			ArrayList<String> valuesInVar_rhs = d.retrieve(pp_pred, column, var_rhs + "");
			if (valuesInVar_rhs != null) {
				for (String value : valuesInVar_rhs) {
					ArrayList<String> al_var_lhs = toPropagate.get(var_lhs);
					if (al_var_lhs == null) {
						ArrayList<String> temp = new ArrayList<String>();
						temp.add(value);
						toPropagate.put(var_lhs + "", temp);
					} else {
						if (!al_var_lhs.contains(value))
							al_var_lhs.add(value);
					}
				}
			} else {
				// check for null constants
				String null_constant = "v" + var_rhs + ":#null";
				if (inst.toString().contains(null_constant)) {
					// add var_lhs -> null mapping
					ArrayList<String> al_var_lhs = toPropagate.get(var_lhs);
					if (al_var_lhs == null) {
						ArrayList<String> temp = new ArrayList<String>();
						temp.add("null");
						toPropagate.put(var_lhs + "", temp);
					} else {
						if (!al_var_lhs.contains("null"))
							al_var_lhs.add("null");
					}
				}
			}
		}

		predBBNumbers.clear();
	}

	public void returnTransferFunction() {

	}

	public void branchTransferFunction(String methodName, SSAConditionalBranchInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		// Get the variable numbers
		int var1 = inst.getUse(0);
		int var2 = inst.getUse(1);

		// Operator used on the conditional
		String op = inst.getOperator().toString();

		// Check if the second variable is a constant.
		// ONLY SECOND variable can be a constant
		// TODO
		if (target.getIR().getSymbolTable().isNullConstant(var2)) {
			// Add this NULL Constant into the propagated value
			propagatedValue.put(Integer.toString(var2), new ArrayList<String>(Arrays.asList("null")));
		}

		DefUse defUse = target.getDU();
		// SSAInstruction def2 = defUse.getDef(var2);

		System.out.println(inst.toString());
		System.out.println(inst.getUse(0) + " " + inst.getUse(1));
		System.out.println(op);

		// System.out.println(t1 + " " + t2);
		Boolean a = target.getIR().getSymbolTable().isNullConstant(var2);

		// System.out.println(inst.getUse(0) + " " + inst.getUse(1));
		// System.out.println("Cond:" + inst.toString());
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
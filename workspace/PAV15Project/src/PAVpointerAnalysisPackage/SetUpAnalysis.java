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
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
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
	HashMap<String, CGNode> hashGlobalMethods = new HashMap<String, CGNode>();

	// Main DATA class to store the analysis results
	private Data d;

	// List of all the program points to analyze. End analysis when this list
	// becomes empty
	private ArrayList<String> workingList = new ArrayList<String>();

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
					workingList.add(pp);

					// // Add the program point to the DATA object
					// // We are creating a 0th column and then mapping "" to ""
					// in
					// // that.
					// // This is the initial value at all program points
					// d.add(pp, 0, "", "");

					// System.out.println(pp);
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
						callSites.addAll(directSites);
					if (!workingList.contains(add))
						workingList.addAll(directSites);
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

		// Add D0 to DATA
		d.add(pp, 0, "", "");
		d.mark(pp, 0);
		// System.out.println(methodName);
	}

	// Main kildall algorithm. This will do the analysis and will return once
	// the WORKINGLIST is empty
	public void kildall() {
		while (!workingList.isEmpty()) {
			String curPP = workingList.get(0);

			// Check if all the columns in the program point are unmarked.
			// If true, continue
			if (!d.checkAllColumnsUnmarked(curPP)) {
				workingList.remove(0);
				continue;
			}

			// Extract info from the program point
			String methodName = curPP.split("[.]")[0];
			int srcBB = Integer.parseInt(curPP.split("[.]")[2]);

			System.out.println("PP:" + curPP);
			CGNode node = hashGlobalMethods.get(methodName);

			workingList.remove(0);
		}
	}

	public void transferFunctionDriver() {

		BasicBlock src = target.getIR().getControlFlowGraph().getBasicBlock(0);
	}
}
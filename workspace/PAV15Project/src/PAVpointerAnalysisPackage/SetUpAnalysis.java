package PAVpointerAnalysisPackage;

/**
 * @authors Sridhar Gopinath	-	g.sridhar53@gmail.com
 * @authors Arpith K			-	arpith@live.com		
 *
 * Null Pointer Dereference analysis,
 * Program Analysis and Verification Course, Fall - 2015,
 * Computer Science and Automation (CSA),
 * Indian Institute of Science (IISc),
 * Bangalore
 *
 */

import java.io.*;
import java.util.*;

import com.ibm.wala.classLoader.CallSiteReference;
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
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

public class SetUpAnalysis {

	private String classpath;
	private String mainClass;
	private String analysisClass;
	private String analysisMethod;

	// CGNODE of the method which we are analyzing
	private CGNode target;

	// List of CGNODEs of all the methods which are present in the given class
	private ArrayList<CGNode> globalMethods = new ArrayList<CGNode>();

	// HashMap which maps the names of the methods to its corresponding CGNodes
	// Key = ClassName + MethodName
	// Ex: Key = "LTestCases/Test1ifTest" where TestCases is the package name
	// and Test1 is the class name and ifTest is the method name
	private HashMap<String, CGNode> hashGlobalMethods = new HashMap<String, CGNode>();

	// Main DATA class to store the analysis results
	private Data data;

	// List of all the program points to analyze. End analysis when this list
	// becomes empty
	private ArrayList<String> workingList = new ArrayList<String>();

	// For all the Methods which are reachable directly or transitively from the
	// analysis methods, this HashMap maps the name of the method with the
	// ArrayList of all the program points present in that method
	private HashMap<String, ArrayList<String>> programPoints = new HashMap<String, ArrayList<String>>();

	// This class is used to keep data at each call site which is used in RETURN
	// TRANSFER FUNCTION
	class callSiteData {
		// Return Site program point
		public String pPoint;

		// Return variable number, if any
		public int varNum;

		// If a call site under column col1 opens a column col2 at the target
		// method, then this HashMap will have a mapping from col2 => col1
		// In recursive functions, a single column at the target program point
		// can be opened by more than 1 columns at the call site. Hence, we
		// require an ArrayList of columns
		//
		// If column 2 at target is opened by both col 1 and col 2 from call
		// site, then we will have mapping from 2 => {1,2}
		//
		// Key is the column at the target method and the Value is the columns
		// at
		// the call site
		public HashMap<Integer, ArrayList<Integer>> columnsOpened;

		// Constructor for this class
		public callSiteData(String succPPoint, int returnVar) {
			pPoint = succPPoint;
			varNum = returnVar;
			columnsOpened = new HashMap<Integer, ArrayList<Integer>>();
			return;
		}
	};

	// HashMap which keeps track of all the callSites of a particular Method
	// If a function foo is being called by bar1 and bar2, then foo will have 2
	// Objects of CALLSITEDATA in the ArrayList one for each callSite
	// Key: MethodName without any extra information. Ex: public void ifTest()
	// will have key "ifTest"
	HashMap<String, ArrayList<callSiteData>> mapToCallSiteData = new HashMap<String, ArrayList<callSiteData>>();

	// FLAG to display either JOIN output or TABLE output
	public static boolean displayJoinedOutput;

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

	public SetUpAnalysis(String classpath, String mainClass, String analysisClass, String analysisMethod, String join) {
		this.classpath = classpath;
		this.mainClass = mainClass;
		this.analysisClass = analysisClass;
		this.analysisMethod = analysisMethod;
		if (join.equals("join") == true)
			displayJoinedOutput = true;
		else
			displayJoinedOutput = false;
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
	public void init() throws FileNotFoundException {

		// TODO Initialization flags for the analysis
		// Initialization of flags required for the run of the analysis
		boolean printToFile = false;
		String outputFile = "test7join.txt";

		// If set, write the output generated to a file
		if (printToFile == true) {
			PrintStream out = new PrintStream(new FileOutputStream(outputFile));
			System.setOut(out);
		}

		// Set TARGET to point to the method which we are analyzing
		if (setTarget() == false) {
			System.out.println("Set Target Failed");
			System.exit(-1);
		}

		// Initialize the GLOBALMETHODS ArrayList
		setGlobalMethods();

		// New Object to hold all the data
		data = new Data();

		return;
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
		// This can be done better!
		Iterator<CGNode> nodes = cg.iterator();
		while (nodes.hasNext()) {
			CGNode node = nodes.next();
			String nodeInfo = node.toString();
			if (nodeInfo.contains("Application")) {
				String[] s = nodeInfo.split("[ ]");
				if (s[2].contains("Application")) {

					// TODO Check if there is a better way to do this
					// Add the CGNode to the GLOBALMETHODS
					// Do not add the Constructor i.e init to the array list or
					// to the hashMap
					if (!nodeInfo.contains("<init>()V")) {
						globalMethods.add(node);

						// Get shorthand name of the method and the className
						// Use this as the KEY and add it to the HashMap
						// IMPORTANT: Only methodName cannot be used as the KEY
						// since multiple methods with same name is possible in
						// a class
						String methodName = node.getMethod().toString().split("[,]")[2].split("[(]")[0].substring(1);
						String className = node.getMethod().toString().split("[,]")[1].substring(1);
						hashGlobalMethods.put(className + methodName, node);
					}
				}
			}
		}
		return;
	}

	// Get all the program points in the methods which can be called directly or
	// transitively from the ROOT method. Store this in PROGRAMPOINTS
	public void getProgramPoints() {

		// Get the list of all the methods reachable directly or transitively
		// from the method which we are analyzing
		ArrayList<CGNode> methods = getDirectAndTransitiveCallSites(target);
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
					// Create a program point for the DATA class
					workingList.add(pp);
					data.addProgramPoint(pp);

					// Add the program point to the hash map containing the set
					// of all program points present in a particular method
					//
					// Check if the method is already present in the
					// programPoints hashMap
					if (programPoints.containsKey(methodName) == false) {

						// List of programPoints doesn't exist. Create a new one
						ArrayList<String> list = new ArrayList<String>();
						list.add(pp);
						programPoints.put(methodName, list);
					} else {
						ArrayList<String> list = programPoints.get(methodName);
						list.add(pp);
					}
				}
			}
		}
		return;
	}

	// Returns the list of all the methods which are called directly or
	// transitively by the TARGET method
	public ArrayList<CGNode> getDirectAndTransitiveCallSites(CGNode node) {
		ArrayList<CGNode> callSites = new ArrayList<CGNode>();
		ArrayList<CGNode> workingList = new ArrayList<CGNode>();

		// Add the method which we are currently analyzing to working list.
		// We want transitive callSites from the method which we are analyzing
		callSites.add(node);
		workingList.add(node);

		while (workingList.isEmpty() == false) {
			CGNode cur = workingList.get(0);

			// Get the direct callSites from CUR
			ArrayList<CGNode> directSites = getDirectCallSites(cur);
			if (directSites.isEmpty() == false) {
				for (CGNode add : directSites) {

					// Add the direct callSites to the workingList and callSites
					// if NOT already present
					if (callSites.contains(add) == false)
						callSites.add(add);
					if (workingList.contains(add) == false)
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
	// This method also creates entries in the mapToCallSiteData as well
	public ArrayList<CGNode> getDirectCallSites(CGNode root) {
		ArrayList<CGNode> callSites = new ArrayList<CGNode>();

		// Iterate over all the call sites in ROOT
		Iterator<CallSiteReference> icsr = root.getIR().iterateCallSites();
		while (icsr.hasNext()) {
			CallSiteReference csr = icsr.next();

			// invokeSpecial is for Constructors
			// invokeVirtual is for method invocation including library methods
			// invokeStatic is for invoking static methods
			// Considering only invokeVirtual and invokeStatic
			// TODO Check if this is sufficient
			if (!csr.isSpecial()) {

				// Check if the target method of this call site is present in
				// the HashMap containing all the Global Methods
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

						// Add this node to callSites
						callSites.add(temp);

						// Add this callSite to the CallSiteData class
						// Check if targetMethodName already exists
						ArrayList<callSiteData> siteData = mapToCallSiteData.get(targetMethodName);
						if (siteData == null) {
							siteData = new ArrayList<callSiteData>();
							mapToCallSiteData.put(targetMethodName, siteData);
						}
					} else
						throw new NullPointerException(
								"getDirectCallSites: size of possibletarget nodes not equal to 1");
				} else
					continue;
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
		data.add(pp, 0, "", "");

		return;
	}

	// Helper function to open new columns for an entire method
	public void openColumnsDriver(String methodName, int column) {
		ArrayList<String> pPoints = programPoints.get(methodName);
		for (String pp : pPoints) {
			data.openColumn(pp, column);
		}

		return;
	}

	// Main kildall algorithm. This will do the analysis and will return once
	// the WORKINGLIST is empty
	public void kildall() {

		while (workingList.isEmpty() == false) {
			String curPP = workingList.get(0);

			// Check if all the columns in the program point are unmarked.
			// If true, continue
			if (data.checkAllColumnsUnmarked(curPP) == true) {
				workingList.remove(0);
				continue;
			}

			// Call the transfer function driver
			transferFunctionDriver(curPP);

			workingList.remove(0);
		}

		// Output of the ENTIRE ANALYSIS
		displayOutput();

		return;
	}

	// Display the output of the ENTIRE ANALYSIS
	// TODO Not CHECKED
	public void displayOutput() {

		for (Map.Entry<String, ArrayList<String>> entry : programPoints.entrySet()) {
			String method = entry.getKey();
			ArrayList<String> iterate = entry.getValue();
			System.out.println("\n\n\n" + method + "");
			for (int i = 0; i < method.length(); i++)
				System.out.print("=");
			System.out.println("\n");
			for (String pPoint : iterate)
				data.displayProgramPoint(pPoint);
		}
		return;
	}

	// Helper function which is used to call the particular transfer function
	// based on the type of instruction
	public void transferFunctionDriver(String pPoint) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		// Get source basic block from the CFG of the method
		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

		// Get the list of all the successor basicBlocks
		Collection<ISSABasicBlock> succBB = cfg.getNormalSuccessors(srcBB);

		// Get the markings present at the current program point
		HashMap<Integer, Boolean> markings = data.getColumnMarkings(pPoint);

		// Iterate ONLY over the set of columns which are marked
		for (Map.Entry<Integer, Boolean> entry : markings.entrySet()) {
			Integer column = entry.getKey();
			Boolean mark = entry.getValue();

			// Unmarked
			if (mark == false)
				continue;

			// Un-mark the current column
			data.unmark(pPoint, column);

			// Create a new hashMap and initialize it to the value present at
			// the current program point. This value will be propagated to the
			// successors
			HashMap<String, ArrayList<String>> propagatedValue = new HashMap<String, ArrayList<String>>();
			propagatedValue = data.retrieve(pPoint, column);

			// Check if the value is BOT. If so, propagate BOT and continue
			if (data.isBOT(pPoint, column) == true) {
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					boolean changed = false;
					changed = data.propagate(succPP, column, propagatedValue);
					if (changed)
						workingList.add(succPP);
				}
				continue;
			}

			// Check if there are no instructions in the basic block. If so,
			// then propagate the same value to the successors
			if (srcBB.getAllInstructions().isEmpty()) {
				for (ISSABasicBlock succ : succBB) {
					String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
					boolean changed = false;
					changed = data.propagate(succPP, column, propagatedValue);
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
					propagate = putTransferFunction(pPoint, column, (SSAPutInstruction) inst, propagatedValue);
				} else if (inst instanceof SSAGetInstruction) {
					propagate = getTransferFunction(pPoint, column, (SSAGetInstruction) inst, propagatedValue);
				} else if (inst instanceof SSAInvokeInstruction) {
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
					changed = data.propagate(succPP, column, propagatedValue);
					if (changed)
						workingList.add(succPP);
				}
			}
		}
		return;
	}

	public void newTransferFunction(String pPoint, int column, SSANewInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];

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

		return;
	}

	public boolean putTransferFunction(String pPoint, int column, SSAPutInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {
		// System.out.println("=====================");
		// System.out.println(inst.toString());

		String varRIGHT = Integer.toString(inst.getVal()); // xyz->{||THIS||}
		String varLEFT = Integer.toString(inst.getUse(0)); // ||THIS||->{xyz}

		// If it is not a OBJECT which we are handling
		if (propagatedValue.get(varRIGHT) == null)
			return true;

		// null constants are added here
		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);
		if (node.getIR().getSymbolTable().isNullConstant(inst.getVal())) {
			propagatedValue.put(Integer.toString(inst.getVal()), new ArrayList<String>(Arrays.asList("null")));
		}

		// xyz.THIS = abc
		// some sorcery to get the data member
		String dataMember = (inst.toString().split("[,]"))[2].substring(1, (inst.toString().split("[,]"))[2].length());

		if (propagatedValue.get(varLEFT).contains("null") == true) {
			if (propagatedValue.get(varLEFT).size() == 1) {
				data.setToBOT(pPoint, column);
				return false;
			}
			propagatedValue.get(varLEFT).remove("null");
		}

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

		return true;
		// System.out.println("=====================");
	}

	public boolean getTransferFunction(String pPoint, int column, SSAGetInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {
		// System.out.println("=====================");
		// System.out.println(inst.toString());

		String varRIGHT = Integer.toString(inst.getUse(0)); // xyz->{||THIS||}
		String varLEFT = Integer.toString(inst.getDef()); // ||THIS||->{xyz}

		// If it is not a OBJECT which we are handling
		if (propagatedValue.get(varRIGHT) == null)
			return true;

		if (inst.getNumberOfUses() > 1)
			throw new Error("getTransferFunction: Number of uses is greater than 1");

		// null constants are added here
		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);
		if (node.getIR().getSymbolTable().isNullConstant(inst.getUse(0))) {
			propagatedValue.put(Integer.toString(inst.getUse(0)), new ArrayList<String>(Arrays.asList("null")));
		}

		// gets the data member
		String dataMember = (inst.toString().split("[,]"))[2].substring(1, (inst.toString().split("[,]"))[2].length());

		if (propagatedValue.get(varLEFT).contains("null") == true) {
			if (propagatedValue.get(varLEFT).size() == 1) {
				data.setToBOT(pPoint, column);
				return false;
			}
			propagatedValue.get(varLEFT).remove("null");
		}

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

		return true;
		// System.out.println(varLEFT + " " + varRIGHT);
		// System.out.println("=====================");
	}

	public void callTransferFunction(String pPoint, Integer column, SSAInvokeInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue, String targetMethodName, String succPPoint) {

		String rootMethodName = pPoint.split("[.]")[0];
		CGNode node = hashGlobalMethods.get(analysisClass + rootMethodName);

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
		int newCol = data.columnMapExists(targetPP, callSiteValue);
		if (newCol == -1) {
			// Does not exist. Get the new column number
			newCol = data.getNewColumnNum(targetPP);
			openColumnsDriver(targetMethodName, newCol);
			data.copyEntireMap(targetPP, newCol, callSiteValue);
			workingList.add(targetPP);
		}

		if (inst.getNumberOfReturnValues() > 1)
			throw new NullPointerException("CallStatement returned more than 1 value:\n" + inst + "\n");

		int returnVar = inst.getReturnValue(0);
//		System.out.println(returnVar);

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
			// No entry for this callSite. Add a new one
			// TODO: Remove the constructor. Variables are public anyway
			thisCallSite = new callSiteData(succPPoint, returnVar);
			thisCallSite.pPoint = new String(succPPoint);
			thisCallSite.varNum = returnVar;
			thisCallSite.columnsOpened = new HashMap<Integer, ArrayList<Integer>>();
			thisCallSite.columnsOpened.put(newCol, new ArrayList<Integer>(Arrays.asList(column)));
			listCallSites.add(thisCallSite);
		} else {
			ArrayList<Integer> columnMapped = thisCallSite.columnsOpened.get(newCol);
			if (columnMapped == null)
				thisCallSite.columnsOpened.put(newCol, new ArrayList<Integer>(Arrays.asList(column)));
			else {
				columnMapped.add(column);
			}

		}
		return;
	}

	public void phiTransferFunction(String pPoint, Integer column, SSAPhiInstruction inst,
			HashMap<String, ArrayList<String>> toPropagate) {

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

		ArrayList<Integer> predBBNumbers = new ArrayList<Integer>();
		while (predNodes.hasNext()) {
			ISSABasicBlock pred_bb = predNodes.next();
			predBBNumbers.add(pred_bb.getNumber());
		}

		int var_lhs = inst.getDef();
		ArrayList<String> al_var_lhs = toPropagate.get(Integer.toString(var_lhs));
		for (int i = 0; i < inst.getNumberOfUses(); i++) {
			String pp_pred = "";

			int var_rhs = inst.getUse(i);
			pp_pred = methodName + "." + predBBNumbers.get(i) + "." + currentBlockNumber;
			// System.out.println(var_rhs + " corresponds to " + pp_pred);

			ArrayList<String> valuesInVar_rhs;
			if (pp_pred.equals(pPoint))
				valuesInVar_rhs = toPropagate.get(Integer.toString(var_rhs));
			else
				valuesInVar_rhs = data.retrieve(pp_pred, column, Integer.toString(var_rhs));

			if (node.getIR().getSymbolTable().isNullConstant(var_rhs)) {
				// Add this NULL Constant into the propagated value
				if (valuesInVar_rhs == null)
					valuesInVar_rhs = new ArrayList<String>();
				if (valuesInVar_rhs.contains("null") == false)
					valuesInVar_rhs.add("null");
			}

			if (valuesInVar_rhs == null)
				continue;

			if (al_var_lhs == null) {
				al_var_lhs = new ArrayList<String>();
				toPropagate.put(Integer.toString(var_lhs), al_var_lhs);
			}
			for (String value : valuesInVar_rhs) {
				if (al_var_lhs.contains(value) == false)
					al_var_lhs.add(value);
			}
		}
	}

	public void returnTransferFunction(String pPoint, Integer column, SSAReturnInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		// copy all symbolic objects to the call site
		for (Map.Entry<String, ArrayList<String>> entry : propagatedValue.entrySet()) {
			String symbolic_object = entry.getKey();

			// current method
			String method = pPoint.split("[.]")[0];

			if (symbolic_object.split("[.]").length == 2 || symbolic_object.split("[.]").length > 3)
				throw new Error("Split failed in returnTransferFunction");

			// not a symbolic object; continue
			// if
			// (!Character.isLetter(symbolic_object.charAt(symbolic_object.length()
			// - 1)))
			//
			// TODO Assuming that fields will be of type (X.Y.t)
			if (symbolic_object.split("[.]").length == 3)
				continue;

			// if the symbolic object does not point to anything; continue
			// (Shouldn't this be an error???)
			if (entry.getValue() == null)
				continue;
			// shouldn't be done
			if (method.contains(analysisMethod.split("[(]")[0]))
				continue;

//			System.out.println(analysisMethod.split("[(]")[0] + ">>>>>>>>>>>>>>>>" + pPoint);
//			System.out.println("method: " + method);

			ArrayList<callSiteData> al_csd2 = mapToCallSiteData.get(pPoint.split("[.]")[0]);
			if (al_csd2 == null)
				throw new NullPointerException("al_csd inside return is null");
//			System.out.println("-------------before for");
			for (callSiteData csd : al_csd2) {
//				System.out.println("-------------in for");
				// if (csd.pPoint == null)
				// continue;
//				System.out.println("-------------" + csd.pPoint);
				ArrayList<String> pointsto_symbolic_obj = propagatedValue.get(symbolic_object);
				for (String s : pointsto_symbolic_obj) {
//					System.out.print("Adding to --" + pPoint + "'s " + symbolic_object);
//					System.out.println("--" + s);
					data.add(csd.pPoint, column, symbolic_object, s);
					data.mark(csd.pPoint, column);
				}
//				System.out.println("-----------------done");
			}
			// ArrayList<String> iterate = entry.getValue();
			// System.out.println("\n\n\n" + method + "");
			// for (int i = 0; i < method.length(); i++)
			// System.out.print("=");
			// System.out.println("\n");
			// for (String pPoint : iterate)
			// d.displayProgramPoint(pPoint);
		}

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

		ArrayList<String> pointsTo = propagatedValue.get(Integer.toString(var));

		// Variable mapping is not there in the method. What will you propagate?
		// Just return.
		if (pointsTo == null)
			return;

		ArrayList<callSiteData> al_csd = mapToCallSiteData.get(methodName);
		if (al_csd == null)
			throw new NullPointerException("al_csd inside return is null");

		boolean changed = false;
		String returnPP = null;
		int returnColumn = -1;
		for (callSiteData csd : al_csd) {
			if (csd.columnsOpened == null) {
				System.out.println("CSD.columndsOPned is nULL");
				break;
			}
			ArrayList<Integer> col_original = csd.columnsOpened.get(column);
			if (col_original == null)
				// This column was not opened by this call. Continue searching
				continue;

			for (Integer iterateColumn : col_original) {
				returnColumn = iterateColumn;

				boolean check;
				returnPP = csd.pPoint;
				for (String v : pointsTo) {
					check = data.add(csd.pPoint, returnColumn, Integer.toString(csd.varNum), v);
					if (check == true)
						changed = true;
				}
				if (changed) {
					workingList.add(returnPP);
					data.mark(returnPP, returnColumn);
				}
			}
		}
		return;
	}

	public void branchTransferFunction(String pPoint, int column, SSAConditionalBranchInstruction inst,
			HashMap<String, ArrayList<String>> propagatedValue) {

		// Extract info from the program point
		String methodName = pPoint.split("[.]")[0];
		int srcBBNum = Integer.parseInt(pPoint.split("[.]")[2]);

		CGNode node = hashGlobalMethods.get(analysisClass + methodName);
		SSACFG cfg = node.getIR().getControlFlowGraph();
		BasicBlock srcBB = cfg.getBasicBlock(srcBBNum);

		// Get the list of all the successor basicBlocks
		Collection<ISSABasicBlock> succBB = cfg.getNormalSuccessors(srcBB);

		// Operator used on the conditional
		String op = inst.getOperator().toString();

		boolean hasNullConstant = false;

		// Iterate over all the variables to check if NULL
		int numOfUses = inst.getNumberOfUses();
		for (int i = 0; i < numOfUses; i++) {
			int varNum = inst.getUse(i);
			String varStr = Integer.toString(varNum);

			if (node.getIR().getSymbolTable().isNullConstant(varNum)) {
				// Add this NULL Constant into the propagated value
				propagatedValue.put(varStr, new ArrayList<String>(Arrays.asList("null")));
				hasNullConstant = true;
			}
		}

		// Check if it is an object comparison. If TRUE, then Deterministic IF,
		// else Non-Deterministic IF
		if (inst.isObjectComparison() == true && hasNullConstant == true) {

			// Get the variable numbers
			int var1 = inst.getUse(0);
			int var2 = inst.getUse(1);
			String var1Str = Integer.toString(var1);
			String var2Str = Integer.toString(var2);

			// Assumption is that the NULL variable is always the second
			// variable
			// If that fails, make var1 as var2 and var1str as var2str
			//
			// Check if first variable is the null constant
			ArrayList<String> tempSwap = propagatedValue.get(var1Str);
			if (tempSwap.size() == 1 && tempSwap.contains("null")) {

				// Swap int and String representing var1 and var2
				int tempInt = var1;
				var1 = var2;
				var2 = tempInt;

				String tempStr = var1Str;
				var1Str = var2Str;
				var2Str = tempStr;
			}

			HashMap<String, ArrayList<String>> trueBranch = new HashMap<String, ArrayList<String>>(propagatedValue);
			HashMap<String, ArrayList<String>> falseBranch = new HashMap<String, ArrayList<String>>(propagatedValue);

			ArrayList<String> v1PointsTo = propagatedValue.get(var1Str);
			ArrayList<String> v2PointsTo = propagatedValue.get(var2Str);

			// Get TRUE and FALSE basicBlocks
			int trueInst = inst.getTarget();
			BasicBlock trueBB = node.getIR().getControlFlowGraph().getBlockForInstruction(trueInst);
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

			// When the pointsTo set is singleton
			if (v1PointsTo.size() == 1 && v2PointsTo.size() == 1) {
				boolean contains = propagatedValue.get(var1Str).containsAll(propagatedValue.get(var2Str));

				// Check if the condition is satisfied
				// System.out.println(contains);
				if (((op.equals("ne") && contains == false)) || (op.equals("eq") && contains == true)) {
					// Condition is satisfied
					// Set false branch to BOT
					data.setToBOT(falseSuccPP, column);
					boolean changed = false;
					changed = data.propagate(trueSuccPP, column, trueBranch);
					if (changed)
						workingList.add(trueSuccPP);
				} else {
					// Condition failed
					// Set true branch to BOT
					data.setToBOT(trueSuccPP, column);
					boolean changed = false;
					changed = data.propagate(falseSuccPP, column, falseBranch);
					if (changed)
						workingList.add(falseSuccPP);
				}
			} else {

				// If not singleton, then send the INTERSECTION to the == branch
				// and ID to != branch
				if (op.equals("ne")) {

					falseBranch.put(var1Str, new ArrayList<String>(Arrays.asList("null")));

					v1PointsTo.remove("null");
					trueBranch.put(var1Str, v1PointsTo);

					boolean changed = false;
					changed = data.propagate(falseSuccPP, column, falseBranch);
					if (changed)
						workingList.add(falseSuccPP);
					// System.out.println("SUcc: " + falseSuccPP);
					// d.displayProgramPointUnderCol(falseSuccPP, column);

					changed = false;
					changed = data.propagate(trueSuccPP, column, trueBranch);
					if (changed)
						workingList.add(trueSuccPP);

				} else if (op.equals("eq")) {

					trueBranch.put(var1Str, new ArrayList<String>(Arrays.asList("null")));

					v1PointsTo.remove("null");
					falseBranch.put(var1Str, v1PointsTo);

					boolean changed = false;
					changed = data.propagate(falseSuccPP, column, falseBranch);
					if (changed)
						workingList.add(falseSuccPP);
					// System.out.println("SUcc: " + falseSuccPP);
					// d.displayProgramPointUnderCol(falseSuccPP, column);

					changed = false;
					changed = data.propagate(trueSuccPP, column, trueBranch);
					if (changed)
						workingList.add(trueSuccPP);

				}
			}
		} else {
			for (ISSABasicBlock succ : succBB) {
				String succPP = methodName + "." + srcBB.getNumber() + "." + succ.getNumber();
				boolean changed = false;
				changed = data.propagate(succPP, column, propagatedValue);
				if (changed)
					workingList.add(succPP);
			}
		}

		return;
	}
}
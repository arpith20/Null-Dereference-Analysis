package PAVpointerAnalysisPackage;

// Do NOT import the pointer analysis package
	import java.io.*;
	import java.util.*;

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
	import com.ibm.wala.ssa.SSACFG;
	import com.ibm.wala.ssa.SSAGetInstruction;
	import com.ibm.wala.ssa.SSAInstruction;
	import com.ibm.wala.ssa.SSAPutInstruction;
	import com.ibm.wala.types.FieldReference;
	import com.ibm.wala.util.CancelException;
	import com.ibm.wala.util.config.AnalysisScopeReader;
	import com.ibm.wala.util.io.FileProvider;
	import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;

	public class SetUpAnalysis {
		
		private String classpath;
		private String mainClass;
		private String analysisClass;
		private String analysisMethod;
		
		
		//START: NO CHANGE REGION
		private AnalysisScope scope;	// scope defines the set of files to be analyzed
		private ClassHierarchy ch;		// Generate the class hierarchy for the scope
		private Iterable<Entrypoint> entrypoints;	// In the call graph, these are the entrypoint nodes
		private AnalysisOptions options;	// Specify options for the call graph builder
		private CallGraphBuilder builder;	// Builder object for the call graph
		private CallGraph cg;			// Call graph for the program
		
		
		public SetUpAnalysis(String classpath, String mainClass, String analysisClass, String analysisMethod)
		{
			this.classpath = classpath;
			this.mainClass = mainClass;
			this.analysisClass = analysisClass;
			this.analysisMethod = analysisMethod;
	
			
		}
		
		/**
		 * Defines the scope of analysis: identifies the classes for analysis
		 * @throws Exception
		 */
		public void buildScope() throws Exception
		{
			FileProvider f = new FileProvider();
			scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(classpath, f.getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
		}
		
		/**
		 * Builds the hierarchy among the classes to be analyzed: if B extends A, then A is a superclass of B, etc
		 * @throws Exception
		 */
		public void buildClassHierarchy() throws Exception
		{
			ch = ClassHierarchy.make(scope);
		}
		
		/**
		 * The nodes of a call graph are methods. This method defines the "entry points" in the call graph.
		 * Note: The entry point may not necessarily be the main method.  
		 */
		public void buildEntryPoints()
		{
			entrypoints = Util.makeMainEntrypoints(scope, ch, mainClass);
		}
		
		/**
		 * Options to build the required call graph.
		 */
		public void setUpCallGraphConstruction()
		{
			options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
			builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), ch, scope);
		}
		
		/**
		 * Build the call graph.
		 * @throws Exception
		 */
		public void generateCallGraph() throws Exception
		{
			cg = builder.makeCallGraph(options, null);
		}
		//END: NO CHANGE REGION
		
		/**
		 * These are methods for testing purposes. You can call to these to check whether Wala is setup properly.
		 * This method prints the nodes of the call graph.
		 */
		public void printNodes()
		{
			System.out.println("Displaying Application's Call Graph nodes: ");
			Iterator<CGNode> nodes = cg.iterator();
		
			// Printout the nodes in the call-graph
			while(nodes.hasNext()) {
				String nodeInfo = nodes.next().toString();
				if(nodeInfo.contains("Application"))
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
			while(nodes.hasNext()) {
				CGNode node = nodes.next();
				String nodeInfo = node.toString();
				if(nodeInfo.contains(analysisClass) && nodeInfo.contains(analysisMethod)) {
					target = node;
					break;
				}
			}
			if(target!=null) {
				System.out.println("The IR of method " + target.getMethod().getSignature() + " is:");
				System.out.println(target.getIR().toString());
			} else {
				System.out.println("The given method in the given class could not be found");
			}
		}
		
		/**
		 * Here, you need to fill in code to complete the rest of the analysis workflow.
		 * see project presentation and the write-up for details
		 */
	}


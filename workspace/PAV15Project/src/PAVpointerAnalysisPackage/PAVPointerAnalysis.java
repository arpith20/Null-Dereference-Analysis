package PAVpointerAnalysisPackage;

/**
 * @author Arpith K, Sridhar G
 * Program Analysis and Verification, 2015
 *
 */

//Do NOT import the slicer package
import java.io.*;
import java.util.*;

import PAVpointerAnalysisPackage.PAVPointerAnalysis;
import PAVpointerAnalysisPackage.SetUpAnalysis;
import PAVpointerAnalysisPackage.NullDereference;

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

public class PAVPointerAnalysis {

	private SetUpAnalysis setup; // Object to setup (check presentation)
	public NullDereference nulldereference;

	public PAVPointerAnalysis(String classpath, String mainClass, String analysisClass, String analysisMethod) {
		setup = new SetUpAnalysis(classpath, mainClass, analysisClass, analysisMethod);
	}

	public void runAnalysis() throws Exception {
		/**
		 * Use setup to construct the relevant data structures (cfg, etc). and
		 * to perform analysis Print out analysis results to file, in the format
		 * specified.
		 */

		// START: NO CHANGE REGION
		setup.buildScope();
		setup.buildClassHierarchy();
		setup.buildEntryPoints();
		setup.setUpCallGraphConstruction();
		setup.generateCallGraph();
		// END: NO CHANGE REGION

		setup.init() ;
		setup.getProgramPoints();
	}

	// START: NO CHANGE REGION
	/**
	 * The main method reads in the arguments from the command line and sets up
	 * the appropriate variables.
	 * 
	 * @param args
	 *            args[0]: Full path of the application jar file. args[1]: Fully
	 *            qualified name of the class containing the main method. The
	 *            name is of the form L<package_name>/<class_name>. For example,
	 *            to analyze the class test1 in package tests, the argument
	 *            would be Ltests/test1 args[2]: Fully qualified name of the
	 *            class containing the method which we want to analyze. args[3]:
	 *            Name of the method we want to analyze.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String classpath, mainClass, analysisClass, analysisMethod;

		classpath = args[0];
		mainClass = args[1];
		analysisClass = args[2];
		analysisMethod = args[3];

		PAVPointerAnalysis pAnalysis = new PAVPointerAnalysis(classpath, mainClass, analysisClass, analysisMethod);

		pAnalysis.runAnalysis();

	}
	// END: NO CHANGE REGION

}

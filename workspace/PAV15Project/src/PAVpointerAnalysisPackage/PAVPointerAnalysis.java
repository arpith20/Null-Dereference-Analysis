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

import PAVpointerAnalysisPackage.PAVPointerAnalysis;
import PAVpointerAnalysisPackage.SetUpAnalysis;

public class PAVPointerAnalysis {

	private SetUpAnalysis setup;

	public PAVPointerAnalysis(String classpath, String mainClass, String analysisClass, String analysisMethod,
			String join) {
		setup = new SetUpAnalysis(classpath, mainClass, analysisClass, analysisMethod, join);
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

		// Initializes the things needed for the analysis
		// FLAGS for writing to the file and to display the JOIN output is also
		// in this method
		setup.init();

		// Creates program points in the methods which we will analyze
		setup.getProgramPoints();

		// Initialize the element D0 on top of which the analysis runs
		setup.setD0();

		// MAIN ALGORITHM
		// This is where the magic happens
		// The kildall iterator which analyzes the methods and stores the
		// results
		setup.kildall();

		return;
	}

	/**
	 * The main method reads in the arguments from the command line and sets up
	 * the appropriate variables.
	 * 
	 * @param args
	 *            args[0]: Full path of the application jar file.
	 * 
	 *            args[1]: Fully qualified name of the class containing the main
	 *            method. The name is of the form L<package_name>/<class_name>.
	 *            For example, to analyze the class test1 in package tests, the
	 *            argument would be Ltests/test1.
	 * 
	 *            args[2]: Fully qualified name of the class containing the
	 *            method which we want to analyze.
	 * 
	 *            args[3]: Name of the method we want to analyze.
	 * 
	 *            args[4]: [OPTIONAL] If "join" is given as the 5th argument,
	 *            then the analysis produces the JOINOUTPUT. Else, TABLEOUTPUT
	 *            is generated.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		String classpath, mainClass, analysisClass, analysisMethod, join;

		classpath = args[0];
		mainClass = args[1];
		analysisClass = args[2];
		analysisMethod = args[3];

		if (args.length == 5)
			join = args[4];
		else
			join = "table";

		PAVPointerAnalysis pAnalysis = new PAVPointerAnalysis(classpath, mainClass, analysisClass, analysisMethod,
				join);

		pAnalysis.runAnalysis();

		return;
	}
}
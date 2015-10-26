package PAVpointerAnalysisPackage;

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

public class NullDereference {

	void entry() {
		System.out.println("this is a successful test");
//		Data d = new Data();
//		d.add_to_var("1", "test");
//		d.add_to_var("1", "test2");
//		d.add_to_var("1", "test3");
//		d.add_to_var("2", "test4");
//		d.add_to_var("2", "test5");
//
//		// d.remove("2");
//		d.get("1");
//		d.get("2");
//		d.join("3", "1", "2");
//		d.get("3");
//
//		d.mark("3");
//		d.unmark("3");
//		if (d.marked("3"))
//			d.get("3");
//		d.remove("3", "test2");
//		d.get("3");
//
//		d.add_to_var("3", "null");
//		d.get("3");
//		d.removenull("3");
//		d.get("3");
//
//		d.add_to_var("1", "test");
//		d.add_to_var("1", "test2");
//		d.add_to_var("1", "test3");
//		d.add_to_var("2", "test4");
//		d.add_to_var("2", "test5");
//		
//		d.print("2");
		Data d = new Data();
		//d.add("BB0", 0, "v1", null);
		//d.add("BB0", 0, "v1", "");
//		d.add("BB0", 0, "v1", "v2");
//		d.add("BB0", 0, "v1", "v1");
//		d.add("BB0 -> BB1", 0, "v2", "v1");
//		d.add("BB0", 0, "v2", "v2");
//		d.add("BB0", 0, "v2", "v2");
//		d.add("BB1", 0, "v2", "v3");
//		d.add("BB1", 0, "v2", "v2");
//		d.add("BB1", 1, "v2", "v1");
//		d.add("BB1", 1, "v2", "v1");
//		
//		d.display();
//		d.remove("BB0", 0, "v1", "null");
//		d.display();
//		
//		d.join("BB3", 0, "BB0", 0, "BB1", 0);
		d.display();
//		
//		d.equals("v1", "v2");
//		
//		d.retrive("BB0", 0, "v1");
		d.add("0", 0, "1", "1");
		d.add("0", 0, "1", "2");
		//d.add("0", 0, "2", "2");
		d.add("1", 0, "1", "1");
		d.add("1", 0, "1", "2");
		d.add("1", 0, "1", "3");
		d.display();
		//d.checkAllColumnsUnmarked("BB#");
		//d.contains(pp1, col1, pp2, col2)
		System.out.println(d.contains("1",0, "0",0));
		HashMap<String, ArrayList<Integer>> hm = new HashMap<String, ArrayList<Integer>>();
		hm.put("test", null);
		System.out.println(hm.get("test"));

	}

}

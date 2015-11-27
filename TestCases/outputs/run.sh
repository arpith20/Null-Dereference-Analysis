# Shell scripts to run the test cases
#
# USAGE:
# ./check.sh TEST_NUMBER
#		This will run the TEST_NUMBER testcase and tests if both the JOIN and the TABLE outputs are correct
#
#./check.sh
#		This will run ALL the tests that are present in ALLTESTS variable
#
# NOTE 1: Before running the script you should have the JAR file containing all the testcases exported in the "jar" folder as "test.jar"
# NOTE 2: While exporting the JAR file, export along with the source file as well
#
# To get the command to run the analysis, do the following in Eclipse:
# Window => Perspective => Open Perspective => Debug
# Right click on the previous run of the program => Properties
# The command will be present under the 'Command line' column
#
# The index of the testcases are taken in the variable ALLTESTS. Change the array to add more testcases
# If the 5th argument for the analysis is given as "join", the analysis outputs the JOIN OUTPUT. If anything other than "join" is specified, including "", TABLE OUTPUT will be generated.
#

allTests="1 2 3 4 5 6 7"

if [ $# -eq 1 ]
then
	cur_tests=$1
else
	cur_tests=$allTests
fi

for type in table join
do
	if [ "$type" = "join" ]
		then
			file=JoinOutput
		else
			file=TableOutput
		fi

	echo "Checking $file"

	for num in $cur_tests
	do
		printf "Executing Test$num..."
	
		/usr/lib/jvm/java-7-openjdk-amd64/bin/java -Dfile.encoding=UTF-8 -classpath /home/sridhar/Documents/pav_project/PAV_project/workspace/PAV15Project/bin:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.util/bin:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.shrike/bin:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.core/bin:/home/sridhar/.p2/pool/plugins/org.eclipse.core.runtime_3.11.0.v20150405-1723.jar:/home/sridhar/.p2/pool/plugins/javax.annotation_1.2.0.v201401042248.jar:/home/sridhar/.p2/pool/plugins/javax.inject_1.0.0.v20091030.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.osgi_3.10.100.v20150529-1857.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.osgi.compatibility.state_1.0.100.v20150402-1551.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.equinox.common_3.7.0.v20150402-1709.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.core.jobs_3.7.0.v20150330-2103.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.core.runtime.compatibility.registry_3.6.0.v20150318-1505/runtime_registry_compatibility.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.equinox.registry_3.6.0.v20150318-1503.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.equinox.app_1.3.300.v20150423-1356.jar:/home/sridhar/.p2/pool/plugins/org.junit_4.12.0.v201504281640/junit.jar:/home/sridhar/.p2/pool/plugins/org.hamcrest.core_1.3.0.v201303031735.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-antlr.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-bcel.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-bsf.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-log4j.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-oro.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-regexp.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-resolver.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-apache-xalan2.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-commons-logging.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-commons-net.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-jai.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-javamail.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-jdepend.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-jmf.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-jsch.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-junit.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-junit4.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-launcher.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-netrexx.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-swing.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant-testutil.jar:/home/sridhar/.p2/pool/plugins/org.apache.ant_1.9.4.v201504302020/lib/ant.jar:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.core.tests/bin:/home/sridhar/.p2/pool/plugins/org.eclipse.pde.core_3.10.100.v20150522-0332.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jface_3.11.0.v20150602-1400.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.swt_3.104.0.v20150528-0211.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.swt.gtk.linux.x86_64_3.104.0.v20150528-0211.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.core.commands_3.7.0.v20150422-0725.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.ui_3.107.0.v20150507-1945.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.ui.workbench_3.107.0.v20150510-1732.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.e4.ui.workbench3_0.13.0.v20150422-0725.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.core.resources_3.10.0.v20150423-0755.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jdt.core_3.11.0.v20150602-1242.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jdt.compiler.apt_1.2.0.v20150514-0146.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jdt.compiler.tool_1.1.0.v20150513-2007.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.ui.ide_3.11.0.v20150510-1749.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jdt_3.11.0.v20150603-2000.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.jdt.launching_3.8.0.v20150527-0946.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.ui.views_3.8.0.v20150422-0725.jar:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.ide/bin:/home/sridhar/.p2/pool/plugins/org.eclipse.osgi.services_3.5.0.v20150519-2006.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.e4.ui.model.workbench_1.1.100.v20150407-1430.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.e4.core.di_1.5.0.v20150421-2214.jar:/home/sridhar/.p2/pool/plugins/org.eclipse.e4.core.di.annotations_1.4.0.v20150528-1451.jar:/home/sridhar/Documents/pav_project/PAV_project/workspace/com.ibm.wala.ide.tests/bin:/home/sridhar/Documents/pav_project/PAV_project/workspace/PAV15Project/lib/commons-lang3-3.4.jar PAVpointerAnalysisPackage.PAVPointerAnalysis /home/sridhar/Documents/pav_project/PAV_project/jar/test.jar LTestCases/PublicTests LTestCases/Test$num startTest\(\)V $type > test$num$type.txt

		diff test$num$type.txt Test$num/$file.txt > test$num${type}diff.txt
		if [ $? -eq 0 ]
		then
			echo "pass"
			rm test$num$type.txt
			rm test$num${type}diff.txt
		else
			echo "Testcase $num failed in $type"
		fi
	done

	printf "\n"
done

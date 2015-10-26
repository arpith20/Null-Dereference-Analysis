package TestCases;

/**
 * @author Shyam S
 */

public class SampleTests {

	public void main(String[] args) {
		SampleTests v1 = new SampleTests();
		SampleTests v2 = null;
		SampleTests v3 = v1;
		SampleTests v4 = v2;
		SampleTests v5 = new SampleTests();
		if (v5 == v1)
			v5 = v4;
		else
			v5 = v3;
		v2 = v5;
		foo(v1);
	}
	
	public void ifTest(SampleTests test1) {
		if(test1==null)
			test1 = new SampleTests();
		test1.toString();
        return;
    }

	public void foo(SampleTests v1) {
		SampleTests v2 = null;
		SampleTests v3 = v1;
		SampleTests v4 = v2;
		SampleTests v5 = new SampleTests();
		if (v1 == null)
			v5 = v4;
		else
			v2 = v3;
		v2 = v5;
	}
}
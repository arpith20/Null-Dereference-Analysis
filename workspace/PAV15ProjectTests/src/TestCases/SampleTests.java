package TestCases;

/**
 * @author Shyam S
 */

public class SampleTests {
	
	public static void p(int i) {
		SampleTests t1 = new SampleTests();
		SampleTests t2 = new SampleTests();
		SampleTests t3 = null;
		if(i>10) {
			t3 = t2;
		} else {
			t3 = t1;
		}
		t3.toString();
	}
	
	public static void foo(int i) {
		SampleTests t1 = new SampleTests();
		SampleTests t2 = new SampleTests();
		SampleTests t3 = null;
		p(10);
		if(i>10) {
			t3 = t2;
		} else {
			t3 = t1;
		}
		t3.toString();
	}
	
	public static void bar(SampleTests t1) {
		SampleTests t2 = new SampleTests();
		SampleTests t3 = null;
		if(t1==null) {
			t3 = t2;
		} else {
			t3 = t1;
		}
		t3.toString();
		bar(t1);
		foo(10);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		foo(10);
		bar(null);
	}

}
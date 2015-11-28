package TestCases;

public class SampleTest2 {
	SampleTests t;
	SampleTests t2;

	public static void foo3() {
		SampleTest2 t1 = new SampleTest2();
		t1.t = new SampleTests();
		SampleTest2 t3 = new SampleTest2();
		bar3(t3);
		t1.t.toString();
	}

	static void bar3(SampleTest2 t2) {
		t2.t = null;
		t2.t2 = null;
		SampleTest2 t3 = new SampleTest2();
		t3.t = null;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		foo3();
		//bar(null);
	}
}

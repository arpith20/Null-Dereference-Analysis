package TestCases;

public class SampleTest2 {
	SampleTests t;

	public static void foo3() {
		SampleTest2 t1 = new SampleTest2();
		t1.t = new SampleTests();
		bar3(t1);
		t1.t.toString();
	}

	static void bar3(SampleTest2 t2) {
		t2.t = null;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		foo3();
		//bar(null);
	}
}

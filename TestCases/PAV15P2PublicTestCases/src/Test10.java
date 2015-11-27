package TestCases;

public class Test10 {
	Test3 t2;
	private void foo() {
		t2.toString();		//should be a null deref
		t2 = new Test3();
	}
	
	public void startTest() {
		Test t1 = new Test();
		t1.t = null;
		t1.t.toString();  	//should be a null deref
		t2 = null;
		foo();
		t2.toString();		//should not be a null deref
	}
	
	class Test {
		Test3 t;
	}
	
}

package TestCases;

public class Test5 {
	
	public PublicTests iterativeReturnTest(PublicTests t1, PublicTests t2, boolean flag) {
		if(flag) {
			return t1;
		} else {
			return t2;
		}
	}
	
	public void startTest() {
		PublicTests t1 = new PublicTests();
		PublicTests t2 = new PublicTests();
		PublicTests t3 = iterativeReturnTest(t1, t2, true);
		PublicTests t4 = iterativeReturnTest(t1, t2, false);
		t3.toString();
		t4.toString();
	}
}

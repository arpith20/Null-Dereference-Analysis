package TestCases;

public class Test1 {
	
	public void ifTest(PublicTests test1) {
		if(test1==null)
			test1 = new PublicTests();
		test1.toString();
        return;
    }
	
	public void startTest() {
		PublicTests t1 = null;
        ifTest(t1);
        t1.toString();
	}
}

package TestCases;

public class Test3 {
	
	public PublicTests createNewClass(int x) {
		if(x>5 && x<=10)
			return new PublicTests();
		else if (x>10 && x<=20)
			return new PublicTests();
		else 
			return null;
	}
	
	public PublicTests loopTest(PublicTests t1) {
		PublicTests t2 = null;
		for(int x = 0; x<t1.i; x++)
			t2 = createNewClass(x);			
		return t2;
	}
	
	public void startTest() {
		PublicTests t = new PublicTests();
		t.i = 10;
		PublicTests res = loopTest(t);
		res.toString();
	}
}

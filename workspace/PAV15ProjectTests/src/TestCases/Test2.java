package TestCases;

public class Test2 {

	public PublicTests returnTest(int i){
		if (i%2==0)
			return null;
        else 
        	return new PublicTests();
    }
	
	public void startTest() {
		PublicTests t2 = new PublicTests();
        t2.toString();
        t2 = returnTest(t2.i);
        t2.toString();
	}
	
}

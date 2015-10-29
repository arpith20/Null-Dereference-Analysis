package TestCases;

public class Test4 {
	
	public PublicTests phiTest(PublicTests t1, PublicTests t2) {
		PublicTests ret;
		PublicTests t3 = new PublicTests();
		PublicTests t4 = new PublicTests();
		PublicTests t5 = new PublicTests();
		PublicTests t6 = new PublicTests();
		if(t1 == null) {
			if(t2 != null) {
				ret = t3;
			} else {
				ret = t4;
			}
		} else {
			if(t2 == null) {
				ret = t5;
			} else {
				ret = t6;
			}
		}
		ret.toString();
		return ret;
	}
	
	public PublicTests iterativeTest(int i) {
		PublicTests ret;
		switch(i) {
		case 0:
			ret = phiTest(null,null);
			break;
		case 1:
			PublicTests t1 = new PublicTests();
			ret = phiTest(t1,null);
			break;
		default:
			PublicTests t2 = new PublicTests();
			PublicTests t3 = new PublicTests();
			ret = phiTest(t2,t3);
			break;
		} 
		return ret;
	}
	
	public void startTest() {
		iterativeTest(0);
	}
}

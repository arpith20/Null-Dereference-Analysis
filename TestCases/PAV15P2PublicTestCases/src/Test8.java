package TestCases;

public class Test8 {
	Test8 t;
	int i;
	public void startTest(){
		Test8 t8=new Test8();
		t8.t=new Test8();
		calltest(t8.t);
		t8.t.toString(); //Should not be a null deref
		}
	private void calltest(Test8 t2) {
		if(i%2==0){
		   t2=null;
		}
		t2.toString();// Should be a null deref
	}
}



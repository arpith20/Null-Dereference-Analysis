package TestCases;

/**
 * @author Shyam S
 */

public class SampleTests {
	int a, b, t;
	public static void p() {
		if(a==0){
			a=a-1;
			p();
			t=a*b;
		}else{
			return;
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
		t = a*b;
		p();
	}

}

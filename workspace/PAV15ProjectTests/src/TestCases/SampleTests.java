package TestCases;

/**
 * @author Shyam S
 */

public class SampleTests {
	
	public static void main(String[] args) {
		SampleTests v1 = new SampleTests() ;
		SampleTests v2 = null ;
		SampleTests v3 = v1 ;
		SampleTests v4 = v2 ;
		SampleTests v5 = new SampleTests() ;
		v1 = v4 ;
	}
	
	public void foo ()
	{
		SampleTests v1 = new SampleTests() ;
		SampleTests v2 = null ;
		SampleTests v3 = v1 ;
		SampleTests v4 = v2 ;
		SampleTests v5 = new SampleTests() ;
		v1 = v4 ;
	}

}
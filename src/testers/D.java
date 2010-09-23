package testers;

public class D extends C{

	protected int d;
	public static void main(String[] argv){
		D lala=new D();
		lala.doD();
		lala.a=new C().c;
	}
	
	public void doD(){
		doC();
	}
}

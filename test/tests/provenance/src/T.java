public class T {
	C c;
	public static void main(String args[]){
		T t1 = new T();
		C c = new C();
		t1.c = c;
		C c1 = t1.c;
		c1.m();
	}
	
	public void put(C c){
		this.c = c;
	}
	public C get(){
		return this.c;
	}

	public void impossi(C c){
		c.m();
	}
}


class B{
	C c;
}

class C{
	Object o = new Object();
	void m(){}
}

class C1 extends C{
	void m(){}
}

class C2 extends C{
	protected void f(){}
}

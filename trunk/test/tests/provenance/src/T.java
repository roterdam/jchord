public class T {
	C c;
	public static void main(String args[]){
		T t1 = new T();
		T t2 = new T();
		C c1 = new C();
		t1.put(c1);
		C c2 = new C1();
		t2.put(c2);
		C c3 = t1.get();
		c3.m();
	}
	void put(C c){
		this.c = c;
	}
	C get(){
		return c;
	}
}

class C{
	void m(){System.out.println("C");}
}

class C1 extends C{
	void m(){System.out.println("C1");}
}

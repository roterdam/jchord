public class T {
	C c;
	public static void main(String args[]){
		T t1 = new T();
		t1.id(t1);
		t1.id(t1);
		t1.id(t1);
	}
	void put(C c){
		this.c = c;
	}
	C get(){
		return c;
	}
	void impossi(){
		c.m();
	}
	T id(T t){
		T ret = id2(t);
		return ret;
	}
	T id2(T t){
		T ret = t;
		return ret;
	}
}

class C{
	void m(){System.out.println("C");}
}

class C1 extends C{
	void m(){System.out.println("C1");}
}

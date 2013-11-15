public class T {
	C c;
	public static void main(String args[]){
		T t1 = new T();
		t1.id(t1);
		t1.id(t1);
		t1.id(t1);
		t1.recur(t1,6);
		t1.loop(t1);
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
	
	T recur(T t, int i){
		if(i >0){
			T temp = t;
			return recur(temp,i-1);
		}
		else{
			T temp = t;
			return temp;
			}
	}
	
	T loop(T t){
		T current = t;
		T next = null;
		for(int i = 0;i < 1000;i++){
			next = current;
			current = next;
		}
		return next;
	}
}

class C{
	void m(){System.out.println("C");}
}

class C1 extends C{
	void m(){System.out.println("C1");}
}

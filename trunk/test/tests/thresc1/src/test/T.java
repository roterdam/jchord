package test;

public class T {
	static {
		(new Thread() { public void run() { }}).start();
	}
	static C g;
	public static void main(String[] args) {
		A a = new A();
		if (args == null) {
			B b = new B();
			a.bf = b;
		} else {
			B b = new B();
			C c = new C();
 			D d = new D();
			a.bf = b;	// loc by ap, aw
			a.cf = c;	// loc by ap, aw
			b.df = d;	// loc by ap, aw
			c.df = d;	// loc by ap, aw
			g = c;
		}
		int i, j;
		{
			B b = a.bf;	// loc by ap, aw
			D d = b.df;	// loc by ap, esc by aw
			i = d.i;	// esc by ap
		}
		{
			C c = a.cf;	// loc by ap, aw
			D d = c.df;	// esc by ap
			j = d.i;	// esc by ap
		}
		System.out.println(i + j);
	}
}

class A {
	B bf;
	C cf;
}

class B {
	D df;
}

class C {
	D df;
}

class D {
	int i;
}


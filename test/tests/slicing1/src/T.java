public class T {
	static A g;
	static int g2;
    public static void main(String[] a) {
		A v1 = new A();	
		A v2 = new A();	
		v1.f1 = v2;		
		int x = v2.f2;		
		g = v1;
		int y = v2.f2;		
		A v4;
		if (a != null) {
			A v3 = new A();	
			v4 = new A();	
			v3.f1 = v4;		
		} else
			v4 = new A();	
		int z = v4.f2;		
		g2 = z;
	}
}

class A {
	A f1;
	int f2;
}

public class T {
	static int g;
    public static void main(String[] args) {
		A a = new A();
		g = a.f;
	}
}

class A {
	int f;
	public A() {
		this.f = 5;
	}
}


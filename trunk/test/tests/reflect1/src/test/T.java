package test;

public class T {
    public static void main(String[] a) throws Exception {
		Class x = Class.forName("test.A");
		Object o = x.newInstance();
		C b = (C) o;
		b.foo();
	}
}


package test;

public class T {
    public static void main(String[] a) throws Exception {
/*
		Object[][] array;
		int n1, n2;
		if (a != null) {
			n1 = 20;
			n2 = 40;
			array = new Object[20][40];
		} else {
			n1 = 10;
			n2 = 30;
			array = new Object[10][30];
		}
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < n2; j++) 
				array[i][j] = new Object();
		}
		System.out.println(T.class);
		a.getClass();
		System.out.println("a" + a[0] + "ccc");
*/
		Class x = Class.forName("test.A");
		Object o = x.newInstance();
		C b = (C) o;
		b.foo();
	}
}


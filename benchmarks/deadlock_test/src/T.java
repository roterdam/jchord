import java.lang.Thread;

public class T {
	private static Object l1 = new Object();
	private static Object l2 = new Object();
	public static void main(String[] args) {
		// thrd 1
		Thread thrd1 = new Thread() {
			public void run() {
				foo();
			}
		};
		thrd1.start();

		// thrd 2
		Thread thrd2 = new Thread() {
			public void run() {
				bar();
			}
		};
		thrd2.start();
	}
	private static void foo() {
		synchronized (l1) {
			baz();
		}
	}
	private static void baz() {
		synchronized (l2) {
			// ...
		}
	}
	private static void bar() {
		synchronized (l2) {
			taz();
		}
	}
	private static void taz() {
		synchronized (l1) {
			// ...
		}
	}
}


package conf_analyzer.stubs;

/*
 * Holds Java classes to be used as analyzable models
 */
public class JavaStubs {

	public static void stubThreadInit(Thread t, Runnable r) {
		r.run();
	}
	
	public static Object anIdentityFunc(Object o) {
		return o;
	}
	
	public static Object aNullReturn(Object o) {
		return null;
	}

	
}

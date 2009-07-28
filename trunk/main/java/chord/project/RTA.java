package chord.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Main.HostedVM;

public class RTA {
	private static Set<jq_Method> reachableMethods = new HashSet<jq_Method>();
	private static List<jq_Method> worklist = new ArrayList<jq_Method>();
	private static Set<jq_Class> reachableAllocTypes = new HashSet<jq_Class>();
	public static Set<jq_Method> getReachableMethods(String mainClassName) {
        HostedVM.initialize();
        jq_Class c = (jq_Class) jq_Type.parseType(mainClassName);
        c.prepare();
        jq_NameAndDesc sign = new jq_NameAndDesc("main", "([Ljava/lang/String;)V");
        jq_Method mainMethod = c.getDeclaredStaticMethod(sign);
        if (mainMethod == null)
            throw new RuntimeException("Class " + c + " lacks a main method");
        addMethod(mainMethod);
        while (!worklist.isEmpty()) {
        	jq_Method m = worklist.remove(worklist.size() - 1);
        	processMethod(m);
        }
        return reachableMethods;
	}
	private static void addMethod(jq_Method m) {
		if (reachableMethods.add(m)) {
			worklist.add(m);
		}
	}
	private static void processMethod(jq_Method m) {
    	// find all allocs in m and add them to reachableAllocTypes
		
	}
}

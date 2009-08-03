package chord.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chord.util.Timer;
import chord.util.IndexHashSet;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Member;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Main.HostedVM;
import joeq.Util.Templates.ListIterator;

public class RTA {
	public static final boolean DEBUG = true;
	private IndexHashSet<jq_Class> preparedClasses =
		new IndexHashSet<jq_Class>();
	private IndexHashSet<jq_Class> reachableAllocClasses =
		new IndexHashSet<jq_Class>();
	// all classes whose clinits and super class/interface clinits
	// have been processed so far in current interation
	private Set<jq_Class> classesAddedForClinit = new HashSet<jq_Class>();
	// all methods deemed reachable so far in current iteration
	private IndexHashSet<jq_Method> seenMethods =
		new IndexHashSet<jq_Method>();
	// worklist for methods seen so far in current iteration but
	// whose cfg's haven't been processed yet
	private List<jq_Method> todoMethods = new ArrayList<jq_Method>();
	private boolean repeat = true;
	private jq_Class javaLangObject;
	public IndexHashSet<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public IndexHashSet<jq_Method> getReachableMethods() {
		return seenMethods;
	}
	public void run(String mainClassName) {
		System.out.println("ENTER: RTA");
		Timer timer = new Timer();
		timer.init();
        HostedVM.initialize();
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
        jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
        jq_NameAndDesc sign = new jq_NameAndDesc("main", "([Ljava/lang/String;)V");
	    prepareClass(mainClass);
        jq_Method mainMethod = mainClass.getDeclaredStaticMethod(sign);
        if (mainMethod == null) {
            throw new RuntimeException("No main method in class " + mainClass);
        }
		for (int i = 0; repeat; i++) {
			if (DEBUG) System.out.println("Iteration: " + i);
			repeat = false;
         	classesAddedForClinit.clear();
        	seenMethods.clear();
            handleClinits(mainClass);
        	handleSeenMethod(mainMethod);
	        while (!todoMethods.isEmpty()) {
	        	jq_Method m = todoMethods.remove(todoMethods.size() - 1);
	        	handleTodoMethod(m);
	        }
        }
		System.out.println("LEAVE: RTA");
		timer.done();
		System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	private void handleSeenMethod(jq_Method m) {
		assert m.isPrepared() : "Method " + m + " in class " +
			m.getDeclaringClass() + " is not prepared";
		if (seenMethods.add(m)) {
			if (DEBUG) System.out.println("\tAdding method: " + m);
			if (!m.isAbstract() && !m.isNative()) {
				todoMethods.add(m);
			}
		}
	}
	private void handleTodoMethod(jq_Method m) {
		if (DEBUG) System.out.println("ENTER handleTodoMethod: " + m);
		ControlFlowGraph cfg = CodeCache.getCode(m);
        for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
        		it.hasNext();) {
        	BasicBlock bb = it.nextBasicBlock();
        	for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
        		Quad q = it2.nextQuad();
        		Operator op = q.getOperator();
        		if (op instanceof Invoke) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Method n = Invoke.getMethod(q).getMethod();
					jq_Class c = n.getDeclaringClass();
					if (op instanceof InvokeVirtual) {
						jq_NameAndDesc nad = n.getNameAndDesc();
						for (jq_Class d : reachableAllocClasses) {
							if (d.extendsClass(c)) {
								n = d.getVirtualMethod(nad);
								assert (n != null);
								handleSeenMethod(n);
							}
						}
					} else if (op instanceof InvokeInterface) {
						jq_NameAndDesc nad = n.getNameAndDesc();
						for (jq_Class d : reachableAllocClasses) {
							if (d.implementsInterface(c)) {
								n = d.getVirtualMethod(nad);
								assert (n != null);
								handleSeenMethod(n);
							}
						}
					} else {
						assert (op instanceof InvokeStatic);
						handleClass(c);
						handleSeenMethod(n);
					}
        		} else if (op instanceof Getstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
        			jq_Class c = Getstatic.getField(q).
						getField().getDeclaringClass();
        			handleClass(c);
        		} else if (op instanceof Putstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
        			jq_Class c = Putstatic.getField(q).
						getField().getDeclaringClass();
        			handleClass(c);
        		} else if (op instanceof New) {
					if (DEBUG) System.out.println("Quad: " + q);
        			jq_Class c = (jq_Class) New.getType(q).getType();
        			handleClass(c);
        			if (reachableAllocClasses.add(c)) {
						if (DEBUG) System.out.println("Setting repeat");
        				repeat = true;
					}
        		}
        	}
        }
		if (DEBUG) System.out.println("LEAVE handleTodoMethod: " + m);
	}

	private void prepareClass(jq_Class c) {
		if (preparedClasses.add(c)) {
	        c.prepare();
			if (DEBUG) System.out.println("\tAdding class: " + c);
			jq_Class d = c.getSuperclass();
			if (d == null)
        		assert (c == javaLangObject);
			else
				prepareClass(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				prepareClass(i);
		}
	}

	private void handleClass(jq_Class c) {
		prepareClass(c);
		handleClinits(c);
	}

	private void handleClinits(jq_Class c) {
		if (classesAddedForClinit.add(c)) {
			jq_ClassInitializer m = c.getClassInitializer();
			// m is null for classes without class initializer method
			if (m != null)
				handleSeenMethod(m);
			jq_Class d = c.getSuperclass();
			if (d != null)
				handleClinits(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				handleClinits(i);
		}
	}
}

package chord.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chord.util.Assertions;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
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
	// classes/interfaces loaded explicitly
	private Set<jq_Class> loadedClasses = new HashSet<jq_Class>();
	private Set<jq_Class> reachableAllocClasses = new HashSet<jq_Class>();
	// all classes whose clinits and super class/interface clinits
	// have been processed so far in current interation
	private Set<jq_Class> classesAddedForClinit = new HashSet<jq_Class>();
	// all methods deemed reachable so far in current iteration
	private Set<jq_Method> seenMethods = new HashSet<jq_Method>();
	// worklist for methods seen so far in current iteration but
	// whose cfg's haven't been processed yet
	private List<jq_Method> todoMethods = new ArrayList<jq_Method>();
	private boolean repeat;
	private jq_Class javaLangObject;
	public Set<jq_Class> getLoadedClasses() {
		return loadedClasses;
	}
	public Set<jq_Method> getReachableMethods() {
		return seenMethods;
	}
	public void run(String mainClassName) {
        HostedVM.initialize();
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
        jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
        jq_NameAndDesc sign = new jq_NameAndDesc("main",
        	"([Ljava/lang/String;)V");
        jq_Method mainMethod = mainClass.getDeclaredStaticMethod(sign);
        if (mainMethod == null) {
            throw new RuntimeException("Class " + mainClass +
            	" lacks a main method");
        }
        do {
         	classesAddedForClinit.clear();
        	seenMethods.clear();
            loadClass(mainClass);
        	handleSeenMethod(mainMethod);
	        while (!todoMethods.isEmpty()) {
	        	jq_Method m = todoMethods.remove(todoMethods.size() - 1);
	        	handleTodoMethod(m);
	        }
        } while (repeat);
	}
	private void handleSeenMethod(jq_Method m) {
		if (seenMethods.add(m) && !m.isAbstract() && !m.isNative())
			todoMethods.add(m);
	}
	private void handleTodoMethod(jq_Method m) {
		ControlFlowGraph cfg = CodeCache.getCode(m);
        for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
        		it.hasNext();) {
        	BasicBlock bb = it.nextBasicBlock();
        	for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
        		Quad q = it2.nextQuad();
        		Operator op = q.getOperator();
        		if (op instanceof InvokeVirtual) {
                    jq_Method n = Invoke.getMethod(q).getMethod();
                    handleSeenMethod(n);
                    jq_Class c = n.getDeclaringClass();
                    jq_NameAndDesc nad = n.getNameAndDesc();
    	            for (jq_Class d : reachableAllocClasses) {
    	            	if (d.extendsClass(c)) {
    	            		n = d.getVirtualMethod(nad);
    	                	Assertions.Assert(n != null);
    	                	handleSeenMethod(n);
    	            	}
    	            }
        		} else if (op instanceof InvokeInterface) {
                    jq_Method n = Invoke.getMethod(q).getMethod();
                    handleSeenMethod(n);
                    jq_Class c = n.getDeclaringClass();
                    jq_NameAndDesc nad = n.getNameAndDesc();
    	            for (jq_Class d : reachableAllocClasses) {
    	                if (d.implementsInterface(c)) {
    	                	n = d.getVirtualMethod(nad);
    	                	Assertions.Assert(n != null);
    	                	handleSeenMethod(n);
    	                }
    	            }
        		} else if (op instanceof InvokeStatic) {
                    jq_Method n = Invoke.getMethod(q).getMethod();
                    handleSeenMethod(n);
                    if (n.isStatic())
                    	loadClass(n.getDeclaringClass());
        		} else if (op instanceof Getstatic) {
        			jq_Class c = Getstatic.getField(q).
        				getField().getDeclaringClass();
        			loadClass(c);
        		} else if (op instanceof Putstatic) {
        			jq_Class c = Putstatic.getField(q).
        				getField().getDeclaringClass();
        			loadClass(c);
        		} else if (op instanceof New) {
        			jq_Class c = (jq_Class) New.getType(q).getType();
        			loadClass(c);
        			if (reachableAllocClasses.add(c))
        				repeat = true;
        		} else if (op instanceof NewArray) {
        			jq_Class c = (jq_Class) NewArray.getType(q).getType();
        			loadClass(c);
        			if (reachableAllocClasses.add(c))
        				repeat = true;
        		}
        	}
        }
	}
	private void loadClass(jq_Class c) {
		if (loadedClasses.add(c))
	        c.prepare();
		handleClinits(c);
	}
	private void handleClinits(jq_Class c) {
		if (classesAddedForClinit.contains(c))
			return;
		Assertions.Assert(c.isPrepared());
        jq_ClassInitializer m = c.getClassInitializer();
        // m is null for classes without class initializer method
        if (m != null)
        	handleSeenMethod(m);
        jq_Class d = c.getSuperclass();
        if (d == null)
        	Assertions.Assert(c == javaLangObject);
        else
            handleClinits(d);
        jq_Class[] interfaces = c.getDeclaredInterfaces();
        for (jq_Class i : interfaces)
        	handleClinits(i);
		classesAddedForClinit.add(c);
	}
}

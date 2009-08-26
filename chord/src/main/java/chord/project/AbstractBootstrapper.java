/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chord.util.IndexSet;
import chord.util.Timer;
import chord.util.IndexHashSet;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Main.HostedVM;
import joeq.Util.Templates.ListIterator;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class AbstractBootstrapper implements IBootstrapper {
	public static final boolean DEBUG = false;
	protected IndexHashSet<jq_Class> preparedClasses =
		new IndexHashSet<jq_Class>();
	// all classes whose clinits and super class/interface clinits have
	// been processed so far in current interation
	protected Set<jq_Class> classesAddedForClinit = new HashSet<jq_Class>();
	// all methods deemed reachable so far in current iteration
	protected IndexHashSet<jq_Method> seenMethods =
		new IndexHashSet<jq_Method>();
	// worklist for methods seen so far in current iteration but
	// whose cfg's haven't been processed yet
	protected List<jq_Method> todoMethods = new ArrayList<jq_Method>();
	protected boolean repeat = true;
	protected jq_Class javaLangObject;
	public IndexHashSet<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public IndexHashSet<jq_Method> getReachableMethods() {
		return seenMethods;
	}
	public void run(List<String> rootMethodSigns) {
		System.out.println("ENTER: bootstrapper");
		Timer timer = new Timer();
		timer.init();
        HostedVM.initialize();
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
		List<jq_Method> rootMethods =
			new ArrayList<jq_Method>(rootMethodSigns.size()); 
		for (String s : rootMethodSigns) {
            int colonIdx = s.indexOf(':');
            int atIdx = s.indexOf('@');
            String mName = s.substring(0, colonIdx);
            String mDesc = s.substring(colonIdx + 1, atIdx);
            String cName = s.substring(atIdx + 1);
        	jq_Class klass = (jq_Class) jq_Type.parseType(cName);
			prepareClass(klass);
			jq_NameAndDesc sign = new jq_NameAndDesc(mName, mDesc);
        	jq_Method method = (jq_Method) klass.getDeclaredMember(sign);
			assert (method != null);
			rootMethods.add(method);
		}
		for (int i = 0; repeat; i++) {
			if (DEBUG) System.out.println("Iteration: " + i);
			repeat = false;
         	classesAddedForClinit.clear();
        	seenMethods.clear();
			for (jq_Method rootMethod : rootMethods) {
				jq_Class c = rootMethod.getDeclaringClass();
				handleClinits(c);
        		handleSeenMethod(rootMethod);
			}
	        while (!todoMethods.isEmpty()) {
	        	jq_Method m = todoMethods.remove(todoMethods.size() - 1);
	        	handleTodoMethod(m);
	        }
        }
		System.out.println("LEAVE: bootstrapper");
		timer.done();
		System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	
	protected void processNew(jq_Class c) { }

	protected void handleSeenMethod(jq_Method m) {
		assert m.isPrepared() : "Method " + m + " is not prepared";
		if (seenMethods.add(m)) {
			if (DEBUG) System.out.println("\tAdding method: " + m);
			if (!m.isAbstract()) {
				todoMethods.add(m);
			}
		}
	}
	
	protected void handleTodoMethod(final jq_Method m) {
		if (DEBUG) System.out.println("ENTER handleTodoMethod: " + m);
		ControlFlowGraph cfg = m.getCFG();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
       			it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					if (DEBUG) System.out.println("Quad: " + q);
					MethodOperand mo = Invoke.getMethod(q);
					mo.resolve();
					jq_Method n = mo.getMethod();
					jq_Class c = n.getDeclaringClass();
					handleClass(c);
					handleSeenMethod(n);
					if (op instanceof InvokeVirtual) {
						assert (!c.isInterface());
						IndexSet<jq_InstanceMethod> targets =
							getTargetsOfInvokeVirtual((jq_InstanceMethod) n);
						for (jq_InstanceMethod o : targets)
							handleSeenMethod(o);
					} else if (op instanceof InvokeInterface) {
						assert (c.isInterface());
						IndexSet<jq_InstanceMethod> targets =
							getTargetsOfInvokeInterface((jq_InstanceMethod) n);
						for (jq_InstanceMethod o : targets)
							handleSeenMethod(o);
					} else
						assert (op instanceof InvokeStatic);
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
					processNew(c);
				}
			}
		}
		if (DEBUG) System.out.println("LEAVE handleTodoMethod: " + m);
	}

	protected void prepareClass(jq_Class c) {
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

	protected void handleClass(jq_Class c) {
		prepareClass(c);
		handleClinits(c);
	}

	protected void handleClinits(jq_Class c) {
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

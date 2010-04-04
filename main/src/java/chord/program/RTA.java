/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Field;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Main.HostedVM;
import joeq.Util.Templates.ListIterator;
import chord.project.Properties;
import chord.util.ChordRuntimeException;
import chord.util.IndexHashSet;
import chord.util.Timer;

/**
 * Rapid Type Analysis algorithm for computing program scope
 * (reachable classes and methods).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class RTA implements IBootstrapper {
	public static final boolean DEBUG = false;
	private static final boolean ENABLE_REFLECTION_SUPPORT = false;
    private IndexHashSet<jq_Class> reachableAllocClasses =
        new IndexHashSet<jq_Class>();
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
	protected ClassHierarchy cha;
	public IndexHashSet<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public IndexHashSet<jq_Method> getReachableMethods() {
		return seenMethods;
	}
	public void run() {
		System.out.println("ENTER: bootstrapper");
		Timer timer = new Timer();
		timer.init();
        HostedVM.initialize();
        if (ENABLE_REFLECTION_SUPPORT) {
        	cha = ClassHierarchy.load();
        }
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
		String mainClassName = Properties.mainClassName;
		if (mainClassName == null) {
			throw new ChordRuntimeException("Main class is not defined " +
				"(use system property chord.main.class)");
		}
       	jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
		prepareClass(mainClass);
		jq_NameAndDesc nd = new jq_NameAndDesc("main", "([Ljava/lang/String;)V");
        jq_Method mainMethod = (jq_Method) mainClass.getDeclaredMember(nd);
		assert (mainMethod != null);
		for (int i = 0; repeat; i++) {
			System.out.println("Iteration: " + i);
			repeat = false;
			initPass();
         	classesAddedForClinit.clear();
        	seenMethods.clear();
			handleClinits(mainClass);
        	handleSeenMethod(mainMethod);
	        while (!todoMethods.isEmpty()) {
	        	jq_Method m = todoMethods.remove(todoMethods.size() - 1);
	        	handleTodoMethod(m);
	        }
			donePass();
        }
		System.out.println("LEAVE: bootstrapper");
		timer.done();
		System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	
	protected void initPass() { }
	protected void donePass() { }

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
					jq_Method n = Invoke.getMethod(q).getMethod();
					jq_Class c = n.getDeclaringClass();
					handleClass(c);
					handleSeenMethod(n);
					if (op instanceof InvokeVirtual ||
							op instanceof InvokeInterface) {
						jq_NameAndDesc nd = n.getNameAndDesc();
						if (c.isInterface()) {
							for (jq_Class d : reachableAllocClasses) {
								assert (!d.isInterface());
								assert (!d.isAbstract());
								if (d.implementsInterface(c)) {
									jq_InstanceMethod m2 = d.getVirtualMethod(nd);
									assert (m2 != null);
									handleSeenMethod(m2);
								}
							}
						} else {
							for (jq_Class d : reachableAllocClasses) {
								assert (!d.isInterface());
								assert (!d.isAbstract());
								if (d.extendsClass(c)) {
									jq_InstanceMethod m2 = d.getVirtualMethod(nd);
									assert (m2 != null);
									handleSeenMethod(m2);
								}
							}
						}
					} else
						assert (op instanceof InvokeStatic);
				} else if (op instanceof CheckCast) {
					if (ENABLE_REFLECTION_SUPPORT) {
						// We optimistically assume that check-cast operations succeed.
						if (DEBUG) System.out.println("Quad: " + q);
						TypeOperand typeOperand = CheckCast.getType(q);
						if (typeOperand != null) {
							jq_Type type = typeOperand.getType();
							if (type instanceof jq_Class) {
								Set<jq_Class> classes = getConcreteClassesRootedAt((jq_Class) type);
								for (jq_Class klass : classes) {
									handleClass(klass);
								}
							}
						}
					}
				} else if (op instanceof Getstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Field f = Getstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					handleClass(c);
				} else if (op instanceof Putstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Field f = Putstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					handleClass(c);
				} else if (op instanceof New) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Class c = (jq_Class) New.getType(q).getType();
					handleClass(c);
					if (reachableAllocClasses.add(c)) {
						if (DEBUG) System.out.println("repeat set");
						repeat = true;
					}
				}
			}
		}
		if (DEBUG) System.out.println("LEAVE handleTodoMethod: " + m);
	}

	private Set<jq_Class> getConcreteClassesRootedAt(jq_Class klass) {
		Set<jq_Class> result = new HashSet<jq_Class>(2);
		addIfEligible(result, klass);
		if (klass.isInterface()) {
			for (jq_Class c : cha.getImplementors(klass)) {
				addIfEligible(result, c);
			}
		} else {
			for (jq_Class c : cha.getSubclasses(klass)) {
				addIfEligible(result, c);
			}
		}
		return result;
	}
	
	private void addIfEligible(Set<jq_Class> concreteClasses, jq_Class c) {
		prepareClass(c);
		if (!c.isAbstract() && !c.isInterface()) {
			concreteClasses.add(c);
		}
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

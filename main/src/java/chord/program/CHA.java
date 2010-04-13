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
import chord.project.Messages;
import chord.util.ChordRuntimeException;
import chord.util.IndexHashSet;
import chord.util.Timer;

/**
 * Class Hierarchy Analysis algorithm for computing program scope
 * (reachable classes and methods).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CHA implements IBootstrapper {
	public static final boolean DEBUG = false;
	private IndexHashSet<jq_Class> preparedClasses = new IndexHashSet<jq_Class>();
	// all classes whose clinits and super class/interface clinits have been
	// processed so far in current interation
	private Set<jq_Class> classesVisitedForClinit = new HashSet<jq_Class>();
	// all methods deemed reachable so far in current iteration
	private IndexHashSet<jq_Method> visitedMethods = new IndexHashSet<jq_Method>();
	// worklist for methods seen so far in current iteration but whose cfg's
	// haven't been processed yet
	private List<jq_Method> methodWorklist = new ArrayList<jq_Method>();
	private jq_Class javaLangObject;
	private ClassHierarchyBuilder chb;
	public IndexHashSet<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public IndexHashSet<jq_Method> getReachableMethods() {
		return visitedMethods;
	}
	public void run() {
		System.out.println("ENTER: CHA");
		Timer timer = new Timer();
		timer.init();
        HostedVM.initialize();
       	chb = new ClassHierarchyBuilder();
		chb.run();
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
		String mainClassName = Properties.mainClassName;
		if (mainClassName == null)
			Messages.fatal("SCOPE.MAIN_CLASS_NOT_DEFINED");
       	jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
		prepareClass(mainClass);
		jq_NameAndDesc nd = new jq_NameAndDesc("main", "([Ljava/lang/String;)V");
        jq_Method mainMethod = (jq_Method) mainClass.getDeclaredMember(nd);
		if (mainMethod == null)
            Messages.fatal("SCOPE.MAIN_METHOD_NOT_FOUND", mainClassName);
		visitClinits(mainClass);
       	visitMethod(mainMethod);
        while (!methodWorklist.isEmpty()) {
        	jq_Method m = methodWorklist.remove(methodWorklist.size() - 1);
			ControlFlowGraph cfg = m.getCFG();
			if (DEBUG) System.out.println("Processing CFG of method: " + m);
			processCFG(cfg);
        }
		System.out.println("LEAVE: CHA");
		timer.done();
		System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	
	private void visitMethod(jq_Method m) {
		if (visitedMethods.add(m)) {
			if (!m.isAbstract()) {
				if (DEBUG) System.out.println("\tAdding method " + m + " to worklist");
				methodWorklist.add(m);
			}
		}
	}

	private void processCFG(ControlFlowGraph cfg) {
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Method n = Invoke.getMethod(q).getMethod();
					jq_Class c = n.getDeclaringClass();
					visitClass(c);
					visitMethod(n);
					if (op instanceof InvokeVirtual || op instanceof InvokeInterface) {
						jq_NameAndDesc nd = n.getNameAndDesc();
						String cName = c.getName();
						if (c.isInterface()) {
							Set<String> implementors = chb.getConcreteImplementors(cName);
							for (String dName : implementors) {
								jq_Class d = (jq_Class) jq_Type.parseType(dName);
								visitClass(d);
								assert (!d.isInterface());
								assert (!d.isAbstract());
								jq_InstanceMethod m2 = d.getVirtualMethod(nd);
								assert (m2 != null);
								visitMethod(m2);
							}
						} else {
							Set<String> subclasses = chb.getConcreteSubclasses(cName);
							for (String dName : subclasses) {
								jq_Class d = (jq_Class) jq_Type.parseType(dName);
								visitClass(d);
								assert (!d.isInterface());
								assert (!d.isAbstract());
								jq_InstanceMethod m2 = d.getVirtualMethod(nd);
								assert (m2 != null);
								visitMethod(m2);
							}
						}
					} else
						assert (op instanceof InvokeStatic);
				} else if (op instanceof Getstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Field f = Getstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof Putstatic) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Field f = Putstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof New) {
					if (DEBUG) System.out.println("Quad: " + q);
					jq_Class c = (jq_Class) New.getType(q).getType();
					visitClass(c);
				}
			}
		}
	}

	private void prepareClass(jq_Class c) {
		if (preparedClasses.contains(c))
			return;
		try {
        	c.prepare();
		} catch (NoClassDefFoundError ex) {
			Messages.log("SCOPE.EXCLUDING_CLASS", c.getName());
			ex.printStackTrace();
			System.exit(1);
		}
		preparedClasses.add(c);
		if (DEBUG) System.out.println("\tAdding class: " + c);
		jq_Class d = c.getSuperclass();
		if (d == null)
			assert (c == javaLangObject);
		else
			prepareClass(d);
		for (jq_Class i : c.getDeclaredInterfaces())
			prepareClass(i);
	}

	private void visitClass(jq_Class c) {
		prepareClass(c);
		visitClinits(c);
	}

	private void visitClinits(jq_Class c) {
		if (classesVisitedForClinit.add(c)) {
			jq_ClassInitializer m = c.getClassInitializer();
			// m is null for classes without class initializer method
			if (m != null)
				visitMethod(m);
			jq_Class d = c.getSuperclass();
			if (d != null)
				visitClinits(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				visitClinits(i);
		}
	}
}

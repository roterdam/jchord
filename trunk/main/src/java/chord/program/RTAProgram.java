/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Array;
import joeq.Class.jq_Class;
import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Field;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Type;
import joeq.Class.Classpath;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Main.HostedVM;
import joeq.Util.Templates.ListIterator;

import chord.project.Messages;
import chord.project.Config;
import chord.util.IndexSet;
import chord.util.Timer;
import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

/**
 * Rapid Type Analysis algorithm for computing program scope
 * (reachable classes and methods).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Omer Tripp (omertripp@post.tau.ac.il)
 */
public class RTAProgram extends Program {
	private static final String MAIN_CLASS_NOT_DEFINED = "ERROR: Property chord.main.class must be set to specify the main class of program to be analyzed.";
	private static final String MAIN_METHOD_NOT_FOUND = "ERROR: Could not find main class `%s` or main method in that class.";
	private static final String METHOD_NOT_FOUND_IN_SUBTYPE = "WARN: Expected instance method %s in class %s implementing/extending interface/class %s.";

    public static final boolean DEBUG = false;

	// Flag enabling inference of the class loaded by calls to
	// <code>Class.forName(s)</code>; the analysis achieves this by
	// intra-procedurally tracking flow of string constants to "s".
	private final boolean handleForNameReflection;

    // Flag enabling inference of the type of objects allocated
	// reflectively by calls to <code>v.newInstance()</code> (where
	// "v" is of type java.lang.Class); the analysis achieves this
	// by inter-procedurally tracking the flow of "v" from each such
	// call site to a subsequent cast of the type of "v", and
	// regards each concrete subtype of the cast type as a possible
	// type of the reflectively allocated object; the analysis
	// consults a statically or dynamically built class hierarchy
	// to find subtypes.
	private final boolean handleNewInstReflection;

	// Flag set to true if either or both of handleNewInstReflection
	// and handleForNameReflection is true.
	private final boolean handleReflection;

	/////////////////////////

	/*
	 * Data structures used only if handleForNameReflection is true
	 */

	private ForNameReflectionAnalyzer forNameReflectionAnalyzer;

	// methods in which forName sites have already been analyzed
	private Set<jq_Method> forNameAnalyzedMethods;

	/////////////////////////

	/*
	 * Data structures used only if handleNewInstReflection is true
	 */

    // Program class hierarchy built either statically or dynamically.
    private ClassHierarchy ch;

    // Map from each method to all its call sites encountered so far.
    private Map<jq_Method, Set<Quad>> methToInvks;

    // Set of local variables to which the return value of some call
	// to java.lang.Class.newInstance() is assigned either directly,
	// or transitively via move/phi/invk statements, until a checkcast
	// statement is found.
    private Set<Register> newInstVars;

    // Set of methods containing a "return v" statement where v is in
    // set newInstVars.
    private Set<jq_Method> reflectRetMeths;

	/////////////////////////

	/*
	 * Data structures reset after every iteration.
	 */

	// Set of all classes whose clinits and super class/interface clinits
	// have been processed so far in current interation; this set is kept
	// to avoid repeatedly visiting super classes/interfaces within an
	// iteration (which incurs a huge runtime penalty) only to find that
 	// all their clinits have already been processed in that iteration.
	private Set<jq_Class> classesVisitedForClinit;

	// Set of all methods deemed reachable so far in current iteration.
	private IndexSet<jq_Method> methods;

	/////////////////////////

	/*
	 * Persistent data structures (not reset after iterations).
	 */

	private ReflectInfo reflectInfo;

	// set of all classes deemed reachable so far
	private IndexSet<jq_Reference> classes;

	// set of all (concrete) classes deemed instantiated so far either
	// by a reachable new/newarray statement or due to reflection
    private IndexSet<jq_Reference> reachableAllocClasses;

	// worklist for methods seen so far in current iteration but whose
	// CFGs haven't been processed yet
	private List<jq_Method> methodWorklist;

	// handle to the representation of class java.lang.Object
	private jq_Class javaLangObject;

	// flag indicating that another iteration is needed; it is set if
	// any of the following sets grows in the current iteration:
	// reachableAllocClasses, newInstVars, reflectRetMeths
	private boolean repeat = true;

	public RTAProgram(boolean _handleForNameReflection,
					  boolean _handleNewInstReflection) {
		handleForNameReflection = _handleForNameReflection;
		handleNewInstReflection = _handleNewInstReflection;
		handleReflection = _handleNewInstReflection ||
			_handleForNameReflection;
	}
	@Override
	protected IndexSet<jq_Method> computeMethods() {
		if (methods == null)
			build();
		return methods;
	}
	@Override
	protected ReflectInfo computeReflectInfo() {
		if (reflectInfo == null)
			build();
		return reflectInfo;
	}
	private void build() {
		if (Config.verbose > 1) System.out.println("ENTER: RTA");
		Timer timer = new Timer();
		timer.init();
		if (handleForNameReflection) {
			Classpath cp = PrimordialClassLoader.loader.getClasspath();
			forNameReflectionAnalyzer = new ForNameReflectionAnalyzer(cp);
			forNameAnalyzedMethods = new HashSet<jq_Method>();
		}
		if (handleNewInstReflection) {
            ch = Program.g().getClassHierarchy();
            methToInvks = new HashMap<jq_Method, Set<Quad>>();
			newInstVars = new HashSet<Register>();
            reflectRetMeths = new HashSet<jq_Method>();
		}
 		classes = new IndexSet<jq_Reference>();
 		classesVisitedForClinit = new HashSet<jq_Class>();
 		reachableAllocClasses = new IndexSet<jq_Reference>();
 		methods = new IndexSet<jq_Method>();
		methodWorklist = new ArrayList<jq_Method>();
		reflectInfo = new ReflectInfo();
        HostedVM.initialize();
        javaLangObject = PrimordialClassLoader.getJavaLangObject();
		String mainClassName = Config.mainClassName;
		if (mainClassName == null)
            Messages.fatal(MAIN_CLASS_NOT_DEFINED);
       	jq_Class mainClass = (jq_Class) jq_Type.parseType(mainClassName);
		prepareClass(mainClass);
        jq_Method mainMethod = (jq_Method) mainClass.getDeclaredMember(
			new jq_NameAndDesc("main", "([Ljava/lang/String;)V"));
		if (mainMethod == null)
			Messages.fatal(MAIN_METHOD_NOT_FOUND, mainClassName);
		for (int i = 0; repeat; i++) {
			if (Config.verbose > 1) System.out.println("Iteration: " + i);
			repeat = false;
         	classesVisitedForClinit.clear();
        	methods.clear();
			visitClinits(mainClass);
        	visitMethod(mainMethod);
	        while (!methodWorklist.isEmpty()) {
				int n = methodWorklist.size();
	        	jq_Method m = methodWorklist.remove(n - 1);
				if (DEBUG) System.out.println("Processing CFG of " + m);
	        	processMethod(m);
	        }
        }
		if (Config.verbose > 1) System.out.println("LEAVE: RTA");
		timer.done();
		if (Config.verbose > 1)
			System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	private void visitMethod(jq_Method m) {
		if (methods.add(m)) {
			if (DEBUG) System.out.println("\tAdding method: " + m);
			if (!m.isAbstract()) {
				methodWorklist.add(m);
			}
		}
	}
	private void processMethod(jq_Method m) {
		if (handleForNameReflection && forNameAnalyzedMethods.add(m)) {
			forNameReflectionAnalyzer.run(m);
			Set<Pair<Quad, jq_Reference>> resolvedForNameSites =
				forNameReflectionAnalyzer.getResolvedForNameSites();
			Set<Pair<Quad, jq_Reference>> resolvedNewInstSites =
				forNameReflectionAnalyzer.getResolvedNewInstSites();
			for (Pair<Quad, jq_Reference> p : resolvedForNameSites) {
				jq_Reference r = p.val1;
				reflectInfo.addResolvedForNameSite(p.val0, r);
				visitClass(r);
			}
			for (Pair<Quad, jq_Reference> p : resolvedNewInstSites) {
				jq_Reference r = p.val1;
				reflectInfo.addResolvedNewInstSite(p.val0, r);
				addReflectClass(r);
			}
		}
		ControlFlowGraph cfg = m.getCFG();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
				it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				if (DEBUG) System.out.println("Quad: " + q);
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					if (op instanceof InvokeVirtual || op instanceof InvokeInterface)
						processVirtualInvk(m, q);
					else
						processStaticInvk(m, q);
				} else if (op instanceof Getstatic) {
					jq_Field f = Getstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof Putstatic) {
					jq_Field f = Putstatic.getField(q).getField();
					jq_Class c = f.getDeclaringClass();
					visitClass(c);
				} else if (op instanceof New) {
					jq_Class c = (jq_Class) New.getType(q).getType();
					visitClass(c);
					if (reachableAllocClasses.add(c))
						repeat = true;
				} else if (op instanceof NewArray) {
					jq_Array a = (jq_Array) NewArray.getType(q).getType();
					visitClass(a);
					if (reachableAllocClasses.add(a))
						repeat = true;
				} else if (op instanceof Move) {
					Operand ro = Move.getSrc(q);
					if (ro instanceof AConstOperand) {
						Object c = ((AConstOperand) ro).getValue();
						if (c instanceof Class) {
							String s = ((Class) c).getName();
							// s is in encoded form only if it is an array type
							if (s.startsWith("["))
								s = Program.typesToStr(s);
							jq_Reference d = (jq_Reference) jq_Type.parseType(s);
							visitClass(d);
						}
					}
				}
				if (handleNewInstReflection) {
					if (op instanceof Move) {
						processMove(q);
					} else if (op instanceof Phi) {
						processPhi(q);
					} else if (op instanceof CheckCast) {
						processCast(q);
					} else if (op instanceof Return) {
						Operand ro = Return.getSrc(q);
						if (ro instanceof RegisterOperand) {
							Register r = ((RegisterOperand) ro).getRegister();
							if (newInstVars.contains(r) && reflectRetMeths.add(m)) 
								repeat = true;
						}
					}
				}
			}
		}
	}
	private void addReflectClass(jq_Reference r) {
		reflectInfo.addReflectClass(r);
		if (reachableAllocClasses.add(r))
			repeat = true;
		if (r instanceof jq_Class) {
			jq_Class c = (jq_Class) r;
			jq_Method n = c.getInitializer(new jq_NameAndDesc("<init>", "()V"));
			if (n != null)
				visitMethod(n);
		}
	}
	private void processVirtualInvk(jq_Method m, Quad q) {
		jq_Method n = Invoke.getMethod(q).getMethod();
		jq_Class c = n.getDeclaringClass();
		visitClass(c);
		visitMethod(n);
		jq_NameAndDesc nd = n.getNameAndDesc();
		if (handleNewInstReflection && c.getName().equals("java.lang.Class") &&
				n.getName().toString().equals("newInstance") &&
				n.getDesc().toString().equals("()Ljava/lang/Object;") &&
				!reflectInfo.getResolvedNewInstSites().contains(q)) {
			Register r = Invoke.getDest(q).getRegister();
			if (newInstVars.add(r))
				repeat = true;
		}
		boolean isInterface = c.isInterface();
		for (jq_Reference r : reachableAllocClasses) {
			if (r instanceof jq_Array)
				continue;
			jq_Class d = (jq_Class) r;
			assert (!d.isInterface());
			assert (!d.isAbstract());
			boolean matches = isInterface ? d.implementsInterface(c) : d.extendsClass(c);
			if (matches) {
				jq_InstanceMethod m2 = d.getVirtualMethod(nd);
				if (m2 == null) {
					Messages.log(METHOD_NOT_FOUND_IN_SUBTYPE,
						nd.toString(), d.getName(), c.getName());
				} else {
					if (handleNewInstReflection)
						propagateReflectArgsAndRet(m, q, m2);
					visitMethod(m2);
				}
			}
		}
	}
	private void processStaticInvk(jq_Method m, Quad q) {
		jq_Method n = Invoke.getMethod(q).getMethod();
		jq_Class c = n.getDeclaringClass();
		visitClass(c);
		visitMethod(n);
		if (handleNewInstReflection)
			propagateReflectArgsAndRet(m, q, n);
	}
	private void propagateReflectArgsAndRet(jq_Method m, Quad q, jq_Method n) {
		ParamListOperand iArgs = Invoke.getParamList(q);
		RegisterOperand lo = Invoke.getDest(q);
		RegisterFactory rf = n.getCFG().getRegisterFactory();
		Set<Quad> invks = methToInvks.get(n);
		if (invks == null) {
			invks = new ArraySet<Quad>(1);
			methToInvks.put(n, invks);
		}
		invks.add(q);
		if (reflectRetMeths.contains(n)) {
			if (lo != null) {
				Register l = lo.getRegister();
				if (newInstVars.add(l))
					repeat = true;
			}
		}
		int k = iArgs.length();
		for (int i = 0; i < k; ++i) {
			Register iArg = iArgs.get(i).getRegister();
			if (newInstVars.contains(iArg)) {
				Register fArg = rf.get(i);
				if (newInstVars.add(fArg)) 
					repeat = true;
			}
		}
	}
	private void processMove(Quad q) {
		Operand ro = Move.getSrc(q);
		if (ro instanceof RegisterOperand) {
			Register r = ((RegisterOperand) ro).getRegister();
			if (newInstVars.contains(r)) {
				Register l = Move.getDest(q).getRegister();
				if (newInstVars.add(l)) 
					repeat = true;
			}
		}
	}
	private void processPhi(Quad q) {
		ParamListOperand roList = Phi.getSrcs(q);
		int n = roList.length();
		for (int i = 0; i < n; i++) {
			RegisterOperand ro = roList.get(i);
			if (ro != null) {
				Register r = ro.getRegister();
				if (newInstVars.contains(r)) {
					Register l = Phi.getDest(q).getRegister();
					if (newInstVars.add(l)) 
						repeat = true;
					break;
				}
			}
		}
	}
    private void processCast(Quad q) {
		Operand ro = CheckCast.getSrc(q);
		if (ro instanceof RegisterOperand) {
			Register r = ((RegisterOperand) ro).getRegister();
			if (newInstVars.contains(r)) {
				jq_Reference t = (jq_Reference) CheckCast.getType(q).getType();
				Set<String> subs = ch.getConcreteSubclasses(t.getName());
				if (subs != null) {
					for (String s : subs) {
						jq_Class d = (jq_Class) jq_Type.parseType(s);
						visitClass(d);
						addReflectClass(d);
					}
				}
			}
		}
    }
	private void prepareClass(jq_Reference r) {
		if (classes.add(r)) {
	        r.prepare();
			if (DEBUG) System.out.println("\tAdding class: " + r);
			if (r instanceof jq_Array)
				return;
			jq_Class c = (jq_Class) r;
			jq_Class d = c.getSuperclass();
			if (d == null)
				assert (c == javaLangObject);
			else
				prepareClass(d);
			for (jq_Class i : c.getDeclaredInterfaces())
				prepareClass(i);
		}
	}
	private void visitClass(jq_Reference r) {
		prepareClass(r);
		if (r instanceof jq_Array)
			return;
		jq_Class c = (jq_Class) r;
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

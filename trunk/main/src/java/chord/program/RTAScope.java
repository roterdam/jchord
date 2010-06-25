/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import chord.project.Properties;
import chord.util.IndexSet;
import chord.util.Timer;
import chord.util.ArraySet;

/**
 * Rapid Type Analysis algorithm for computing program scope
 * (reachable classes and methods).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Omer Tripp (omertripp@post.tau.ac.il)
 */
public class RTAScope implements IScope {
    public static final boolean DEBUG = false;

    // flag determining whether scope construction must analyze calls
    // to Class.newInstance()
	private final boolean handleNewInstReflection;

	// flag determining whether scope construction must analyze calls
	// to Class.forName()
	private final boolean handleForNameReflection;

	private final boolean handleReflection;

	/*
	 * data structures used if handleNewInstReflection or
	 * handleForNameReflection is true.
	 */

	// set of classes loaded/instantiated reflectively
	private IndexSet<jq_Reference> reflectClasses;

	/*
	 * data structures used only if handleNewInstReflection is true
	 */

    // program class hierarchy built either statically or dynamically
    private ClassHierarchy ch;

    // map from each method to all its call sites encountered so far
    private Map<jq_Method, Set<Quad>> methToInvks;

    // set of local variables to which the return value of some call to
    // Class.newInstance() is assigned either directly, or transitively
    // via move/phi/invk statements, until a checkcast statement is found
    private Set<Register> newInstReflectVars;

    // set of methods containing a "return v" statement where v is in
    // set newInstReflectVars
    private Set<jq_Method> reflectRetMeths;

	/*
	 * data structures used only if handleForNameReflection is true
	 */

	// classpath of program being analyzed
	private Classpath classpath;

	// set of local variables flowing to Class.forName() either directly
	// or transitively via move/phi/actual-to-formal copy statements
	private Set<Register> forNameReflectVars;

	// subset of forNameReflectVars whose tracking has stopped because
	// it has become too complicated
	// maintained only to prevent redundant warning messages
	private Set<Register> warnedReflectVars;

	// set of string constants (supposedly class names) that flowed to
	// some Class.forName() call but were not found in the classpath
	// maintained only to prevent redundant warning messages
	private Set<String> warnedReflectClassNames;

	/*
	 * data structures reset after every iteration
	 */

	// set of all classes whose clinits and super class/interface clinits
	// have been processed so far in current interation; this set is kept
	// to avoid repeatedly visiting super classes/interfaces within an
	// iteration (which incurs a huge runtime penalty) only to find that
 	// all their clinits have already been processed in that iteration.
	private Set<jq_Class> classesVisitedForClinit;

	// set of all methods deemed reachable so far in current iteration
	private IndexSet<jq_Method> methods;

	/*
	 * persistent data structures
	 */

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
	// reachableAllocClasses, newInstReflectVars, forNameReflectVars,
	// reflectRetMeths
	private boolean repeat = true;

	public RTAScope(boolean _handleNewInstReflection,
					boolean _handleForNameReflection) {
		handleNewInstReflection = _handleNewInstReflection;
		handleForNameReflection = _handleForNameReflection;
		handleReflection = _handleNewInstReflection || _handleForNameReflection;
	}
	public IndexSet<jq_Method> getMethods() {
		if (methods == null)
			build();
		return methods;
	}
	public IndexSet<jq_Reference> getReflectClasses() {
		if (!handleNewInstReflection && !handleForNameReflection)
			return new IndexSet<jq_Reference>(0);
		if (reflectClasses == null)
			build();
		return reflectClasses;
	}
	private void build() {
		System.out.println("ENTER: RTA");
		Timer timer = new Timer();
		timer.init();
 		classesVisitedForClinit = new HashSet<jq_Class>();
 		methods = new IndexSet<jq_Method>();
 		classes = new IndexSet<jq_Reference>();
 		reachableAllocClasses = new IndexSet<jq_Reference>();
		methodWorklist = new ArrayList<jq_Method>();
		if (handleReflection)
			reflectClasses = new IndexSet<jq_Reference>();
		if (handleNewInstReflection) {
            ch = Program.getProgram().getClassHierarchy();
            methToInvks = new HashMap<jq_Method, Set<Quad>>();
			newInstReflectVars = new HashSet<Register>();
            reflectRetMeths = new HashSet<jq_Method>();
		}
		if (handleForNameReflection) {
			classpath = PrimordialClassLoader.loader.getClasspath();
			forNameReflectVars = new HashSet<Register>();
			warnedReflectVars = new HashSet<Register>();
			warnedReflectClassNames = new HashSet<String>();
		}
        HostedVM.initialize();
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
		for (int i = 0; repeat; i++) {
			System.out.println("Iteration: " + i);
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
		System.out.println("LEAVE: RTA");
		timer.done();
		System.out.println("Time: " + timer.getInclusiveTimeStr());
	}
	private void visitMethod(jq_Method m) {
		if (methods.add(m)) {
			if (DEBUG) System.out.println("\tAdding method: " + m);
			if (!m.isAbstract()) 
				methodWorklist.add(m);
		}
	}
	private void processMethod(jq_Method m) {
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
				} else {
					if (handleNewInstReflection) {
						if (op instanceof Move) {
							processNewInstReflectionInMove(q);
						} else if (op instanceof Phi) {
							processNewInstReflectionInPhi(q);
						} else if (op instanceof CheckCast) {
                      	 	processNewInstReflectionInCast(q);
                   		} else if (op instanceof Return) {
							processNewInstReflectionInRetn(q, m);
                   		}
					}
					if (handleForNameReflection) {
						if (op instanceof Move) {
							processForNameReflectionInMove(q);
						} else if (op instanceof Phi) {
							processForNameReflectionInPhi(q);
						}
					}
				}
			}
		}
	}
	private void processVirtualInvk(jq_Method m, Quad q) {
		jq_Method n = Invoke.getMethod(q).getMethod();
		jq_Class c = n.getDeclaringClass();
		visitClass(c);
		visitMethod(n);
		jq_NameAndDesc nd = n.getNameAndDesc();
		boolean isInterface = c.isInterface();
		if (handleNewInstReflection && !isInterface &&
				c.getName().equals("java.lang.Class") &&
				n.getName().toString().equals("newInstance") &&
				n.getDesc().toString().equals("()Ljava/lang/Object;")) {
			Register r = Invoke.getDest(q).getRegister();
			if (newInstReflectVars.add(r))
				repeat = true;
		}
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
					Messages.log("SCOPE.METHOD_NOT_FOUND_IN_SUBTYPE",
						nd.toString(), d.getName(), c.getName());
				} else {
					if (handleReflection)
						propagateReflectArgsAndRet(m, q, m2, true);
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
		boolean check = false;
		if (handleForNameReflection) {
			if (c.getName().equals("java.lang.Class") &&
				n.getName().toString().equals("forName")) {
				RegisterOperand ro = Invoke.getParamList(q).get(0);
				Register r = ro.getRegister();
				if (forNameReflectVars.add(r))
					repeat = true;
			} else
				check = true;
		}
		if (handleReflection)
			propagateReflectArgsAndRet(m, q, n, check);
	}
	private void propagateReflectArgsAndRet(jq_Method m, Quad q, jq_Method n, boolean check) {
		ParamListOperand iArgs = Invoke.getParamList(q);
		RegisterOperand lo = Invoke.getDest(q);
		RegisterFactory rf = n.getCFG().getRegisterFactory();
		if (handleNewInstReflection) {
			Set<Quad> invks = methToInvks.get(n);
			if (invks == null) {
				invks = new ArraySet<Quad>(1);
				methToInvks.put(n, invks);
			}
			invks.add(q);
			if (reflectRetMeths.contains(n)) {
				if (lo != null) {
					Register l = lo.getRegister();
					if (newInstReflectVars.add(l))
						repeat = true;
				}
			}
			int k = iArgs.length();
			for (int i = 0; i < k; ++i) {
				Register iArg = iArgs.get(i).getRegister();
				if (newInstReflectVars.contains(iArg)) {
					Register fArg = rf.get(i);
					if (newInstReflectVars.add(fArg)) 
						repeat = true;
				}
			}
		}
		if (handleForNameReflection) {
			if (lo != null) {
				Register l = lo.getRegister();
				if (forNameReflectVars.contains(l) && check && warnedReflectVars.add(l)) {
					Messages.log("SCOPE.STOP_TRACKING_REFLECTION",
						l.toString(), m.toString(), q.toString());
				}
			}
			int k = n.getParamTypes().length;
			for (int i = 0; i < k; i++) {
				Register fArg = rf.get(i);
				if (forNameReflectVars.contains(fArg)) {
					Register iArg = iArgs.get(i).getRegister();
					if (forNameReflectVars.add(iArg)) 
						repeat = true;
				}
			}
		}
	}
	private void processNewInstReflectionInMove(Quad q) {
       	Operand ro = Move.getSrc(q);
		if (ro instanceof RegisterOperand) {
			Register r = ((RegisterOperand) ro).getRegister();
			if (newInstReflectVars.contains(r)) {
				Register l = Move.getDest(q).getRegister();
				if (newInstReflectVars.add(l)) 
					repeat = true;
			}
		}
	}
	private void processForNameReflectionInMove(Quad q) {
		Register l = Move.getDest(q).getRegister();
		if (forNameReflectVars.contains(l)) {
			Operand ro = Move.getSrc(q);
			if (ro instanceof RegisterOperand) {
				Register r = ((RegisterOperand) ro).getRegister();
				if (forNameReflectVars.add(r)) 
					repeat = true;
			} else if (ro instanceof AConstOperand) {
				Object v = ((AConstOperand) ro).getValue();
				if (v instanceof String) {
					String clsName = (String) v;
					// check whether class is present in the classpath to avoid
					// a NoClassDefFoundError
					String resName = Classpath.classnameToResource(clsName);
					if (classpath.getResourcePath(resName) == null) {
						if (warnedReflectClassNames.add(clsName))
							Messages.log("SCOPE.DYNAMIC_CLASS_NOT_FOUND", clsName);
					} else {
						jq_Reference r = (jq_Reference) jq_Type.parseType(clsName);
						reflectClasses.add(r);
						visitClass(r);
						// optimistically assume this class will be instantiated
						if (r instanceof jq_Array) {
							if (reachableAllocClasses.add(r))
								repeat = true;
						} else {
							jq_Class c = (jq_Class) r;
							if (!c.isAbstract() && !c.isInterface() &&
									reachableAllocClasses.add(r))
								repeat = true;
							// optimistically assume any method may be called
							for (jq_Method m : c.getDeclaredStaticMethods())
								visitMethod(m);
							for (jq_Method m : c.getDeclaredInstanceMethods()) 
								visitMethod(m);
						}
					}
				}
			}
		}
	}
	private void processNewInstReflectionInPhi(Quad q) {
		ParamListOperand roList = Phi.getSrcs(q);
		int n = roList.length();
		for (int i = 0; i < n; i++) {
			RegisterOperand ro = roList.get(i);
			if (ro != null) {
				Register r = ro.getRegister();
				if (newInstReflectVars.contains(r)) {
					Register l = Phi.getDest(q).getRegister();
					if (newInstReflectVars.add(l)) 
						repeat = true;
					break;
				}
			}
		}
	}
	private void processForNameReflectionInPhi(Quad q) {
		Register l = Phi.getDest(q).getRegister();
		if (forNameReflectVars.contains(l)) {
			ParamListOperand roList = Phi.getSrcs(q);
			int n = roList.length();
			for (int i = 0; i < n; i++) {
				RegisterOperand ro = roList.get(i);
				if (ro != null) {
					Register r = ro.getRegister();
					if (forNameReflectVars.add(r)) {
						repeat = true;
					}
				}
			}
		}
	}
    private void processNewInstReflectionInCast(Quad q) {
        jq_Type t = CheckCast.getType(q).getType();
        if (t instanceof jq_Reference) {
            Operand ro = CheckCast.getSrc(q);
            if (ro instanceof RegisterOperand) {
                Register r = ((RegisterOperand) ro).getRegister();
                if (newInstReflectVars.contains(r)) {
                    String tName = t.getName();
                    jq_Reference ref = (jq_Reference) t;
                    // Note: ref may not be prepared; don't call any methods on it
                    Set<String> concreteImps = ch.getConcreteImplementors(tName);
                    Set<String> concreteSubs = ch.getConcreteSubclasses(tName);
                    assert (concreteImps == null || concreteSubs == null);
                    visitClass(ref);
                    processConcreteClasses(concreteImps);
                    processConcreteClasses(concreteSubs);
                }
            }
        }
    }
    // set may be null
    private void processConcreteClasses(Set<String> set) {
        if (set != null) {
            for (String s : set) {
                jq_Class d = (jq_Class) jq_Type.parseType(s);
                reflectClasses.add(d);
                visitClass(d);
                if (reachableAllocClasses.add(d))
                    repeat = true;
            }
        }
    }
    private void processNewInstReflectionInRetn(Quad q, jq_Method m) {
        Operand ro = Return.getSrc(q);
        if (ro instanceof RegisterOperand) {
            Register r = ((RegisterOperand) ro).getRegister();
            if (newInstReflectVars.contains(r) && reflectRetMeths.add(m)) 
                repeat = true;
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

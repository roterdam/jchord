/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.java2html.Java2HTML;

import chord.util.IndexHashSet;
 
import joeq.Util.Templates.ListIterator;
import joeq.UTF.Utf8;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.SSA.EnterSSA;
import joeq.Main.Helper;

/**
 * Representation of a Java program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Program {
	private static boolean isInited;
	private static IndexHashSet<jq_Class> preparedClasses;
	private static IndexHashSet<jq_Method> reachableMethods;
	private static IndexHashSet<jq_Type> reachableTypes;
	private static Map<String, jq_Class> nameToClassMap;
	private static Map<jq_Class, List<jq_Method>> classToMethodsMap;
	private static jq_Method mainMethod;
	private static boolean HTMLizedJavaSrcFiles;
	private static final Map<Inst, jq_Method> instToMethodMap = 
		new HashMap<Inst, jq_Method>();
	
	private Program() { }

	public static void init() {
		try {
			if (isInited)
				return;
			File classesFile = new File(Properties.outDirName, "classes.txt");
			File methodsFile = new File(Properties.outDirName, "methods.txt");
			if (classesFile.exists() && methodsFile.exists()) {
				preparedClasses = new IndexHashSet<jq_Class>();
				{
					BufferedReader r =
						new BufferedReader(new FileReader(classesFile));
					String s;
					while ((s = r.readLine()) != null) {
						System.out.println("Loading: " + s);
						jq_Class c = (jq_Class) Helper.load(s);
						// if (c == null) {
						//	System.out.println("WARNING: failed to load class: " + c);
						//	continue;
						// }
						assert (c != null);
						c.prepare();
						preparedClasses.add(c);
					}
					r.close();
				}
				buildReachableTypes();
				reachableMethods = new IndexHashSet<jq_Method>();
				{
					BufferedReader r =
						new BufferedReader(new FileReader(methodsFile));
					String s;
					while ((s = r.readLine()) != null) {
						int sep1 = s.indexOf(':');
						int sep2 = s.indexOf('@', sep1 + 1);
						String mName = s.substring(0, sep1);
						String mDesc = s.substring(sep1 + 1, sep2);
						String cName = s.substring(sep2 + 1);
						jq_Class c = getPreparedClass(cName);
						assert (c != null);
						jq_Method m = (jq_Method) c.getDeclaredMember(mName, mDesc);
						reachableMethods.add(m);
					}
					r.close();
				}
			} else {
				String mainClassName = Properties.mainClassName;
				assert (mainClassName != null);
				RTA rta = new RTA();
				rta.run(mainClassName);
				preparedClasses = rta.getPreparedClasses();
				PrintWriter classesFileWriter = new PrintWriter(classesFile);
				for (jq_Class c : preparedClasses) {
					classesFileWriter.println(c);
				}
				classesFileWriter.close();
				buildReachableTypes();
				reachableMethods = rta.getReachableMethods();
				PrintWriter methodsFileWriter = new PrintWriter(methodsFile);
				for (jq_Method m : reachableMethods) {
					methodsFileWriter.println(m);
				}
				methodsFileWriter.close();
				/*
				Project.runTask("cipa-0cfa-dlog");
				ProgramRel relReachableT =
					(ProgramRel) Project.getTrgt("reachableT");
				relReachableT.load();
				classes.clear();
				nameToClassMap = null;
				Iterable<jq_Type> tuples = relReachableT.getAry1ValTuples();
				for (jq_Type t : tuples) {
					if (t instanceof jq_Class)
						classes.add((jq_Class) t);
				}
				relReachableT.close();
				PrintWriter w = new PrintWriter(file);
				for (jq_Class c : classes) {
					w.println(c.getName());
				}
				w.close();
				instToMethodMap.clear();
				Project.resetAll();
				*/
			}
			isInited = true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void mapInstToMethod(Inst i, jq_Method m) {
		instToMethodMap.put(i, m);
	}

	public static jq_Method getMethod(Inst i) {
		jq_Method m = instToMethodMap.get(i);
		if (m == null) {
			throw new RuntimeException(
				"Cannot find method containing inst: " + i);
		}
		return m;
	}

	private static void buildReachableTypes() {
		reachableTypes = new IndexHashSet<jq_Type>();
		for (Object o : jq_Type.list) {
			jq_Type t = (jq_Type) o;
			// if (t.getName().startsWith("joeq."))
			//	continue;
			reachableTypes.add(t);
		}
	}
	private static void buildNameToClassMap() {
		nameToClassMap = new HashMap<String, jq_Class>();
		for (jq_Type t : reachableTypes) {
			if (t instanceof jq_Class) {
				jq_Class c = (jq_Class) t;
				nameToClassMap.put(t.getName(), c);
			}
		}
	}
	private static void buildClassToMethodsMap() {
		classToMethodsMap = new HashMap<jq_Class, List<jq_Method>>();
		for (jq_Method m : reachableMethods) {
			jq_Class c = m.getDeclaringClass();
			assert (preparedClasses.contains(c));
			List<jq_Method> methods = classToMethodsMap.get(c);
			if (methods == null) {
				methods = new ArrayList<jq_Method>();
				classToMethodsMap.put(c, methods);
			}
			methods.add(m);
		}
	}
	
	public static IndexHashSet<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public static IndexHashSet<jq_Method> getReachableMethods() { 
		return reachableMethods;
	}
	public static IndexHashSet<jq_Type> getReachableTypes() {
		return reachableTypes;
	}
	public static jq_Class getPreparedClass(String name) {
		if (nameToClassMap == null)
			buildNameToClassMap();
		return nameToClassMap.get(name);
	}
	public static List<jq_Method> getReachableMethods(jq_Class c) {
		if (classToMethodsMap == null)
			buildClassToMethodsMap();
		List<jq_Method> methods = classToMethodsMap.get(c);
		if (methods == null)
			return Collections.emptyList();
		return methods;
	}
	public static jq_Method getReachableMethod(String cName,
			String mName, String mDesc) {
		jq_Class c = getPreparedClass(cName);
		if (c == null)
			return null;
		List<jq_Method> methods = getReachableMethods(c);
		for (jq_Method m : methods) {
			if (m.getName().toString().equals(mName) &&
				m.getDesc().toString().equals(mDesc))
				return m;
		}
		return null;
	}
	
    private static ControlFlowGraph buildThreadStartMethodCFG(jq_Method m) {
    	jq_Class c = m.getDeclaringClass();
    	jq_NameAndDesc ndOfRun = new jq_NameAndDesc("run", "()V");
    	jq_Method run = c.getDeclaredInstanceMethod(ndOfRun);
    	assert (run != null);
    	RegisterFactory rf = new RegisterFactory(0, 1);
    	Register r = rf.getOrCreateLocal(0, c);
    	ControlFlowGraph cfg = new ControlFlowGraph(m, 1, 0, rf);
    	RegisterOperand ro = new RegisterOperand(r, c);
    	MethodOperand mo = new MethodOperand(run);
    	Quad q1 = Invoke.create(0, Invoke.INVOKEVIRTUAL_V.INSTANCE, null, mo, 1);
    	Invoke.setParam(q1, 0, ro);
    	Quad q2 = Return.create(1, Return.RETURN_V.INSTANCE);
    	BasicBlock bb = cfg.createBasicBlock(1, 1, 2, null);
    	bb.appendQuad(q1);
    	bb.appendQuad(q2);
    	BasicBlock entry = cfg.entry();
    	BasicBlock exit = cfg.exit();
    	bb.addPredecessor(entry);
    	bb.addSuccessor(exit);
    	entry.addSuccessor(bb);
    	exit.addPredecessor(bb);
		m.unsynchronize();
		return cfg;
    }

	private static ControlFlowGraph buildEmptyCFG(jq_Method m) {
		jq_Type[] argTypes = m.getParamTypes();
		int n = argTypes.length;
		RegisterFactory rf = new RegisterFactory(0, n);
		for (int i = 0; i < n; i++) {
			jq_Type t = argTypes[i];
    		rf.getOrCreateLocal(i, t);
		}
		ControlFlowGraph cfg = new ControlFlowGraph(m, 1, 0, rf);
		Operator retOp;
		Quad q;
		if (m.getReturnType().isReferenceType()) {
    		q = Return.create(1, Return.RETURN_V.INSTANCE);
		} else {
			q = Return.create(1, Return.RETURN_A.INSTANCE);
			Return.setSrc(q, new AConstOperand(null));
		}
		BasicBlock bb = cfg.createBasicBlock(1, 1, 1, null);
		bb.appendQuad(q);
		BasicBlock entry = cfg.entry();
		BasicBlock exit = cfg.exit();
		bb.addPredecessor(entry);
		bb.addSuccessor(exit);
		entry.addSuccessor(bb);
		exit.addPredecessor(bb);
		return cfg;
	}

	public static boolean isThreadStartMethod(jq_Method m) {
		return m.getName().toString().equals("start") &&
			m.getDeclaringClass().getName().equals("java.lang.Thread") &&
			m.getDesc().toString().equals("()V");
	}

	public static Map getBCMap(jq_Method m) {
		if (isThreadStartMethod(m))
			return null;
		if (m.getBytecode() == null)
			return null;
		return CodeCache.getBCMap(m);
	}

	private static Map<jq_Method, ControlFlowGraph> methToCFGmap =
		new HashMap<jq_Method, ControlFlowGraph>();

    public static ControlFlowGraph getCFG(jq_Method m) {
		assert (!m.isAbstract());
		ControlFlowGraph cfg = methToCFGmap.get(m);
		if (cfg == null) {
			if (isThreadStartMethod(m)) {
				cfg = buildThreadStartMethodCFG(m);
			} else if (m.isNative()) {
				System.out.println("WARNING: Regarding CFG of native method " +
					m + " as no-op.");
				cfg = buildEmptyCFG(m);
			} else {
				try {
					cfg = CodeCache.getCode(m);
					(new EnterSSA()).visitCFG(cfg);
					assert (cfg != null);
				} catch (Exception ex) {
					System.out.println("WARNING: Failed to get CFG of method " +
						m + "; setting it to no-op.  Error follows.");
					ex.printStackTrace();
					cfg = buildEmptyCFG(m);
				}
			}
			methToCFGmap.put(m, cfg);
		}
		return cfg;
	}

	public static jq_Method getMainMethod() {
		if (mainMethod == null) {
			String mainClassName = Properties.mainClassName;
			assert (mainClassName != null);
			mainMethod = getReachableMethod(mainClassName, "main",
				"([Ljava/lang/String;)V");
			assert (mainMethod != null);
		}
		return mainMethod;
	}

	public static jq_Method getThreadStartMethod() {
		return getReachableMethod("java.lang.Thread", "start", "()V");
	}

	public static String getSign(jq_Method m) {
		String d = m.getDesc().toString();
		return m.getName().toString() + methodDescToStr(d);
	}
	
	// convert the given method descriptor string to a string
	// denoting the comma-separated list of types of the method's
	// arguments in human-readable form
	// e.g.: convert <tt>([Ljava/lang/String;I)V<tt> to
	// <tt>(java.lang.String[],int)</tt>
	public static String methodDescToStr(String desc) {
		String t = desc.substring(1, desc.indexOf(')'));
		return "(" + typesToStr(t) + ")";
	}
	
	// convert the given bytecode string encoding a (possibly empty)
	// list of types to a string denoting the comma-separated list
	// of those types in human-readable form
	// e.g. convert <tt>[Ljava/lang/String;I</tt> to
	// <tt>java.lang.String[],int</tt>
	public static String typesToStr(String types) {
    	String result = "";
    	boolean needsSep = false;
        while (types.length() != 0) {
            boolean isArray = false;
            int numDim = 0;
            String baseType;
            // Handle array case
            while(types.startsWith("[")) {
            	isArray = true;
            	numDim++;
            	types = types.substring(1);
            }
            // Determine base type
            if (types.startsWith("B")) {
            	baseType = "byte";
            	types = types.substring(1);
            } else if (types.startsWith("C")) {
            	baseType = "char";
            	types = types.substring(1);
            } else if (types.startsWith("D")) {
            	baseType = "double";
            	types = types.substring(1);
            } else if (types.startsWith("F")) {
            	baseType = "float";
            	types = types.substring(1);
            } else if (types.startsWith("I")) {
            	baseType = "int";
            	types = types.substring(1);
            } else if(types.startsWith("J")) {
            	baseType = "long";
            	types = types.substring(1);
            } else if(types.startsWith("L")) {
            	int index = types.indexOf(';');
            	if(index == -1)
            		throw new RuntimeException("Class reference has no ending ;");
            	String className = types.substring(1, index);
            	baseType = className.replace('/', '.');
            	types = types.substring(index + 1);
            } else if(types.startsWith("S")) {
            	baseType = "short";
            	types = types.substring(1);
            } else if(types.startsWith("Z")) {
            	baseType = "boolean";
            	types = types.substring(1);
            } else if(types.startsWith("V")) {
            	baseType = "void";
            	types = types.substring(1);
            } else
            	throw new RuntimeException("Unknown field type!");
            if (needsSep)
            	result += ",";
            result += baseType;
            if (isArray) {
            	for (int i = 0; i < numDim; i++)
            		result += "[]";
            }
            needsSep = true;
        }
        return result;
	}
	
	/**
	 * Dumps this program's Java source files in HTML form.
	 */
	public static void HTMLizeJavaSrcFiles() {
		if (!HTMLizedJavaSrcFiles) {
			String srcPathName = Properties.srcPathName;
			assert (srcPathName != null);
			String[] srcDirNames =
				srcPathName.split(File.pathSeparator);
			try {
				Java2HTML java2HTML = new Java2HTML();
				java2HTML.setMarginSize(4);
				java2HTML.setTabSize(4);
				java2HTML.setJavaDirectorySource(srcDirNames);
				java2HTML.setDestination(Properties.outDirName);
				java2HTML.buildJava2HTML();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
			HTMLizedJavaSrcFiles = true;
		}
	}
	
	public static int getBCI(Quad q, jq_Method m) {
		Map<Quad, Integer> bcMap = getBCMap(m);
		if (bcMap == null)
			return -1;
		Integer bci = bcMap.get(q);
		if (bci == null)
			return -1;
		return bci.intValue();
	}

	public static int getLineNumber(Quad q, jq_Method m) {
		Map<Quad, Integer> bcMap = getBCMap(m);
		if (bcMap == null)
			return 0;
		Integer bci = bcMap.get(q);
		if (bci == null)
			return 0;
		return m.getLineNumber(bci.intValue());
	}
	
	public static int getLineNumber(Inst i, jq_Method m) {
		if (i instanceof Quad)
			return getLineNumber((Quad) i, m);
		return 0;
	}
	
	public static String toString(RegisterOperand op) {
		return "<" + op.getType().getName() + " " + op.getRegister() + ">";
	}

	public static String toString(Operand op) {
		if (op instanceof RegisterOperand)
			return toString((RegisterOperand) op);
		return op.toString();
	}

	public static String toStringInvokeInst(Quad q) {
		String s = "";
		RegisterOperand ro = Invoke.getDest(q);
		if (ro != null) 
			s = toString(ro) + " := ";
		else
			s = "";
		jq_Method m = Invoke.getMethod(q).getMethod();
		s += m.getNameAndDesc().toString() + "(";
		ParamListOperand po = Invoke.getParamList(q);
		int n = po.length();
		for (int i = 0; i < n; i++) {
			s += toString(po.get(i));
			if (i < n - 1)
				s += ",";
		}
		s += ")";
		return location(q) + s;
	}
	public static String toString(int bci, String mName, String mDesc,
			String cName) {
		return bci + "!" + mName + ":" + mDesc + "@" + cName;
	}
	public static String toString(String name, String desc, String cName) {
		return name + ":" + desc + "@" + cName;
	}

	public static String toStringNewInst(Quad q) {
		String t, l;
		if (q.getOperator() instanceof New) {
			l = toString(New.getDest(q));
			t = New.getType(q).getType().getName();
		} else {
			l = toString(NewArray.getDest(q));
			t = NewArray.getType(q).getType().getName();
		}
		return location(q) + l + " = new " + t;
	}
	
	public static String toStringHeapInst(Quad q) {
		Operator op = q.getOperator();
		String s;
		if (Program.isWrHeapInst(op)) {
			String b, f, r;
			if (op instanceof Putfield) {
				b = toString(Putfield.getBase(q)) + ".";
				f = Putfield.getField(q).getField().toString();
				r = toString(Putfield.getSrc(q));
			} else if (op instanceof AStore) {
				b = toString(AStore.getBase(q));
				f = "[*]";
				r = toString(AStore.getValue(q));
			} else {
				b = "";
				f = Putstatic.getField(q).getField().toString();
				r = toString(Putstatic.getSrc(q));
			}
			s = b + f + " := " + r;
		} else {
			String l, b, f;
			if (op instanceof Getfield) {
				l = toString(Getfield.getDest(q));
				b = toString(Getfield.getBase(q)) + ".";
				f = Getfield.getField(q).getField().toString();
			} else if (op instanceof ALoad) {
				l = toString(ALoad.getDest(q));
				b = toString(ALoad.getBase(q));
				f = "[*]";
			} else {
				l = toString(Getstatic.getDest(q));
				b = "";
				f = Getstatic.getField(q).getField().toString();
			}
			s = l + " := " + b + f;
			
		}
		return location(q) + s;
	}
	
	public static String toStringLockInst(Inst q) {
		jq_Method m = getMethod(q);
		String fileName = Program.getSourceFileName(m.getDeclaringClass());
		int lineNumber = getLineNumber(q, m);
		String s;
		if (q instanceof Quad)
			s = Monitor.getSrc((Quad) q).toString();
		else
			s = getMethod(q).toString();
		return fileName + ":" + lineNumber + ": monitorenter " + s;
	}

	public static String location(Quad q) {
		jq_Method m = getMethod(q);
		jq_Class c = m.getDeclaringClass();
		String fileName = Program.getSourceFileName(c);
		int lineNumber = Program.getLineNumber(q, m);
		return fileName + ":" + lineNumber + ": ";
	}
	
	public static String toString(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Move) {
			return location(q) + Move.getDest(q) + " := " +
				Move.getSrc(q);
		}
		if (op instanceof Getfield || op instanceof Putfield ||
				op instanceof ALoad || op instanceof AStore ||
				op instanceof Getstatic || op instanceof Putstatic) {
			return toStringHeapInst(q);
		}
		if (op instanceof New || op instanceof NewArray)
			return toStringNewInst(q);
		if (op instanceof Invoke)
			return toStringInvokeInst(q);
		return location(q) + q.toString();
	}
	
	public static jq_Field getField(Quad e) {
		Operator op = e.getOperator();
		if (op instanceof ALoad || op instanceof AStore)
			return null;
		if (op instanceof Getfield)
			return Getfield.getField(e).getField();
		if (op instanceof Putfield)
			return Putfield.getField(e).getField();
		if (op instanceof Getstatic)
			return Getstatic.getField(e).getField();
		if (op instanceof Putstatic)
			return Putstatic.getField(e).getField();
		throw new RuntimeException();
	}
	
	public static boolean isHeapInst(Operator op) {
		return op instanceof ALoad || op instanceof AStore ||
			op instanceof Getfield || op instanceof Putfield ||
			op instanceof Getstatic || op instanceof Putstatic;
	}
	
	public static boolean isWrHeapInst(Operator op) {
		return op instanceof Putfield || op instanceof AStore ||
			op instanceof Putstatic;
	}
	
	public static String getSourceFileName(jq_Class c) {
		String t = c.getName();
		String s = t.substring(0, t.lastIndexOf('.') + 1);
		Utf8 f = c.getSourceFile();
		return s.replace('.', '/') + f;
	}
	
	public static int getNumVarsOfRefType(jq_Method m) {
		ControlFlowGraph cfg = getCFG(m);
		RegisterFactory rf = cfg.getRegisterFactory();
		int n = 0;
		for (Object o : rf) {
			Register r = (Register) o;
			if (r.getType().isReferenceType())
				n++;
		}
		return n;
	}
	public static void printClass(String name) {
	}
	public static void printMethod(String sign) {
	}
	public static void print() {
		for (jq_Method m : getReachableMethods()) {
			System.out.println(m);
			if (!m.isAbstract()) {
				ControlFlowGraph cfg = getCFG(m);
				for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
                		it.hasNext();) {
					BasicBlock bb = it.nextBasicBlock();
					for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
						Quad q = it2.nextQuad();			
						int bci = getBCI(q, m);
						System.out.println("\t" + bci + "#" + q.getID());
					}
				}
				System.out.println(cfg.fullDump());
			}
		}
	}
}

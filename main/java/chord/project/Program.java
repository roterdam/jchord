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
import java.util.Set;

import com.java2html.Java2HTML;

import chord.util.Assertions;

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
	private static List<jq_Class> loadedClasses;
	private static List<jq_Class> preparedClasses;
	private static List<jq_Method> reachableMethods;
	private static List<jq_Type> reachableTypes;
	private static Map<String, jq_Class> nameToClassMap;
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
				loadedClasses = new ArrayList<jq_Class>();
				{
					BufferedReader r =
						new BufferedReader(new FileReader(classesFile));
					String s;
					while ((s = r.readLine()) != null) {
						System.out.println("Loading: " + s);
						jq_Class c = (jq_Class) Helper.load(s);
						Assertions.Assert(c != null);
						loadedClasses.add(c);
					}
					r.close();
				}
				buildNameToClassMap();
				reachableMethods = new ArrayList<jq_Method>();
				{
					BufferedReader r =
						new BufferedReader(new FileReader(methodsFile));
					String s;
					while ((s = r.readLine()) != null) {
						String[] a = s.split("@");
						Assertions.Assert(a.length == 3);
						jq_Class c = getClass(a[0]);
						Assertions.Assert(c != null);
						Assertions.Assert(c.isPrepared());
						jq_Method m = (jq_Method) c.getDeclaredMember(
							a[1], a[2]);
						reachableMethods.add(m);
					}
					r.close();
				}
			} else {
				String mainClassName = Properties.mainClassName;
				Assertions.Assert(mainClassName != null);
				RTA rta = new RTA();
				rta.run(mainClassName);
				Set<jq_Class> cset = rta.getLoadedClasses();
				loadedClasses = new ArrayList<jq_Class>(cset.size());
				PrintWriter classesFileWriter =
					new PrintWriter(classesFile);
				for (jq_Class c : cset) {
					loadedClasses.add(c);
					classesFileWriter.println(c.getName());
				}
				classesFileWriter.close();
				buildNameToClassMap();
				Set<jq_Method> mset = rta.getReachableMethods();
				reachableMethods = new ArrayList<jq_Method>(mset.size());
				PrintWriter methodsFileWriter =
					new PrintWriter(methodsFile);
				for (jq_Method m : mset) {
					reachableMethods.add(m);
					String s = m.getDeclaringClass().getName() + "@" +
						m.getName() + "@" + m.getDesc().toString();
					methodsFileWriter.println(s);
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
			initThreadStart();
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

	private static void buildNameToClassMap() {
		reachableTypes = jq_Type.list;
		nameToClassMap = new HashMap<String, jq_Class>();
		preparedClasses = new ArrayList<jq_Class>();
		for (jq_Type t : reachableTypes) {
			if (t instanceof jq_Class) {
				jq_Class c = (jq_Class) t;
				nameToClassMap.put(t.getName(), c);
				if (c.isPrepared())
					preparedClasses.add(c);
			}
		}
	}
	
	public static jq_Class getClass(String name) {
		return nameToClassMap.get(name);
	}

	public static List<jq_Class> getPreparedClasses() {
		return preparedClasses;
	}
	public static List<jq_Method> getReachableMethods() { 
		return reachableMethods;
	}
	public static List<jq_Type> getReachableTypes() {
		return reachableTypes;
	}
	
    private static void initThreadStart() {
		jq_Method m = getThreadStartMethod();
		if (m == null)
			return;
    	jq_Class threadClass = m.getDeclaringClass();
    	jq_NameAndDesc nadOfRun = new jq_NameAndDesc("run", "()V");
    	jq_Method run = threadClass.getDeclaredInstanceMethod(nadOfRun);
    	Assertions.Assert(run != null);
    	RegisterFactory rf = new RegisterFactory(0, 1);
    	Register r = rf.getOrCreateLocal(0, threadClass);
    	ControlFlowGraph cfg = new ControlFlowGraph(m, 1, 0, rf);
    	RegisterOperand ro = new RegisterOperand(r, threadClass);
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
		System.out.println("SETTING THREAD START");
		CodeCache.cache.setMap(m, cfg);
		CodeCache.cache.setBCMap(m, null);
    }

	public static Map getBCMap(jq_Method m) {
		if (m.getBytecode() == null)
			return null;
		return CodeCache.getBCMap(m);
	}

	private static Map<jq_Method, ControlFlowGraph> methToCFGmap =
		new HashMap<jq_Method, ControlFlowGraph>();

	private static ControlFlowGraph getOrMakeEmptyCFG(jq_Method m) {
		ControlFlowGraph cfg = methToCFGmap.get(m);
		if (cfg == null) {
			jq_Type[] argTypes = m.getParamTypes();
			int n = argTypes.length;
			RegisterFactory rf = new RegisterFactory(0, n);
			for (int i = 0; i < n; i++) {
				jq_Type t = argTypes[i];
    			rf.getOrCreateLocal(i, t);
			}
			cfg = new ControlFlowGraph(m, 1, 0, rf);
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
			entry.addSuccessor(bb);
			bb.addPredecessor(entry);
			exit.addPredecessor(bb);
			bb.addSuccessor(exit);
			methToCFGmap.put(m, cfg);
		}
		return cfg;
	}

    public static ControlFlowGraph getCFG(jq_Method m) {
		Assertions.Assert(!m.isAbstract());
/*
    	String nad = m.getNameAndDesc().toString();
    	if (nad.equals("equals (Ljava/lang/Object;)Z") ||
    		nad.equals("hashCode ()I") ||
    		nad.equals("toString ()Ljava/lang/String;"))
    		return null;
*/
		if (m.isNative())
			return getOrMakeEmptyCFG(m);
		ControlFlowGraph cfg;
		try {
			cfg = CodeCache.getCode(m);
			// (new EnterSSA()).visitCFG(cfg);
			if (cfg == null) {
				System.out.println("Method " + m + " has empty CFG");
				Assertions.Assert(false);
			}
		} catch (Exception ex) {
			System.out.println("Failed to get CFG for method: " +
				m + "; setting it to nop.  Error follows.");
			ex.printStackTrace();
			return getOrMakeEmptyCFG(m);
		}
		return cfg;
	}

	public static jq_Method getMainMethod() {
		if (mainMethod == null) {
			if (Properties.mainClassName == null) {
				throw new RuntimeException();
			}
			jq_Class clazz = getClass(Properties.mainClassName);
			if (clazz == null) {
				throw new RuntimeException();
			}
			jq_NameAndDesc sign = new jq_NameAndDesc(
				"main", "([Ljava/lang/String;)V");
			mainMethod = clazz.getDeclaredStaticMethod(sign);
			if (mainMethod == null) {
				throw new RuntimeException();
			}
		}
		return mainMethod;
	}

	public static String getSign(jq_Method m) {
		String d = m.getDesc().toString();
		return m.getName().toString() + methodDescToStr(d);
	}
	
	public static jq_Method getThreadStartMethod() {
    	jq_Class threadClass = getClass("java.lang.Thread");
		if (threadClass == null)
			return null;
    	jq_NameAndDesc nadOfStart = new jq_NameAndDesc("start", "()V");
    	jq_Method start = threadClass.getDeclaredInstanceMethod(nadOfStart);
		Assertions.Assert(start != null);
		return start;
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
			Assertions.Assert(srcPathName != null);
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
		Integer bci = bcMap.get(q);
		Assertions.Assert(bci != null);
		return bci.intValue();
	}

	public static int getLineNumber(Quad q, jq_Method m) {
		Map<Quad, Integer> bcMap = getBCMap(m);
		if (bcMap == null)
			return 0;
		Integer bci = bcMap.get(q);
		if (bci != null)
			return m.getLineNumber(bci.intValue());
		return 0;
	}
	
	public static int getLineNumber(Inst i, jq_Method m) {
		if (i instanceof Quad)
			return getLineNumber((Quad) i, m);
		return 0;
	}
	
	public static String toString(jq_Type t) {
		return t.getName();
	}
	
	public static String toString(jq_Field f) {
		return "<" + f.getDeclaringClass().getName() + ": " +
			f.getType().getName() +  " " +
			f.getName().toString() + ">";
	}
	
	public static String toString(jq_Method m) {
		String s;
		jq_Type[] argTypes = m.getParamTypes();
		int n = argTypes.length;
		if (n > 0) {
			s = argTypes[0].getName();
			for (int i = 1; i < n; i++)
				s += "," + argTypes[i].getName();
		} else
			s = "";
		return "<" + m.getDeclaringClass().getName() + ": " +
			m.getReturnType().getName() + " " +
			m.getName() + "(" + s + ")>";
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
				f = toString(Putfield.getField(q).getField());
				r = toString(Putfield.getSrc(q));
			} else if (op instanceof AStore) {
				b = toString(AStore.getBase(q));
				f = "[*]";
				r = toString(AStore.getValue(q));
			} else {
				b = "";
				f = toString(Putstatic.getField(q).getField());
				r = toString(Putstatic.getSrc(q));
			}
			s = b + f + " := " + r;
		} else {
			String l, b, f;
			if (op instanceof Getfield) {
				l = toString(Getfield.getDest(q));
				b = toString(Getfield.getBase(q)) + ".";
				f = toString(Getfield.getField(q).getField());
			} else if (op instanceof ALoad) {
				l = toString(ALoad.getDest(q));
				b = toString(ALoad.getBase(q));
				f = "[*]";
			} else {
				l = toString(Getstatic.getDest(q));
				b = "";
				f = toString(Getstatic.getField(q).getField());
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
			s = Program.toString(getMethod(q));
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
}

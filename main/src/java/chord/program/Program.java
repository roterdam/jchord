/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.java2html.Java2HTML;

import chord.project.OutDirUtils;
import chord.project.Messages;
import chord.project.Properties;
import chord.util.IndexHashSet;
import chord.util.IndexSet;
import chord.util.ChordRuntimeException;
 
import joeq.Util.Templates.ListIterator;
import joeq.UTF.Utf8;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.CheckCast;
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
import joeq.Main.Helper;

/**
 * Representation of a Java program.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Program {
	private static Program instance;
	public static Program v() {
		if (instance == null)
			instance = new Program();
		return instance;
	}
	private IndexSet<jq_Class> classes;
	private IndexSet<jq_Method> methods;
	private IndexSet<jq_Type> types;
	private Map<String, jq_Class> nameToClassMap;
	private Map<jq_Class, List<jq_Method>> classToMethodsMap;
	private jq_Method mainMethod;
	private boolean HTMLizedJavaSrcFiles;
	private final Map<Inst, jq_Method> instToMethodMap = 
		new HashMap<Inst, jq_Method>();

	private Program() {
		if (Properties.verbose)
			jq_Method.setVerbose();
		if (Properties.doSSA)
			jq_Method.doSSA();
        String[] scopeExcludedPrefixes = Properties.toArray(Properties.scopeExcludeStr);
		jq_Method.exclude(scopeExcludedPrefixes);
		try {
			boolean filesExist =
				(new File(Properties.classesFileName)).exists() &&
				(new File(Properties.methodsFileName)).exists();
			if (Properties.reuseScope && filesExist)
				initFromCache();
			else {
				String scopeKind = Properties.scopeKind;
				if (scopeKind.equals("rta")) {
					init(new RTA());
				} else if (scopeKind.equals("dynamic")) {
					initFromDynamic();
				} else
					assert (false);
			}
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	private void loadClasses(String fileName) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(fileName));
		String s;
		while ((s = r.readLine()) != null) {
			if (Properties.verbose)
				Messages.log("SCOPE.LOADING_CLASS", s);
			jq_Class c;
			try {
				c = (jq_Class) Helper.load(s);
			} catch (Exception ex) {
				Messages.log("SCOPE.EXCLUDING_CLASS", s);
				ex.printStackTrace();
				continue;
			}
			assert (c != null);
			classes.add(c);
		}
		r.close();
	}
	private void initFromCache() throws IOException {
		classes = new IndexHashSet<jq_Class>();
		loadClasses(Properties.classesFileName);
		methods = new IndexHashSet<jq_Method>();
		readMethods(Properties.methodsFileName);
		buildTypes();
	}

	private void readMethods(String fileName) throws IOException {
		BufferedReader r = new BufferedReader(new FileReader(fileName));
		String s;
		while ((s = r.readLine()) != null) {
			MethodSign sign = MethodSign.parse(s);
			String cName = sign.cName;
			jq_Class c = getPreparedClass(cName);
			if (c == null)
				Messages.log("SCOPE.EXCLUDING_METHOD", s);
			else {
				String mName = sign.mName;
				String mDesc = sign.mDesc;
				jq_Method m = (jq_Method) c.getDeclaredMember(mName, mDesc);
				methods.add(m);
			}
		}
		r.close();
	}

	private void init(IBootstrapper bootstrapper) throws IOException {
		bootstrapper.run();
		classes = new IndexHashSet<jq_Class>();
		for (jq_Class c : bootstrapper.getPreparedClasses()) 
			classes.add(c);
		methods = new IndexHashSet<jq_Method>();
		for (jq_Method m : bootstrapper.getReachableMethods()) {
			jq_Class c = m.getDeclaringClass();
			if (!classes.contains(c))
				Messages.log("SCOPE.EXCLUDING_METHOD", m);
			else
				methods.add(m);
		}
		buildTypes();
		write();
	}

	private void initFromDynamic() throws IOException {
		String mainClassName = Properties.mainClassName;
		if (mainClassName == null)
			Messages.fatal("SCOPE.MAIN_CLASS_NOT_DEFINED");
		String classPathName = Properties.classPathName;
		if (classPathName == null)
			Messages.fatal("SCOPE.CLASS_PATH_NOT_DEFINED");
        String[] runIDs = Properties.runIDs.split(Properties.LIST_SEPARATOR);
		assert(runIDs.length > 0);
        final String cmd = "java " + Properties.runtimeJvmargs +
            " -cp " + classPathName +
            " -agentpath:" + Properties.instrAgentFileName +
            "=classes_file_name=" + Properties.classesFileName +
            " " + mainClassName + " ";
		classes = new IndexHashSet<jq_Class>();
        for (String runID : runIDs) {
            String args = System.getProperty("chord.args." + runID, "");
			OutDirUtils.executeWithFailOnError(cmd + args);
			loadClasses(Properties.classesFileName);
		}
		methods = new IndexHashSet<jq_Method>();
		for (jq_Class c : classes) {
			for (jq_Method m : c.getDeclaredInstanceMethods()) 
				methods.add(m);
			for (jq_Method m : c.getDeclaredStaticMethods()) 
				methods.add(m);
		}
		buildTypes();
		write();
	}

	private void write() throws IOException {
		PrintWriter classesFileWriter =
			new PrintWriter(Properties.classesFileName);
		for (jq_Class c : classes) {
			classesFileWriter.println(c);
		}
		classesFileWriter.close();
		PrintWriter methodsFileWriter =
			new PrintWriter(Properties.methodsFileName);
		for (jq_Method m : methods) {
			methodsFileWriter.println(m);
		}
		methodsFileWriter.close();
	}

	public void mapInstToMethod(Inst i, jq_Method m) {
		instToMethodMap.put(i, m);
	}

	public jq_Method getMethod(Inst i) {
		jq_Method m = instToMethodMap.get(i);
		if (m == null) {
			throw new ChordRuntimeException("Cannot find method containing inst: " + i);
		}
		return m;
	}

	private void buildTypes() {
		types = new IndexHashSet<jq_Type>();
		for (jq_Class c : classes)
			types.add(c);
	}

	private void buildNameToClassMap() {
		nameToClassMap = new HashMap<String, jq_Class>();
		for (jq_Class c : classes) {
			nameToClassMap.put(c.getName(), c);
		}
	}
	
	private void buildClassToMethodsMap() {
		classToMethodsMap = new HashMap<jq_Class, List<jq_Method>>();
		for (jq_Method m : methods) {
			jq_Class c = m.getDeclaringClass();
			List<jq_Method> methods = classToMethodsMap.get(c);
			if (methods == null) {
				methods = new ArrayList<jq_Method>();
				classToMethodsMap.put(c, methods);
			}
			methods.add(m);
		}
	}
	
	public IndexSet<jq_Class> getPreparedClasses() {
		return classes;
	}
	
	public IndexSet<jq_Method> getReachableMethods() { 
		return methods;
	}

	public IndexSet<jq_Type> getReachableTypes() {
		return types;
	}
	
	public jq_Class getPreparedClass(String name) {
		if (nameToClassMap == null)
			buildNameToClassMap();
		return nameToClassMap.get(name);
	}
	
	public List<jq_Method> getReachableMethods(jq_Class c) {
		if (classToMethodsMap == null)
			buildClassToMethodsMap();
		List<jq_Method> methods = classToMethodsMap.get(c);
		if (methods == null)
			return Collections.emptyList();
		return methods;
	}

	public jq_Method getReachableMethod(String mName, String mDesc, String cName) {
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
	
	public jq_Method getReachableMethod(MethodSign sign) {
		return getReachableMethod(sign.mName, sign.mDesc, sign.cName);
	}

	public jq_Method getMainMethod() {
		if (mainMethod == null) {
			String mainClassName = Properties.mainClassName;
			if (mainClassName == null)
				Messages.fatal("SCOPE.MAIN_CLASS_NOT_DEFINED");
			mainMethod = getReachableMethod("main", "([Ljava/lang/String;)V", mainClassName);
			if (mainMethod == null)
				Messages.fatal("SCOPE.MAIN_METHOD_NOT_FOUND", mainClassName);
		}
		return mainMethod;
	}

	public jq_Method getThreadStartMethod() {
		return getReachableMethod("start", "()V", "java.lang.Thread");
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
	public void HTMLizeJavaSrcFiles() {
		if (!HTMLizedJavaSrcFiles) {
			String srcPathName = Properties.srcPathName;
			if (srcPathName == null)
				Messages.fatal("SCOPE.SRC_PATH_NOT_DEFINED");
			String[] srcDirNames = srcPathName.split(File.pathSeparator);
			try {
				Java2HTML java2HTML = new Java2HTML();
				java2HTML.setMarginSize(4);
				java2HTML.setTabSize(4);
				java2HTML.setJavaDirectorySource(srcDirNames);
				java2HTML.setDestination(Properties.outDirName);
				java2HTML.buildJava2HTML();
			} catch (Exception ex) {
				throw new ChordRuntimeException(ex);
			}
			HTMLizedJavaSrcFiles = true;
		}
	}
	
	public static int getLineNumber(Quad q, jq_Method m) {
		int bci = m.getBCI(q);
		if (bci == -1)
			return 0;
		return m.getLineNumber(bci);
	}
	
	public static int getLineNumber(Inst i, jq_Method m) {
		if (i instanceof Quad)
			return getLineNumber((Quad) i, m);
		return 0;
	}
	
	public static String toString(int bci, String mName, String mDesc,
			String cName) {
		return bci + "!" + mName + ":" + mDesc + "@" + cName;
	}
	
	public static String toString(String name, String desc, String cName) {
		return name + ":" + desc + "@" + cName;
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
		Utf8 f = c.getSourceFile();
		if (f == null)
			return null;
		String t = c.getName();
		int i = t.lastIndexOf('.') + 1;
		String s = t.substring(0, i);
		return s.replace('.', '/') + f;
	}

	/**************************************************************
	 * Functions for printing program structures in various formats
	 **************************************************************/

	public String toVerboseStr(Quad q) {
		return toBytePosStr(q) + " (" + toJavaPosStr(q) + ") [" + toQuadStr(q) + "]";
	}

	public String toPosStr(Quad q) {
		return toBytePosStr(q) + " (" + toJavaPosStr(q) + ")";
	}

	public String toJavaPosStr(Quad q) {
		jq_Method m = getMethod(q);
		jq_Class c = m.getDeclaringClass();
		String fileName = getSourceFileName(c);
		int lineNumber = getLineNumber(q, m);
		return fileName + ":" + lineNumber;
	}

	public String toBytePosStr(Inst i) {
        if (i == null)
            return "null";
        jq_Method m = getMethod(i);
        int bci;
        if (i instanceof Quad)
            bci = m.getBCI((Quad) i);
        else {
            BasicBlock b = (BasicBlock) i;
            if (b.isEntry())
                bci = -1;
            else {
                assert (b.isExit());
                bci = -2;
            }
        }
        String mName = m.getName().toString();
        String mDesc = m.getDesc().toString();
        String cName = m.getDeclaringClass().getName();
        return toString(bci, mName, mDesc, cName);
	}

	public String toQuadStr(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Move || op instanceof CheckCast) {
			return Move.getDest(q) + " = " + Move.getSrc(q);
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
		return q.toString();
	}

	public static String toString(RegisterOperand op) {
		return "<" + op.getType().getName() + " " + op.getRegister() + ">";
	}

	public static String toString(Operand op) {
		if (op instanceof RegisterOperand)
			return toString((RegisterOperand) op);
		return op.toString();
	}

	public String toStringInvokeInst(Quad q) {
		String s = "";
		RegisterOperand ro = Invoke.getDest(q);
		if (ro != null) 
			s = toString(ro) + " = ";
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
		return s + ")";
	}
	
	public String toStringNewInst(Quad q) {
		String t, l;
		if (q.getOperator() instanceof New) {
			l = toString(New.getDest(q));
			t = New.getType(q).getType().getName();
		} else {
			l = toString(NewArray.getDest(q));
			t = NewArray.getType(q).getType().getName();
		}
		return l + " = new " + t;
	}
	
	public String toStringHeapInst(Quad q) {
		Operator op = q.getOperator();
		String s;
		if (isWrHeapInst(op)) {
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
			s = b + f + " = " + r;
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
			s = l + " = " + b + f;
			
		}
		return s;
	}
	
	public String toStringLockInst(Inst q) {
		String s;
		if (q instanceof Quad)
			s = Monitor.getSrc((Quad) q).toString();
		else
			s = getMethod(q).toString();
		return "monitorenter " + s;
	}
	
	public void print() {
		for (jq_Method m : getReachableMethods()) {
			System.out.println(m);
			if (!m.isAbstract()) {
				ControlFlowGraph cfg = m.getCFG();
				for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
                		it.hasNext();) {
					BasicBlock bb = it.nextBasicBlock();
					for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
						Quad q = it2.nextQuad();			
						int bci = m.getBCI(q);
						System.out.println("\t" + bci + "#" + q.getID());
					}
				}
				System.out.println(cfg.fullDump());
			}
		}
	}
}

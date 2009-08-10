/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import javassist.*;
import javassist.expr.*;

import chord.util.FileUtils;
import chord.util.IndexHashMap;
import chord.util.IndexMap;
import chord.util.IndexSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

import java.io.File;
import java.util.List;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Instrumentor {
	private static IndexMap<String> Hmap;
	private static IndexMap<String> Emap;
	private static IndexMap<String> Lmap;
	private static IndexMap<String> Fmap;
	private static IndexMap<String> Mmap;
	private static ClassPool pool;
	private static CtClass exType;
	private static MyExprEditor exprEditor = new MyExprEditor();
	private static CtBehavior method;

	public static void main(String[] args) throws Exception {
		String fullClassPathName = Properties.classPathName +
			File.pathSeparator + Properties.bootClassPathName;
		pool = new ClassPool();
		String[] pathElems = fullClassPathName.split(File.pathSeparator);
		for (String pathElem : pathElems) {
			File file = new File(pathElem);
			if (!file.exists()) {
				System.out.println("WARNING: Ignoring: " + pathElem);
				continue;
			}
			pool.appendClassPath(pathElem);
		}
		pool.appendSystemPath();

		if (InstrFormat.instrMethodEnterAndLeave) {
			exType = pool.get("java.lang.Throwable");
			assert (exType != null);
		}
		if (InstrFormat.needsHmap())
			Hmap = new IndexHashMap<String>();
		if (InstrFormat.needsEmap())
			Emap = new IndexHashMap<String>();
		if (InstrFormat.needsLmap()) 
			Lmap = new IndexHashMap<String>();
		if (InstrFormat.needsFmap())
			Fmap = new IndexHashMap<String>();
		if (InstrFormat.needsMmap())
			Mmap = new IndexHashMap<String>();

		String classesDirName = Properties.classesDirName;
		IndexSet<jq_Class> classes = Program.getPreparedClasses();

		for (jq_Class c : classes) {
			String cName = c.getName();
			if (cName.equals("java.lang.J9VMInternals"))
				continue;
			System.out.println("YYY " + cName);
			CtClass clazz = pool.get(cName);
			List<jq_Method> methods = Program.getReachableMethods(c);
			CtBehavior[] inits = clazz.getDeclaredConstructors();
			CtBehavior[] meths = clazz.getDeclaredMethods();
			for (jq_Method m : methods) {
				String mName = m.getName().toString();
				if (mName.equals("<clinit>")) {
					method = clazz.getClassInitializer();
					assert (method != null);
					process();
				} else if (mName.equals("<init>")) {
					String mDesc = m.getDesc().toString();
					method = null;
					for (CtBehavior x : inits) {
						if (x.getSignature().equals(mDesc)) {
							method = x;
							break;
						}
					}
					assert (method != null);
					process();
				} else {
					String mDesc = m.getDesc().toString();
					method = null;
					for (CtBehavior x : meths) {
						if (x.getName().equals(mName) &&
							x.getSignature().equals(mDesc)) {
							method = x;
							break;
						}
					}
					assert (method != null);
					process();
				}
			}
			System.out.println("Writing class: " + cName);
			clazz.writeFile(classesDirName);
		}

		String outDirName = Properties.outDirName;
		if (Hmap != null) {
			FileUtils.writeMapToFile(Hmap,
				(new File(outDirName, "H.dynamic.txt")).getAbsolutePath());
		}
		if (Emap != null) {
			FileUtils.writeMapToFile(Emap,
				(new File(outDirName, "E.dynamic.txt")).getAbsolutePath());
		}
		if (Lmap != null) {
			FileUtils.writeMapToFile(Lmap,
				(new File(outDirName, "L.dynamic.txt")).getAbsolutePath());
		}
		if (Fmap != null) {
			FileUtils.writeMapToFile(Fmap,
				(new File(outDirName, "F.dynamic.txt")).getAbsolutePath());
		}
		if (Mmap != null) {
			FileUtils.writeMapToFile(Mmap,
				(new File(outDirName, "M.dynamic.txt")).getAbsolutePath());
		}
	}

	private static int set2(IndexMap<String> map, int bci) {
        String mName;
        if (method instanceof CtConstructor) {
            mName = ((CtConstructor) method).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = method.getName();
        String mDesc = method.getSignature();
        String cName = method.getDeclaringClass().getName();
		String s = Program.toString(bci, mName, mDesc, cName);
		System.out.println(s);
		int n = map.size();
		int i = map.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private static int set() {
        String mName;
        if (method instanceof CtConstructor) {
            mName = ((CtConstructor) method).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = method.getName();
        String mDesc = method.getSignature();
        String cName = method.getDeclaringClass().getName();
		String s = Program.toString(mName, mDesc, cName);
		int n = Mmap.size();
		int i = Mmap.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private static void process() {
		System.out.println("XXX " + method);
		try {
			int mods = method.getModifiers();
			if (Modifier.isNative(mods) || Modifier.isAbstract(mods))
				return;
			if (Lmap != null && Modifier.isSynchronized(mods)) {
				String syncExpr = (Modifier.isStatic(mods)) ? "$class" : "$0";
				System.out.print("S ");
				int lIdx = set2(Lmap, -1);
				method.insertBefore("{ chord.project.Runtime.acqLock(" +
					lIdx + "," + syncExpr + "); }");
			}
			if (InstrFormat.instrMethodAndLoopCounts) {
				ControlFlowGraph cfg = Program.getCFG(method);
				CFGLoopFinder finder = new CFGLoopFinder(cfg);
				Set<BasicBlock> heads = finder.getLoopHeads();
				for (BasicBlock head : heads) {
					
				}

	
				// find all loops of m
				// get bytecode index of each loop header
				// and bytecode index of each loop exits
			}
			method.instrument(exprEditor);
			if (InstrFormat.instrThreadSpawnAndStart) {
				if (method.getName().equals("start") &&
					method.getSignature().equals("()V") &&
					method.getDeclaringClass().getName().equals("java.lang.Thread")) {
					String s = "chord.project.Runtime.threadStart($0);";
					method.insertBefore(s);
				}
			}
			if (Mmap != null) {
				int mIdx = set();
				method.insertBefore("{ chord.project.Runtime.methodEnter(" + mIdx + "); }");
				method.insertAfter ("{ chord.project.Runtime.methodLeave(" + mIdx + "); }");
				String s = "{ chord.project.Runtime.methodLeave(" + mIdx + "); throw($e); }";
				method.addCatch(s, exType);
			}
		} catch (Exception ex) {
			System.err.println("WARNING: Ignoring instrumenting method: " +
				method.getLongName());
			ex.printStackTrace();
		}
	}

	static class MyExprEditor extends ExprEditor {
		private static int set(IndexMap<String> map, Expr e) {
			return set2(map, e.indexOfOriginalBytecode());
		}
		public String insertBefore(int pos) {
			return null; // TODO
		}
		public void edit(NewExpr e) {
			if (Hmap == null)
				return;
			try {
				System.out.print("H ");
				int hIdx = set(Hmap, e);
				String s = "chord.project.Runtime.befNew(" + hIdx + ");";
				String t = "chord.project.Runtime.aftNew(" + hIdx + ",$_);";
				e.replace("{ " + s + " $_ = $proceed($$); " + t + " }");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		public void edit(NewArray e) {
			if (Hmap == null)
				return;
			try {
				System.out.print("N ");
				int hIdx = set(Hmap, e);
				String s = "chord.project.Runtime.newArray(" + hIdx + ",$_);";
				e.replace("{ $_ = $proceed($$); " + s + " }");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		public void edit(FieldAccess e) {
			try {
				boolean isStatic = e.isStatic();
				if (isStatic) {
					if (!InstrFormat.instrStatFldInst)
						return;
				} else {
					if (!InstrFormat.instrInstFldInst)
						return;
				}
				boolean isWr = e.isWriter();
				CtField fld = e.getField();
				boolean isPrim = fld.getType().isPrimitive();
				String s1 = isWr ? "$proceed($$);" : "$_ = $proceed();";
				String s2;
				// String l = isPrim ? "null" : "$_";
				String r = isPrim ? "null" : "$1";
				if (isStatic) {
					if (isWr) {
						s2 = "chord.project.Runtime.statFldWr(" + r + ");";
					} else
						s2 = "";
				} else {
					String b = "$0";
					String fName = fld.getName();
					String fDesc = fld.getSignature();
					String cName = fld.getDeclaringClass().getName();
					String s = Program.toString(fName, fDesc, cName);
					int fIdx = Fmap.getOrAdd(s);
					System.out.print("E ");
					int eIdx = set(Emap, e);
					if (isWr) {
						s2 = "chord.project.Runtime.instFldWr(" +
							eIdx + "," + b + "," + fIdx + "," + r + ");";
					} else {
						s2 = "chord.project.Runtime.instFldRd(" +
							eIdx + "," + b + "," + fIdx + ");";
					}
				}
				e.replace("{ " + s1 + " " + s2 + " }");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		public void edit(ArrayAccess e) {
			if (!InstrFormat.instrAryElemInst)
				return;
			try {
				boolean isWr = e.isWriter();
				boolean isPrim = e.getElemType().isPrimitive();
				String s1, s2;
				String f = "$1";
				System.out.print("A ");
				int eIdx = set(Emap, e);
				if (isWr) {
					s1 = "$proceed($$);";
					String r = isPrim ? "null" : "$2";
					s2 = "chord.project.Runtime.aryElemWr(" +
						eIdx + ",$0," + f + "," + r + ");";
				} else {
					s1 = "$_ = $proceed($$);";
					// String l = isPrim ? "null" : "$_";
					s2 = "chord.project.Runtime.aryElemRd(" +
						eIdx + ",$0," + f + ");";
				}
				e.replace("{ " + s1 + " " + s2 + " }");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		public void edit(MethodCall e) {
			if (!InstrFormat.instrThreadSpawnAndStart)
				return;
			try {
				CtMethod m = e.getMethod();
				if (m.getName().equals("start") &&
					m.getSignature().equals("()V") &&
					m.getDeclaringClass().getName().equals("java.lang.Thread")) {
					String s = "chord.project.Runtime.threadSpawn($0);";
					e.replace("{ $_ = $proceed($$); " + s + " }");
				}
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		public void edit(MonitorEnter e) {
			if (!InstrFormat.instrAcqLockInst)
				return;
			try {
				System.out.print("L ");
				int lIdx = set(Lmap, e);
				String s = "chord.project.Runtime.acqLock(" + lIdx + ",$0);";
				e.replace("{ $proceed(); " + s + " }");
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}


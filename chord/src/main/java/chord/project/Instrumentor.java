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
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Instrumentor {
	private IndexMap<String> Hmap;
	private IndexMap<String> Emap;
	private IndexMap<String> Lmap;
	private IndexMap<String> Fmap;
	private IndexMap<String> Mmap;
	private ClassPool pool;
	private CtClass exType;
	private CFGLoopFinder finder = new CFGLoopFinder();
	private MyExprEditor exprEditor = new MyExprEditor();
	private CtBehavior currentMethod;
	private Program program;
	
	public void visit(Program program) {
		this.program = program;
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
			try {
				pool.appendClassPath(pathElem);
			} catch (NotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		pool.appendSystemPath();

		if (InstrFormat.instrMethodEnterAndLeave) {
			try {
				exType = pool.get("java.lang.Throwable");
			} catch (NotFoundException ex) {
				throw new RuntimeException(ex);
			}
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
		IndexSet<jq_Class> classes = program.getPreparedClasses();

		for (jq_Class c : classes) {
			String cName = c.getName();
			if (cName.equals("java.lang.J9VMInternals"))
				continue;
			System.out.println("YYY " + cName);
			CtClass clazz;
			try {
				clazz = pool.get(cName);
			} catch (NotFoundException ex) {
				throw new RuntimeException(ex);
			}
			List<jq_Method> methods = program.getReachableMethods(c);
			CtBehavior[] inits = clazz.getDeclaredConstructors();
			CtBehavior[] meths = clazz.getDeclaredMethods();
			for (jq_Method m : methods) {
				String mName = m.getName().toString();
				if (mName.equals("<clinit>")) {
					currentMethod = clazz.getClassInitializer();
					assert (currentMethod != null);
					process(m);
				} else if (mName.equals("<init>")) {
					String mDesc = m.getDesc().toString();
					currentMethod = null;
					for (CtBehavior x : inits) {
						if (x.getSignature().equals(mDesc)) {
							currentMethod = x;
							break;
						}
					}
					assert (currentMethod != null);
					process(m);
				} else {
					String mDesc = m.getDesc().toString();
					currentMethod = null;
					for (CtBehavior x : meths) {
						if (x.getName().equals(mName) &&
							x.getSignature().equals(mDesc)) {
							currentMethod = x;
							break;
						}
					}
					assert (currentMethod != null);
					process(m);
				}
			}
			System.out.println("Writing class: " + cName);
			try {
				clazz.writeFile(classesDirName);
			} catch (CannotCompileException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
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

	private int set2(IndexMap<String> map, int bci) {
        String mName;
        if (currentMethod instanceof CtConstructor) {
            mName = ((CtConstructor) currentMethod).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = currentMethod.getName();
        String mDesc = currentMethod.getSignature();
        String cName = currentMethod.getDeclaringClass().getName();
		String s = program.toString(bci, mName, mDesc, cName);
		System.out.println(s);
		int n = map.size();
		int i = map.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private int set() {
        String mName;
        if (currentMethod instanceof CtConstructor) {
            mName = ((CtConstructor) currentMethod).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = currentMethod.getName();
        String mDesc = currentMethod.getSignature();
        String cName = currentMethod.getDeclaringClass().getName();
		String s = program.toString(mName, mDesc, cName);
		int n = Mmap.size();
		int i = Mmap.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private void process(jq_Method m) {
		try {
			int mods = currentMethod.getModifiers();
			if (Modifier.isNative(mods) || Modifier.isAbstract(mods))
				return;
			if (Lmap != null && Modifier.isSynchronized(mods)) {
				String syncExpr = (Modifier.isStatic(mods)) ? "$class" : "$0";
				System.out.print("S ");
				int lIdx = set2(Lmap, -1);
				currentMethod.insertBefore("{ chord.project.Runtime.acqLock(" +
					lIdx + "," + syncExpr + "); }");
			}
			if (InstrFormat.instrMethodAndLoopCounts) {
				ControlFlowGraph cfg = m.getCFG();
				finder.visit(cfg);
				Set<BasicBlock> heads = finder.getLoopHeads();
				for (BasicBlock head : heads) {
				}
				// get bytecode index of each loop header
				// and bytecode index of each loop exits
			}
			currentMethod.instrument(exprEditor);
			if (InstrFormat.instrThreadSpawnAndStart) {
				if (currentMethod.getName().equals("start") &&
					currentMethod.getSignature().equals("()V") &&
					currentMethod.getDeclaringClass().getName().equals("java.lang.Thread")) {
					String s = "chord.project.Runtime.threadStart($0);";
					currentMethod.insertBefore(s);
				}
			}
			if (Mmap != null) {
				int mIdx = set();
				currentMethod.insertBefore("{ chord.project.Runtime.methodEnter(" + mIdx + "); }");
				currentMethod.insertAfter ("{ chord.project.Runtime.methodLeave(" + mIdx + "); }");
				String s = "{ chord.project.Runtime.methodLeave(" + mIdx + "); throw($e); }";
				currentMethod.addCatch(s, exType);
			}
		} catch (Exception ex) {
			System.err.println("WARNING: Ignoring instrumenting method: " +
				currentMethod.getLongName());
			ex.printStackTrace();
		}
	}

	class MyExprEditor extends ExprEditor {
		private int set(IndexMap<String> map, Expr e) {
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


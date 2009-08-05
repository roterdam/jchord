/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import javassist.*;
import javassist.bytecode.Descriptor;
import javassist.expr.*;

import chord.util.ClasspathUtils;
import chord.util.FileUtils;
import chord.util.IndexHashMap;
import chord.util.IndexMap;
import chord.util.IndexSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Instrumentor {
	private static IndexMap<String> Hmap;
	private static IndexMap<String> Emap;
	// private static IndexMap<String> Lmap;
	private static IndexMap<String> Fmap;
	private static IndexMap<String> Mmap;
	private static int numH, numE;
	// private static int numL;

	public static void main(String[] args) throws Exception {
		String fullClassPathName = Properties.classPathName +
			File.pathSeparator + Properties.bootClassPathName;
		ClassPool pool = new ClassPool();
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

		Hmap = new IndexHashMap<String>();
		Emap = new IndexHashMap<String>();
		// Lmap = new IndexHashMap<String>();
		Fmap = new IndexHashMap<String>();
		Mmap = new IndexHashMap<String>();
		numH = Hmap.size();
		numE = Emap.size();
		// numL = Lmap.size();

		String classesDirName = Properties.classesDirName;
		IndexSet<jq_Class> classes = Program.getPreparedClasses();

		for (jq_Class c : classes) {
			CtClass clazz = pool.get(c.getName());
			List<jq_Method> methods = Program.getReachableMethods(c);
			CtBehavior[] inits = clazz.getDeclaredConstructors();
			CtBehavior[] meths = clazz.getDeclaredMethods();
			for (jq_Method m : methods) {
				String mName = m.getName().toString();
				if (mName.equals("<clinit>")) {
					CtBehavior clinit = clazz.getClassInitializer();
					assert (clinit != null);
					process(clinit);
				} else if (mName.equals("<init>")) {
					String mDesc = m.getDesc().toString();
					CtBehavior init = null;
					for (CtBehavior x : inits) {
						if (x.getSignature().equals(mDesc)) {
							init = x;
							break;
						}
					}
					assert (init != null);
					process(init);
				} else {
					String mDesc = m.getDesc().toString();
					CtBehavior meth = null;
					for (CtBehavior x : meths) {
						if (x.getName().equals(mName) &&
							x.getSignature().equals(mDesc)) {
							meth = x;
							break;
						}
					}
					assert (meth != null);
					process(meth);
				}
			}
			clazz.writeFile(classesDirName);
			System.out.println("Writing class: " + clazz.getName());
		}

		String outDirName = Properties.outDirName;
		FileUtils.writeMapToFile(Hmap, (new File(outDirName, "H.dynamic.txt")).getAbsolutePath());
		FileUtils.writeMapToFile(Emap, (new File(outDirName, "E.dynamic.txt")).getAbsolutePath());
		// FileUtils.writeMapToFile(Lmap, (new File(outDirName, "L.dynamic.txt")).getAbsolutePath());
		FileUtils.writeMapToFile(Fmap, (new File(outDirName, "F.dynamic.txt")).getAbsolutePath());
		FileUtils.writeMapToFile(Mmap, (new File(outDirName, "M.dynamic.txt")).getAbsolutePath());
	}

	private static int set(IndexMap<String> map, int bci, CtBehavior m) {
        String mName;
        if (m instanceof CtConstructor) {
            mName = ((CtConstructor) m).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = m.getName();
        String mDesc = m.getSignature();
        String cName = m.getDeclaringClass().getName();
		String s = Program.toString(bci, mName, mDesc, cName);
		int n = map.size();
		int i = map.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private static int set(CtBehavior m) {
        String mName;
        if (m instanceof CtConstructor) {
            mName = ((CtConstructor) m).isClassInitializer() ?
                "<clinit>" : "<init>";
        } else
            mName = m.getName();
        String mDesc = m.getSignature();
        String cName = m.getDeclaringClass().getName();
		String s = Program.toString(mName, mDesc, cName);
		int n = Mmap.size();
		int i = Mmap.getOrAdd(s);
		assert (i == n);
		return i;
	}

	private static int set(IndexMap<String> map, Expr e, CtBehavior method) {
		return set(map, e.indexOfBytecode(), method);
	}

	private static void process(final CtBehavior method) {
		try {
			process1(method);
			process2(method);
			assert (numH == Hmap.size());
			assert (numE == Emap.size());
			// assert (numL == Lmap.size());
		} catch (Exception ex) {
			System.err.println("WARNING: Ignoring instrumenting method: " + method.getLongName());
			ex.printStackTrace();
			numH = Hmap.size();
			numE = Emap.size();
			// numL = Lmap.size();
		}
	}
	private static void process1(final CtBehavior method) throws Exception {
		set(method);
		int mods = method.getModifiers();
		if (Modifier.isNative(mods))
			return;
		if (Modifier.isAbstract(mods))
			return;
/*
		if (Modifier.isSynchronized(mods)) {
			set(Lmap, -1, method);
		}
*/
		method.instrument(new ExprEditor() {
			public void edit(NewExpr e) {
				set(Hmap, e, method);
			}
			public void edit(NewArray e) {
				set(Hmap, e, method);
			}
			public void edit(FieldAccess e) {
				set(Emap, e, method);
			}
			public void edit(ArrayAccess e) {
				set(Emap, e, method);
			}
			public void edit(MonitorEnter e) {
				// set(Lmap, e, method);
			}
		});
	}
	private static void process2(final CtBehavior method) throws Exception {
		int mods = method.getModifiers();
		if (Modifier.isNative(mods))
			return;
		if (Modifier.isAbstract(mods))
			return;
/*
		if (Modifier.isSynchronized(mods)) {
			String syncExpr = (Modifier.isStatic(mods)) ? "$class" : "$0";
			method.insertBefore("{ chord.project.Runtime.acqLockInst(" +
				numL + "," + syncExpr + "); }");
			numL++;
		}
*/
		if (method.getName().equals("start") &&
				method.getDeclaringClass().getName().equals("java.lang.Thread")) {
			method.insertBefore("{ chord.project.Runtime.forkHeadInst($0); }");
		}
/*
		method.insertBefore("{ chord.project.Runtime.methodEnter(); }");
		method.insertAfter ("{ chord.project.Runtime.methodLeave(); }");
*/
		method.instrument(new ExprEditor() {
			public void edit(NewExpr e) {
				try {
					String s = "chord.project.Runtime.befNewInst(" + numH + ");";
					String t = "chord.project.Runtime.aftNewInst(" + numH + ",$_);";
					e.replace("{ " + s + " $_ = $proceed($$); " + t + " }");
					numH++;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			public void edit(NewArray e) {
				try {
					String s = "chord.project.Runtime.newArrayInst(" + numH + ",$_);";
					e.replace("{ $_ = $proceed($$); " + s + " }");
					numH++;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			public void edit(FieldAccess e) {
				try {
					boolean isWr = e.isWriter();
					boolean isStatic = e.isStatic();
					CtField fld = e.getField();
					String fName = fld.getName();
					String fDesc = fld.getSignature();
					String cName = fld.getDeclaringClass().getName();
					String s = Program.toString(fName, fDesc, cName);
					int fIdx = Fmap.getOrAdd(s);
					boolean isPrim = fld.getType().isPrimitive();
					String s1 = isWr ? "$proceed($$);" : "$_ = $proceed();";
					String s2;
					String l = isPrim ? "null" : "$_";
					String r = isPrim ? "null" : "$1";
					if (isStatic) {
						if (isWr) {
							s2 = "chord.project.Runtime.statFldWrInst(" +
								fIdx + "," + r + ");";
						} else
							s2 = "";
					} else {
						String b = "$0";
						if (isWr) {
							s2 = "chord.project.Runtime.instFldWrInst(" +
								numE + "," + b + "," + fIdx + "," + r + ");";
						} else {
							s2 = "chord.project.Runtime.instFldRdInst(" +
								numE + "," + b + "," + fIdx + ");";
						}
					}
					e.replace("{ " + s1 + " " + s2 + " }");
					numE++;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			public void edit(ArrayAccess e) {
				try {
					boolean isWr = e.isWriter();
					boolean isPrim = e.getElemType().isPrimitive();
					String s1, s2;
					String f = "$1";
					if (isWr) {
						s1 = "$proceed($$);";
						String r = isPrim ? "null" : "$2";
						s2 = "chord.project.Runtime.aryElemWrInst(" +
							numE + ",$0," + f + "," + r + ");";
					} else {
						s1 = "$_ = $proceed($$);";
						String l = isPrim ? "null" : "$_";
						s2 = "chord.project.Runtime.aryElemRdInst(" +
							numE + ",$0," + f + ");";
					}
					e.replace("{ " + s1 + " " + s2 + " }");
					numE++;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			public void edit(MonitorEnter e) {
/*
				try {
					e.replace("{ $proceed(); chord.project.Runtime.acqLockInst(" + numL + ",$0); }");
					numL++;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
*/
			}
		});
	}
}


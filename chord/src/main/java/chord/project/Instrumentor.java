/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import gnu.trove.TIntObjectHashMap;
import javassist.*;
import javassist.expr.*;

import chord.instr.InstrScheme;
import chord.instr.InstrScheme.EventFormat;
import chord.util.FileUtils;
import chord.util.IndexHashMap;
import chord.util.IndexMap;
import chord.util.IndexSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Instrumentor {
	private IndexMap<String> Hmap;
	private IndexMap<String> Emap;
	private IndexMap<String> Pmap;
	private IndexMap<String> Fmap;
	private IndexMap<String> Mmap;
	private IndexMap<BasicBlock> Wmap;
	private ClassPool pool;
	private CtClass exType;
	private String mStr;
	private CFGLoopFinder finder = new CFGLoopFinder();
	private MyExprEditor exprEditor = new MyExprEditor();
	private TIntObjectHashMap<String> loopInstrMap =
		new TIntObjectHashMap<String>();
	private InstrScheme scheme = InstrScheme.v();
	private final EventFormat enterAndLeaveMethodEvent =
		scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
	private final EventFormat newAndNewArrayEvent =
		scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
	private final EventFormat getstaticPrimitiveEvent =
		scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
	private final EventFormat getstaticReferenceEvent =
		scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
	private final EventFormat putstaticPrimitiveEvent =
		scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
	private final EventFormat putstaticReferenceEvent =
		scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
	private final EventFormat getfieldPrimitiveEvent =
		scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
	private final EventFormat getfieldReferenceEvent =
		scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
	private final EventFormat putfieldPrimitiveEvent =
		scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
	private final EventFormat putfieldReferenceEvent =
		scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
	private final EventFormat aloadPrimitiveEvent =
		scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
	private final EventFormat aloadReferenceEvent =
		scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
	private final EventFormat astorePrimitiveEvent =
		scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
	private final EventFormat astoreReferenceEvent =
		scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
	private final EventFormat threadStartEvent =
		scheme.getEvent(InstrScheme.THREAD_START);
	private final EventFormat threadJoinEvent =
		scheme.getEvent(InstrScheme.THREAD_JOIN);
	private final EventFormat acquireLockEvent =
		scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
	private final EventFormat releaseLockEvent =
		scheme.getEvent(InstrScheme.RELEASE_LOCK);
	private final EventFormat waitEvent =
		scheme.getEvent(InstrScheme.WAIT);
	private final EventFormat notifyEvent =
		scheme.getEvent(InstrScheme.NOTIFY);
	
	public void visit(Program program) {
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
				throw new ChordRuntimeException(ex);
			}
		}
		pool.appendSystemPath();

		if (enterAndLeaveMethodEvent.present() ||
				scheme.getInstrMethodAndLoopBound() > 0) {
			try {
				exType = pool.get("java.lang.Throwable");
			} catch (NotFoundException ex) {
				throw new ChordRuntimeException(ex);
			}
			assert (exType != null);
		}
		if (scheme.needsHmap())
			Hmap = new IndexHashMap<String>();
		if (scheme.needsEmap())
			Emap = new IndexHashMap<String>();
		if (scheme.needsPmap())
			Pmap = new IndexHashMap<String>();
		if (scheme.needsFmap())
			Fmap = new IndexHashMap<String>();
		if (scheme.needsMmap())
			Mmap = new IndexHashMap<String>();
		if (scheme.getInstrMethodAndLoopBound() > 0)
			Wmap = new IndexHashMap<BasicBlock>();
		
		String classesDirName = Properties.classesDirName;
		IndexSet<jq_Class> classes = program.getPreparedClasses();

		for (jq_Class c : classes) {
			String cName = c.getName();
			if (cName.equals("java.lang.J9VMInternals"))
				continue;
			CtClass clazz;
			try {
				clazz = pool.get(cName);
			} catch (NotFoundException ex) {
				throw new ChordRuntimeException(ex);
			}
			List<jq_Method> methods = program.getReachableMethods(c);
			CtBehavior[] inits = clazz.getDeclaredConstructors();
			CtBehavior[] meths = clazz.getDeclaredMethods();
			for (jq_Method m : methods) {
				CtBehavior method = null;
				String mName = m.getName().toString();
				if (mName.equals("<clinit>")) {
					method = clazz.getClassInitializer();
				} else if (mName.equals("<init>")) {
					String mDesc = m.getDesc().toString();
					for (CtBehavior x : inits) {
						if (x.getSignature().equals(mDesc)) {
							method = x;
							break;
						}
					}
				} else {
					String mDesc = m.getDesc().toString();
					for (CtBehavior x : meths) {
						if (x.getName().equals(mName) &&
							x.getSignature().equals(mDesc)) {
							method = x;
							break;
						}
					}
				}
				assert (method != null);
				try {
					process(method, m);
				} catch (ChordRuntimeException ex) {
					System.err.println("WARNING: Ignoring instrumenting method: " +
						method.getLongName());
					ex.printStackTrace();
				}
			}
			System.out.println("Writing class: " + cName);
			try {
				clazz.writeFile(classesDirName);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			} catch (IOException ex) {
				throw new ChordRuntimeException(ex);
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
		if (Pmap != null) {
			FileUtils.writeMapToFile(Pmap,
				(new File(outDirName, "P.dynamic.txt")).getAbsolutePath());
		}
		if (Fmap != null) {
			FileUtils.writeMapToFile(Fmap,
				(new File(outDirName, "F.dynamic.txt")).getAbsolutePath());
		}
		if (enterAndLeaveMethodEvent.hasMid()) {
			FileUtils.writeMapToFile(Mmap,
				(new File(outDirName, "M.dynamic.txt")).getAbsolutePath());
		}
	}

	private int getBCI(BasicBlock b, jq_Method m) {
		int n = b.size();
		for (int i = 0; i < n; i++) {
			Quad q = b.getQuad(i);
	        int bci = m.getBCI(q);
	        if (bci != -1)
	            return bci;
		}
		throw new ChordRuntimeException();
	}
	private void processLoopEnterCheck(int wId, int headBCI) {
		String sHead = enterLoopCheck + wId + ");";
		String s = loopInstrMap.get(headBCI);
		loopInstrMap.put(headBCI, (s == null) ? sHead : sHead + s);
	}
	private void processLoopLeaveCheck(int wId, int exitBCI) {
		String sExit = leaveLoopCheck + wId + ");";
		String s = loopInstrMap.get(exitBCI);
		loopInstrMap.put(exitBCI, (s == null) ? sExit : s + sExit);
 	}
	private void process(CtBehavior javassistMethod, jq_Method joeqMethod) {
		int mods = javassistMethod.getModifiers();
		if (Modifier.isNative(mods) || Modifier.isAbstract(mods))
			return;
		int mId = -1;
		String mName;
		if (javassistMethod instanceof CtConstructor) {
			mName = ((CtConstructor) javassistMethod).isClassInitializer() ?
				"<clinit>" : "<init>";
		} else
			mName = javassistMethod.getName();
		String mDesc = javassistMethod.getSignature();
		String cName = javassistMethod.getDeclaringClass().getName();
		mStr = Program.toString(mName, mDesc, cName);
		if (Mmap != null) {
			int n = Mmap.size();
			mId = Mmap.getOrAdd(mStr);
			assert (mId == n);
		}
		Map<Quad, Integer> bcMap = joeqMethod.getBCMap();
		if (bcMap != null) {
			if (scheme.getInstrMethodAndLoopBound() > 0) {
				ControlFlowGraph cfg = joeqMethod.getCFG();
				finder.visit(cfg);
				loopInstrMap.clear();
				Set<BasicBlock> heads = finder.getLoopHeads();
				for (BasicBlock head : heads) {
					int headBCI = getBCI(head, joeqMethod);
					int wId = Wmap.getOrAdd(head);
					processLoopEnterCheck(wId, headBCI);
					Set<BasicBlock> exits = finder.getLoopExits(head);
					for (BasicBlock exit : exits) {
						int exitBCI = getBCI(exit, joeqMethod);
						processLoopLeaveCheck(wId, exitBCI);
					}
				}
			}
			try {
				javassistMethod.instrument(exprEditor);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		} else {
			System.out.println(
				"WARNING: Skipping instrumenting body of method: " +
				joeqMethod);
		}
		// NOTE: do not move insertBefore or insertAfter or addCatch
		// calls to a method to before bytecode instrumentation, else
		// bytecode instrumentation offsets could get messed up 
		String enterStr = "";
		String leaveStr = "";
		if (scheme.getInstrMethodAndLoopBound() > 0) {
			enterStr = enterStr + enterMethodCheck + mId + "); ";
			leaveStr = leaveMethodCheck + mId + "); " + leaveStr;
		}
		if (acquireLockEvent.present() && Modifier.isSynchronized(mods)) {
			String syncExpr = Modifier.isStatic(mods) ? "$class" : "$0";
			int pId = set(Pmap, -1);
			enterStr += " chord.project.Runtime.acquireLock(" +
				pId + "," + syncExpr + ");";
		}
		if (enterAndLeaveMethodEvent.present()) {
			enterStr = enterStr + enterMethod + mId + ");";
			leaveStr = leaveMethod + mId + ");" + leaveStr;
		}
		if (!enterStr.equals("")) {
			try {
				javassistMethod.insertBefore("{" + enterStr + "}");
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		if (!leaveStr.equals("")) {
			try {
				javassistMethod.insertAfter("{" + leaveStr + "}");
				String eventCall = "{" + leaveStr + "throw($e);" + "}";
				javassistMethod.addCatch(eventCall, exType);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
	}

	private int set(IndexMap<String> map, Expr e) {
		return set(map, e.indexOfOriginalBytecode());
	}
	private int set(IndexMap<String> map, int bci) {
		int n = map.size();
		int i = map.getOrAdd(bci + "!" + mStr);
		assert (i == n);
		return i;
	}

	class MyExprEditor extends ExprEditor {
		public String insertBefore(int pos) {
			String s = loopInstrMap.get(pos);
			// s may be null in which case this method won't
			// add any instrumentation
			return s;
		}
		public void edit(NewExpr e) {
			if (!newAndNewArrayEvent.present())
				return;
			try {
				int hId = set(Hmap, e);
				String s = befNew + hId + ");";
				String t = aftNew + hId + ",$_);";
				e.replace("{ " + s + " $_ = $proceed($$); " + t + " }");
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		public void edit(NewArray e) {
			if (!newAndNewArrayEvent.present())
				return;
			try {
				int hId = set(Hmap, e);
				String s = newArray + hId + ",$_);";
				e.replace("{ $_ = $proceed($$); " + s + " }");
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		public void edit(FieldAccess e) {
			try {
				boolean isStatic = e.isStatic();
				CtField field;
				CtClass type;
				try {
					field = e.getField();
					type = field.getType();
				} catch (NotFoundException ex) {
					throw new ChordRuntimeException(ex);
				}
				boolean isPrim = type.isPrimitive();
				boolean isWr = e.isWriter();
				String eventCall;
				if (isStatic) {
					if (!scheme.hasStaticEvent())
						return;
					if (isWr) {
						eventCall = isPrim ? putstaticPrimitive(e, field) :
							putstaticReference(e, field);
					} else {
						eventCall = isPrim ? getstaticPrimitive(e, field) :
							getstaticReference(e, field);
					}
				} else {
					if (!scheme.hasFieldEvent())
						return;
					if (isWr) {
						eventCall = isPrim ? putfieldPrimitive(e, field) :
							putfieldReference(e, field);
					} else {
						eventCall = isPrim ? getfieldPrimitive(e, field) :
							getfieldReference(e, field);
					}
				}
				e.replace(eventCall);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		public void edit(ArrayAccess e) {
			if (!scheme.hasArrayEvent())
				return;
			try {
				boolean isWr = e.isWriter();
				boolean isPrim = e.getElemType().isPrimitive();
				String eventCall;
				if (isWr) {
					eventCall = isPrim ? astorePrimitive(e) : astoreReference(e);
				} else {
					eventCall = isPrim ? aloadPrimitive(e) : aloadReference(e);
				}
				e.replace(eventCall);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		private int getFid(CtField field) {
			String fName = field.getName();
			String fDesc = field.getSignature();
			String cName = field.getDeclaringClass().getName();
			String s = Program.toString(fName, fDesc, cName);
			return Fmap.getOrAdd(s);
		}
		private String getstaticPrimitive(FieldAccess e, CtField f) {
			int eId = getstaticPrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			int fId = getstaticPrimitiveEvent.hasFid() ? getFid(f) : -1;
			return "{ $_ = $proceed($$); " + getstaticPrimitive + eId + "," + fId + "); }"; 
		}
		private String getstaticReference(FieldAccess e, CtField f) {
			int eId = getstaticReferenceEvent.hasEid() ? set(Emap, e) : -1;
			int fId = getstaticReferenceEvent.hasFid() ? getFid(f) : -1;
			String oId = getstaticReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + getstaticReference + eId + "," + fId + "," + oId + "); }"; 
		}
		private String putstaticPrimitive(FieldAccess e, CtField f) {
			int eId = putstaticPrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			int fId = putstaticPrimitiveEvent.hasFid() ? getFid(f) : -1;
			return "{ $proceed($$); " + putstaticPrimitive + eId + "," + fId + "); }"; 
		}
		private String putstaticReference(FieldAccess e, CtField f) {
			int eId = putstaticReferenceEvent.hasEid() ? set(Emap, e) : -1;
			int fId = putstaticReferenceEvent.hasFid() ? getFid(f) : -1;
			String oId = putstaticReferenceEvent.hasOid() ? "$0" : "null";
			return "{ $proceed($$); " + putstaticReference + eId + "," + fId + "," + oId + "); }"; 
		}
		private String getfieldPrimitive(FieldAccess e, CtField f) {
			int eId = getfieldPrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			String bId = getfieldPrimitiveEvent.hasBid() ? "$0" : "null";
			int fId = getfieldPrimitiveEvent.hasFid() ? getFid(f) : -1;
			return "{ $_ = $proceed($$); " + getfieldPrimitive + eId + "," + bId + "," + fId + "); }"; 
		}
		private String getfieldReference(FieldAccess e, CtField f) {
			int eId = getfieldReferenceEvent.hasEid() ? set(Emap, e) : -1;
			String bId = getfieldReferenceEvent.hasOid() ? "$0" : "null";
			int fId = getfieldReferenceEvent.hasFid() ? getFid(f) : -1;
			String oId = getfieldReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + getfieldReference + eId + "," + bId + "," + fId + "," + oId + "); }"; 
		}
		private String putfieldPrimitive(FieldAccess e, CtField f) {
			int eId = putfieldPrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			String bId = putfieldPrimitiveEvent.hasBid() ? "$0" : "null";
			int fId = putfieldPrimitiveEvent.hasFid() ? getFid(f) : -1;
			return "{ $proceed($$); " + putfieldPrimitive + eId + "," + bId + "," + fId + "); }"; 
		}
		private String putfieldReference(FieldAccess e, CtField f) {
			int eId = putfieldReferenceEvent.hasEid() ? set(Emap, e) : -1;
			String bId = putfieldReferenceEvent.hasBid() ? "$0" : "null";
			int fId = putfieldReferenceEvent.hasFid() ? getFid(f) : -1;
			String oId = putfieldReferenceEvent.hasOid() ? "$1" : "null";
			return "{ $proceed($$); " + putfieldReference + eId + "," + bId + "," + fId + "," + oId + "); }"; 
		}
		private String aloadPrimitive(ArrayAccess e) {
			int eId = aloadPrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			String bId = aloadPrimitiveEvent.hasBid() ? "$0" : "null";
			String iId = aloadPrimitiveEvent.hasIid() ? "$1" : "-1";
			return "{ $_ = $proceed($$); " + aloadPrimitive + eId + "," + bId + "," + iId + "); }"; 
		}
		private String aloadReference(ArrayAccess e) {
			int eId = aloadReferenceEvent.hasEid() ? set(Emap, e) : -1;
			String bId = aloadReferenceEvent.hasBid() ? "$0" : "null";
			String iId = aloadReferenceEvent.hasIid() ? "$1" : "-1";
			String oId = aloadReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + aloadReference + eId + "," + bId + "," + iId + "," + oId + "); }"; 
		}
		private String astorePrimitive(ArrayAccess e) {
			int eId = astorePrimitiveEvent.hasEid() ? set(Emap, e) : -1;
			String bId = astorePrimitiveEvent.hasBid() ? "$0" : "null";
			String iId = astorePrimitiveEvent.hasIid() ? "$1" : "-1";
			return "{ $proceed($$); " + astorePrimitive + eId + "," + bId + "," + iId + "); }"; 
		}
		private String astoreReference(ArrayAccess e) {
			int eId = astoreReferenceEvent.hasEid() ? set(Emap, e) : -1;
			String bId = astoreReferenceEvent.hasBid() ? "$0" : "null";
			String iId = astoreReferenceEvent.hasIid() ? "$1" : "-1";
			String oId = astoreReferenceEvent.hasOid() ? "$2" : "null";
			return "{ $proceed($$); " + astoreReference + eId + "," + bId + "," + iId + "," + oId + "); }"; 
		}
		public void edit(MethodCall e) {
			CtMethod m;
			try {
				m = e.getMethod();
			} catch (NotFoundException ex) {
				throw new ChordRuntimeException(ex);
			}
			String cName = m.getDeclaringClass().getName();
			String eventCall = null;
			if (cName.equals("java.lang.Object")) {
				String mName = m.getName();
				String mDesc = m.getSignature();
				if (mName.equals("wait") && (mDesc.equals("()V") ||
						mDesc.equals("(L)V") || mDesc.equals("(LI)V"))) {
					if (!waitEvent.present())
						return;
					int pId = waitEvent.hasPid() ? set(Pmap, e) : -1;
					String lId = waitEvent.hasLid() ? "$0" : "null";
					eventCall = wait + pId + "," + lId + ");";
				} else if ((mName.equals("notify") ||
						mName.equals("notifyAll")) && mDesc.equals("()V")) {
					if (!notifyEvent.present())
						return;
					int pId = notifyEvent.hasPid() ? set(Pmap, e) : -1;
					String lId = notifyEvent.hasLid() ? "$0" : "null";
					eventCall = notify + pId + "," + lId + ");";
				}
			} else if (cName.equals("java.lang.Thread")) {
				String mName = m.getName();
				String mDesc = m.getSignature();
				if (mName.equals("start") && mDesc.equals("()V")) {
					if (!threadStartEvent.present())
						return;
					int pId = threadStartEvent.hasPid() ? set(Pmap, e) : -1;
					String oId = threadStartEvent.hasOid() ? "$0" : "null";
					eventCall = threadStart + pId + "," + oId + ");";
				} else if (mName.equals("join") && (mDesc.equals("()V") ||
						mDesc.equals("(L)V") || mDesc.equals("(LI)V"))) {
					if (!threadJoinEvent.present())
						return;
					int pId = threadJoinEvent.hasPid() ? set(Pmap, e) : -1;
					String oId = threadJoinEvent.hasOid() ? "$0" : "null";
					eventCall = threadJoin + pId + "," + oId + ");";
				}
			}
			if (eventCall != null) {
				try {
					e.replace("{ $proceed($$); " + eventCall + " }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(MonitorEnter e) {
			if (!acquireLockEvent.present())
				return;
			try {
				int pId = acquireLockEvent.hasPid() ? set(Pmap, e) : -1;
				String lId = acquireLockEvent.hasLid() ? "$0" : "null";
				String eventCall = acquireLock + pId + "," + lId + ");";
				e.replace("{ $proceed(); " + eventCall + " }");
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
		public void edit(MonitorExit e) {
			if (!releaseLockEvent.present())
				return;
			try {
				int pId = releaseLockEvent.hasPid() ? set(Pmap, e) : -1;
				String lId = releaseLockEvent.hasLid() ? "$0" : "null";
				String eventCall = releaseLock + pId + "," + lId + ");";
				e.replace("{ $proceed(); " + eventCall + " }");
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			}
		}
	}
	private static final String runtimeClassName = "chord.project.Runtime.";
	private static final String enterMethodCheck = runtimeClassName + "enterMethodCheck(";
	private static final String leaveMethodCheck = runtimeClassName + "leaveMethodCheck(";
	private static final String enterLoopCheck = "chord.project.Runtime.enterLoopCheck(";
	private static final String leaveLoopCheck = "chord.project.Runtime.leaveLoopCheck(";
	private static final String enterMethod = runtimeClassName + "enterMethod(";
	private static final String leaveMethod = runtimeClassName + "leaveMethod(";
	private static final String befNew = runtimeClassName + "befNew(";
	private static final String aftNew = runtimeClassName + "aftNew(";
	private static final String newArray = runtimeClassName + "newArray(";
	private static final String getstaticPrimitive = "chord.project.Runtime.getstaticPrimitive(";
	private static final String putstaticPrimitive = "chord.project.Runtime.putstaticPrimitive(";
	private static final String getstaticReference = "chord.project.Runtime.getstaticReference(";
	private static final String putstaticReference = "chord.project.Runtime.putstaticReference(";
	private static final String getfieldPrimitive = "chord.project.Runtime.getfieldPrimitive(";
	private static final String putfieldPrimitive = "chord.project.Runtime.putfieldPrimitive(";
	private static final String getfieldReference = "chord.project.Runtime.getfieldReference(";
	private static final String putfieldReference = "chord.project.Runtime.putfieldReference(";
	private static final String aloadPrimitive = "chord.project.Runtime.aloadPrimitive(";
	private static final String aloadReference = "chord.project.Runtime.aloadReference(";
	private static final String astorePrimitive = "chord.project.Runtime.astorePrimitive(";
	private static final String astoreReference = "chord.project.Runtime.astoreReference(";
	private static final String threadStart = runtimeClassName + "threadStart(";
	private static final String threadJoin = runtimeClassName + "threadJoin(";
	private static final String acquireLock = runtimeClassName + "acquireLock(";
	private static final String releaseLock = runtimeClassName + "releaseLock(";
	private static final String wait = runtimeClassName + "wait(";
	private static final String notify = runtimeClassName + "notify(";
}

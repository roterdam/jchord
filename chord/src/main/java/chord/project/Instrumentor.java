/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import gnu.trove.TIntObjectHashMap;
import javassist.*;
import javassist.expr.*;

import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomP;
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
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Util.Templates.ListIterator;

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
	private DomF domF;
	private DomM domM;
	private DomH domH;
	private DomE domE;
	private DomI domI;
	private DomP domP;
	
	protected IndexMap<String> Fmap;
	protected IndexMap<String> Mmap;
	protected IndexMap<String> Hmap;
	protected IndexMap<String> Emap;
	protected IndexMap<String> Imap;
	protected IndexMap<String> Pmap;
	
	private IndexMap<BasicBlock> Wmap;
	private ClassPool pool;
	private CtClass exType;
	private String mStr;
	private CFGLoopFinder finder = new CFGLoopFinder();
	private MyExprEditor exprEditor = new MyExprEditor();
	private TIntObjectHashMap<String> bciToInstrMap =
		new TIntObjectHashMap<String>();
	private InstrScheme scheme;
	private EventFormat enterAndLeaveMethodEvent;
	private EventFormat newAndNewArrayEvent;
	private EventFormat getstaticPrimitiveEvent;
	private EventFormat getstaticReferenceEvent;
	private EventFormat putstaticPrimitiveEvent;
	private EventFormat putstaticReferenceEvent;
	private EventFormat getfieldPrimitiveEvent;
	private EventFormat getfieldReferenceEvent;
	private EventFormat putfieldPrimitiveEvent;
	private EventFormat putfieldReferenceEvent;
	private EventFormat aloadPrimitiveEvent;
	private EventFormat aloadReferenceEvent;
	private EventFormat astorePrimitiveEvent;
	private EventFormat astoreReferenceEvent;
	private EventFormat threadStartEvent;
	private EventFormat threadJoinEvent;
	private EventFormat acquireLockEvent;
	private EventFormat releaseLockEvent;
	private EventFormat waitEvent;
	private EventFormat notifyEvent;
	private EventFormat methodCallEvent;
	private EventFormat moveEvent;
	private int instrMethodAndLoopBound;
	private boolean convert;
	public void visit(Program program, InstrScheme scheme) {
		String fullClassPathName = Properties.classPathName +
			File.pathSeparator + Properties.mainClassPathName;
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
		this.scheme = scheme;
		instrMethodAndLoopBound = scheme.getInstrMethodAndLoopBound();
		enterAndLeaveMethodEvent = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
		newAndNewArrayEvent = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
		getstaticPrimitiveEvent = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
		getstaticReferenceEvent = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
		putstaticPrimitiveEvent = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
		putstaticReferenceEvent = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
		getfieldPrimitiveEvent = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
		getfieldReferenceEvent = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
		putfieldPrimitiveEvent = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
		putfieldReferenceEvent = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
		aloadPrimitiveEvent = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
		aloadReferenceEvent = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
		astorePrimitiveEvent = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
		astoreReferenceEvent = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
		threadStartEvent = scheme.getEvent(InstrScheme.THREAD_START);
		threadJoinEvent = scheme.getEvent(InstrScheme.THREAD_JOIN);
		acquireLockEvent = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
		releaseLockEvent = scheme.getEvent(InstrScheme.RELEASE_LOCK);
		waitEvent = scheme.getEvent(InstrScheme.WAIT);
		notifyEvent = scheme.getEvent(InstrScheme.NOTIFY);
		methodCallEvent = scheme.getEvent(InstrScheme.METHOD_CALL);
		moveEvent = scheme.getEvent(InstrScheme.MOVE);
		convert = scheme.isConverted();
		
		if (moveEvent.present())
			assert (convert);
		
		if (scheme.needsFmap()) {
			if (convert) {
				domF = (DomF) Project.getTrgt("F");
				Project.runTask(domF);
				Fmap = getUniqueStringMap(domF);
			} else
				Fmap = new IndexHashMap<String>();
		}
		if (scheme.needsMmap()) {
			if (convert) {
				domM = (DomM) Project.getTrgt("M");
				Project.runTask(domM);
				Mmap = getUniqueStringMap(domM);
			} else
				Mmap = new IndexHashMap<String>();
		}
		if (scheme.needsHmap()) {
			if (convert) {
				domH = (DomH) Project.getTrgt("H");
				Project.runTask(domH);
				Hmap = getUniqueStringMap(domH);
			} else
				Hmap = new IndexHashMap<String>();
		}
		if (scheme.needsEmap()) {
			if (convert) {
				domE = (DomE) Project.getTrgt("E");
				Project.runTask(domE);
				Emap = getUniqueStringMap(domE);
			} else
				Emap = new IndexHashMap<String>();
		}
		if (scheme.needsImap()) {
			if (convert) {
				domI = (DomI) Project.getTrgt("I");
				Project.runTask(domI);
				Imap = getUniqueStringMap(domI);
			} else
				Imap = new IndexHashMap<String>();
		}
		if (scheme.needsPmap()) {
			if (convert) {
				domP = (DomP) Project.getTrgt("P");
				Project.runTask(domP);
				Pmap = getUniqueStringMap(domP);
			} else
				Pmap = new IndexHashMap<String>();
		}
		if (enterAndLeaveMethodEvent.present() ||
				releaseLockEvent.present()) {
			try {
				exType = pool.get("java.lang.Throwable");
			} catch (NotFoundException ex) {
				throw new ChordRuntimeException(ex);
			}
			assert (exType != null);
		}
		if (instrMethodAndLoopBound > 0)
			Wmap = new IndexHashMap<BasicBlock>();
		
		String bootClassesDirName = Properties.bootClassesDirName;
		String classesDirName = Properties.classesDirName;
		IndexSet<jq_Class> classes = program.getPreparedClasses();

		for (jq_Class c : classes) {
			String cName = c.getName();
			if (cName.equals("java.lang.J9VMInternals") ||
					cName.startsWith("java.lang.ref."))
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
				String dirName = c.isSystemClass() ? bootClassesDirName : classesDirName;
				clazz.writeFile(dirName);
			} catch (CannotCompileException ex) {
				throw new ChordRuntimeException(ex);
			} catch (IOException ex) {
				throw new ChordRuntimeException(ex);
			}
		}

		String outDirName = Properties.outDirName;
		if (Fmap != null) {
			FileUtils.writeMapToFile(Fmap,
				(new File(outDirName, "F.dynamic.txt")).getAbsolutePath());
		}
		if (Mmap != null) {
			FileUtils.writeMapToFile(Mmap,
				(new File(outDirName, "M.dynamic.txt")).getAbsolutePath());
		}
		if (Hmap != null) {
			FileUtils.writeMapToFile(Hmap,
				(new File(outDirName, "H.dynamic.txt")).getAbsolutePath());
		}
		if (Emap != null) {
			FileUtils.writeMapToFile(Emap,
				(new File(outDirName, "E.dynamic.txt")).getAbsolutePath());
		}
		if (Imap != null) {
			FileUtils.writeMapToFile(Imap,
				(new File(outDirName, "I.dynamic.txt")).getAbsolutePath());
		}
		if (Pmap != null) {
			FileUtils.writeMapToFile(Pmap,
				(new File(outDirName, "P.dynamic.txt")).getAbsolutePath());
		}
	}

	public IndexMap<String> getFmap() { return Fmap; }
	public IndexMap<String> getMmap() { return Mmap; }
	public IndexMap<String> getHmap() { return Hmap; }
	public IndexMap<String> getEmap() { return Emap; }
	public IndexMap<String> getImap() { return Imap; }
	public IndexMap<String> getPmap() { return Pmap; }

	private IndexMap<String> getUniqueStringMap(ProgramDom dom) {
		IndexMap<String> map = new IndexHashMap<String>(dom.size());
		for (int i = 0; i < dom.size(); i++) {
			String s = dom.toUniqueString(dom.get(i));
			if (map.contains(s))
				throw new RuntimeException("Map for domain " + dom +
					" already contains: " + s);
			map.getOrAdd(s);
		}
		return map;
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
	private void attachInstrToBCI(String str, int bci) {
		String s = bciToInstrMap.get(bci);
		bciToInstrMap.put(bci, (s == null) ? str : s + str);
	}
	private void processEnterLoopCheck(int wId, int headBCI) {
		String sHead = enterLoopCheckCall + wId + ");";
		String s = bciToInstrMap.get(headBCI);
		bciToInstrMap.put(headBCI, (s == null) ? sHead : sHead + s);
	}
	private void processLeaveLoopCheck(int wId, int exitBCI) {
		String sExit = leaveLoopCheckCall + wId + ");";
		String s = bciToInstrMap.get(exitBCI);
		bciToInstrMap.put(exitBCI, (s == null) ? sExit : sExit + s);
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
			if (moveEvent.present() || instrMethodAndLoopBound > 0) {
				ControlFlowGraph cfg = joeqMethod.getCFG();
				bciToInstrMap.clear();
				if (moveEvent.present()) {
					for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
							it.hasNext();) {
						BasicBlock bb = it.nextBasicBlock();
						int n = bb.size();
						if (n == 0)
							continue;
						int i = 0;
						String phiStr = "";
						for (; i < n; i++) {
							Quad q = bb.getQuad(i);
							Operator op = q.getOperator();
							if (op instanceof Phi) {
								int pId;
								if (moveEvent.hasPid()) {
									pId = domP.indexOf(q);
									assert (pId != -1);
								} else
									pId = Runtime.MISSING_FIELD_VAL;
								phiStr += moveEventCall + pId + ");";
							} else
								break;
						}
						if (i > 0) {
							int bci = getBCI(bb, joeqMethod);
							if (bci == -1)
								throw new ChordRuntimeException();
							attachInstrToBCI(phiStr, bci);
						}
						for (; i < n; i++) {
							Quad q = bb.getQuad(i);
							Operator op = q.getOperator();
							assert (!(op instanceof Phi));
							if (op instanceof Move) {
								int pId;
								if (moveEvent.hasPid()) {
									pId = domP.indexOf(q);
									assert (pId != -1);
								} else
									pId = Runtime.MISSING_FIELD_VAL;
								assert (pId != -1);
								String moveStr = moveEventCall + pId + ");";
								int bci = joeqMethod.getBCI(q);
								if (bci == -1)
									throw new ChordRuntimeException();
								attachInstrToBCI(moveStr, bci);
							}
						}
					}
				}
				if (instrMethodAndLoopBound > 0) {
					finder.visit(cfg);
					Set<BasicBlock> heads = finder.getLoopHeads();
					for (BasicBlock head : heads) {
						int wId = Wmap.getOrAdd(head);
						int headBCI = getBCI(head, joeqMethod);
						processEnterLoopCheck(wId, headBCI);
					}
					for (BasicBlock head : heads) {
						Set<BasicBlock> exits = finder.getLoopExits(head);
						for (BasicBlock exit : exits) {
							int wId = Wmap.getOrAdd(exit);
							int exitBCI = getBCI(exit, joeqMethod);
							processLeaveLoopCheck(wId, exitBCI);
						}
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
		if (instrMethodAndLoopBound > 0) {
			enterStr = enterStr + enterMethodCheckCall + mId + "); ";
			leaveStr = leaveMethodCheckCall + mId + "); " + leaveStr;
		}
		if (Modifier.isSynchronized(mods)) {
			String syncExpr = Modifier.isStatic(mods) ? "$class" : "$0";
			if (acquireLockEvent.present()) {
				int pId = set(Pmap, -1);
				String str = acquireLockEventCall + pId + "," +
					syncExpr + "); ";
				enterStr = enterStr + str;
			}
			if (releaseLockEvent.present()) {
				int pId = set(Pmap, -2);
				String str = releaseLockEventCall + pId + "," +
					syncExpr + "); ";
				leaveStr = str + leaveStr;
			}
		}
		if (enterAndLeaveMethodEvent.present()) {
			enterStr = enterStr + enterMethodEventCall + mId + ");";
			leaveStr = leaveMethodEventCall + mId + ");" + leaveStr;
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
		String s = bci + "!" + mStr;
		int id;
		if (convert) {
			id = map.indexOf(s);
			if (id == -1) {
				// throw new ChordRuntimeException("Element " + s +
				//	" not found in map.");
				return -1;
			}
		} else {
			int n = map.size();
			id = map.getOrAdd(bci + "!" + mStr);
			assert (id == n);
		}
		return id;
	}

	class MyExprEditor extends ExprEditor {
		public String insertBefore(int pos) {
			String s = bciToInstrMap.get(pos);
			// s may be null in which case this method won't
			// add any instrumentation
			return s;
		}
		public void edit(NewExpr e) {
			if (newAndNewArrayEvent.present()) {
				int hId = newAndNewArrayEvent.hasPid() ? set(Hmap, e) :
					Runtime.MISSING_FIELD_VAL;
				String instr1, instr2;
				if (newAndNewArrayEvent.hasOid()) {
					instr1 = befNewEventCall + hId + ");";
					instr2 = aftNewEventCall + hId + ",$_);";
				} else {
					instr1 = newEventCall + hId + ");";
					instr2 = "";
				}
				try {
					e.replace("{ " + instr1 + " $_ = $proceed($$); " +
						instr2 + " }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(NewArray e) {
			if (newAndNewArrayEvent.present()) {
				int hId = newAndNewArrayEvent.hasPid() ? set(Hmap, e) :
					Runtime.MISSING_FIELD_VAL;
				String instr = newArrayEventCall + hId + ",$_);";
				try {
					e.replace("{ $_ = $proceed($$); " + instr + " }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(FieldAccess e) {
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
			String instr;
			if (isStatic) {
				if (!scheme.hasStaticEvent())
					return;
				if (isWr) {
					instr = isPrim ? putstaticPrimitive(e, field) :
						putstaticReference(e, field);
				} else {
					instr = isPrim ? getstaticPrimitive(e, field) :
						getstaticReference(e, field);
				}
			} else {
				if (!scheme.hasFieldEvent())
					return;
				if (isWr) {
					instr = isPrim ? putfieldPrimitive(e, field) :
						putfieldReference(e, field);
				} else {
					instr = isPrim ? getfieldPrimitive(e, field) :
						getfieldReference(e, field);
				}
			}
			if (instr != null) {
				try {
					e.replace(instr);
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(ArrayAccess e) {
			if (scheme.hasArrayEvent()) {
				boolean isWr = e.isWriter();
				boolean isPrim = e.getElemType().isPrimitive();
				String instr;
				if (isWr) {
					instr = isPrim ? astorePrimitive(e) : astoreReference(e);
					} else {
					instr = isPrim ? aloadPrimitive(e) : aloadReference(e);
				}
				if (instr != null) {
					try {
						e.replace(instr);
					} catch (CannotCompileException ex) {
						throw new ChordRuntimeException(ex);
					}
				}
			}
		}
		public void edit(MethodCall e) {
			String instr = processMethodCall(e);
			if (instr != null) {
				try {
					// NOTE: added $_ recently
					e.replace("{ " + instr + " $_ = $proceed($$); }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(MonitorEnter e) {
			if (acquireLockEvent.present()) {
				int pId = acquireLockEvent.hasPid() ? set(Pmap, e) :
					Runtime.MISSING_FIELD_VAL;
				String l = acquireLockEvent.hasLid() ? "$0" : "null";
				String instr = acquireLockEventCall + pId + "," + l + ");";
				try {
					e.replace("{ $proceed(); " + instr + " }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
		}
		public void edit(MonitorExit e) {
			if (releaseLockEvent.present()) {
				int pId = releaseLockEvent.hasPid() ? set(Pmap, e) :
					Runtime.MISSING_FIELD_VAL;
				String l = releaseLockEvent.hasLid() ? "$0" : "null";
				String instr = releaseLockEventCall + pId + "," + l + ");";
				try {
					e.replace("{ " + instr + " $proceed(); }");
				} catch (CannotCompileException ex) {
					throw new ChordRuntimeException(ex);
				}
			}
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
		if (getstaticPrimitiveEvent.present()) {
			int eId = getstaticPrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			int fId = getstaticPrimitiveEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			return "{ $_ = $proceed($$); " + getstaticPriEventCall + eId +
				"," + fId + "); }"; 
		}
		return null;
	}
	private String getstaticReference(FieldAccess e, CtField f) {
		if (getstaticReferenceEvent.present()) {
			int eId = getstaticReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			int fId = getstaticReferenceEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			String oId = getstaticReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + getstaticRefEcentCall + eId +
				"," + fId + "," + oId + "); }"; 
		}
		return null;
	}
	private String putstaticPrimitive(FieldAccess e, CtField f) {
		if (putstaticPrimitiveEvent.present()) {
			int eId = putstaticPrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			int fId = putstaticPrimitiveEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			return "{ $proceed($$); " + putstaticPriEventCall + eId +
				"," + fId + "); }"; 
		}
		return null;
	}
	private String putstaticReference(FieldAccess e, CtField f) {
		if (putstaticReferenceEvent.present()) {
			int eId = putstaticReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			int fId = putstaticReferenceEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			String oId = putstaticReferenceEvent.hasOid() ? "$1" : "null";
			return "{ $proceed($$); " + putstaticRefEventCall + eId +
				"," + fId + "," + oId + "); }";
		}
		return null;
	}
	private String getfieldPrimitive(FieldAccess e, CtField f) {
		if (getfieldPrimitiveEvent.present()) {
			int eId = getfieldPrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = getfieldPrimitiveEvent.hasBid() ? "$0" : "null";
			int fId = getfieldPrimitiveEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			return "{ $_ = $proceed($$); " + getfieldPriEventCall +
				eId + "," + bId + "," + fId + "); }"; 
		}
		return null;
	}
	private String getfieldReference(FieldAccess e, CtField f) {
		if (getfieldReferenceEvent.present()) {
			int eId = getfieldReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = getfieldReferenceEvent.hasBid() ? "$0" : "null";
			int fId = getfieldReferenceEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			String oId = getfieldReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + getfieldReference +
				eId + "," + bId + "," + fId + "," + oId + "); }"; 
		}
		return null;
	}
	private String putfieldPrimitive(FieldAccess e, CtField f) {
		if (putfieldPrimitiveEvent.present()) {
			int eId = putfieldPrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = putfieldPrimitiveEvent.hasBid() ? "$0" : "null";
			int fId = putfieldPrimitiveEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			return "{ $proceed($$); " + putfieldPriEventCall + eId +
				"," + bId + "," + fId + "); }"; 
		}
		return null;
	}
	private String putfieldReference(FieldAccess e, CtField f) {
		if (putfieldReferenceEvent.present()) {
			int eId = putfieldReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = putfieldReferenceEvent.hasBid() ? "$0" : "null";
			int fId = putfieldReferenceEvent.hasFid() ? getFid(f) :
				Runtime.MISSING_FIELD_VAL;
			String oId = putfieldReferenceEvent.hasOid() ? "$1" : "null";
			return "{ $proceed($$); " + putfieldRefEventCall +
				eId + "," + bId + "," + fId + "," + oId + "); }"; 
		}
		return null;
	}
	private String aloadPrimitive(ArrayAccess e) {
		if (aloadPrimitiveEvent.present()) {
			int eId = aloadPrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = aloadPrimitiveEvent.hasBid() ? "$0" : "null";
			String iId = aloadPrimitiveEvent.hasIid() ? "$1" : "-1";
			return "{ $_ = $proceed($$); " + aloadPriEventCall +
				eId + "," + bId + "," + iId + "); }"; 
		}
		return null;
	}
	private String aloadReference(ArrayAccess e) {
		if (aloadReferenceEvent.present()) {
			int eId = aloadReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = aloadReferenceEvent.hasBid() ? "$0" : "null";
			String iId = aloadReferenceEvent.hasIid() ? "$1" : "-1";
			String oId = aloadReferenceEvent.hasOid() ? "$_" : "null";
			return "{ $_ = $proceed($$); " + aloadRefEventCall +
				eId + "," + bId + "," + iId + "," + oId + "); }"; 
		}
		return null;
	}
	private String astorePrimitive(ArrayAccess e) {
		if (astorePrimitiveEvent.present()) {
			int eId = astorePrimitiveEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = astorePrimitiveEvent.hasBid() ? "$0" : "null";
			String iId = astorePrimitiveEvent.hasIid() ? "$1" : "-1";
			return "{ $proceed($$); " + astorePriEventCall +
				eId + "," + bId + "," + iId + "); }"; 
		}
		return null;
	}
	private String astoreReference(ArrayAccess e) {
		if (astoreReferenceEvent.present()) {
			int eId = astoreReferenceEvent.hasPid() ? set(Emap, e) :
				Runtime.MISSING_FIELD_VAL;
			String bId = astoreReferenceEvent.hasBid() ? "$0" : "null";
			String iId = astoreReferenceEvent.hasIid() ? "$1" : "-1";
			String oId = astoreReferenceEvent.hasOid() ? "$2" : "null";
			return "{ $proceed($$); " + astoreRefEventCall +
				eId + "," + bId + "," + iId + "," + oId + "); }"; 
		}
		return null;
	}
	private String processMethodCall(MethodCall e) {
		// Part 1: add METHOD_CALL event if present
		String instr1;
		if (methodCallEvent.present()) {
			int iId = methodCallEvent.hasPid() ? set(Imap, e) :
				Runtime.MISSING_FIELD_VAL;
			instr1 = " " + methodCallEventCall + iId + ");";
		} else
			instr1 = null;
		// Part 2: add THREAD_START, THREAD_JOIN, WAIT, or NOTIFY event
		// if present and applicable
		String instr2 = null;
		CtMethod m;
		try {
			m = e.getMethod();
		} catch (NotFoundException ex) {
			throw new ChordRuntimeException(ex);
		}
		String cName = m.getDeclaringClass().getName();
		if (cName.equals("java.lang.Object")) {
			String mName = m.getName();
			String mDesc = m.getSignature();
			if (mName.equals("wait") && (mDesc.equals("()V") ||
					mDesc.equals("(L)V") || mDesc.equals("(LI)V"))) {
				if (waitEvent.present()) {
					int pId = waitEvent.hasPid() ? set(Pmap, e) :
						Runtime.MISSING_FIELD_VAL;
					String lId = waitEvent.hasLid() ? "$0" : "null";
					instr2 = waitEventCall + pId + "," + lId + ");";
				}
			} else if ((mName.equals("notify") ||
					mName.equals("notifyAll")) && mDesc.equals("()V")) {
				if (notifyEvent.present()) {
					int pId = notifyEvent.hasPid() ? set(Pmap, e) :
						Runtime.MISSING_FIELD_VAL;
					String lId = notifyEvent.hasLid() ? "$0" : "null";
					instr2 = notifyEventCall + pId + "," + lId + ");";
				}
			}
		} else if (cName.equals("java.lang.Thread")) {
			String mName = m.getName();
			String mDesc = m.getSignature();
			if (mName.equals("start") && mDesc.equals("()V")) {
				if (threadStartEvent.present()) {
					int pId = threadStartEvent.hasPid() ? set(Pmap, e) :
						Runtime.MISSING_FIELD_VAL;
					String oId = threadStartEvent.hasOid() ? "$0" : "null";
					instr2 = threadStartEventCall + pId + "," + oId + ");";
				}
			} else if (mName.equals("join") && (mDesc.equals("()V") ||
					mDesc.equals("(L)V") || mDesc.equals("(LI)V"))) {
				if (threadJoinEvent.present()) {
					int pId = threadJoinEvent.hasPid() ? set(Pmap, e) :
						Runtime.MISSING_FIELD_VAL;
					String oId = threadJoinEvent.hasOid() ? "$0" : "null";
					instr2 = threadJoinEventCall + pId + "," + oId + ");";
				}
			}
		}
		if (instr1 == null)
			return instr2;
		if (instr2 == null)
			return instr1;
		return instr1 + instr2;
	}

	private static final String runtimeClassName = "chord.project.Runtime.";
	private static final String enterMethodCheckCall = runtimeClassName + "enterMethodCheck(";
	private static final String leaveMethodCheckCall = runtimeClassName + "leaveMethodCheck(";
	private static final String enterLoopCheckCall = runtimeClassName + "enterLoopCheck(";
	private static final String leaveLoopCheckCall = runtimeClassName + "leaveLoopCheck(";
	private static final String enterMethodEventCall = runtimeClassName + "enterMethod(";
	private static final String leaveMethodEventCall = runtimeClassName + "leaveMethod(";
	private static final String befNewEventCall = runtimeClassName + "befNew(";
	private static final String aftNewEventCall = runtimeClassName + "aftNew(";
	private static final String newEventCall = runtimeClassName + "new(";
	private static final String newArrayEventCall = runtimeClassName + "newArray(";
	private static final String getstaticPriEventCall = runtimeClassName + "getstaticPrimitive(";
	private static final String putstaticPriEventCall = runtimeClassName + "putstaticPrimitive(";
	private static final String getstaticRefEcentCall = runtimeClassName + "getstaticReference(";
	private static final String putstaticRefEventCall = runtimeClassName + "putstaticReference(";
	private static final String getfieldPriEventCall = runtimeClassName + "getfieldPrimitive(";
	private static final String putfieldPriEventCall = runtimeClassName + "putfieldPrimitive(";
	private static final String getfieldReference = runtimeClassName + "getfieldReference(";
	private static final String putfieldRefEventCall = runtimeClassName + "putfieldReference(";
	private static final String aloadPriEventCall = runtimeClassName + "aloadPrimitive(";
	private static final String aloadRefEventCall = runtimeClassName + "aloadReference(";
	private static final String astorePriEventCall = runtimeClassName + "astorePrimitive(";
	private static final String astoreRefEventCall = runtimeClassName + "astoreReference(";
	private static final String methodCallEventCall = runtimeClassName + "methodCall(";
	private static final String threadStartEventCall = runtimeClassName + "threadStart(";
	private static final String threadJoinEventCall = runtimeClassName + "threadJoin(";
	private static final String waitEventCall = runtimeClassName + "wait(";
	private static final String notifyEventCall = runtimeClassName + "notify(";
	private static final String acquireLockEventCall = runtimeClassName + "acquireLock(";
	private static final String releaseLockEventCall = runtimeClassName + "releaseLock(";
	private static final String moveEventCall = runtimeClassName + "move(";
}

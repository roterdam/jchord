/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.util.HashSet;
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
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Invoke.InvokeInterface;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Main.HostedVM;
import joeq.Util.Templates.List;
import joeq.Util.Templates.ListIterator;

import chord.project.Messages;
import chord.project.ChordProperties;
import chord.util.ArraySet;
import chord.util.Timer;
import chord.util.tuple.object.Pair;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ForNameReflectionAnalyzer {
	private static final String DYNAMIC_CLASS_NOT_FOUND =
		"WARN: Class named `%s` likely loaded dynamically was not found in classpath; skipping.";
	private static final boolean DEBUG = false;
	private final Classpath classpath;
	private ControlFlowGraph cfg;
	private int numArgs;
	// sets of all forname/newinst call sites
	// initialized in first iteration
	private final Set<Quad> forNameSites = new HashSet<Quad>();
	private final Set<Quad> newInstSites = new HashSet<Quad>();
	// local vars not tracked because they were assigned something
	// that is unanalyzable (e.g., a formal argument, return result of
	// a function call, a static/instance field, etc.)
	private final Set<Register> abortedVars = new HashSet<Register>();
	private final Set<Register> trackedVars = new HashSet<Register>();
	private final Set<Pair<Register, jq_Reference>> resolutions =
		new HashSet<Pair<Register, jq_Reference>>();
	private final Set<Pair<Quad, jq_Reference>> resolvedForNameSites =
		new ArraySet<Pair<Quad, jq_Reference>>();
	private final Set<Pair<Quad, jq_Reference>> resolvedNewInstSites =
		new ArraySet<Pair<Quad, jq_Reference>>();
	private boolean changed;

	public Set<Pair<Quad, jq_Reference>> getResolvedForNameSites() {
		return resolvedForNameSites;
	}
	public Set<Pair<Quad, jq_Reference>> getResolvedNewInstSites() {
		return resolvedNewInstSites;
	}
	public ForNameReflectionAnalyzer(Classpath cp) {
		classpath = cp;
	}
	public void run(jq_Method m) {
		resolvedForNameSites.clear();
		resolvedNewInstSites.clear();
		cfg = m.getCFG();
		initForNameAndNewInstSites();
		if (forNameSites.isEmpty())
			return;
		numArgs = m.getParamTypes().length;
		resolveForNameSites();
		if (newInstSites.isEmpty())
			return;
		resolveNewInstSites();
	}
	private void initForNameAndNewInstSites() {
		forNameSites.clear();
		newInstSites.clear();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
				it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					jq_Method n = Invoke.getMethod(q).getMethod();
					String cName = n.getDeclaringClass().getName();
					if (cName.equals("java.lang.Class")) {
						String mName = n.getName().toString();
						if (mName.equals("forName"))
							forNameSites.add(q);
						else if (mName.equals("newInstance"))
							newInstSites.add(q);
					}
				}
			}
		}
		if (DEBUG) {
			if (!forNameSites.isEmpty()) {
				System.out.println("*** FORNAME SITES in method: " + cfg.getMethod());
				for (Quad q : forNameSites)
					System.out.println("\t" + q);
			}
			if (!newInstSites.isEmpty()) {
				System.out.println("*** NEWINST SITES in method: " + cfg.getMethod());
				for (Quad q : newInstSites)
					System.out.println("\t" + q);
			}
		}
	}
	private void resolveForNameSites() {
		abortedVars.clear();
		initAbortedVars(false);
		trackedVars.clear();
		for (Quad q : forNameSites) {
			Register r = Invoke.getParamList(q).get(0).getRegister();
			trackedVars.add(r);
		}
		resolutions.clear();
		changed = true;
		while (changed) {
			changed = false;
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
					it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
					Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof Move || op instanceof CheckCast) {
						Register l = Move.getDest(q).getRegister();
						Operand ro = Move.getSrc(q);
						if (ro instanceof RegisterOperand) {
							Register r = ((RegisterOperand) ro).getRegister();
							processCopy(l, r);
						} else if (ro instanceof AConstOperand &&
								trackedVars.contains(l)) {
							Object v = ((AConstOperand) ro).getValue();
							if (v instanceof String) {
								jq_Reference t = resolveType((String) v);
								if (t != null) {
									Pair<Register, jq_Reference> p =
										new Pair<Register, jq_Reference>(l, t);
									if (resolutions.add(p))
										changed = true;
								}
							}
						}
					} else if (op instanceof Phi)
						processPhi(q);
				}
			}
		}
		for (Quad q : forNameSites) {
			Register v = Invoke.getParamList(q).get(0).getRegister();
			if (!abortedVars.contains(v)) {
				for (Pair<Register, jq_Reference> p : resolutions) {
					if (p.val0 == v) {
						Pair<Quad, jq_Reference> p2 =
							new Pair<Quad, jq_Reference>(q, p.val1);
						resolvedForNameSites.add(p2);
					}
				}
			}
		}
		if (DEBUG) {
			if (!resolvedForNameSites.isEmpty()) {
				System.out.println("*** FORNAME RESOLUTIONS in method: " +
					cfg.getMethod());
				for (Pair<Quad, jq_Reference> p : resolvedForNameSites)
					System.out.println("\t" + p);
			}
		}
	}
	private void resolveNewInstSites() {
		resolutions.clear();
		for (Pair<Quad, jq_Reference> p : resolvedForNameSites) {
			Quad q = p.val0;
			RegisterOperand lo = Invoke.getDest(q);
			if (lo != null) {
				Register l = lo.getRegister();
				Pair<Register, jq_Reference> p2 =
					new Pair<Register, jq_Reference>(l, p.val1);
				resolutions.add(p2);
			}
		}
		abortedVars.clear();
		initAbortedVars(true);
		trackedVars.clear();
		for (Quad q : newInstSites) {
			Register r = Invoke.getParamList(q).get(0).getRegister();
			trackedVars.add(r);
		}
		changed = true;
		while (changed) {
			changed = false;
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
					it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
					Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof Move || op instanceof CheckCast) {
						Operand ro = Move.getSrc(q);
						if (ro instanceof RegisterOperand) {
							Register l = Move.getDest(q).getRegister();
							Register r = ((RegisterOperand) ro).getRegister();
							processCopy(l, r);
						}
					} else if (op instanceof Phi)
						processPhi(q);
				}
			}
		}
		for (Quad q : newInstSites) {
			Register v = Invoke.getParamList(q).get(0).getRegister();
			if (!abortedVars.contains(v)) {
				for (Pair<Register, jq_Reference> p : resolutions) {
					if (p.val0 == v) {
						Pair<Quad, jq_Reference> p2 =
							new Pair<Quad, jq_Reference>(q, p.val1);
						resolvedNewInstSites.add(p2);
					}
				}
			}
		}
		if (DEBUG) {
			if (!resolvedNewInstSites.isEmpty()) {
				System.out.println("*** NEWINST RESOLUTIONS in method: " +
					cfg.getMethod());
				for (Pair<Quad, jq_Reference> p : resolvedNewInstSites)
					System.out.println("\t" + p);
			}
		}
	}
	private void initAbortedVars(boolean isNewInst) {
		RegisterFactory rf = cfg.getRegisterFactory();
		for (int i = 0; i < numArgs; i++)
			abortedVars.add(rf.get(i));
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
				it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				Quad q = it2.nextQuad();
				Operator op = q.getOperator();
				if (op instanceof Move || op instanceof CheckCast ||
						op instanceof Phi)
					continue;
				if (isNewInst && op instanceof Invoke &&
						forNameSites.contains(q)) {
					boolean isResolved = false;
					for (Pair<Quad, jq_Reference> p : resolvedForNameSites) {
						if (p.val0 == q) {
							isResolved = true;
							break;
						}
					}
					if (isResolved)
						continue;
				}
				ListIterator.RegisterOperand it3 =
					q.getDefinedRegisters().registerOperandIterator();
				while (it3.hasNext()) {
					Register r = it3.nextRegisterOperand().getRegister();
					abortedVars.add(r);
				}
			}
		}
	}
	private void processCopy(Register l, Register r) {
		if (abortedVars.contains(r)) {
			if (abortedVars.add(l))
				changed = true;
		} else if (trackedVars.contains(l)) {
			if (trackedVars.add(r))
				changed = true;
			else {
				Set<jq_Reference> tl = new ArraySet<jq_Reference>();
				for (Pair<Register, jq_Reference> p : resolutions) {
					if (p.val0 == r)
						tl.add(p.val1);
				}
				for (jq_Reference t : tl) {
					Pair<Register, jq_Reference> p =
						new Pair<Register, jq_Reference>(l, t);
					if (resolutions.add(p))
						changed = true;
				 }
			}
		}
	}

	private void processPhi(Quad q) {
		Register l = Phi.getDest(q).getRegister();
		ParamListOperand roList = Phi.getSrcs(q);
		int n = roList.length();
		for (int i = 0; i < n; i++) {
			RegisterOperand ro = roList.get(i);
			if (ro != null) {
				Register r = ro.getRegister();
				processCopy(l, r);
			}
		}
	}
	private jq_Reference resolveType(String clsName) {
		// check whether class is present in the classpath to avoid a
		// NoClassDefFoundError
		String resName = Classpath.classnameToResource(clsName);
		if (classpath.getResourcePath(resName) == null) {
			Messages.log(DYNAMIC_CLASS_NOT_FOUND, clsName);
			return null;
		}
		return (jq_Reference) jq_Type.parseType(clsName);
	}
}

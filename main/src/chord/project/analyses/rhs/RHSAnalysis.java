/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses.rhs;

import gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.ICICG;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.program.Location;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.JavaAnalysis;
import chord.util.Alarm;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Implementation of the Reps-Horwitz-Sagiv algorithm for context-sensitive
 * dataflow analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class RHSAnalysis<PE extends IEdge, SE extends IEdge> extends
		JavaAnalysis {
	protected static boolean DEBUG = false;
	protected List<Trio<Location, PE, Integer>> workList = new ArrayList<Trio<Location, PE, Integer>>();
	// Here, a SortedMap of Set is used rather than a single set to represent
	// the rings described in the Bebop paper
	protected Map<Inst, Map<Integer, Set<PE>>> pathEdges = new HashMap<Inst, Map<Integer, Set<PE>>>();
	protected Map<jq_Method, Set<SE>> summEdges = new HashMap<jq_Method, Set<SE>>();
	protected DomI domI;
	protected DomM domM;
	protected ICICG cicg;
	protected TObjectIntHashMap<Inst> quadToRPOid;
	protected Map<Quad, Location> invkQuadToLoc;
	protected Map<jq_Method, Set<Quad>> callersMap = new HashMap<jq_Method, Set<Quad>>();
	protected Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();
	protected boolean isInited;
	private int timeout = Integer.getInteger("chord.rhs.timeout", 0);
	private Alarm alarm;
	protected final boolean useBFS = useBFS();
	protected final boolean mustMerge = mustMerge();
	protected final boolean mayMerge = mayMerge();
	protected final boolean keepRings = keepRings();
	protected jq_Method currentMethod;
	protected BasicBlock currentBB;

	protected RHSAnalysis() {
		if (mustMerge && !mayMerge) {
			Messages.fatal("Cannot create RHS analysis '" + getName()
					+ "' with mustMerge but without mayMerge.");
		}
		if (mustMerge && keepRings) {
			Messages.fatal("Cnnot create RHS analysis '" + getName()
					+ "' with keepRings and mustMerge");
		}
	}

	// get the initial set of path edges
	public abstract Set<Pair<Location, PE>> getInitPathEdges();

	// get the path edge(s) in callee for target method m called from call site
	// q
	// with caller path edge pe
	public abstract PE getInitPathEdge(Quad q, jq_Method m, PE pe);

	// get outgoing path edge(s) from q, given incoming path edge pe into q.
	// q is guaranteed to not be an invoke statement, return statement, entry
	// basic block, or exit basic block
	// the set returned can be reused by client
	public abstract PE getMiscPathEdge(Quad q, PE pe);

	// q is an invoke statement and m is the target method.
	// get path edge to successor of q given path edge into q and summary edge
	// of
	// a target method m of the call site q.
	// returns null if the path edge into q is not compatible with the summary
	// edge.
	public abstract PE getInvkPathEdge(Quad q, PE clrPE, jq_Method m, SE tgtSE);

	public abstract PE getCopy(PE pe);

	// m is a method and pe is a path edge from entry to exit of m
	// (in case of forward analysis) or vice versa (in case of backward
	// analysis)
	// that must be lifted to a summary edge
	public abstract SE getSummaryEdge(jq_Method m, PE pe);

	public Set<PE> getPathEdge(Inst inst) {
		Set<PE> ret = new HashSet<PE>();
		for (Map.Entry<Integer, Set<PE>> entry : pathEdges.get(inst).entrySet()) {
			ret.addAll(entry.getValue());
		}
		return ret;
	}

	/**
	 * Determines whether this analysis should merge path edges at each program
	 * point that have the same source state but different target states and,
	 * likewise, summary edges of each method that have the same source state
	 * and different target states.
	 * 
	 * @return true iff (path or summary) edges with the same source state and
	 *         different target states should be merged.
	 */
	public abstract boolean mayMerge();

	public abstract boolean mustMerge();

	/**
	 * Determines whether this analysis should keep rings to record the length
	 * of the shortest trajectory to reachable program point. Note if keepRings
	 * returns True, mustMerge must return false.
	 * 
	 * @return true iff rings are used to keep the length of the shortest
	 *         trajectories
	 */
	public abstract boolean keepRings();

	public boolean useBFS() {
		return true;
	}

	/**
	 * Provides the call graph to be used by the analysis.
	 * 
	 * @return The call graph to be used by the analysis.
	 */
	public abstract ICICG getCallGraph();

	private Set<Quad> getCallers(jq_Method m) {
		Set<Quad> callers = callersMap.get(m);
		if (callers == null) {
			callers = cicg.getCallers(m);
			callersMap.put(m, callers);
		}
		return callers;
	}

	private Set<jq_Method> getTargets(Quad i) {
		Set<jq_Method> targets = targetsMap.get(i);
		if (targets == null) {
			targets = cicg.getTargets(i);
			targetsMap.put(i, targets);
		}
		return targets;
	}

	protected void done() {
		if (timeout > 0)
			alarm.doneAllPasses();
	}

	protected void init() {
		if (isInited)
			return;
		if (timeout > 0) {
			alarm = new Alarm(timeout);
			alarm.initAllPasses();
		}
		domI = (DomI) ClassicProject.g().getTrgt("I");
		ClassicProject.g().runTask(domI);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		cicg = getCallGraph();
		quadToRPOid = new TObjectIntHashMap<Inst>();
		invkQuadToLoc = new HashMap<Quad, Location>();
		for (jq_Method m : cicg.getNodes()) {
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			quadToRPOid.put(cfg.entry(), 0);
			int rpoId = 1;
			for (BasicBlock bb : cfg.reversePostOrder()) {
				for (int i = 0; i < bb.size(); i++) {
					Quad q = bb.getQuad(i);
					quadToRPOid.put(q, rpoId++);
					if (q.getOperator() instanceof Invoke) {
						Location loc = new Location(m, bb, i, q);
						invkQuadToLoc.put(q, loc);
					}
				}
			}
			quadToRPOid.put(cfg.exit(), rpoId);
		}
		isInited = true;
	}

	/**
	 * Run an instance of the analysis afresh. Clients may call this method
	 * multiple times from their {@link #run()} method. Clients must override
	 * method {@link #getInitPathEdges()} to return a new "seed" each time they
	 * call this method.
	 */
	protected void runPass() throws TimeoutException {
		init();
		if (timeout > 0)
			alarm.initNewPass();
		// clear these sets since client may call this method multiple times
		workList.clear();
		summEdges.clear();
		pathEdges.clear();
		Set<Pair<Location, PE>> initPEs = getInitPathEdges();
		for (Pair<Location, PE> pair : initPEs) {
			Location loc = pair.val0;
			PE pe = pair.val1;
			addPathEdge(loc, pe, 0);
		}
		propagate();
	}

	protected void printSummaries() {
		for (jq_Method m : summEdges.keySet()) {
			System.out.println("SE of " + m);
			Set<SE> seSet = summEdges.get(m);
			if (seSet != null) {
				for (SE se : seSet)
					System.out.println("\tSE " + se);
			}
		}
		for (Inst i : pathEdges.keySet()) {
			System.out.println("PE of " + i);
			Set<PE> peSet = getPathEdge(i);
			if (peSet != null) {
				for (PE pe : peSet)
					System.out.println("\tPE " + pe);
			}
		}
	}

	/**
	 * Propagate forward or backward until fixpoint is reached.
	 */
	private void propagate() throws TimeoutException {
		while (!workList.isEmpty()) {
			if (timeout > 0 && alarm.passTimedOut()) {
				System.out.println("TIMED OUT");
				throw new TimeoutException();
			}
			if (DEBUG) {
				System.out.println("WORKLIST:");
				for (Trio<Location, PE, Integer> trio : workList) {
					Location loc = trio.val0;
					Inst i = (loc.q == null) ? (Inst) loc.bb : loc.q;
					int id = quadToRPOid.get(i);
					System.out.println("\t" + trio + " " + id);
				}
			}
			int last = workList.size() - 1;
			Trio<Location, PE, Integer> trio = workList.remove(last);
			Location loc = trio.val0;
			PE pe = trio.val1;
			Quad q = loc.q;
			jq_Method m = loc.m;
			BasicBlock bb = loc.bb;
			int trajLength = trio.val2;
			int newTrajLength = this.keepRings ? trio.val2 + 1 : trio.val2;
			currentMethod = m;
			currentBB = bb;
			if (DEBUG)
				System.out.println("Processing loc: " + loc + " PE: " + pe);
			if (q == null) {
				// method entry or method exit
				if (bb.isEntry()) {
					for (BasicBlock bb2 : bb.getSuccessors()) {
						Quad q2;
						int q2Idx;
						if (bb2.size() == 0) {
							q2 = null;
							q2Idx = -1;
						} else {
							q2 = bb2.getQuad(0);
							q2Idx = 0;
						}
						Location loc2 = new Location(m, bb2, q2Idx, q2);
						PE pe2 = mayMerge ? getCopy(pe) : pe;
						addPathEdge(loc2, pe2, newTrajLength);
					}
				} else {
					assert (bb.isExit());
					processExit(m, pe);
				}
			} else {
				// invoke or misc quad
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					Set<jq_Method> targets = getTargets(q);
					if (targets.isEmpty()) {
						PE pe2 = mayMerge ? getCopy(pe) : pe;
						propagatePEtoPE(m, bb, loc.qIdx, pe2, trajLength);
					} else {
						for (jq_Method m2 : targets) {
							if (DEBUG)
								System.out.println("\tTarget: " + m2);
							PE pe2 = getInitPathEdge(q, m2, pe);
							BasicBlock bb2 = m2.getCFG().entry();
							Location loc2 = new Location(m2, bb2, -1, null);
							addPathEdge(loc2, pe2, trajLength);
							Set<SE> seSet = summEdges.get(m2);
							if (seSet == null) {
								if (DEBUG)
									System.out.println("\tSE set empty");
								continue;
							}
							for (SE se : seSet) {
								if (DEBUG)
									System.out.println("\tTesting SE: " + se);
								if (propagateSEtoPE(pe, trajLength, loc, m2, se)) {
									if (DEBUG)
										System.out.println("\tMatched");
									if (mustMerge) {
										// this was only SE; stop looking for
										// more
										break;
									}
								} else {
									if (DEBUG)
										System.out.println("\tDid not match");
								}
							}
						}
					}
				} else {
					PE pe2 = getMiscPathEdge(q, pe);
					propagatePEtoPE(m, bb, loc.qIdx, pe2, newTrajLength);
				}
			}
		}
	}

	private void processExit(jq_Method m, PE pe) {
		SE se = getSummaryEdge(m, pe);
		Set<SE> seSet = summEdges.get(m);
		if (DEBUG)
			System.out.println("\tChecking if " + m + " has SE: " + se);
		SE seToAdd = se;
		if (seSet == null) {
			seSet = new HashSet<SE>();
			summEdges.put(m, seSet);
			seSet.add(se);
			if (DEBUG)
				System.out.println("\tNo, adding it as first SE");
		} else if (mayMerge) {
			boolean matched = false;
			for (SE se2 : seSet) {
				if (se2.canMerge(se)) {
					if (DEBUG)
						System.out.println("\tNo, but matches SE: " + se2);
					boolean changed = se2.mergeWith(se);
					if (DEBUG)
						System.out.println("\tNew SE after merge: " + se2);
					if (!changed) {
						if (DEBUG)
							System.out.println("\tExisting SE did not change");
						return;
					}
					if (DEBUG)
						System.out.println("\tExisting SE changed");
					// se2 is already in summEdges(m), so no need to add it
					seToAdd = se2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG)
					System.out.println("\tNo, adding");
				seSet.add(se);
			}
		} else if (!seSet.add(se)) {
			if (DEBUG)
				System.out.println("\tYes, not adding");
			return;
		}
		for (Quad q2 : getCallers(m)) {
			if (DEBUG)
				System.out.println("\tCaller: " + q2 + " in " + q2.getMethod());
			Map<Integer, Set<PE>> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			// make a copy as propagateSEtoPE might add a path edge to this set
			// itself;
			// in this case we could get a ConcurrentModification exception if
			// we don't
			// make a copy.
			List<Map.Entry<Integer, Set<PE>>> peList = new ArrayList<Map.Entry<Integer, Set<PE>>>(
					peSet.entrySet());
			Location loc2 = invkQuadToLoc.get(q2);
			for (Map.Entry<Integer, Set<PE>> entry : peList) {
				for (PE pe2 : entry.getValue()) {
					if (DEBUG)
						System.out.println("\tTesting PE: " + pe2);
					boolean match = propagateSEtoPE(pe2, entry.getKey(), loc2,
							m, seToAdd);
					if (match) {
						if (DEBUG)
							System.out.println("\tMatched");
					} else {
						if (DEBUG)
							System.out.println("\tDid not match");
					}
				}
			}
		}
	}

	private void addPathEdgeToRings(PE pe, int trajLength,
			Map<Integer, Set<PE>> rings) {
		Set<PE> peSet = rings.get(trajLength);
		if (peSet == null) {
			peSet = new HashSet<PE>();
			rings.put(trajLength, peSet);
		}
		peSet.add(pe);
	}

	private void addPathEdge(Location loc, PE pe, int trajLength) {
		if (DEBUG)
			System.out.println("\tChecking if " + loc + " has PE: " + pe);
		Quad q = loc.q;
		Inst i = (q != null) ? q : (Inst) loc.bb;
		Map<Integer, Set<PE>> rings = pathEdges.get(i);
		PE peToAdd = pe;
		if (rings == null) {// The first path edge of this location
			rings = new TreeMap<Integer, Set<PE>>();
			pathEdges.put(i, rings);
			Set<PE> peSet = new HashSet<PE>();
			peSet.add(pe);
			rings.put(trajLength, peSet);
			if (DEBUG)
				System.out.println("\tNo, adding it as first PE");
		} else if (mayMerge) {// if mayMerge==true, the method would try to
								// merge the new path edge with existing path
								// edges
			boolean matched = false;
			PE peToMove = null;
			Integer toMovePETrajLength = null;
			out: for (Map.Entry<Integer, Set<PE>> entry : rings.entrySet()) {
				for (PE storedPE : entry.getValue()) {
					if (storedPE.canMerge(pe)) {
						if (DEBUG)
							System.out.println("\tNo, but matches PE: "
									+ storedPE);
						boolean changed = storedPE.mergeWith(pe);
						if (DEBUG)
							System.out.println("\tNew PE after merge: "
									+ storedPE);
						if (!changed) {
							if (storedPE.equals(pe)) {// storePE == pe,
														// trajLength =
														// min(pe.trajLength,
														// storedPE.trajLength)
								if (entry.getKey() <= trajLength) {
									if (DEBUG) {
										System.out
												.println("\tExisting PE and its trajectory length did not change");
									}
									return;
								}
								if (DEBUG) {
									System.out
											.println("\tExisting PE did not change but its trajectory length changed");
								}
							}
						} else if (DEBUG)
							System.out.println("\tExisting PE changed");
						peToAdd = peToMove = storedPE;
						toMovePETrajLength = entry.getKey();
						matched = true;
						break out;
					}
				}
			}
			if (matched == true) {
				assert (peToMove != null && toMovePETrajLength != null);
				rings.get(toMovePETrajLength).remove(peToMove);
				this.addPathEdgeToRings(peToMove, trajLength, rings);
				for (int j = workList.size() - 1; j >= 0; j--) {
					Trio<Location, PE, Integer> trio = workList.get(j);
					PE peInWL = trio.val1;
					if (peInWL.equals(peToMove)) {
						trio.val2 = Math.min(trio.val2, trajLength);
						return;
					}
				}
			} else {
				if (DEBUG)
					System.out.println("\tNo, adding");
				this.addPathEdgeToRings(pe, trajLength, rings);
			}
		} else {// Not merge at all
			boolean ifAdded = true;
			boolean ifMoved = false;
			Integer peToMoveTrajLength = null;
			out: for (Map.Entry<Integer, Set<PE>> entry : rings.entrySet()) {
				for (PE storedPE : entry.getValue()) {
					if (storedPE.equals(pe)) {
						ifAdded = false;
						if (entry.getKey() > trajLength)
							ifMoved = true;
						break out;
					}
				}
			}
			if (!ifAdded && !ifMoved) {
				if (DEBUG)
					System.out.println("\tYes, not adding nor moving");
				return;
			}
			if (peToMoveTrajLength != null)
				rings.get(peToMoveTrajLength).remove(pe);
			rings.get(trajLength).add(pe);
		}
		assert (peToAdd != null);
		Trio<Location, PE, Integer> trio = new Trio<Location, PE, Integer>(loc,
				peToAdd, trajLength);
		int j = workList.size() - 1;
		if (useBFS) {
			jq_Method m = loc.m;
			int rpoId = quadToRPOid.get(i);
			for (; j >= 0; j--) {
				Location loc2 = workList.get(j).val0;
				if (loc2.m != m)
					break;
				Inst i2 = (loc2.q != null) ? loc2.q : (Inst) loc2.bb;
				int rpoId2 = quadToRPOid.get(i2);
				if (rpoId2 > rpoId)
					break;
			}
		}
		if (DEBUG)
			System.out.println("\tAlso adding to worklist at " + (j + 1));
		workList.add(j + 1, trio);
	}

	private void propagatePEtoPE(jq_Method m, BasicBlock bb, int qIdx, PE pe,
			int trajLength) {
		if (qIdx != bb.size() - 1) {
			int q2Idx = qIdx + 1;
			Quad q2 = bb.getQuad(q2Idx);
			Location loc2 = new Location(m, bb, q2Idx, q2);
			addPathEdge(loc2, pe, trajLength);
			return;
		}
		boolean isFirst = true;
		for (BasicBlock bb2 : bb.getSuccessors()) {
			int q2Idx;
			Quad q2;
			if (bb2.size() == 0) {
				q2Idx = -1;
				q2 = null;
			} else {
				q2Idx = 0;
				q2 = bb2.getQuad(0);
			}
			Location loc2 = new Location(m, bb2, q2Idx, q2);
			PE pe2;
			if (!mayMerge)
				pe2 = pe;
			else {
				if (isFirst) {
					pe2 = pe;
					isFirst = false;
				} else
					pe2 = getCopy(pe);
			}
			addPathEdge(loc2, pe2, trajLength);
		}
	}

	private boolean propagateSEtoPE(PE clrPE, int trajLength, Location loc,
			jq_Method tgtM, SE tgtSE) {
		Quad q = loc.q;
		PE pe2 = getInvkPathEdge(q, clrPE, tgtM, tgtSE);
		if (pe2 == null)
			return false;
		propagatePEtoPE(loc.m, loc.bb, loc.qIdx, pe2,
				keepRings ? trajLength + 1 : trajLength);
		return true;
	}

	public class PEIterator implements Iterator<PE> {
		private Trio<Inst,PE,Integer> currentTrio;
		private Stack<Inst> callStack;
		public PEIterator(Inst currentInst,PE currentPE, Integer currentTrajLength){
			currentTrio = new Trio<Inst,PE,Integer>(currentInst,currentPE,currentTrajLength);
			callStack = new Stack<Inst>();
		}
		@Override
		public boolean hasNext() {
			return currentTrio.val2!=0;
		}

		@Override
		public PE next() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Remove operation is not supported here!");
		}

	}
}

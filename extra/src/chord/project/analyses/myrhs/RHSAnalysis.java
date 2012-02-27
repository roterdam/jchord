/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses.myrhs;

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
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
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
	protected List<Pair<Location, WrappedEdge<PE>>> workList = new ArrayList<Pair<Location, WrappedEdge<PE>>>();
	// Here, a SortedMap of Set is used rather than a single set to represent
	// the rings described in the Bebop paper
	protected Map<Inst, Set<WrappedEdge<PE>>> pathEdges = new HashMap<Inst, Set<WrappedEdge<PE>>>();
	protected Map<jq_Method, Set<WrappedEdge<SE>>> summEdges = new HashMap<jq_Method, Set<WrappedEdge<SE>>>();
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
			Messages.fatal("Cannot create RHS analysis '" + getName()
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
		Set<WrappedEdge<PE>> peSet = pathEdges.get(inst);
		if (peSet == null)
			return ret;
		for (WrappedEdge<PE> wpe : peSet) {
			ret.add(wpe.edge);
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
			Inst inst = (loc.q == null) ? (EntryOrExitBasicBlock) loc.bb
					: loc.q;
			WrappedEdge<PE> initWPE = new WrappedEdge<PE>(inst, pe, 0, null);
			addPathEdge(loc, initWPE);
		}
		propagate();
	}

	protected void printSummaries() {
		for (jq_Method m : summEdges.keySet()) {
			System.out.println("SE of " + m);
			Set<WrappedEdge<SE>> seSet = summEdges.get(m);
			if (seSet != null) {
				for (WrappedEdge<SE> se : seSet)
					System.out.println("\tSE " + se.edge);
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
				for (Pair<Location, WrappedEdge<PE>> pair : workList) {
					Location loc = pair.val0;
					Inst i = (loc.q == null) ? (Inst) loc.bb : loc.q;
					int id = quadToRPOid.get(i);
					System.out.println("\t" + pair + " " + id);
				}
			}
			int last = workList.size() - 1;
			Pair<Location, WrappedEdge<PE>> pair = workList.remove(last);
			Location loc = pair.val0;
			WrappedEdge<PE> wpe = pair.val1;
			PE pe = wpe.edge;
			Quad q = loc.q;
			jq_Method m = loc.m;
			BasicBlock bb = loc.bb;
			int trajLength = wpe.pathLength;
			int newTrajLength = this.keepRings ? trajLength + 1 : trajLength;
			int trajIncrease = this.keepRings?1:0;
			WrappedEdge<PE> newPre = this.keepRings ? wpe : null;
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
						Inst ninst = (q2 == null) ? (EntryOrExitBasicBlock) bb2
								: q2;
						WrappedEdge<PE> nwpe = new WrappedEdge<PE>(ninst, pe2,
								newTrajLength, newPre);
						addPathEdge(loc2, nwpe);
					}
				} else {
					assert (bb.isExit());
					processExit(m, wpe);
				}
			} else {
				// invoke or misc quad
				Operator op = q.getOperator();
				if (op instanceof Invoke) {
					Set<jq_Method> targets = getTargets(q);
					if (targets.isEmpty()) {
						PE pe2 = mayMerge ? getCopy(pe) : pe;
						propagatePEtoPE(m, bb, loc.qIdx, pe2, newTrajLength,
								newPre);
					} else {
						for (jq_Method m2 : targets) {
							if (DEBUG)
								System.out.println("\tTarget: " + m2);
							PE pe2 = getInitPathEdge(q, m2, pe);
							EntryOrExitBasicBlock bb2 = m2.getCFG().entry();
							Location loc2 = new Location(m2, bb2, -1, null);
							WrappedEdge<PE> nwpe = new WrappedEdge<PE>(bb2,
									pe2, newTrajLength, newPre);
							addPathEdge(loc2, nwpe);
							Set<WrappedEdge<SE>> seSet = summEdges.get(m2);
							if (seSet == null) {
								if (DEBUG)
									System.out.println("\tSE set empty");
								continue;
							}
							for (WrappedEdge<SE> se : seSet) {
								if (DEBUG)
									System.out.println("\tTesting SE: " + se.edge);
								if (propagateSEtoPE(pe, trajLength+trajIncrease*(se.pathLength+1), newPre,
										loc, m2, se.edge)) {
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
					propagatePEtoPE(m, bb, loc.qIdx, pe2, newTrajLength, newPre);
				}
			}
		}
	}

	private void processExit(jq_Method m, WrappedEdge<PE> wpe) {
		SE se = getSummaryEdge(m, wpe.edge);
		WrappedEdge<SE> wse = new WrappedEdge<SE>(null,se,0,null);
		if(this.keepRings){
			for(WrappedEdge<PE> swpe: pathEdges.get(m.getCFG().entry())){
				if(swpe.edge.matchSourse(wpe.edge)){
					wse.pathLength = wpe.pathLength- swpe.pathLength+1;
				}
			}
		}
		Set<WrappedEdge<SE>> wseSet = summEdges.get(m);
		if (DEBUG)
			System.out.println("\tChecking if " + m + " has SE: " + se);
		WrappedEdge<SE> wseToAdd = wse;
		if (wseSet == null) {
			wseSet = new HashSet<WrappedEdge<SE>>();
			summEdges.put(m, wseSet);
			wseSet.add(wse);
			if (DEBUG)
				System.out.println("\tNo, adding it as first SE");
		} else if (mayMerge) {
			boolean matched = false;
			for (WrappedEdge<SE> wse2 : wseSet) {
				if (wse2.canMerge(wse) >= 0) {
					if (DEBUG)
						System.out.println("\tNo, but matches SE: " + wse2.edge);
					boolean changed = wse2.mergeWith(wse);
					if (DEBUG)
						System.out.println("\tNew SE after merge: " + wse2.edge);
					if (!changed) {
						if (DEBUG)
							System.out.println("\tExisting SE did not change");
						return;
					}
					if (DEBUG)
						System.out.println("\tExisting SE changed");
					// se2 is already in summEdges(m), so no need to add it
					wseToAdd = wse2;
					matched = true;
					break;
				}
			}
			if (!matched) {
				if (DEBUG)
					System.out.println("\tNo, adding");
				wseSet.add(wse);
			}
		} else if (!wseSet.add(wse)) {
			if (DEBUG)
				System.out.println("\tYes, not adding");
			return;
		}
		for (Quad q2 : getCallers(m)) {
			if (DEBUG)
				System.out.println("\tCaller: " + q2 + " in " + q2.getMethod());
			Set<WrappedEdge<PE>> peSet = pathEdges.get(q2);
			if (peSet == null)
				continue;
			// make a copy as propagateSEtoPE might add a path edge to this set
			// itself;
			// in this case we could get a ConcurrentModification exception if
			// we don't
			// make a copy.
			List<WrappedEdge<PE>> peList = new ArrayList<WrappedEdge<PE>>(peSet);
			Location loc2 = invkQuadToLoc.get(q2);
			for (WrappedEdge<PE> storedWPE : peList) {
				PE storedPE = storedWPE.edge;
				if (DEBUG)
					System.out.println("\tTesting PE: " + storedPE);
				int newTrajLength = keepRings ? storedWPE.pathLength + wseToAdd.pathLength+1
						: storedWPE.pathLength;
				WrappedEdge<PE> pre = keepRings ? storedWPE : null;
				boolean match = propagateSEtoPE(storedPE, newTrajLength, pre,
						loc2, m, wseToAdd.edge);
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

	private void addPathEdgeToRings(PE pe, int trajLength,
			Map<Integer, Set<PE>> rings) {
		Set<PE> peSet = rings.get(trajLength);
		if (peSet == null) {
			peSet = new HashSet<PE>();
			rings.put(trajLength, peSet);
		}
		peSet.add(pe);
	}

	private boolean isWPEinWorkList(Location loc, WrappedEdge<PE> wpe) {
		for (Pair<Location, WrappedEdge<PE>> pair : workList) {
			if (!pair.val0.equals(loc))
				continue;
			if (wpe.equals(pair.val1))
				return true;
		}
		return false;
	}

	private void addPathEdge(Location loc, WrappedEdge<PE> wpe) {
		if (DEBUG)
			System.out.println("\tChecking if " + loc + " has PE: "
					+ wpe.edge);
		Quad q = loc.q;
		Inst i = (q != null) ? q : (Inst) loc.bb;
		Set<WrappedEdge<PE>> wpes = pathEdges.get(i);
		WrappedEdge<PE> wpeToAdd = wpe;
		if (wpes == null) {// The first path edge of this location
			wpes = new HashSet<WrappedEdge<PE>>();
			pathEdges.put(i, wpes);
			wpes.add(wpe);
			if (DEBUG)
				System.out.println("\tNo, adding it as first PE");
		} else if (mayMerge) {// if mayMerge==true, the method would try to
								// merge the new path edge with existing path
								// edges
			boolean matched = false;
			for (WrappedEdge<PE> storedWPE : wpes) {
				int canMerge = storedWPE.canMerge(wpe);
				if (canMerge == 2) {// The new path edge is bigger, this would
									// change the current Edge
					if (!isWPEinWorkList(loc, storedWPE))
						continue;
				}
				if (canMerge >= 0) {
					if (DEBUG)
						System.out.println("\tNo, but matches PE: " + wpe);
					boolean changed = storedWPE.mergeWith(wpe);
					if (DEBUG)
						System.out
								.println("\tNew PE after merge: " + storedWPE);
					if (!changed) {

						if (DEBUG) {
							System.out
									.println("\tExisting PE and its trajectory length did not change");
						}
						return;
					} else {
						if (DEBUG)
							System.out.println("\tExisting PE changed");
					}
					matched = true;
					wpeToAdd = storedWPE;
					break;
				}

			}
			if (matched == true) {
				for (int j = workList.size() - 1; j >= 0; j--) {
					Pair<Location, WrappedEdge<PE>> pair = workList.get(j);
					if (!pair.val0.equals(loc))
						continue;
					WrappedEdge<PE> wpeInWL = pair.val1;
					int canMerge = wpeInWL.canMerge(wpeToAdd);
					if (canMerge >= 0) {
						wpeInWL.mergeWith(wpeToAdd);
						return;
					}
				}
			} else {
				if (DEBUG)
					System.out.println("\tNo, adding");
				wpes.add(wpe);
			}
		} else {// Not merge at all
			boolean ifAdded = true;
			boolean ifChanged = false;
			for (WrappedEdge<PE> storedWPE : wpes) {
				if (storedWPE.edge.equals(wpe.edge)) {
					ifAdded = false;
					if (storedWPE.pathLength > wpe.pathLength) {
						storedWPE.pathLength = wpe.pathLength;
						storedWPE.provence = wpe.provence;
						ifChanged = true;
						wpeToAdd = storedWPE;
					}
				}
			}
			if (!ifAdded&&!ifChanged) {
				if (DEBUG)
					System.out.println("\tYes, not adding");
				return;
			}
			if(ifAdded&&!ifChanged)
			wpes.add(wpeToAdd);
		}
		assert (wpeToAdd != null);
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
		workList.add(j + 1, new Pair<Location, WrappedEdge<PE>>(loc, wpeToAdd));
	}

	private void propagatePEtoPE(jq_Method m, BasicBlock bb, int qIdx, PE pe,
			int trajLength, WrappedEdge<PE> pre) {
		if (qIdx != bb.size() - 1) {
			int q2Idx = qIdx + 1;
			Quad q2 = bb.getQuad(q2Idx);
			Location loc2 = new Location(m, bb, q2Idx, q2);
			WrappedEdge<PE> nwpe = new WrappedEdge<PE>(q2, pe, trajLength, pre);
			addPathEdge(loc2, nwpe);
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
			Inst inst = (q2 == null) ? (EntryOrExitBasicBlock) bb2 : q2;
			WrappedEdge<PE> nwpe = new WrappedEdge<PE>(inst, pe2, trajLength,
					pre);
			addPathEdge(loc2, nwpe);
		}
	}

	private boolean propagateSEtoPE(PE clrPE, int trajLength,
			WrappedEdge<PE> pre, Location loc, jq_Method tgtM, SE tgtSE) {
		Quad q = loc.q;
		PE pe2 = getInvkPathEdge(q, clrPE, tgtM, tgtSE);
		if (pe2 == null)
			return false;
		propagatePEtoPE(loc.m, loc.bb, loc.qIdx, pe2, trajLength, pre);
		return true;
	}

	public BackTraceIterator getBackTraceIterator(WrappedEdge<PE> wpe) {
		return new BackTraceIterator(wpe);
	}

	/**
	 * The backward trace iterator.
	 * 
	 * @author xin
	 * 
	 */
	public class BackTraceIterator implements Iterator<WrappedEdge<PE>> {
		private WrappedEdge<PE> currentWPE;
		private Stack<WrappedEdge<PE>> callStack;

		public BackTraceIterator(WrappedEdge<PE> wpe) {
			if (keepRings == false)
				Messages.fatal("Cannot create a backward trace iterator when no rings are kept to record the trajactory length.");
			currentWPE = wpe;
			callStack = new Stack<WrappedEdge<PE>>();
		}

		@Override
		public boolean hasNext() {
			return currentWPE.pathLength != 0;
		}

		@Override
		public WrappedEdge<PE> next() {
			if (currentWPE.inst instanceof EntryOrExitBasicBlock) {
				EntryOrExitBasicBlock bb = (EntryOrExitBasicBlock) currentWPE.inst;
				if (bb.isEntry() && !callStack.empty()) {
					currentWPE = callStack.pop();
					return currentWPE;
				}
			}
			WrappedEdge<PE> preWPE = currentWPE.provence;
			PE pe = currentWPE.edge;
			PE prePE = preWPE.edge;
			Inst inst = currentWPE.inst;
			Inst preInst = preWPE.inst;
			boolean ifEntry =false;
			if(inst instanceof BasicBlock){
				BasicBlock bb = (BasicBlock)inst;
				ifEntry = bb.isEntry();
			}
			if (!ifEntry&&preInst instanceof Quad) {
				Quad preQuad = (Quad) preInst;
				if (preQuad.getOperator() instanceof Invoke) {
					Set<jq_Method> targets = getTargets(preQuad);
					if (targets == null || targets.isEmpty()) {
						currentWPE = preWPE;
						return currentWPE;
					}
					for (jq_Method m : targets) {
						Set<WrappedEdge<SE>> wseSet = summEdges.get(m);
						if(wseSet!=null)
						for (WrappedEdge<SE> wse : wseSet) {
							if(preWPE.pathLength+wse.pathLength+1>currentWPE.pathLength)
								continue;
							PE invkPE = getInvkPathEdge(preQuad, prePE, m, wse.edge);
							if (invkPE != null && invkPE.equals(pe)) {
								callStack.push(preWPE);
								Inst exit = m.getCFG().exit();
								for (WrappedEdge<PE> exitWPE : pathEdges
										.get(exit)) {
									if (getSummaryEdge(m, exitWPE.edge)
											.equals(wse.edge)) {
										currentWPE = exitWPE;
										return currentWPE;
									}
								}
							}
						}
					}
					throw new RuntimeException("Couldn't find the right summary edge, something is wrong with the forward analysis!");
				}
			}
			currentWPE = preWPE;

			return currentWPE;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException(
					"Remove operation is not supported here!");
		}

	}
}

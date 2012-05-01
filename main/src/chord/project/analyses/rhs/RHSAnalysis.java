package chord.project.analyses.rhs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator.Invoke;

import gnu.trove.map.hash.TObjectIntHashMap;
import chord.util.tuple.object.Pair;
import chord.program.Loc;
import chord.analyses.alias.ICICG;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.util.ArraySet;
import chord.util.Alarm;
import chord.project.Messages;

/**
 * Implementation of the Reps-Horwitz-Sagiv algorithm for context-sensitive dataflow analysis.
 *
 * Relevant system properties:
 * - chord.rhs.merge = [lossy|pjoin|naive] (default = lossy)
 * - chord.rhs.order = [bfs|dfs] (default = bfs) 
 * - chord.rhs.trace = [none|any|shortest] (default = none)
 * - chord.rhs.timeout = N milliseconds (default N = 0, no timeouts)
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class RHSAnalysis<PE extends IEdge, SE extends IEdge> extends JavaAnalysis {
    protected static boolean DEBUG = false;

    protected List<Pair<Loc, PE>> workList = new ArrayList<Pair<Loc, PE>>();
    protected Map<Inst, Set<PE>> pathEdges = new HashMap<Inst, Set<PE>>();
    protected Map<jq_Method, Set<SE>> summEdges = new HashMap<jq_Method, Set<SE>>();
    protected DomI domI;
    protected DomM domM;
    protected ICICG cicg;
    protected TObjectIntHashMap<Inst> quadToRPOid;
    protected Map<Quad, Loc> invkQuadToLoc;
    protected Map<jq_Method, Set<Quad>> callersMap = new HashMap<jq_Method, Set<Quad>>();
    protected Map<Quad, Set<jq_Method>> targetsMap = new HashMap<Quad, Set<jq_Method>>();
    protected boolean isInit, isDone;

    protected OrderKind orderKind;
    protected MergeKind mergeKind;
    protected TraceKind traceKind;

    private int timeout;
    private Alarm alarm;

    protected boolean mustMerge;
    protected boolean mayMerge;

    protected Set<Quad> trackedInvkSites = new HashSet<Quad>();

	/*********************************************************************************
	 * Methods that clients must define.
	 *********************************************************************************/

    // get the initial set of path edges
    public abstract Set<Pair<Loc, PE>> getInitPathEdges();

    // get the path edge(s) in callee for target method m called from call site q
    // with caller path edge pe
    public abstract PE getInitPathEdge(Quad q, jq_Method m, PE pe);

    // get outgoing path edge(s) from q, given incoming path edge pe into q.
	// q is guaranteed to not be an invoke statement, return statement, entry
	// basic block, or exit basic block.
	// the set returned can be reused by client.
    public abstract PE getMiscPathEdge(Quad q, PE pe);
 
    // q is an invoke statement and m is the target method.
	// get path edge to successor of q given path edge into q and summary edge of a
	// target method m of the call site q.
    // returns null if the path edge into q is not compatible with the summary edge.
    public abstract PE getInvkPathEdge(Quad q, PE clrPE, jq_Method m, SE tgtSE);

    public abstract PE getPECopy(PE pe);

    public abstract SE getSECopy(SE pe);

    // m is a method and pe is a path edge from entry to exit of m
    // (in case of forward analysis) or vice versa (in case of backward analysis)
    // that must be lifted to a summary edge.
    public abstract SE getSummaryEdge(jq_Method m, PE pe);

    /**
     * Provides the call graph to be used by the analysis.
     *
     * @return  The call graph to be used by the analysis.
     */
    public abstract ICICG getCallGraph();

    /*********************************************************************************
     * Methods that clients might want to override but is not mandatory; alternatively,
     * their default behavior can be changed by setting relevant chord.rhs.* property.
     *********************************************************************************/

    /**
	 * Determines how this analysis must merge PEs at each program point that
	 * have the same source state but different target states and, likewise,
	 * SEs of each method that have the same source state but different target
	 * states.
     */
    public void setMergeKind() {
		String s = System.getProperty("chord.rhs.merge", "lossy");
		if (s.equals("lossy"))
			mergeKind = MergeKind.LOSSY;
		else if (s.equals("pjoin"))
			mergeKind = MergeKind.PJOIN;
		else if (s.equals("naive"))
			mergeKind = MergeKind.NAIVE;
		else
			throw new RuntimeException("Unknown value for property chord.rhs.merge: " + s);
	}

    public void setOrderKind() {
		String s = System.getProperty("chord.rhs.order", "bfs");
		if (s.equals("bfs"))
			orderKind = OrderKind.BFS;
		else if (s.equals("dfs"))
			orderKind = OrderKind.DFS;
		else
			throw new RuntimeException("Unknown value for property chord.rhs.order: " + s); 
    }

    public void setTraceKind() {
		String s = System.getProperty("chord.rhs.trace", "none");
		if (s.equals("none"))
			traceKind = TraceKind.NONE;
		else if (s.equals("any"))
			traceKind = TraceKind.ANY;
		else if (s.equals("shortest"))
			traceKind = TraceKind.SHORTEST;
		else
			throw new RuntimeException("Unknown value for property chord.rhs.trace: " + s);
    }

    public void setTimeout() {
        timeout = Integer.getInteger("chord.rhs.timeout", 0);
    }

    public void setTrackedInvkSites(Set<Quad> trackedInvkSites) {
        this.trackedInvkSites = trackedInvkSites;
    }

    /*********************************************************************************
     * Methods that client may call/override.  Example usage:
	 * init();
     * while (*) {
     *   runPass();
     *   // done building path/summary edges; clients can now call:
     *   // getPEs(i), getSEs(m), getAllPEs(), getAllSEs(), getBackTracIterator(pe), print()
     * }
     * done();
     *********************************************************************************/

    public void init() {
        if (isInit) return;
		isInit = true;

		// start configuring the analysis
		setMergeKind();
		setOrderKind();
		setTraceKind();
        mayMerge  = (mergeKind != MergeKind.NAIVE);
        mustMerge = (mergeKind == MergeKind.LOSSY);
        if (mustMerge && !mayMerge) {
            Messages.fatal("Cannot create RHS analysis '" + getName() + "' with mustMerge but without mayMerge.");
        }
        if (mustMerge && traceKind != TraceKind.NONE) {
            Messages.fatal("Cannot create RHS analysis '" + getName() + "' with mustMerge and trace generation.");
        }
		setTimeout();
		// done configuring the analysis

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
        invkQuadToLoc = new HashMap<Quad, Loc>();
        for (jq_Method m : cicg.getNodes()) {
            if (m.isAbstract()) continue;
            ControlFlowGraph cfg = m.getCFG();
            quadToRPOid.put(cfg.entry(), 0);
            int rpoId = 1;
            for (BasicBlock bb : cfg.reversePostOrder()) {
                for (int i = 0; i < bb.size(); i++) {
                    Quad q = bb.getQuad(i);
                    quadToRPOid.put(q, rpoId++);
                    if (q.getOperator() instanceof Invoke) {
                        Loc loc = new Loc(q, i);
                        invkQuadToLoc.put(q, loc);
                    }
                }
            }
            quadToRPOid.put(cfg.exit(), rpoId);
        }
    }

    public void done() {
		if (isDone) return;
		isDone = true;
        if (timeout > 0)
            alarm.doneAllPasses();
    }

    /**
     * Run an instance of the analysis afresh.
     * Clients may call this method multiple times from their {@link #run()} method.
     */
    public void runPass() throws TimeoutException {
        if (timeout > 0)
            alarm.initNewPass();
        // clear these sets since client may call this method multiple times
        workList.clear();
        summEdges.clear();
        pathEdges.clear();
        Set<Pair<Loc, PE>> initPEs = getInitPathEdges();
        for (Pair<Loc, PE> pair : initPEs) {
            Loc loc = pair.val0;
            PE pe = pair.val1;
            addPathEdge(loc, pe);
        }
        propagate();
    }

    // TODO: might have to change the argument type to PE
    public BackTraceIterator<PE,SE> getBackTraceIterator(IWrappedPE<PE, SE> wpe) {
        if (traceKind == TraceKind.NONE) {
            throw new RuntimeException("trace generation not enabled");
        }
        return new BackTraceIterator<PE,SE>(wpe);
    }

	public Set<PE> getPEs(Inst i) {
		return pathEdges.get(i);
	}

	public Map<Inst, Set<PE>> getAllPEs() {
		return pathEdges;
	}

	public Set<SE> getSEs(jq_Method m) {
		return summEdges.get(m);
	}

	public Map<jq_Method, Set<SE>> getAllSEs() {
		return summEdges;
	}

    public void print() {
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
            Set<PE> peSet = pathEdges.get(i);
            if (peSet != null) {
                for (PE pe : peSet)
                    System.out.println("\tPE " + pe);
            }
        }
    }
        
    /*********************************************************************************
     * Internal methods.
     *********************************************************************************/

    private Set<Quad> getCallers(jq_Method m) {
        Set<Quad> callers = callersMap.get(m);
        if (callers == null) {
            callers = cicg.getCallers(m);
            callersMap.put(m, callers);
        }
        return callers;
    }

    private final Set<jq_Method> getTargets(Quad i) {
        Set<jq_Method> targets = targetsMap.get(i);
        if (targets == null) {
            targets = cicg.getTargets(i);
            targetsMap.put(i, targets);
        }
        return targets;
    }
    
    /**
     * Propagate analysis results until fixpoint is reached.
     */
    private void propagate() throws TimeoutException {
        while (!workList.isEmpty()) {
            if (timeout > 0 && alarm.passTimedOut()) {
                System.out.println("TIMED OUT");
                throw new TimeoutException();
            }
            if (DEBUG) {
                System.out.println("WORKLIST:");
                for (Pair<Loc, PE> pair : workList) {
                    Loc loc = pair.val0;
                    int id = quadToRPOid.get(loc.i);
                    System.out.println("\t" + pair + " " + id);
                }
            }
            int last = workList.size() - 1;
            Pair<Loc, PE> pair = workList.remove(last);
            Loc loc = pair.val0;
            PE pe = pair.val1;
            Inst i = loc.i;
            if (DEBUG) System.out.println("Processing loc: " + loc + " PE: " + pe);
            if (i instanceof EntryOrExitBasicBlock) {
                // method entry or method exit
                EntryOrExitBasicBlock bb = (EntryOrExitBasicBlock) i;
                if (bb.isEntry()) {
                    processEntry(bb, pe);
                } else {
                    assert (bb.isExit());
                    processExit(bb, pe);
                }
            } else {
                Quad q = (Quad) i;
                // invoke or misc quad
                Operator op = q.getOperator();
                if (op instanceof Invoke) {
                    processInvk(loc, pe);
                } else {
                    PE pe2 = getMiscPathEdge(q, pe);
                    propagatePEtoPE(loc, pe2);
                }
            }
        }
    }

    private void processInvk(Loc loc, PE pe) {
        Quad q = (Quad) loc.i;
        Set<jq_Method> targets = getTargets(q);
        if (targets.isEmpty()) {
            PE pe2 = mayMerge ? getPECopy(pe) : pe;
            propagatePEtoPE(loc, pe2);
        } else {
            for (jq_Method m2 : targets) {
                if (DEBUG) System.out.println("\tTarget: " + m2);
                PE pe2 = getInitPathEdge(q, m2, pe);
                if (trackedInvkSites.contains(q)) {
                    EntryOrExitBasicBlock bb2 = m2.getCFG().exit();
                    Loc loc2 = new Loc(bb2, -1);
                    addPathEdge(loc2, pe2);
                } else {
                    EntryOrExitBasicBlock bb2 = m2.getCFG().entry();
                    Loc loc2 = new Loc(bb2, -1);
                    addPathEdge(loc2, pe2);
                }
                Set<SE> seSet = summEdges.get(m2);
                if (seSet == null) {
                    if (DEBUG) System.out.println("\tSE set empty");
                    continue;
                }
                for (SE se : seSet) {
                    if (DEBUG) System.out.println("\tTesting SE: " + se);
                    if (propagateSEtoPE(pe, loc, m2, se)) {
                        if (DEBUG) System.out.println("\tMatched");
                        if (mustMerge) {
                            // this was only SE; stop looking for more
                            break;
                        }
                    } else {
                        if (DEBUG) System.out.println("\tDid not match");
                    }
                }
            }
        }
    }

    private void processEntry(EntryOrExitBasicBlock bb, PE pe) {
        for (BasicBlock bb2 : bb.getSuccessors()) {
            Inst i2;
            int q2Idx;
            if (bb2.size() == 0) {
                assert (bb2.isExit());
                i2 = (EntryOrExitBasicBlock) bb2;
                q2Idx = -1;
            } else {
                i2 = bb2.getQuad(0);
                q2Idx = 0;
            }
            Loc loc2 = new Loc(i2, q2Idx);
            PE pe2 = mayMerge ? getPECopy(pe) : pe;
            addPathEdge(loc2, pe2);
        }
    }

    private void processExit(EntryOrExitBasicBlock bb, PE pe) {
        jq_Method m = bb.getMethod();
        SE se = getSummaryEdge(m, pe);
        Set<SE> seSet = summEdges.get(m);
        if (DEBUG) System.out.println("\tChecking if " + m + " has SE: " + se);
        SE seToAdd = se;
        if (seSet == null) {
            seSet = new HashSet<SE>();
            summEdges.put(m, seSet);
            seSet.add(se);
            if (DEBUG) System.out.println("\tNo, adding it as first SE");
        } else if (mayMerge) {
            boolean matched = false;
            for (SE se2 : seSet) {
                if (se2.canMerge(se, mustMerge) >= 0) {
                    if (DEBUG) System.out.println("\tNo, but matches SE: " + se2);
                    boolean changed = se2.mergeWith(se);
                    if (DEBUG) System.out.println("\tNew SE after merge: " + se2);
                    if (!changed) {
                        if (DEBUG) System.out.println("\tExisting SE did not change");
                        return;
                    }
                    if (DEBUG) System.out.println("\tExisting SE changed");
                    // se2 is already in summEdges(m), so no need to add it
                    seToAdd = se2;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (DEBUG) System.out.println("\tNo, adding");
                seSet.add(se);
            }
        } else if (!seSet.add(se)) {
            if (DEBUG) System.out.println("\tYes, not adding");
            return;
        }
        for (Quad q2 : getCallers(m)) {
            if (DEBUG) System.out.println("\tCaller: " + q2 + " in " + q2.getMethod());
            Set<PE> peSet = pathEdges.get(q2);
            if (peSet == null)
                continue;
            // make a copy as propagateSEtoPE might add a path edge to this set itself;
            // in this case we could get a ConcurrentModification exception if we don't
            // make a copy.
            List<PE> peList = new ArrayList<PE>(peSet);
            Loc loc2 = invkQuadToLoc.get(q2);
            for (PE pe2 : peList) {
                if (DEBUG) System.out.println("\tTesting PE: " + pe2);
                boolean match = propagateSEtoPE(pe2, loc2, m, seToAdd);
                if (match) {
                    if (DEBUG) System.out.println("\tMatched");
                } else {
                    if (DEBUG) System.out.println("\tDid not match");
                }
            }
        }
    }

    private void addPathEdge(Loc loc, PE pe) {
        if (DEBUG) System.out.println("\tChecking if " + loc + " has PE: " + pe);
        Inst i = loc.i;
        Set<PE> peSet = pathEdges.get(i);
        PE peToAdd = pe;
        if (peSet == null) {
            peSet = new HashSet<PE>();
            pathEdges.put(i, peSet);
            peSet.add(pe);
            if (DEBUG) System.out.println("\tNo, adding it as first PE");
        } else if (mayMerge) {
            boolean matched = false;
            for (PE pe2 : peSet) {
                if (pe2.canMerge(pe, mustMerge) >= 0) {
                    if (DEBUG) System.out.println("\tNo, but matches PE: " + pe2);
                    boolean changed = pe2.mergeWith(pe);
                    if (DEBUG) System.out.println("\tNew PE after merge: " + pe2); 
                    if (!changed) {
                        if (DEBUG) System.out.println("\tExisting PE did not change");
                        return;
                    }
                    if (DEBUG) System.out.println("\tExisting PE changed");
                    // pe2 is already in pathEdges(i), so no need to add it;
                    // but it may or may not be in workList
                    for (int j = workList.size() - 1; j >= 0; j--) {
                        Pair<Loc, PE> pair = workList.get(j);
                        PE pe3 = pair.val1;
                        if (pe3 == pe2)
                            return;
                    }
                    peToAdd = pe2;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (DEBUG) System.out.println("\tNo, adding");
                peSet.add(pe);
            }
        } else if (!peSet.add(pe)) {
            if (DEBUG) System.out.println("\tYes, not adding");
            return;
        }
        assert (peToAdd != null);
        Pair<Loc, PE> pair = new Pair<Loc, PE>(loc, peToAdd);
        int j = workList.size() - 1;
        if (orderKind == OrderKind.BFS) {
            jq_Method m = loc.i.getMethod();
            int rpoId = quadToRPOid.get(i);
            for (; j >= 0; j--) {
                Loc loc2 = workList.get(j).val0;
				Inst i2 = loc2.i;
                if (i2.getMethod() != m) break;
                int rpoId2 = quadToRPOid.get(i2);
                if (rpoId2 > rpoId)
                    break;
            }
        }
        if (DEBUG) System.out.println("\tAlso adding to worklist at " + (j + 1));
        workList.add(j + 1, pair);
    }

    private void propagatePEtoPE(Loc loc, PE pe) {
        int qIdx = loc.qIdx;
        BasicBlock bb = loc.i.getBasicBlock();
        if (qIdx != bb.size() - 1) {
            int q2Idx = qIdx + 1;
            Quad q2 = bb.getQuad(q2Idx);
            Loc loc2 = new Loc(q2, q2Idx);
            addPathEdge(loc2, pe);
            return;
        }
        boolean isFirst = true;
        for (BasicBlock bb2 : bb.getSuccessors()) {
            Inst i2;
            int q2Idx;
            if (bb2.size() == 0) {
				assert (bb2.isExit());
				i2 = (EntryOrExitBasicBlock) bb2;
                q2Idx = -1;
            } else {
                i2 = bb2.getQuad(0);
                q2Idx = 0;
            }
            Loc loc2 = new Loc(i2, q2Idx);
            PE pe2;
            if (!mayMerge)
                pe2 = pe;
            else {
                if (isFirst) {
                    pe2 = pe;
                    isFirst = false;
                } else
                    pe2 = getPECopy(pe);
            }
            addPathEdge(loc2, pe2);
        }
    }

    private boolean propagateSEtoPE(PE clrPE, Loc loc, jq_Method tgtM, SE tgtSE) {
        Quad q = (Quad) loc.i;
        PE pe2 = getInvkPathEdge(q, clrPE, tgtM, tgtSE);
        if (pe2 == null)
            return false;
        propagatePEtoPE(loc, pe2);
        return true;
    }
}


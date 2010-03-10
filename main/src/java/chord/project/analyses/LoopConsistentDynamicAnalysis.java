/**
 * 
 */
package chord.project.analyses;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.util.Set;
import java.util.Stack;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.doms.DomB;
import chord.doms.DomM;
import chord.instr.InstrScheme;
import chord.program.CFGLoopFinder;

/**
 * This sub-class of {@link DynamicAnalysis} guarantees that the loop-related events observed by its 
 * sub-classes via the <code>onLoopEnter</code> and <code>onLoopExit</code> methods are balanced. This is not
 * guaranteed by <code>processEnterLoop</code> and <code>processLeaveLoop</code>.
 * 
 * @author omert
 *
 */
public class LoopConsistentDynamicAnalysis extends DynamicAnalysis {

	private static abstract class Record {
		protected int id;
		
		/* We must not implement state-dependent versions of <code>hashCode</code> and <code>equals</code>. */
	}
	
	private static class LoopRecord extends Record {
		protected final int t;
		
		public LoopRecord(int id, int t) {
			this.id = id;
			this.t = t;
		}
	}
	
	private static class MethodRecord extends Record {
		public MethodRecord(int id) {
			this.id = id;
		}
	}
	
	private final Stack<Record> stack = new Stack<Record>();
	private final TIntObjectHashMap<TIntHashSet> loopHead2body = new TIntObjectHashMap<TIntHashSet>(16);
	private TIntHashSet visited4loops = new TIntHashSet();
	
	private InstrScheme instrScheme;
	private DomM domM;
	private DomB domB;
	
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
    	instrScheme.setEnterAndLeaveMethodEvent();
    	instrScheme.setEnterAndLeaveLoopEvent();
    	instrScheme.setBasicBlockEvent();
    	return instrScheme;
	}
	
	@Override
	public void initAllPasses() {
		super.initAllPasses();
		domM = instrumentor.getDomM();
		domB = instrumentor.getDomB();
	}
	
	@Override
	public void processEnterMethod(int m, int t) {
		stack.add(new MethodRecord(m));
		if (!visited4loops.contains(m)) {
			visited4loops.add(m);
			jq_Method mthd = domM.get(m);
			// Perform a slightly eager computation to map each loop header to its body (in terms of <code>DomB</code>).
			ControlFlowGraph cfg = mthd.getCFG();
			CFGLoopFinder finder = new CFGLoopFinder();
			finder.visit(cfg);
			for (BasicBlock head : finder.getLoopHeads()) {
				TIntHashSet S = new TIntHashSet();
				loopHead2body.put(domB.getOrAdd(head), S);
				Set<BasicBlock> loopBody = finder.getLoopBody(head);
				for (BasicBlock bb : loopBody) {
					S.add(domB.getOrAdd(bb));
				}
			}
		}
	}
	
	@Override
	public void processLeaveMethod(int m, int t) {
		while (stack.peek() instanceof LoopRecord) {
			LoopRecord top = (LoopRecord) stack.pop();
			onLoopExit(top.id, top.t);
		}
		
		// The present method should be at the stop of the stack.
		if (stack.peek().id == m) {
			stack.pop();
		}
	}

	@Override
	public void processEnterLoop(int w, int t) {
		stack.add(new LoopRecord(w, t));
		onLoopEnter(w, t);
	}
	
	@Override
	public void processLeaveLoop(int w, int t) {
		/*
		 * It's not necessarily the case that the loop-exit event matches a loop-enter event at the top of the stack, 
		 * but an important invariant is that if there is such a loop-enter event, then it's guaranteed to be at the top
		 * of the stack, as all other loop- and method-enter events succeeding it have been matched by corresponding 
		 * exit events.
		 */
		Record top = stack.peek();
		if (top instanceof LoopRecord && top.id == w) {
			stack.pop();
			LoopRecord lr = (LoopRecord) top;
			onLoopExit(lr.id, lr.t);
		}
	}
	
	@Override
	public void processBasicBlock(int b, int t) {
		boolean hasRemoved = false;
		do {
			Record r = stack.peek();
			if (r instanceof LoopRecord) {
				TIntHashSet loopBody = loopHead2body.get(r.id);
				if (loopBody != null && !loopBody.contains(b)) {
					stack.pop();
					LoopRecord lr = (LoopRecord) r;
					onLoopExit(lr.id, lr.t);
					hasRemoved = true;
				}
			}
		} while (hasRemoved);
	}
	
	protected void onLoopExit(int id, int t) {
		// This is a no-op that should be overriden by sub-classes.
	}
	
	protected void onLoopEnter(int id, int t) {
		// This is a no-op that should be overriden by sub-classes.
	}
}

package chord.project;

import java.util.Stack;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import chord.util.tuple.object.Pair;
import chord.util.ArraySet;

import joeq.Util.Templates.List;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;

public class CFGLoopFinder {
	public final ControlFlowGraph cfg;
    private Set<Pair<BasicBlock, BasicBlock>> backEdges;
    private Set<BasicBlock> visitedBef;
    private Set<BasicBlock> visitedAft;
	private Map<BasicBlock, Set<BasicBlock>> headToBody;
	private Map<BasicBlock, Set<BasicBlock>> headToExits;
	public CFGLoopFinder(ControlFlowGraph cfg) {
		this.cfg = cfg;
	}
	public Set<BasicBlock> getLoopHeads() {
		if (headToBody == null)
			buildHeadToBody();
		return headToBody.keySet();
	}
	public Set<BasicBlock> getLoopBody(BasicBlock head) {
		if (headToBody == null)
			buildHeadToBody();
		return headToBody.get(head);
	}
	public Set<BasicBlock> getLoopExits(BasicBlock head) {
		if (headToExits == null)
			buildHeadToExits();
		return headToExits.get(head);
	}
	private void buildHeadToBody() {
		buildBackEdges();
		headToBody = new HashMap<BasicBlock, Set<BasicBlock>>();
		for (Pair<BasicBlock, BasicBlock> edge : backEdges) {
			BasicBlock tail = edge.val0;
			BasicBlock head = edge.val1;
			// tail->head is a back edge
			Set<BasicBlock> body = headToBody.get(head);
			if (body == null) {
				body = new ArraySet<BasicBlock>();
				headToBody.put(head, body);
				body.add(head);
			}
			Stack<BasicBlock> working = new Stack<BasicBlock>();
			working.push(tail);
			while (!working.isEmpty()) {
				BasicBlock curr = working.pop();
				if (body.add(curr)) {
					for (BasicBlock pred : curr.getPredecessors())
						working.push(pred);
				}
			}
		}
	}
	private void buildHeadToExits() {
		if (headToBody == null)
			buildHeadToBody();
		headToExits = new HashMap<BasicBlock, Set<BasicBlock>>();
		for (BasicBlock head : headToBody.keySet()) {
			Set<BasicBlock> body = headToBody.get(head);
			for (BasicBlock curr : body) {
				boolean isCurrExit = false;
				for (BasicBlock succ : curr.getSuccessors()) {
					if (!body.contains(succ)) {
						isCurrExit = true;
						break;
					}
				}
				if (isCurrExit) {
					Set<BasicBlock> exits = headToExits.get(head);
					if (exits == null) {
						exits = new ArraySet<BasicBlock>();
						headToExits.put(head, exits);
					}
					exits.add(curr);
				}
			}
		}
	}
	private void buildBackEdges() {
		visitedBef = new ArraySet<BasicBlock>();
		visitedAft = new ArraySet<BasicBlock>();
		backEdges = new ArraySet<Pair<BasicBlock, BasicBlock>>();
		visit(cfg.entry());
	}
    private void visit(BasicBlock curr) {
        visitedBef.add(curr);
        for (BasicBlock succ : curr.getSuccessors()) {
            if (visitedBef.contains(succ)) {
                if (!visitedAft.contains(succ)) {
					Pair<BasicBlock, BasicBlock> edge =
						new Pair<BasicBlock, BasicBlock>(curr, succ);
                    backEdges.add(edge);
				}
            } else
                visit(succ);
        }
        visitedAft.add(curr);
    }
}

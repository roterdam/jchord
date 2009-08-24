/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.util.Stack;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import chord.util.tuple.object.Pair;
import chord.util.ArraySet;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CFGLoopFinder implements ICFGVisitor {
	public static final boolean DEBUG = false;
	private Set<BasicBlock> visitedBef;
    private Set<BasicBlock> visitedAft;
    private Set<Pair<BasicBlock, BasicBlock>> backEdges;
    	private Map<BasicBlock, Set<BasicBlock>> headToBody;
	private Map<BasicBlock, Set<BasicBlock>> headToExits;
	public void visit(ControlFlowGraph cfg) {
		// build back edges
		visitedBef = new ArraySet<BasicBlock>();
		visitedAft = new ArraySet<BasicBlock>();
		backEdges = new ArraySet<Pair<BasicBlock, BasicBlock>>();
		visit(cfg.entry());
		// build headToBody
		headToBody = new HashMap<BasicBlock, Set<BasicBlock>>();
		for (Pair<BasicBlock, BasicBlock> edge : backEdges) {
			BasicBlock tail = edge.val0;
			BasicBlock head = edge.val1;
			assert (!head.isEntry());
			assert (!head.isExit());
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
					for (Object o : curr.getPredecessors()) {
						BasicBlock pred = (BasicBlock) o;
						working.push(pred);
					}
				}
			}
		}
		// build headToExits
		headToExits = new HashMap<BasicBlock, Set<BasicBlock>>();
		for (BasicBlock head : headToBody.keySet()) {
			Set<BasicBlock> exits = new ArraySet<BasicBlock>();
			headToExits.put(head, exits);
			Set<BasicBlock> body = headToBody.get(head);
			for (BasicBlock curr : body) {
				for (Object o : curr.getSuccessors()) {
					BasicBlock succ = (BasicBlock) o;
					if (!body.contains(succ)) {
						assert (!succ.isEntry());
						assert (!succ.isExit());
						exits.add(succ);
						break;
					}
				}
			}
		}
		if (DEBUG) {
			System.out.println(cfg.fullDump());
			Set<BasicBlock> heads = getLoopHeads();
			for (BasicBlock head : heads) {
				System.out.println(head);
				System.out.println("BODY:");
				for (BasicBlock b : getLoopBody(head))
					System.out.println("\t" + b);
				System.out.println("TAILS:");
				for (BasicBlock b : getLoopExits(head))
					System.out.println("\t" + b);
			}
		}
	}
	public Set<BasicBlock> getLoopHeads() {
		return headToBody.keySet();
	}
	public Set<BasicBlock> getLoopBody(BasicBlock head) {
		return headToBody.get(head);
	}
	public Set<BasicBlock> getLoopExits(BasicBlock head) {
		return headToExits.get(head);
	}
	public Map<BasicBlock, Set<BasicBlock>> getHeadToExitsMap() {
		return headToExits;
	}
	public Map<BasicBlock, Set<BasicBlock>> getHeadToBodyMap() {
		return headToBody;
	}
    private void visit(BasicBlock curr) {
        visitedBef.add(curr);
        for (Object o : curr.getSuccessors()) {
        	BasicBlock succ = (BasicBlock) o;
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

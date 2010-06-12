/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import java.util.Map;
import java.util.HashMap;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import chord.program.visitors.IMethodVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;
import joeq.Util.Templates.ListIterator;

/**
 * Domain of basic blocks.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "B"
)
public class DomB extends ProgramDom<BasicBlock>
		implements IMethodVisitor {
	protected Map<BasicBlock, jq_Method> basicBlockToMethodMap;
	@Override
	public void init() {
		basicBlockToMethodMap = new HashMap<BasicBlock, jq_Method>();
	}
	@Override
	public void visit(jq_Class c) { }
	@Override
	public void visit(jq_Method m) {
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
				it.hasNext();) {
			BasicBlock b = it.nextBasicBlock();
			basicBlockToMethodMap.put(b, m);
			getOrAdd(b);
		}
	}
	public jq_Method getMethod(BasicBlock b) {
		return basicBlockToMethodMap.get(b);
	}
	@Override
    public String toUniqueString(BasicBlock b) {
        return b.getID() + "!" + getMethod(b);
    }
}

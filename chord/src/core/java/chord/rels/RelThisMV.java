/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Program;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (m,z,v) such that local variable
 * v is the zth argument variable of method m.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "thisMV",
	sign = "M0,V0:M0_V0"
)
public class RelThisMV extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		DomV domV = (DomV) doms[1];
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract() || m.isStatic())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			RegisterFactory rf = cfg.getRegisterFactory();
			Register v = rf.get(0);
			int vIdx = domV.indexOf(v);
			add(mIdx, vIdx);
		}
	}
}

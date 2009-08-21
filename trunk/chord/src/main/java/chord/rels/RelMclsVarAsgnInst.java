/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (m,v,b) such that method m
 * contains a statement of the form <tt>v = b.getClass()</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MclsVarAsgnInst",
	sign = "M0,V0,V1:M0_V0xV1"
)
public class RelMclsVarAsgnInst extends ProgramRel {
	public void fill() {
		/*
		DomM domM = (DomM) doms[0];
		DomV domV = (DomV) doms[1];
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			Method mVal = domM.get(mIdx);
			CFG cfg = mVal.getCFG();
			if (cfg != null) {
				for (Inst inst : cfg.getNodes()) {
					if (inst instanceof ClsVarAsgnInst) {
						ClsVarAsgnInst iVal = (ClsVarAsgnInst) inst;
						int vIdx = domV.get(iVal.getVar());
						int bIdx = domV.get(iVal.getBase());
						add(mIdx, vIdx, bIdx);
					}
				}
			}
		}
		*/
	}
}

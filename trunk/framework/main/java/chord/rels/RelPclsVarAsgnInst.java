/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import chord.doms.DomP;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (p,v,b) such that the statement
 * at program point p is of the form <tt>v = b.getClass()</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PclsVarAsgnInst",
	sign = "P0,V0,V1:P0_V0xV1"
)
public class RelPclsVarAsgnInst extends ProgramRel {
	public void fill() {
		/* TODO
		DomP domP = (DomP) doms[0];
		DomV domV = (DomV) doms[1];
		Program program = project.getProgram();
		for (Type type : program.getTypes()) {
			for (Method method : type.getMethods()) {
				CFG cfg = method.getCFG();
				if (cfg == null)
					continue;
				for (Inst inst : cfg.getNodes()) {
					if (inst instanceof ClsVarAsgnInst) {
						ClsVarAsgnInst iVal = (ClsVarAsgnInst) inst;
						int pIdx = domP.get(inst);
						int vIdx = domV.get(iVal.getVar());
						int bIdx = domV.get(iVal.getBase());
						add(pIdx, vIdx, bIdx);
					}
				}
			}
		}
		*/
	}
}

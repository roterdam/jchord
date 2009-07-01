/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import chord.doms.DomF;
import chord.doms.DomT;
import chord.project.Chord;
import chord.project.ProgramRel;

/**
 * Relation containing each tuple (t,f) such that f is the static
 * field named <tt>class</tt> of type <tt>java.lang.Class</tt>
 * declared in type t.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "clsTF",
	sign = "T0,F0:F0_T0"
)
public class RelClsTF extends ProgramRel {
	public void fill() {
		// TODO
		/*
		DomF domF = (DomF) doms[0];
		DomT domT = (DomT) doms[1];
		int numT = domT.size();
		for (int tIdx = 0; tIdx < numT; tIdx++) {
			jq_Type tVal = domT.get(tIdx);
			jq_Field fVal = tVal.getField("class");
			if (fVal == null)
				continue;
			int fIdx = domF.get(fVal);
			if (fIdx == -1)
				continue;
			add(fIdx, tIdx);
		}
		*/
	}
}

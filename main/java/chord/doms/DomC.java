/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.analyses.alias.Ctxt;
import chord.project.Program;
import chord.project.ProgramDom;
import chord.project.Project;
import chord.util.Assertions;

/**
 * Domain of abstract contexts of methods.
 * <p>
 * The 0th element in this domain denotes the distinguished
 * abstract context <tt>epsilon</tt>
 * (see {@link chord.analyses.alias.Ctxt}).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomC extends ProgramDom<Ctxt> {
	private DomH domH;
	private DomI domI;
	public Ctxt setCtxt(Quad[] elems) {
		Ctxt cVal = new Ctxt(elems);
		int cIdx = get(cVal);
		if (cIdx != -1)
			return (Ctxt) get(cIdx);
		set(cVal);
		return cVal;
	}
	public String toXMLAttrsString(Ctxt cVal) {
		if (domH == null)
			domH = (DomH) Project.getTrgt("H");
		if (domI == null)
			domI = (DomI) Project.getTrgt("I");
		Quad[] elems = cVal.getElems();
		int n = elems.length;
		if (n == 0)
			return "";
		String s = "ids=\"";
		for (int i = 0; i < n; i++) {
			Quad eVal = elems[i];
			Operator op = eVal.getOperator();
			if (op instanceof New || op instanceof NewArray) {
				int hIdx = domH.get(eVal);
				s += "H" + hIdx;
			} else if (op instanceof Invoke) {
				int iIdx = domI.get(eVal);
				s += "I" + iIdx;
			} else
				Assertions.Assert(false);
			if (i < n - 1)
				s += " ";
		}
		return s + "\" ";
	}
}

/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.alias.common;

import java.util.Set;

import chord.project.ProgramDom;
import chord.project.Project;

/**
 * Domain of abstract objects.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomO extends ProgramDom<Obj> {
	private DomC domC;
	public String toXMLAttrsString(Obj oVal) {
		if (domC == null)
			domC = (DomC) Project.getTrgt("C");
		Set<Ctxt> pts = oVal.pts;
		if (pts.size() == 0)
			return "";
		String s = "Cids=\"";
		for (Ctxt cVal : pts) {
			int cIdx = domC.indexOf(cVal);
			s += "C" + cIdx + " ";
		}
		s = s.substring(0, s.length() - 1);
        return s + "\"";
	}
}

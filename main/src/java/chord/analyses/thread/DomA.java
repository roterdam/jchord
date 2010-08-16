/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import chord.project.Project;
import chord.project.analyses.ProgramDom;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.util.tuple.object.Trio;
import chord.doms.DomM;

/**
 * Domain of abstract threads.
 * <p>
 * An abstract thread is a triple <tt>(o,c,m)</tt> denoting the thread
 * whose abstract object is 'o' and which starts at method 'm' in
 * abstract context 'c'.
 *
 * @see chord.analyses.thread.ThreadsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomA extends ProgramDom<Trio<Ctxt, Ctxt, jq_Method>> {
	private DomC domC;
	private DomM domM;
    public String toXMLAttrsString(Trio<Ctxt, Ctxt, jq_Method> aVal) {
		if (domC == null)
			domC = (DomC) Project.getTrgt("C");
		if (domM == null)
			domM = (DomM) Project.getTrgt("M");
		if (aVal == null)
			return "";
		int o = domC.indexOf(aVal.val0);
		int c = domC.indexOf(aVal.val1);
		int m = domM.indexOf(aVal.val2);
		return "Oid=\"C" + o + "\" Cid=\"C" + c + "\" Mid=\"M" + m + "\"";
    }
}

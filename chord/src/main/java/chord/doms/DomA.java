/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Method;
import chord.project.Program;
import chord.project.ProgramDom;
import chord.project.Project;
import chord.analyses.alias.Ctxt;
import chord.util.tuple.object.Pair;
import chord.doms.DomM;
import chord.doms.DomC;

/**
 * Domain of abstract threads.
 * <p>
 * Each abstract thread is a pair <tt>(c,m)</tt> such that <tt>m</tt>
 * is the thread's entry-point method and <tt>c</tt> is an abstract
 * context of the method.
 * <p>
 * The 0th element in this domain is null and does not denote any
 * abstract thread.
 * <p>
 * The 1st element in this domain denotes the implicitly created main
 * thread and is of the form <tt>(epsilon, main)</tt> where
 * <tt>main</tt> is the main method of the program and <tt>epsilon</tt>
 * is its lone abstract context.
 * <p>
 * The 2nd element onwards in this domain denotes an explicitly
 * created thread and is of the form <tt>(c, start)</tt> where
 * <tt>start</tt> denotes the <tt>start()</tt> method of class
 * <tt>java.lang.Thread</tt> and <tt>c</tt> is an abstract context
 * of that method.
 * 
 * @see chord.analyses.thread.ForkCtxtsAnalysis
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DomA extends ProgramDom<Pair<Ctxt, jq_Method>> {
	private DomC domC;
	private DomM domM;
    public String toXMLAttrsString(Pair<Ctxt, jq_Method> aVal) {
		if (domC == null)
			domC = (DomC) Project.getTrgt("C");
		if (domM == null)
			domM = (DomM) Project.getTrgt("M");
		if (aVal == null)
			return "";
		int c = domC.indexOf(aVal.val0);
		int m = domM.indexOf(aVal.val1);
		return "Cid=\"C" + c + "\" Mid=\"M" + m + "\"";
    }
}

/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.JavaAnalysis;
import chord.project.Project;
import chord.analyses.alias.Ctxt;
import chord.doms.DomA;
import chord.doms.DomC;
import chord.doms.DomM;
import chord.project.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Reachable abstract threads analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "fork-ctxts-java",
	consumedNames = { "forkC" },
	producedNames = { "A", "forkACM" },
    namesOfSigns = { "forkACM" },
    signs = { "A0,C0,M0:A0_M0_C0" },
	namesOfTypes = { "A" },
	types = { DomA.class }
)
public class ForkCtxtsAnalysis extends JavaAnalysis {
	public void run() {
        ProgramRel relForkC =
        	(ProgramRel) Project.getTrgt("forkC");
        relForkC.load();
        DomC domC = (DomC) Project.getTrgt("C");
        DomM domM = (DomM) Project.getTrgt("M");
        jq_Method mainMeth = domM.get(0);
        jq_Method forkMeth = domM.get(1);
        DomA domA = (DomA) Project.getTrgt("A");
        domA.clear();
		domA.getOrAdd(null);
        domA.getOrAdd(new Pair<Ctxt, jq_Method>(domC.get(0), mainMeth));
        Iterable<Ctxt> tuples = relForkC.getAry1ValTuples();
        for (Ctxt ctxt : tuples) {
            domA.getOrAdd(new Pair<Ctxt, jq_Method>(ctxt, forkMeth));
        }
        domA.save();
        relForkC.close();
        ProgramRel relForkACM =
        	(ProgramRel) Project.getTrgt("forkACM");
        relForkACM.zero();
        for (int a = 1; a < domA.size(); a++) {
            Pair<Ctxt, jq_Method> cm = domA.get(a);
            int c = domC.indexOf(cm.val0);
            int m = domM.indexOf(cm.val1);
            relForkACM.add(a, c, m);
        }
        relForkACM.save();
	}
}

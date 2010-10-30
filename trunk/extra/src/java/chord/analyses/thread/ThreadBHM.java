/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.doms.DomM;
import chord.doms.DomH;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Static analysis computing reachable abstract threads.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "threadBHM-java",
	consumes = { "threadH" },
	produces = { "B", "threadBHM" },
    namesOfSigns = { "threadBHM" },
    signs = { "B0,H0,M0:B0_M0_H0" },
	namesOfTypes = { "B" },
	types = { DomB.class }
)
public class ThreadBHM extends JavaAnalysis {
	public void run() {
		Program program = Program.g();
        DomH domH = (DomH) ClassicProject.g().getTrgt("H");
        DomM domM = (DomM) ClassicProject.g().getTrgt("M");
        DomB domB = (DomB) ClassicProject.g().getTrgt("B");
        domB.clear();
		domB.add(null);
        jq_Method mainMeth = program.getMainMethod();
        domB.add(new Pair<Quad, jq_Method>(null, mainMeth));
        jq_Method threadStartMeth = program.getThreadStartMethod();
		if (threadStartMeth != null) {
        	ProgramRel relThreadH = (ProgramRel) ClassicProject.g().getTrgt("threadH");
			relThreadH.load();
			Iterable<Quad> tuples = relThreadH.getAry1ValTuples();
			for (Quad q : tuples) {
				domB.add(new Pair<Quad, jq_Method>(q, threadStartMeth));
			}
        	relThreadH.close();
		}
		domB.save();
        ProgramRel relThreadBHM = (ProgramRel) ClassicProject.g().getTrgt("threadBHM");
        relThreadBHM.zero();
        for (int b = 1; b < domB.size(); b++) {
			Pair<Quad, jq_Method> hm = domB.get(b);
			int h = domH.indexOf(hm.val0);
            int m = domM.indexOf(hm.val1);
            relThreadBHM.add(b, h, m);
        }
        relThreadBHM.save();
	}
}

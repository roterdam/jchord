/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.thread;

import joeq.Class.jq_Method;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.analyses.method.DomM;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pair;

/**
 * Static analysis computing reachable abstract threads.
 * <p>
 * Domain A is the domain of reachable abstract threads.
 * The 0th element does not denote any abstract thread; it is a placeholder for convenience.
 * The 1st element denotes the main thread.
 * The remaining elements denote threads explicitly created by calling the
 * {@code java.lang.Thread.start()} method; there is a separate element for each abstract
 * object to which the {@code this} argument of that method may point, as dictated by the
 * points-to analysis used.
 * <p>
 * Relation threadACM contains each tuple (a,c,m) such that abstract thread 'a' is started
 * at thread-root method 'm' in abstract context 'c'.  Thread-root method 'm' may be either:
 * <ul>
 *   <li>
 *     the main method, in which case 'c' is epsilon (element 0 in domain C), or
 *   </li>
 *   <li>
 *     the {@code java.lang.Thread.start()} method, in which case 'c' may be epsilon
 *     (if the call graph is built using 0-CFA) or it may be a chain of possibly
 *     interspersed call/allocation sites (if the call graph is built using k-CFA or
 *     k-object-sensitive analysis or a combination of the two).
 *   </li>
 * </ul>
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "threads-java",
	consumes = { "threadC" },
	produces = { "A", "threadACM" },
	namesOfSigns = { "threadACM" },
	signs = { "A0,C0,M0:A0_M0_C0" },
	namesOfTypes = { "A" },
	types = { DomA.class }
)
public class ThreadsAnalysis extends JavaAnalysis {
	public void run() {
		Program program = Program.g();
		DomC domC = (DomC) ClassicProject.g().getTrgt("C");
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		DomA domA = (DomA) ClassicProject.g().getTrgt("A");
		domA.clear();
		domA.add(null);
		jq_Method mainMeth = program.getMainMethod();
		Ctxt epsilon = domC.get(0);
		domA.add(new Pair<Ctxt, jq_Method>(epsilon, mainMeth));
		jq_Method threadStartMeth = program.getThreadStartMethod();
		if (threadStartMeth != null) {
			ProgramRel relThreadC = (ProgramRel) ClassicProject.g().getTrgt("threadC");
			relThreadC.load();
			Iterable<Ctxt> tuples = relThreadC.getAry1ValTuples();
			for (Ctxt c : tuples) {
				domA.add(new Pair<Ctxt, jq_Method>(c, threadStartMeth));
			}
			relThreadC.close();
		}
		domA.save();
		ProgramRel relThreadACM = (ProgramRel) ClassicProject.g().getTrgt("threadACM");
		relThreadACM.zero();
		for (int a = 1; a < domA.size(); a++) {
			Pair<Ctxt, jq_Method> cm = domA.get(a);
			int c = domC.indexOf(cm.val0);
			int m = domM.indexOf(cm.val1);
			relThreadACM.add(a, c, m);
		}
		relThreadACM.save();
	}
}

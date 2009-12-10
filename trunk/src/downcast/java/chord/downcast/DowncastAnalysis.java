/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.downcast;

import chord.project.Chord;
import chord.project.Project;
import chord.project.JavaAnalysis;
import chord.project.ProgramRel;

/**
 * Static downcast safety analysis.
 * <p> 
 * Outputs relations <tt>safeDowncast</tt> and <tt>unsafeDowncast</tt>
 * containing pairs (v,t) such that local variable v of reference type
 * (say) t' may be cast to reference type t which is not a supertype
 * of t', and the cast, as deemed by this analysis, is either provably
 * safe or possibly unsafe, respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 * <li><tt>chord.max.iters</tt> (default is 0)</li>
 * <li>All system properties recognized by abstract contexts analysis
 * (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "downcast-java"
)
public class DowncastAnalysis extends JavaAnalysis {
	private int maxIters;

	private ProgramRel relRefineH;
	private ProgramRel relRefineM;
	private ProgramRel relRefineV;
	private ProgramRel relRefineI;

	public void run() {
		maxIters = Integer.getInteger("chord.max.iters", 0);
		assert (maxIters >= 0);

		relRefineH = (ProgramRel) Project.getTrgt("refineH");
		relRefineM = (ProgramRel) Project.getTrgt("refineM");
		relRefineV = (ProgramRel) Project.getTrgt("refineV");
		relRefineI = (ProgramRel) Project.getTrgt("refineI");

		for (int numIters = 0; true; numIters++) {
			Project.runTask("downcast-dlog");
			if (numIters == maxIters)
				break;
			Project.runTask("downcast-feedback-dlog");
			Project.runTask("refine-hybrid-dlog");
			relRefineH.load();
			int numRefineH = relRefineH.size();
			relRefineH.close();
			relRefineM.load();
			int numRefineM = relRefineM.size();
			relRefineM.close();
			relRefineV.load();
			int numRefineV = relRefineV.size();
			relRefineV.close();
			relRefineI.load();
			int numRefineI = relRefineI.size();
			relRefineI.close();
			if (numRefineH == 0 && numRefineM == 0 &&
				numRefineV == 0 && numRefineI == 0)
				break;
			Project.resetTaskDone("ctxts-java");
		}
	}
}

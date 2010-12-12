/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.rels;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Class;
import joeq.Class.jq_NameAndDesc;
import joeq.Compiler.Quad.Quad;

import chord.doms.DomI;
import chord.doms.DomH;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.JavaAnalysis;
import chord.util.tuple.object.Pair;

/**
 * Analysis producing the following two relations:
 * 1. objNewInstIH: Relation containing each tuple (i,h) such that call site i
 *	calling method "static Object newInstance()" defined in class
 *	"java.lang.Class" is treated as object allocation site h.
 * 2. objNewInstIM: Relation containing each tuple (i,m) such that call site i
 *	calling method "static Object newInstance()" defined in class
 *	"java.lang.Class" is treated as calling the nullary constructor m
 *	on the freshly created object.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name		 = "objNewInst-java",
	consumes	 = { "I", "H", "M" },
	produces	 = { "objNewInstIH", "objNewInstIM" },
	namesOfSigns = { "objNewInstIH", "objNewInstIM" },
	signs		= { "I0,H0:I0_H0", "I0,M0:I0xM0" }
)
public class ObjNewInstAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		ProgramRel relObjNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIH");
		ProgramRel relObjNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("objNewInstIM");
		relObjNewInstIH.zero();
		relObjNewInstIM.zero();
		DomI domI = (DomI) ClassicProject.g().getTrgt("I");
		DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		List<Pair<Quad, List<jq_Reference>>> l =
			Program.g().getReflect().getResolvedObjNewInstSites();
		for (Pair<Quad, List<jq_Reference>> p : l) {
			Quad q = p.val0;
			int iIdx = domI.indexOf(q);
			if(iIdx < 0)
				System.err.println("ObjNewInstAnalysis can't resolve quad " + q + " in " + q.getMethod() + " " + q.getMethod().getDeclaringClass());
			assert (iIdx >= 0);
			int hIdx = domH.indexOf(q);
			assert (hIdx >= 0);
			relObjNewInstIH.add(iIdx, hIdx);
			for (jq_Reference r : p.val1) {
				if (r instanceof jq_Class) {
					jq_Class c = (jq_Class) r;
					jq_Method m = c.getInitializer(new jq_NameAndDesc("<init>", "()V"));
					if (m != null) {
						int mIdx = domM.indexOf(m);
						if (mIdx >= 0)
							   relObjNewInstIM.add(iIdx, mIdx);
					}
				}
			}
		}
		relObjNewInstIH.save();
		relObjNewInstIM.save();
	}
}


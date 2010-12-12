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
import joeq.Class.jq_InstanceMethod;
import joeq.Compiler.Quad.Quad;

import chord.doms.DomI;
import chord.doms.DomH;
import chord.doms.DomM;
import chord.project.ClassicProject;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Analysis producing the following two relations:
 * 1. conNewInstIH: Relation containing each tuple (i,h) such that call site i
 *	calling method "Object newInstance(Object[] initargs)" defined in class
 *	"java.lang.reflect.Constructor" is treated as object allocation site h.
 * 2. conNewInstIM: Relation containing each tuple (i,m) such that call site i
 *	calling method "Object newInstance(Object[] initargs)" defined in class
 *	"java.lang.reflect.Constructor" is treated as calling constructor m
 *	on the freshly created object.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "conNewInst-java",
	consumes	 = { "I", "H", "M" },
	produces	 = { "conNewInstIH", "conNewInstIM" },
	namesOfSigns = { "conNewInstIH", "conNewInstIM" },
	signs		= { "I0,H0:I0_H0", "I0,M0:I0xM0" }
)
public class ConNewInstAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		ProgramRel relConNewInstIH = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIH");
		ProgramRel relConNewInstIM = (ProgramRel) ClassicProject.g().getTrgt("conNewInstIM");
		relConNewInstIH.zero();
		relConNewInstIM.zero();
		DomI domI = (DomI) ClassicProject.g().getTrgt("I");
		DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		List<Pair<Quad, List<jq_Reference>>> l =
			Program.g().getReflect().getResolvedConNewInstSites();
		for (Pair<Quad, List<jq_Reference>> p : l) {
			Quad q = p.val0;
			int iIdx = domI.indexOf(q);
			assert (iIdx >= 0) : ("Quad " + q.toLocStr() + " not found in domain I.");
			int hIdx = domH.indexOf(q);
			assert (hIdx >= 0) : ("Quad " + q.toLocStr() + " not found in domain H.");
			relConNewInstIH.add(iIdx, hIdx);
			for (jq_Reference r : p.val1) {
				if (r instanceof jq_Class) {
					jq_Class c = (jq_Class) r;
					jq_InstanceMethod[] meths = c.getDeclaredInstanceMethods();
					for (int i = 0; i < meths.length; i++) {
						jq_InstanceMethod m = meths[i];
						if (m.getName().toString().equals("<init>")) {
							int mIdx = domM.indexOf(m);
							if (mIdx >= 0)
								relConNewInstIM.add(iIdx, mIdx);
						}
					}
				}
			}
		}
		relConNewInstIH.save();
		relConNewInstIM.save();
	}
}


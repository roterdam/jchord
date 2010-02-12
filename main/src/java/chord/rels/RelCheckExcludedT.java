/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import chord.doms.DomT;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.project.Properties;

/**
 * Relation containing each type t the prefix of whose name
 * is contained in the value of system property
 * <tt>chord.check.exclude</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "checkExcludedT",
	sign = "T0:T0"
)
public class RelCheckExcludedT extends ProgramRel {
	public void fill() {
		DomT domT = (DomT) doms[0];
		String[] checkExcludedPrefixes = Properties.toArray(
			Properties.checkExcludeStr);
		for (jq_Class c : Program.v().getPreparedClasses()) {
			String cName = c.getName();
			for (String prefix : checkExcludedPrefixes) {
				if (cName.startsWith(prefix)) {
					int tIdx = domT.indexOf(c);
					add(tIdx);
					break;
				}
			}
		}
	}
}

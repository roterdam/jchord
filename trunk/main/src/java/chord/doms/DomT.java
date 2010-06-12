/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of classes.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "T"
)
public class DomT extends ProgramDom<jq_Class> {
	@Override
	public void fill() {
		for (jq_Class c : Program.v().getPreparedClasses()) {
			getOrAdd(c);
		}
	}
	@Override
    public String toXMLAttrsString(jq_Class c) {
        String name = c.getName();
        String file = Program.getSourceFileName(c);
        int line = 0;  // TODO
        return "name=\"" + name +
            "\" file=\"" + file +
            "\" line=\"" + line + "\"";
    }
}

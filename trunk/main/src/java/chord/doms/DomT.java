/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Type;
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
public class DomT extends ProgramDom<jq_Type> {
	@Override
	public void fill() {
		for (jq_Type t : Program.v().getAllTypes()) {
			getOrAdd(t);
		}
	}
	@Override
    public String toXMLAttrsString(jq_Type t) {
        String name = t.getName();
		String file;
		if (t instanceof jq_Class)
        	file = Program.getSourceFileName((jq_Class) t);
		else
			file = "";
        int line = 0;  // TODO
        return "name=\"" + name +
            "\" file=\"" + file +
            "\" line=\"" + line + "\"";
    }
}

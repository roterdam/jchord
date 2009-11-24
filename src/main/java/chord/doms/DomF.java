/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramDom;
import chord.visitors.IFieldVisitor;

/**
 * Domain of fields.
 * <p>
 * The 0th element in this domain (null) denotes a distinguished
 * hypothetical field that is regarded as accessed whenever an
 * array element is accessed.  This field can be used by analyses
 * that do not distinguish between different elements of the
 * same array.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "F"
)
public class DomF extends ProgramDom<jq_Field>
		implements IFieldVisitor {
	public void init() {
		// Reserve index 0 for the distinguished hypothetical field 
		// representing all array elements
		getOrAdd(null);
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Field f) {
		getOrAdd(f);
	}
	public String toXMLAttrsString(jq_Field f) {
		String sign;
		String file;
		int line;
		if (f == null) {
			sign = "[*]";
			file = "";
			line = 0;
		} else {
			jq_Class c = f.getDeclaringClass();
			sign = c.getName() + "." + f.getName();
			file = Program.getSourceFileName(c);
			line = 0; // TODO
		}
        return "sign=\"" + sign +
            "\" file=\"" + file +
            "\" line=\"" + line + "\"";
	}
}

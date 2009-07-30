/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramDom;

/**
 * Domain of types.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "T"
)
public class DomT extends ProgramDom<jq_Type> {
	public void fill() {
		for (jq_Class c : Program.getPreparedClasses()) {
			set(c);
			for (Object o : c.getMembers()) {
				if (o instanceof jq_Field) {
					jq_Field f = (jq_Field) o;
					jq_Type t = f.getType();
					set(t);
				} else {
					jq_Method m = (jq_Method) o;
					for (jq_Type t : m.getParamTypes()) {
						set(t);
					}
					set(m.getReturnType());
					if (m.isAbstract())
						continue;
					ControlFlowGraph cfg = Program.getCFG(m);
					RegisterFactory rf = cfg.getRegisterFactory();
					for (Object o2 : rf) {
						Register v = (Register) o2;
						jq_Type t = v.getType();
						set(t);
					}
				}
			}
		}
	}
    public String toXMLAttrsString(jq_Type t) {
        String name = t.getName();
        String file = (t instanceof jq_Class) ?
        	Program.getSourceFileName((jq_Class) t) : "null";
        int line = 0;  // TODO
        return "name=\"" + name +
            "\" file=\"" + file +
            "\" line=\"" + line + "\"";
    }
    public String toString(jq_Type t) {
    	return Program.toString(t);
    }
}

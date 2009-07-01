/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import java.util.HashMap;
import java.util.Map;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramDom;
import chord.visitors.IVarVisitor;

/**
 * Domain of local variables of reference type.
 * <p>
 * Each local variable declared in each block of each method is
 * represented by a unique element in this domain, in particular,
 * local variables that have the same name but are declared in
 * different methods or in different blocks of the same method
 * are represented by different elements in this domain.
 * <p>
 * All local variables of the same method are assigned contiguous
 * indices in this domain, in particular, the set of local
 * variables of a method is the disjoint union of its argument
 * variables and its temporary variables, and the argument
 * variables are assigned contiguous indices in order followed by
 * the temporary variables.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "V"
)
public class DomV extends ProgramDom<Register>
		implements IVarVisitor {
	private Map<Register, jq_Method> varToMethodMap;
	private jq_Method ctnrMethod;
	public void init() {
		varToMethodMap = new HashMap<Register, jq_Method>();
	}
	public jq_Method getMethod(Register v) {
		return varToMethodMap.get(v);
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visit(Register v) {
		if (!v.getType().isPrimitiveType()) {
			varToMethodMap.put(v, ctnrMethod);
			set(v);
		}
	}
}

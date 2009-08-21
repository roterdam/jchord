/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (p,v,f) such that the statement
 * at program point p is of the form <tt>v = f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PgetStatFldInst",
	sign = "P0,V0,F0:F0_P0_V0"
)
public class RelPgetStatFldInst extends ProgramRel
		implements IHeapInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitHeapInst(Quad p) {
		Operator op = p.getOperator();
		if (op instanceof Getstatic) {
			jq_Field f = Getstatic.getField(p).getField();
			if (f.getType().isReferenceType()) {
				RegisterOperand lo = Getstatic.getDest(p);
				Register l = lo.getRegister();
				add(p, l, f);
			}
		}
	}
}

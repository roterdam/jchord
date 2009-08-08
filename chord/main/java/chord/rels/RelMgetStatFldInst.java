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
 * Relation containing each tuple (m,v,f) such that method m contains
 * a statement of the form <tt>v = f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MgetStatFldInst",
	sign = "M0,V0,F0:F0_M0_V0"
)
public class RelMgetStatFldInst extends ProgramRel
		implements IHeapInstVisitor {
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Getstatic) {
			jq_Field f = Getstatic.getField(q).getField();
			if (f.getType().isReferenceType()) {
				RegisterOperand lo = Getstatic.getDest(q);
				Register l = lo.getRegister();
				try {
					add(ctnrMethod, l, f);
				} catch (RuntimeException ex) {
					// TODO
				}
			}
		}
	}
}

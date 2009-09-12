/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Method;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (p,f,v) such that the statement
 * at program point p is of the form <tt>f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PputStatFldInst",
	sign = "P0,F0,V0:F0_P0_V0"
)
public class RelPputStatFldInst extends ProgramRel
		implements IHeapInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitHeapInst(Quad p) {
		Operator op = p.getOperator();
		if (op instanceof Putstatic) {
			jq_Field f = Putstatic.getField(p).getField();
			if (f.getType().isReferenceType()) {
				Operand rx = Putstatic.getSrc(p);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					add(p, f, r);
				}
			}
		}
	}
}

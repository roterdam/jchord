/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IMoveInstVisitor;

/**
 * Relation containing each tuple (p,v1,v2) such that the statement
 * at program point p is of the form <tt>v1 = v2</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PobjVarAsgnInst",
	sign = "P0,V0,V1:P0_V0xV1"
)
public class RelPobjVarAsgnInst extends ProgramRel
		implements IMoveInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitMoveInst(Quad p) {
		Operand rx = Move.getSrc(p);
		if (rx instanceof RegisterOperand) {
			RegisterOperand ro = (RegisterOperand) rx;
			if (ro.getType().isReferenceType()) {
				Register r = ro.getRegister();
				RegisterOperand lo = Move.getDest(p);
				Register l = lo.getRegister();
				add(p, l, r);
			}
		}
	}
}

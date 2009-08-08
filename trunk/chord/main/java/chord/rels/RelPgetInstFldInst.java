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
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (m,b,f,v) such that the statement
 * at program point p is of the form <tt>b.f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PgetInstFldInst",
	sign = "P0,V0,F0,V1:F0_P0_V0xV1"
)
public class RelPgetInstFldInst extends ProgramRel
		implements IHeapInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitHeapInst(Quad p) {
		Operator op = p.getOperator();
		if (op instanceof ALoad) {
			if (((ALoad) op).getType().isReferenceType()) {
				RegisterOperand lo = ALoad.getDest(p);
				Register l = lo.getRegister();
				RegisterOperand bo = (RegisterOperand) ALoad.getBase(p);
				Register b = bo.getRegister();
				jq_Field f = null;
				add(p, l, f, b);
			}
			return;
		}
		if (op instanceof Getfield) {
			jq_Field f = Getfield.getField(p).getField();
			if (f.getType().isReferenceType()) {
				RegisterOperand lo = Getfield.getDest(p);
				Register l = lo.getRegister();
				RegisterOperand bo = (RegisterOperand) Getfield.getBase(p);
				Register b = bo.getRegister();
				add(p, l, f, b);
			}
		}
	}
}

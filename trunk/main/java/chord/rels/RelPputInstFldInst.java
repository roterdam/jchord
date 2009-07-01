/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (p,b,f,v) such that the statement
 * at program point p is of the form <tt>b.f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "PputInstFldInst",
	sign = "P0,V0,F0,V1:F0_P0_V0xV1"
)
public class RelPputInstFldInst extends ProgramRel
		implements IHeapInstVisitor {
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitHeapInst(Quad p) {
		Operator op = p.getOperator();
		if (op instanceof AStore) {
			if (((AStore) op).getType().isReferenceType()) {
				Operand rx = AStore.getValue(p);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					RegisterOperand bo = (RegisterOperand) AStore.getBase(p);
					Register b = bo.getRegister();
					jq_Field f = null;
					add(p, b, f, r);
				}
			}
			return;
		}
		if (op instanceof Putfield) {
			jq_Field f = Putfield.getField(p).getField();
			if (f.getType().isReferenceType()) {
				Operand rx = Putfield.getSrc(p);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					RegisterOperand bo = (RegisterOperand) Putfield.getBase(p);
					Register b = bo.getRegister();
					add(p, b, f, r);
				}
			}
		}
	}
}

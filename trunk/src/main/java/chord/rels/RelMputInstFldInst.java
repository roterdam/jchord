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
 * Relation containing each tuple (m,b,f,v) such that method m
 * contains a statement of the form <tt>b.f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MputInstFldInst",
	sign = "M0,V0,F0,V1:F0_M0_V0xV1"
)
public class RelMputInstFldInst extends ProgramRel
		implements IHeapInstVisitor {
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof AStore) {
			if (((AStore) op).getType().isReferenceType()) {
				Operand rx = AStore.getValue(q);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					RegisterOperand bo = (RegisterOperand) AStore.getBase(q);
					Register b = bo.getRegister();
					jq_Field f = null;
					try {
						add(ctnrMethod, b, f, r);
					} catch (RuntimeException ex) {
						// TODO
					}
				}
			}
			return;
		}
		if (op instanceof Putfield) {
			jq_Field f = Putfield.getField(q).getField();
			if (f.getType().isReferenceType()) {
				Operand rx = Putfield.getSrc(q);
				if (rx instanceof RegisterOperand) {
					Operand bx = Putfield.getBase(q);
					if (bx instanceof RegisterOperand) {
						RegisterOperand bo = (RegisterOperand) bx;
						RegisterOperand ro = (RegisterOperand) rx;
						Register b = bo.getRegister();
						Register r = ro.getRegister();
						try {
							add(ctnrMethod, b, f, r);
						} catch (RuntimeException ex) {
							// TODO
						}
					}
				}
			}
		}
	}
}

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
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (m,v,b,f) such that method m
 * contains a statement of the form <tt>v = b.f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MgetInstFldInst",
	sign = "M0,V0,V1,F0:F0_M0_V0xV1"
)
public class RelMgetInstFldInst extends ProgramRel
		implements IHeapInstVisitor {
	private jq_Method ctnrMethod;
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof ALoad) {
			if (((ALoad) op).getType().isReferenceType()) {
				RegisterOperand lo = ALoad.getDest(q);
				Register l = lo.getRegister();
				RegisterOperand bo = (RegisterOperand) ALoad.getBase(q);
				Register b = bo.getRegister();
				jq_Field f = null;
				add(ctnrMethod, l, b, f);
			}
			return;
		}
		if (op instanceof Getfield) {
			jq_Field f = Getfield.getField(q).getField();
			if (f.getType().isReferenceType()) {
				Operand bx = Getfield.getBase(q);
				if (bx instanceof RegisterOperand) {
					RegisterOperand bo = (RegisterOperand) bx;
					Register b = bo.getRegister();
					RegisterOperand lo = Getfield.getDest(q);
					Register l = lo.getRegister();
					try {
						add(ctnrMethod, l, b, f);
					} catch (RuntimeException ex) {
						// TODO
					}
				} else
					assert (bx instanceof AConstOperand);
			}
		}
	}
}

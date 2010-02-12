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
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomM;
import chord.doms.DomF;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.ProgramRel;
import chord.visitors.IHeapInstVisitor;

/**
 * Relation containing each tuple (m,f,v) such that method m contains
 * a statement of the form <tt>f = v</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MputStatFldInst",
	sign = "M0,F0,V0:F0_M0_V0"
)
public class RelMputStatFldInst extends ProgramRel
		implements IHeapInstVisitor {
    private DomM domM;
    private DomF domF;
    private DomV domV;
	private jq_Method ctnrMethod;
    public void init() {
        domM = (DomM) doms[0];
        domF = (DomF) doms[1];
        domV = (DomV) doms[2];
    }
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Putstatic) {
			FieldOperand fo = Putstatic.getField(q);
			fo.resolve();
			jq_Field f = fo.getField();
			if (f.getType().isReferenceType()) {
				Operand rx = Putstatic.getSrc(q);
				if (rx instanceof RegisterOperand) {
					RegisterOperand ro = (RegisterOperand) rx;
					Register r = ro.getRegister();
					int mIdx = domM.indexOf(ctnrMethod);
					assert (mIdx != -1);
					int rIdx = domV.indexOf(r);
					assert (rIdx != -1);
					int fIdx = domF.indexOf(f);
					if (fIdx == -1) {
						System.out.println("WARNING: MputStatFldInst: method: " +
							ctnrMethod + " quad: " + q);
					} else
						add(mIdx, fIdx, rIdx);
				}
			}
		}
	}
}

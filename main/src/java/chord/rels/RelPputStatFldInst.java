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
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomP;
import chord.doms.DomF;
import chord.doms.DomV;
import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

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
    private DomP domP;
    private DomF domF;
    private DomV domV;
    public void init() {
        domP = (DomP) doms[0];
        domF = (DomF) doms[1];
        domV = (DomV) doms[2];
    }
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
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
					int pIdx = domP.indexOf(q);
					assert (pIdx != -1);
					int rIdx = domV.indexOf(r);
					assert (rIdx != -1);
					int fIdx = domF.indexOf(f);
					if (fIdx == -1) {
						System.out.println("WARNING: PputStatFldInst: " +
							" quad: " + q);
					} else
						add(pIdx, fIdx, rIdx);
				}
			}
		}
	}
}

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
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomP;
import chord.doms.DomV;
import chord.doms.DomF;
import chord.program.visitors.IHeapInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

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
    private DomP domP;
    private DomV domV;
    private DomF domF;
    public void init() {
        domP = (DomP) doms[0];
        domV = (DomV) doms[1];
        domF = (DomF) doms[2];
    }
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { }
	public void visitHeapInst(Quad q) {
		Operator op = q.getOperator();
		if (op instanceof Getstatic) {
			jq_Field f = Getstatic.getField(q).getField();
			if (f.getType().isReferenceType()) {
				RegisterOperand lo = Getstatic.getDest(q);
				Register l = lo.getRegister();
				int pIdx = domP.indexOf(q);
				assert (pIdx != -1);
				int lIdx = domV.indexOf(l);
				assert (lIdx != -1);
				int fIdx = domF.indexOf(f);
				if (fIdx == -1) {
					System.out.println("WARNING: PgetStatFldInst: " +
						" quad: " + q);
				} else
					add(pIdx, lIdx, fIdx);
			}
		}
	}
}

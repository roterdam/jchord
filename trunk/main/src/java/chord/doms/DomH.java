/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;

import chord.project.Chord;
import chord.project.Project;
import chord.program.Program;
import chord.project.analyses.ProgramDom;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.Operator;

/**
 * Domain of object allocation statements.
* <p>		
 * The 0th element of this domain (null) is a distinguished		
 * hypothetical object allocation statement that may be used		
 * for various purposes.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "H",
	consumedNames = { "M" }
)
public class DomH extends QuadDom {
	public void init() {		
		super.init();		
		getOrAdd(null);		
	}
    public void fill() {
        DomM domM = (DomM) Project.getTrgt("M");
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            jq_Method m = domM.get(mIdx);
            if (m.isAbstract())
                continue;
            ControlFlowGraph cfg = m.getCFG();
			ctnrMethod = m;
            for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
                    it.hasNext();) {
                BasicBlock bb = it.nextBasicBlock();
                for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
                    Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof New || op instanceof NewArray) {
                    	getOrAdd(q);
					} else if (op instanceof InvokeVirtual) {
						jq_Method m2 = InvokeVirtual.getMethod(q).getMethod();
						String mDesc = m2.getDesc().toString();
						if (mDesc.equals("()Ljava/lang/Object;")) {
							String mName = m2.getName().toString();
							String cName = m2.getDeclaringClass().getName();
							if ((mName.equals("newInstance") && cName.equals("java.lang.Class")) ||
								(mName.equals("clone") && cName.equals("java.lang.Object"))) {
								getOrAdd(q);
							}
						}
					}
                }
            }
        }
    }
	public String toXMLAttrsString(Inst i) {
		if (i == null)
			return "";
		Quad q = (Quad) i;
		Operator op = q.getOperator();
		String type = (op instanceof New) ? New.getType(q).getType().getName() :
			(op instanceof NewArray) ? NewArray.getType(q).getType().getName() : "unknown";
		return super.toXMLAttrsString(q) + " type=\"" + type + "\"";
	}
}

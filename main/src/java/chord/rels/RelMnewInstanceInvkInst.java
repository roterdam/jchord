/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each tuple (p,v,f) such that the statement
 * at program point p is of the form <tt>v = f</tt>.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "MnewInstanceInvkInst",
	sign = "M0,V0:M0_V0"
)
public class RelMnewInstanceInvkInst extends ProgramRel
		implements IInvokeInstVisitor {
	
    private DomM domM;
    private DomV domV;
    private jq_Method ctnrMethod;
	
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) { 
		ctnrMethod = m;
	}
	
	public void init() {
        domM = (DomM) doms[0];
        domV = (DomV) doms[1];
    }
	
	public void visitInvokeInst(Quad q) {
		jq_Method mthd = Invoke.getMethod(q).getMethod();
		String mName = mthd.getName().toString();
		String cName = mthd.getDeclaringClass().getName();
		if ((mName.equals("newInstance") && cName.equals("java.lang.Class"))) {
			Register l = Invoke.getDest(q).getRegister();
			int lIdx = domV.indexOf(l);
			assert (lIdx != -1);
			int mIdx = domM.indexOf(ctnrMethod);
			assert (mIdx != -1);
			add(mIdx, lIdx);
		}
	}
}

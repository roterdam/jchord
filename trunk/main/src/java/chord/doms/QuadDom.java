/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import chord.program.Program;
import chord.project.Project;
import chord.project.analyses.ProgramDom;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;

/**
 * Abstract domain of quads and entry/exit basic blocks of methods.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class QuadDom extends ProgramDom<Inst> {
	protected DomM domM;
	protected jq_Method ctnrMethod;
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public int getOrAdd(Inst i) {
		if (i != null)
			Program.v().mapInstToMethod(i, ctnrMethod);
		return super.getOrAdd(i);
	}
	public String toUniqueString(Inst i) {
		return Program.v().toBytePosStr(i);
	}
	public String toXMLAttrsString(Inst i) {
		jq_Method m = Program.v().getMethod(i);
		String file = Program.getSourceFileName(m.getDeclaringClass());
		int line = Program.getLineNumber(i, m);
		int mIdx = domM.indexOf(m);
		return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
			"Mid=\"M" + mIdx + "\"";
	}
}

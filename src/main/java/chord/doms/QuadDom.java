/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import chord.project.Program;
import chord.project.ProgramDom;
import chord.project.Project;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

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

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
import joeq.Compiler.Quad.Quad;

public abstract class QuadDom extends ProgramDom<Quad> {
	protected DomM domM;
	protected jq_Method ctnrMethod;
	public void init() {
		domM = (DomM) Project.getTrgt("M");
	}
	public void visit(jq_Class c) { }
	public void visit(jq_Method m) {
		ctnrMethod = m;
	}
	public int set(Quad q) {
		// XXX
		if (q != null)
			Program.mapInstToMethod(q, ctnrMethod);
		return super.set(q);
	}
	public String toUniqueIdString(Quad inst) {
		/*
		Method method = inst.getOutermostCtnrMethod();
		Type type = method.getCtnrType();
		return inst.getBCI() + "@" + method.getSign() + "@" + type.getName();
		*/
		return null; // TODO
	}
	public String toXMLAttrsString(Quad q) {
		jq_Method m = Program.getMethod(q);
		String fileName = Program.getSourceFileName(m.getDeclaringClass());
		int lineNumber = Program.getLineNumber(q, m);
		return "file=\"" + fileName + "\" " + "line=\"" + lineNumber + "\" " +
			"Mid=\"M" + domM.get(m) + "\"";
	}
	public String toString(Quad q) {
		return Program.toString(q);
	}
}

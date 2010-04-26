package chord.analyses.slicer;

import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Class.jq_Method;

public class LocalVar implements Expr {
	public final Register r;
	public final jq_Method m;
	public LocalVar(Register r, jq_Method m) {
		this.r = r;
		this.m = m;
	}
}

package chord.analyses.slicer;

import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Class.jq_Method;

public class LocalVar implements Expr {
	public final Register v;
	public LocalVar(Register v) {
		this.v = v;
	}
	@Override
	public int hashCode() {
		return v.getNumber();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof LocalVar) {
			LocalVar e = (LocalVar) o;
			return e.v == this.v;
		}
		return false;
	}
	@Override
	public String toString() {
		return v.toString();
	}
}

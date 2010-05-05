package chord.analyses.slicer;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.Quad;

public class InstField implements Expr {
	public final Quad q;
	public final jq_Field f;
	public InstField(Quad q, jq_Field f) {
		this.q = q;
		this.f = f;
	}
	@Override
	public int hashCode() {
		return q.hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof InstField) {
			InstField e = (InstField) o;
			return e.q == this.q && e.f == this.f;
		}
		return false;
	}
	@Override
	public String toString() {
		return "<" + f.toString() + "," + q.toString() + ">";
	}
}

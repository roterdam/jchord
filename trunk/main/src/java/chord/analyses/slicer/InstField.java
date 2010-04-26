package chord.analyses.slicer;

import joeq.Class.jq_Field;

public class InstField implements Expr {
	public final jq_Field f;
	public InstField(jq_Field f) {
		this.f = f;
	}
}

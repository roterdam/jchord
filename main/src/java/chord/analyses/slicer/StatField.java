package chord.analyses.slicer;

import joeq.Class.jq_Field;

public class StatField implements Expr {
	public final jq_Field f;
	public StatField(jq_Field f) {
		this.f = f;
	}
}

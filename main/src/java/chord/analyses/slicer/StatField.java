package chord.analyses.slicer;

import joeq.Class.jq_Field;

public class StatField implements Expr {
	public final jq_Field f;
	public StatField(jq_Field f) {
		this.f = f;
	}
    @Override
    public int hashCode() {
        return f.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        if (o instanceof StatField) {
            StatField e = (StatField) o;
            return e.f == this.f;
        }
        return false;
    }
	@Override
	public String toString() {
		return f.toString();
	}
}

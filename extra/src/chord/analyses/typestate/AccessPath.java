package chord.analyses.typestate;

import java.util.List;
import joeq.Class.jq_Field;

public abstract class AccessPath {
	// non-null and immutable; may be empty
	protected final List<jq_Field> fields;
	public AccessPath(List<jq_Field> f) {
		this.fields = f;
	}
}


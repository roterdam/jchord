import java.util.Collections;
import java.util.List;

import joeq.Class.jq_Field;


public class GlobalAccessPath {
	jq_Field global;  // static field
	public GlobalAccessPath(jq_Field g, List<jq_Field> fields) {
		super(fields);
		assert (g != null);
		this.global = g;
	}
	public GlobalAccessPath(jq_Field g) {
		super(Collections.EMPTY_LIST);
		this.global = g;
	}
	@Override
	public int hashCode() {
		// TODO
	}
	@Override
	public boolean equals(Object obj) {
		// TODO
	}
}


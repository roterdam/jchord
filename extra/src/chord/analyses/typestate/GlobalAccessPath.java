
public class RegisterAccessPath extends AccessPath {
	// non-null
	jqField global;  // static field
	public RegisterAccessPath(jqField g, List<jqField> fields) {
		super(fields);
		assert (g != null);
		this.global = g;
	}
	public RegisterAccessPath(jqField g) {
		super(Collections.emptyList);
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


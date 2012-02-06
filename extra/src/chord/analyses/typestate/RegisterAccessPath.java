public class RegisterAccessPath extends AccessPath {
	// non-null
	private final Register var;
	public RegisterAccessPath(Register v, List<jq_Field> fields) {
		super(fields);
		assert (v != null);
		this.var = v;
	}
	public RegisterAccessPath(Register v) {
		super(Collections.emptyList);
		this.var = v;
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


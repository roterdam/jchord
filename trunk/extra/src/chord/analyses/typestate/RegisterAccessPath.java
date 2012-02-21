package chord.analyses.typestate;

import java.util.Collections;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Class.jq_Field;

public class RegisterAccessPath extends AccessPath {
	// non-null
	private final Register var;

	public RegisterAccessPath(Register v, List<jq_Field> fields) {
		super(fields);
		assert (v != null);
		this.var = v;
	}

	public RegisterAccessPath(Register v) {
		super(Collections.<jq_Field> emptyList());
		this.var = v;
	}

	public Register getRootRegister() {
		return var;
	}

	@Override
	public int hashCode() {
		return 31 * var.hashCode() + super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof RegisterAccessPath))
			return false;
		RegisterAccessPath that = (RegisterAccessPath) obj;
		return this.var.equals(that.var)
				&& this.fields.containsAll(that.fields)
				&& that.fields.containsAll(this.fields);
	}

	@Override
	public String toString() {
		String ret = "Local Path:";
		ret += ",Base Register:" + (var == null ? "EMPTY" : var.toString());
		ret += ",Fields:" + super.toString();
		return ret;
	}
}

package chord.analyses.typestate;
import java.util.Collections;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.RegisterFactory.Register;


public class GlobalAccessPath extends AccessPath {
	jq_Field global;  // static field
	public GlobalAccessPath(jq_Field g, List<jq_Field> fields) {
		super(fields);
		assert (g != null);
		this.global = g;
	}
	public GlobalAccessPath(jq_Field g) {
		super(Collections.<jq_Field> emptyList());
		this.global = g;
	}
	
	
	@Override
	public int hashCode() {
		return 31*global.hashCode() + super.hashCode(); 
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof GlobalAccessPath)) return false;
		GlobalAccessPath that = (GlobalAccessPath)obj;
		return that.global.equals(this.global) && this.fields.containsAll(that.fields) && that.fields.containsAll(this.fields);
	}
}


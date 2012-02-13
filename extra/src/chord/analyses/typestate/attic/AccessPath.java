package chord.analyses.typestate;
import java.util.List;
import joeq.Class.jq_Field;

public abstract class AccessPath {
	// non-null and immutable; may be empty
	protected final List<jq_Field> fields;
	public AccessPath(List<jq_Field> f) {
		this.fields = f;
	}
	
	@Override
	public int hashCode(){
		int code = 0;
		for(jq_Field f:fields){
			code = code ^ f.hashCode();
		}
		return code;
	}
	
	@Override
	public boolean equals(Object obj){
		if (this == obj) return true;
		if (!(obj instanceof AccessPath)) return false;
		AccessPath that = (AccessPath)obj;
		return that.fields.containsAll(this.fields) && this.fields.containsAll(that.fields);
	}
}


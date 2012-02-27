package chord.analyses.typestate;
import java.util.ArrayList;

import chord.util.ArraySet;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.UTF.Utf8;

/***
 * This is abstract state that will be maintained for each heap object
 * 
 * @author machiry
 * 
 */
public class AbstractState {
	final Quad alloc;
	final TypeState ts;
	final ArraySet<AccessPath> mustSet;
	boolean canReturn = false;
	boolean mayAlias = false;

	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
	}

	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms,
			boolean ret) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
		canReturn = ret;
	}

	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms,
			ArrayList<Utf8> transitions) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
	}

	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms,
			Boolean uni, Boolean m) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
	}

	@Override
	public int hashCode() {
		return alloc.hashCode() ^ mustSet.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AbstractState))
			return false;
		AbstractState that = (AbstractState) obj;
		return alloc == that.alloc && ts == that.ts
				&& mustSet.containsAll(that.mustSet)
				&& that.mustSet.containsAll(mustSet);
	}

	@Override
	public String toString() {
		String ret = "";
		ret = "Alloc:" + alloc.toString();
		ret += ",TypeState:" + ts.toString();
		ret += ",Can regturn:" + (canReturn ? "true" : "false");
		ret += ",Must Set:" + (mustSet.isEmpty() ? "EMPTY\n" : "\n");
		for (AccessPath ap : mustSet) {
			ret += ap + "\n";
		}
		return ret;
	}
}

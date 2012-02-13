
import java.util.ArrayList;

import chord.util.ArraySet;
import joeq.Compiler.Quad.Quad;
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
	final ArrayList<Utf8> stateTransitions;
	Boolean unique;
 	// mustSet is never null and is always immutable
	Boolean isReturn = false;
	final ArraySet<AccessPath> mustSet;
	Boolean may;
	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
		unique = true;
		may = false;
		stateTransitions = new ArrayList<Utf8>();
	}
	
	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms,ArrayList<Utf8> transitions) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
		unique = true;
		may = false;
		assert(transitions != null);
		stateTransitions = transitions;
	}
	
	public AbstractState(Quad a, TypeState t, ArraySet<AccessPath> ms,Boolean uni,Boolean m) {
		this.alloc = a;
		this.ts = t;
		assert (ms != null);
		this.mustSet = ms;
		unique = uni;
		may = m;
		stateTransitions = new ArrayList<Utf8>();
	}
	
	@Override
	public int hashCode() {
		return alloc.hashCode() ^ mustSet.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof AbstractState)) return false;
		AbstractState that = (AbstractState) obj;
		return alloc == that.alloc && ts == that.ts && mustSet.equals(that.mustSet);
	}
}


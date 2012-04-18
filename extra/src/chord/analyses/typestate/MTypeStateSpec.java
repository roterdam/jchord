package chord.analyses.typestate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.UTF.Utf8;

import chord.analyses.typestate.TypeState;
import chord.analyses.typestate.TypeStateSpec;
import chord.util.ArraySet;

public class MTypeStateSpec extends TypeStateSpec{

	protected TypeState bestState;
	
	public MTypeStateSpec() {
		super();
		bestState = null;
	}
	
	public TypeState getBestState() { return bestState; }
	
	/**
	 * This method adds the best state to the type specification
	 * 
	 * @param best
	 */
	public void addBestState(TypeState best) {
		bestState = best;
	}
	
	@Override
	public boolean isMethodOfInterest(jq_Method method) {
		return !method.isStatic();
	}

	@Override
	public String toString() {
		String s = "Type=" + type + " Start_State=" + startState + " Error_State=" + errorState + " Best_State=" + bestState;
		s += "\nUpdates:";
		for (Map.Entry<Utf8, Map<TypeState, TypeState>> e : updates.entrySet()) {
			s += "\n\t" + e.getKey() + "=";
			for (Map.Entry<TypeState, TypeState> e2 : e.getValue().entrySet())
				s += e2.getKey() + "->" + e2.getValue() + " ";
		}
		s += "\nAsserts:";
		for (Map.Entry<Utf8, Set<TypeState>> e : asserts.entrySet()) {
			s += "\n\t" + e.getKey() + "=";
			for (TypeState ts : e.getValue())
				s += ts + " ";
		}
		return s;
	}
}

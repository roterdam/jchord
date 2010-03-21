package chord.analyses.snapshot;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntProcedure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// This is a slow version which has an ensureComputed().
// Do not use this.  Instead, use ReachableFromAbstraction or PointedToByAllocAbstraction.
public class ReachabilityAbstraction extends Abstraction {
	static class Spec {
		public boolean pointedTo, matchRepeatedFields, matchFirstField,
				matchLastField;
	}

	Spec spec;
	TIntObjectHashMap<List<String>> o2pats = new TIntObjectHashMap<List<String>>(); // o -> list of path patterns that describe o

	public ReachabilityAbstraction(Spec spec) {
		this.spec = spec;
	}

	@Override
	public String toString() {
		if (spec.pointedTo)
			return "reach(point)";
		if (spec.matchRepeatedFields)
			return "reach(f*)";
		if (spec.matchFirstField)
			return "reach(first_f)";
		if (spec.matchLastField)
			return "reach(last_f)";
		return "reach";
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
	}

	@Override
	public void nodeDeleted(int o) {
	}

	@Override
	public void ensureComputed() {
		o2pats.clear();
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				o2pats.put(o, new ArrayList<String>());
				return true;
			}
		});

		// For each node, compute reachability from sources
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				String source = "H" + state.o2h.get(o);
				// X.logs("--- source=%s, a=%s", source, astr(a));
				search(source, -1, -1, 0, o);
				return true;
			}
		});

		// Compute the values
		state.o2h.forEachKey(new TIntProcedure() {
			public boolean execute(int o) {
				setValue(o, computeValue(o));
				return true;
			}
		});
	}

	// Source: variable or heap-allocation site.
	// General recipe for predicates is some function of the list of fields from
	// source to the node.
	// Examples:
	// Reachable-from: path must exist.
	// Pointed-to-by: length must be one.
	// Reachable-following-a-single field
	// Reachable-from-with-first-field
	// Reachable-from-with-last-field

	private void search(String source, int first_f, int last_f, int len, int o) {
		String pat = null;
		/* if (len > 0) */{ // Avoid trivial predicates
			if (spec.pointedTo) {
				if (len == 1)
					pat = source;
			} else if (spec.matchRepeatedFields) {
				assert (first_f == last_f);
				pat = source + "." + first_f + "*";
			} else if (spec.matchFirstField)
				pat = source + "." + first_f + ".*";
			else if (spec.matchLastField)
				pat = source + ".*." + last_f;
			else
				// Plain reachability
				pat = source;
		}

		List<String> pats = o2pats.get(o);

		if (pat != null && pats.indexOf(pat) != -1)
			return; // Already have it

		if (pat != null)
			pats.add(pat);
		// X.logs("source=%s first_f=%s last_f=%s len=%s a=%s: v=%s", source,
		// fstr(first_f), fstr(last_f), len, astr(a), a2ws[a]);

		if (spec.pointedTo && len >= 1)
			return;

		// Recurse
		List<Edge> edges = state.o2edges.get(o);
		if (edges == null) {
			X.errors("o=%s returned no edges (shouldn't happen!)", o);
		} else {
			for (Edge e : edges) {
				if (spec.matchRepeatedFields && first_f != -1 && first_f != e.f)
					continue; // Must have same field
				search(source, len == 0 ? e.f : first_f, e.f, len + 1, e.o);
			}
		}
	}

	private Object computeValue(int o) {
		List<String> pats = o2pats.get(o);
		Collections.sort(pats); // Canonicalize
		return pats.toString();
	}
}

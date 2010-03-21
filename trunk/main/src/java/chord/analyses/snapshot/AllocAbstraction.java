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

public class AllocAbstraction extends LocalAbstraction {
	int kCFA, kOS;

	public AllocAbstraction(int kCFA, int kOS) {
		this.kCFA = kCFA;
		this.kOS = kOS;
	}

	@Override
	public String toString() {
		if (kCFA == 0 && kOS == 0) return "alloc";
		//return String.format("alloc(kCFA=%d,kOS=%d)", kCFA, kOS);
		return String.format("alloc(k=%d)", kCFA);
	}

	@Override
	public void nodeCreated(ThreadInfo info, int o) {
		setValue(o, computeValue(info, o));
	}

	@Override
	public void nodeDeleted(int o) {
		removeValue(o);
	}

	@Override
	public void ensureComputed() {
	}

	public Object computeValue(ThreadInfo info, int o) {
		if (kCFA == 0 && kOS == 0)
			return state.o2h.get(o); // No context

		StringBuilder buf = new StringBuilder();
		buf.append(state.o2h.get(o));

		if (kCFA > 0) {
			for (int i = 0; i < kCFA; i++) {
				int j = info.callSites.size() - i - 1;
				if (j < 0)
					break;
				buf.append('_');
				buf.append(info.callSites.get(j));
			}
		}

		return buf.toString();
	}
}

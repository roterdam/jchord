/**
 * 
 */
package chord.project.analyses;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntIntProcedure;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import chord.instr.InstrScheme;

/**
 * @author omert
 *
 */
public abstract class LabelBasedAnalysis extends DynamicAnalysis {

	protected static interface Label {
	}
	
	private class Procedure implements TIntIntProcedure {
		private final TIntHashSet worklist;
		private final TIntHashSet visited;
		private final Set<Label> labels;
		private final boolean isPos;

		public Procedure(TIntHashSet worklist, TIntHashSet visited, Set<Label> labels, boolean isPos) {
			this.worklist = worklist;
			this.visited = visited;
			this.labels = labels;
			this.isPos = isPos;
		}
		
		@Override
		public boolean execute(int arg0, int arg1) {
			if (arg1 != 0) {
				for (Label label : labels) {
					if (isPos) 
						posLabel(arg1, label);
					else 
						negLabel(arg1, label);
					if (!visited.contains(arg1)) {
						worklist.add(arg1);
					}
				}
			}
			return true;
		}
	}
	
//	private final static int ARRAY_CONTENT = Integer.MIN_VALUE;
	
	private InstrScheme instrScheme;
	private final TIntObjectHashMap<TIntIntHashMap> heapGraph = new TIntObjectHashMap<TIntIntHashMap>();
	protected final TIntObjectHashMap<Set<Label>> object2labels = new TIntObjectHashMap<Set<Label>>();

	protected abstract TIntHashSet getRoots(Label l);
	
	private Set<Label> getLabels(int b) {
		return object2labels.get(b);
	}
	
	private void posLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		if (S == null) {
			object2labels.put(o, S = new HashSet<Label>(1));
		}
		S.add(l);
	}
	
	private void negLabel(int o, Label l) {
		Set<Label> S = object2labels.get(o);
		if (S != null) {
			S.remove(l);
		}
	}
	
	@Override
	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setPutfieldReferenceEvent(false, false, true, true, true);
		instrScheme.setAstoreReferenceEvent(false, false, true, true, true);
		return instrScheme;
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		if (b != 0) {
			updateHeapGraph(b, i, o);
		}
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		if (b != 0 && f >= 0) {
			updateHeapGraph(b, f, o);
		}
	}
	
	private void updateHeapGraph(int b, int f, int o) {
		TIntIntHashMap M = heapGraph.get(b);
		if (M == null) {
			heapGraph.put(b, M = new TIntIntHashMap());
		}
		M.put(f, o);
		Set<Label> labels = getLabels(b);
		if (labels != null) {
			if (o == 0) {
				propagateLabels(o, labels, false);
				for (Label l : labels) {
					TIntHashSet roots = getRoots(l);
					for (TIntIterator it=roots.iterator(); it.hasNext(); ) {
						propagateLabels(it.next(), Collections.<Label> singleton(l), true);
					}
				}
			} else {
				propagateLabels(o, labels, true);
			}
		}
	}
	
	private void propagateLabels(int o, Set<Label> labels, boolean isPos) {
		TIntHashSet worklist = new TIntHashSet();
		TIntHashSet visited = new TIntHashSet();
		for (Label l : labels) {
			if (isPos)
				posLabel(o, l);
			else
				negLabel(o, l);
		}
		worklist.add(o);
		while (!worklist.isEmpty()) {
			TIntIterator it = worklist.iterator();
			worklist = new TIntHashSet();
			Procedure proc = new Procedure(worklist, visited, labels, isPos);
			while (it.hasNext()) {
				final int next = it.next();
				visited.add(next);
				TIntIntHashMap M = heapGraph.get(next);
				if (M != null) {
					M.forEachEntry(proc);
				}
			}
		}
	}
}

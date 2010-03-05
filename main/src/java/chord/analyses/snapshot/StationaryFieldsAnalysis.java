/**
 * 
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.util.HashMap;
import java.util.Map;

import chord.project.Chord;

/**
 * @author omert
 * 
 */
@Chord(name = "ss-stat-fld")
public class StationaryFieldsAnalysis extends SnapshotAnalysis {
	
	private static class StationaryFieldQuery extends Query {
		public final int f;

		public StationaryFieldQuery(int f) {
			this.f = f;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + f;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StationaryFieldQuery other = (StationaryFieldQuery) obj;
			if (f != other.f)
				return false;
			return true;
		}
	}

	private final TIntHashSet stationaryFields = new TIntHashSet();
	private final TIntHashSet accessedFields = new TIntHashSet();
	private final Map<Object, TIntHashSet> obj2readFields = new HashMap<Object, TIntHashSet>(16);

	@Override
	public String propertyName() {
		return "stationary-fields";
	}

	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		onFieldWrite(e, abstraction.getValue(b), f);
	}

	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		onFieldWrite(e, abstraction.getValue(b), f);
	}

	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		onFieldRead(e, abstraction.getValue(b), f);
	}

	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		onFieldRead(e, abstraction.getValue(b), f);
	}
	
	@Override
	public void initPass() {
		super.initPass();
		accessedFields.clear();
	}

	@Override
	public void donePass() {
		super.donePass();
		abstraction.ensureComputed();
		TIntIterator it = accessedFields.iterator();
		while (it.hasNext()) {
			StationaryFieldQuery q = new StationaryFieldQuery(it.next());
			if (shouldAnswerQueryHit(q)) {
				answerQuery(q, stationaryFields.contains(q.f));
			}
		}
	}
	
	private void onFieldRead(int e, Object b, int f) {
		if (e >= 0 && b != null && f != 0) {
			if (!accessedFields.contains(f)) {
				accessedFields.add(f);
				stationaryFields.add(f);
			}
			TIntHashSet S = obj2readFields.get(b);
			if (S == null) {
				S = new TIntHashSet();
				obj2readFields.put(b, S);
			}
			S.add(f);
		}
	}

	private void onFieldWrite(int e, Object b, int f) {
		if (e >= 0 && b != null && f != 0) {
			if (!accessedFields.contains(f)) {
				accessedFields.add(f);
				stationaryFields.add(f);
			}
			if (obj2readFields.containsKey(b)) {
				TIntHashSet S = obj2readFields.get(b);
				if (S != null) {
					stationaryFields.remove(f);
				}
			}
		}
	}

	@Override
	public SnapshotResult takeSnapshot() {
		return null;
	}
}

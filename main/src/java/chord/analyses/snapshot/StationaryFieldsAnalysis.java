/**
 * 
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chord.project.Chord;

/**
 * @author omert
 * @author pliang
 * 
 */
@Chord(name = "ss-stationary-fields")
public class StationaryFieldsAnalysis extends SnapshotAnalysis {
	private class StationaryFieldQuery extends Query {
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
    @Override public String toString() { return fstr(f); }
	}

	private final TIntHashSet stationaryFields = new TIntHashSet();
	private final TIntHashSet accessedFields = new TIntHashSet();
	private final Map<Object, TIntHashSet> obj2readFields = new HashMap<Object, TIntHashSet>(16);

	@Override
	public String propertyName() {
		return "stationary-fields";
	}

	@Override
	public void onProcessPutfieldPrimitive(int e, int t, int b, int f) {
    onFieldWrite(e, abstraction.getValue(b), f);
	}

	@Override
	public void onProcessPutfieldReference(int e, int t, int b, int f, int o) {
    onFieldWrite(e, abstraction.getValue(b), f);
	}

	@Override
	public void onProcessGetfieldPrimitive(int e, int t, int b, int f) {
    onFieldRead(e, abstraction.getValue(b), f);
	}

	@Override
	public void onProcessGetfieldReference(int e, int t, int b, int f, int o) {
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
		TIntIterator it = accessedFields.iterator();
		while (it.hasNext()) {
			StationaryFieldQuery q = new StationaryFieldQuery(it.next());
			if (!fieldIsExcluded(q.f))
				answerQuery(q, !stationaryFields.contains(q.f)); // True iff non-stationary
		}
	}
	
	private void onFieldRead(int e, Object b, int f) {
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

	private void onFieldWrite(int e, Object b, int f) {
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

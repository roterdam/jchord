/**
 * 
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import chord.instr.InstrScheme;
import chord.project.Chord;

/**
 * @author omert
 *
 */
@Chord(name="ss-shared-lock-access")
public class SharedLockAccessAnalysis extends SnapshotAnalysis {
	private final Map<Object, TIntHashSet> abstraction2threads = new HashMap<Object, TIntHashSet>();
	private final TIntHashSet visitedStatements = new TIntHashSet();
	private final TIntHashSet sharedLockAcquisitionStatements = new TIntHashSet();

	@Override
	public String propertyName() {
		return "shared-lock-access";
	}

  private class SharedLockAccessQuery extends Query {
    public final int l;
    public SharedLockAccessQuery(int l) { this.l = l; }
    @Override public int hashCode() { return l; }
    @Override public boolean equals(Object that) { return this.l == ((SharedLockAccessQuery)that).l; }
    @Override public String toString() { return lstr(l); }
  }
	
	@Override
	public void donePass() {
		super.donePass();
		for (TIntIterator it = visitedStatements.iterator(); it.hasNext(); ) {
			int l = it.next();
			SharedLockAccessQuery q = new SharedLockAccessQuery(l);
			if (!lockIsExcluded(l)) {
				/* 
				 * Give a positive answer iff the lock used by the lock-acquisition statement has been accessed by
				 * more than one thread. 
				 */
				answerQuery(q, sharedLockAcquisitionStatements.contains(l));
			}
		}
		abstraction2threads.clear();
		sharedLockAcquisitionStatements.clear();
		visitedStatements.clear();
	}

	@Override
	public void onProcessAcquireLock(int l, int t, int o) {
    if (l < 0) return;

		Object abs = abstraction.getValue(o);
		TIntHashSet S = abstraction2threads.get(abs);
		if (S == null) {
			S = new TIntHashSet();
			abstraction2threads.put(abs, S);
		}
		S.add(t);
		if (S.size() > 1) {
			sharedLockAcquisitionStatements.add(l);
		}
		visitedStatements.add(l);
	}
}

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
@Chord(name="ss-lock-access")
public class SharedLockAccessAnalysis extends SnapshotAnalysis {

	private class SharedLockAccessQuery extends Query {
		public final int e;
		public SharedLockAccessQuery(int e) {
			this.e = e;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + e;
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
			SharedLockAccessQuery other = (SharedLockAccessQuery) obj;
			if (e != other.e)
				return false;
			return true;
		}
    @Override public String toString() { return estr(e); }
	}
	
	private static class Event {
		public final int l;
		public final int t;
		public final int o;
		
		public Event(int l, int t, int o) {
			this.l = l;
			this.t = t;
			this.o = o;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + l;
			result = prime * result + o;
			result = prime * result + t;
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
			Event other = (Event) obj;
			if (l != other.l)
				return false;
			if (o != other.o)
				return false;
			if (t != other.t)
				return false;
			return true;
		}
	}
	
	private final Map<Object, TIntHashSet> abstraction2threads = new HashMap<Object, TIntHashSet>();
	private final List<Event> events = new LinkedList<Event>();
	private final TIntHashSet visitedStatements = new TIntHashSet();
	private final TIntHashSet sharedLockAcquisitionStatements = new TIntHashSet();

	@Override
	public String propertyName() {
		return "shared-lock-access";
	}
	
	@Override
	public void donePass() {
		super.donePass();
		for (TIntIterator it = visitedStatements.iterator(); it.hasNext(); ) {
			int e = it.next();
			SharedLockAccessQuery q = new SharedLockAccessQuery(e);
			if (shouldAnswerQueryHit(q)) {
				/* 
				 * Give a positive answer iff the lock used by the lock-acquisition statement has been accessed by
				 * more than one thread. 
				 */
				answerQuery(q, sharedLockAcquisitionStatements.contains(e));
			}
		}
		abstraction2threads.clear();
		events.clear();
		sharedLockAcquisitionStatements.clear();
		visitedStatements.clear();
	}

	@Override
	public SnapshotResult takeSnapshot() {
		if (queryOnlyAtSnapshot) {
			abstraction.ensureComputed();
			for (Event e : events) {
				process(e.l, e.t, e.o);
			}
			events.clear();
		}
		return null;
	}
	
	@Override
	protected boolean decideIfSelected() {
		return true;
	}
	
	@Override
	protected InstrScheme getBaselineScheme() {
		InstrScheme instrScheme = new InstrScheme();
		instrScheme.setAcquireLockEvent(true, true, true);
		return instrScheme;
	}

	@Override
	public void onProcessAcquireLock(int l, int t, int o) {
		super.onProcessAcquireLock(l, t, o);
		if (queryOnlyAtSnapshot) {
			events.add(new Event(l, t, o));
		} else {
			abstraction.ensureComputed();
			process(l, t, o);
		}
	}

	private void process(int l, int t, int o) {
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

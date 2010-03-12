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

import chord.project.Chord;

/**
 * @author omert
 *
 */
@Chord(name="ss-thr-access")
public class ThreadAccessAnalysis extends SnapshotAnalysis {

	private static class ThreadAccessQuery extends Query {
		public final int e;
		
		public ThreadAccessQuery(int e) {
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
			ThreadAccessQuery other = (ThreadAccessQuery) obj;
			if (e != other.e)
				return false;
			return true;
		}
	}
	
	private static class Event {
		public final int t;
		public final int b;
		public final int e;

		public Event(int t, int b, int e) {
			this.t = t;
			this.b = b;
			this.e = e;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + b;
			result = prime * result + e;
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
			if (b != other.b)
				return false;
			if (e != other.e)
				return false;
			if (t != other.t)
				return false;
			return true;
		}
	}

	private final List<Event> events = new LinkedList<Event>();
	private final Map<Object, TIntHashSet> abs2threads = new HashMap<Object, TIntHashSet>();
	private final TIntHashSet visitedLocations = new TIntHashSet();
	private final TIntHashSet sharedAccessingLocations = new TIntHashSet();
	
	@Override
	public String propertyName() {
		return "thread-access";
	}
	
	@Override
	public void initPass() {
		super.initPass();
		abs2threads.clear();
		visitedLocations.clear();
		sharedAccessingLocations.clear();
		events.clear();
	}
	
	@Override
	public void donePass() {
		super.donePass();
		for (TIntIterator it = visitedLocations.iterator(); it.hasNext(); ) {
			ThreadAccessQuery q = new ThreadAccessQuery(it.next());
			if (shouldAnswerQueryHit(q)) {
				// The query is answered positively iff the statement did access an object touched my more than one thread.
				answerQuery(q, sharedAccessingLocations.contains(q.e));
			}
		}
	}

	@Override
	protected boolean decideIfSelected() {
		return true;
	}
	
	@Override
	public SnapshotResult takeSnapshot() {
		if (queryOnlyAtSnapshot) {
			abstraction.ensureComputed();
			for (Event evt : events) {
				registerThreadAccess(evt.t, evt.b, evt.e);
			}
			events.clear();
		}
		return null;
	}

//	@Override
//	public void processNewOrNewArray(int h, int t, int o) {
//		super.processNewOrNewArray(h, t, o);
//		if (o != 0) {
//			if (queryOnlyAtSnapshot) {
//				Event event = new Event(t, o, h);
//				events.add(event);
//			} else {
//				abstraction.ensureComputed();
//				registerThreadAccess(t, o, h);
//			}
//		}
//	}
	
	@Override
	public void fieldAccessed(int e, int t, int b, int f, int o) {
		super.fieldAccessed(e, t, b, f, o);
		if (e >= 0 && b != 0) {
			if (queryOnlyAtSnapshot) {
				Event event = new Event(t, b, e);
				events.add(event);
			} else {
				abstraction.ensureComputed();
				registerThreadAccess(t, b, e);
			}
		}
	}

	private void registerThreadAccess(int t, int b, int e) {
		Object abs = abstraction.getValue(b);
		TIntHashSet S = abs2threads.get(abs);
		if (S == null) {
			S = new TIntHashSet();
			abs2threads.put(abs, S);
		}
		S.add(t);
		if (S.size() > 1) {
			sharedAccessingLocations.add(e);
		}
		visitedLocations.add(e);
	}
}

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
@Chord(name="ss-thread-access")
public class ThreadAccessAnalysis extends SnapshotAnalysis {
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
	}
	
	@Override
	public void donePass() {
		super.donePass();
		for (TIntIterator it = visitedLocations.iterator(); it.hasNext(); ) {
			ProgramPointQuery q = new ProgramPointQuery(it.next());
			if (!statementIsExcluded(q.e)) {
				// The query is answered positively iff the statement did access an object touched my more than one thread.
				answerQuery(q, sharedAccessingLocations.contains(q.e));
			}
		}
	}

	@Override
	public void fieldAccessed(int e, int t, int b, int f, int o) {
		super.fieldAccessed(e, t, b, f, o);
    registerThreadAccess(t, b, e);
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

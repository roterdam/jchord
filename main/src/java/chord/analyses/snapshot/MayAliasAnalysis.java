/**
 * 
 */
package chord.analyses.snapshot;

import gnu.trove.TIntObjectHashMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import chord.bddbddb.Rel.IntPairIterable;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;

/**
 * @author omert
 *
 */
@Chord(
	name = "ss-may-alias",
	consumedNames = { "startingRacePair" }
)
public class MayAliasAnalysis extends SnapshotAnalysis {
	
	private static class MayAliasQuery extends Query {
		public final int e1;
		public final int e2;

		public MayAliasQuery(int e1, int e2) {
			this.e1 = e1;
			this.e2 = e2;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + e1;
			result = prime * result + e2;
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
			MayAliasQuery other = (MayAliasQuery) obj;
			if (e1 != other.e1)
				return false;
			if (e2 != other.e2)
				return false;
			return true;
		}
	}

	private final TIntObjectHashMap<Set<Object>> loc2abstractions = new TIntObjectHashMap<Set<Object>>();
	protected final Set<IntPair> startingRacePairSet = new HashSet<IntPair>();
	protected final Set<IntPair> aliasingRacePairSet = new HashSet<IntPair>(); 
	
	@Override
	public String propertyName() {
		return "may-alias";
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processGetstaticPrimitive(int e, int t, int b, int f) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updatePointsTo(e, abstraction.getValue(b));
	}
	
	@Override
	public void initAllPasses() {
		super.initAllPasses();
		ProgramRel relStartingRacePair =
			(ProgramRel) Project.getTrgt("startingRacePair");
		relStartingRacePair.load();
		IntPairIterable tuples = relStartingRacePair.getAry2IntTuples();
		for (IntPair p : tuples) 
			startingRacePairSet.add(p);
		relStartingRacePair.close();
	}
	
	@Override
	public void initPass() {
		super.initPass();
		loc2abstractions.clear();
	}
	
	@Override
	public void donePass() {
		super.donePass();
		abstraction.ensureComputed();
		for (IntPair p : startingRacePairSet) {
			if (!aliasingRacePairSet.contains(p)) {
				int e1 = p.idx0;
				int e2 = p.idx1;
				if (aliases(e1, e2))
					aliasingRacePairSet.add(p);
			}
		}
		for (IntPair p : startingRacePairSet) {
			int e1 = p.idx0;
			int e2 = p.idx1;
			MayAliasQuery q = new MayAliasQuery(e1, e2);
			if (shouldAnswerQueryHit(q)) {
				answerQuery(q, aliasingRacePairSet.contains(p));
			}
		}
	}
	
	public boolean aliases(int e1, int e2) {
		final Set<Object> pts1 = loc2abstractions.get(e1);
		if (pts1 == null)
			return false;
		final Set<Object> pts2 = loc2abstractions.get(e2);
		if (pts2 == null)
			return false;
        return !(Collections.disjoint(pts1, pts2));
	}

	private void updatePointsTo(int e, Object b) {
		if (e >= 0 && b != null) {
			Set<Object> pts = loc2abstractions.get(e);
			if (pts == null) {
				pts = new HashSet<Object>();
				loc2abstractions.put(e, pts);
			}
			pts.add(b);
		}
	}

	@Override
	public SnapshotResult takeSnapshot() {
		return null;
	}
}

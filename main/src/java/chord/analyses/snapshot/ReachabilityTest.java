/**
 * 
 */
package chord.analyses.snapshot;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;

import java.util.HashSet;
import java.util.Set;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.util.IndexMap;

/**
 * @author omertripp
 *
 */
@Chord(name="dynamic-reach-test")
public class ReachabilityTest extends LabelBasedAnalysis {

	private static class AllocationSiteLabel implements Label {
		public final int h;
		
		public AllocationSiteLabel(int h) {
			this.h = h;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + h;
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
			AllocationSiteLabel other = (AllocationSiteLabel) obj;
			if (h != other.h)
				return false;
			return true;
		}
	}
	
	private final TIntObjectHashMap<TIntHashSet> alloc2objects = new TIntObjectHashMap<TIntHashSet>();
	private IndexMap<String> Emap;
	
	@Override
	public InstrScheme getInstrScheme() {
		InstrScheme result = super.getInstrScheme();
		result.setNewAndNewArrayEvent(true, false, true);
		result.setPutfieldReferenceEvent(true, false, true, true, true);
		result.setAstoreReferenceEvent(true, false, true, true, true);
		return result;
	}
	
	@Override
	public void processNewOrNewArray(int h, int t, int o) {
		if (o != 0 && h >= 0) {
			Set<Label> S = new HashSet<Label>(1);
			S.add(new AllocationSiteLabel(h));
			object2labels.put(o, S);
			TIntHashSet T = alloc2objects.get(h);
			if (T == null) {
				T = new TIntHashSet();
				alloc2objects.put(h, T);
			}
			T.add(o);
		}
	}
	
	@Override
	protected TIntHashSet getRoots(Label l) {
		assert (l instanceof AllocationSiteLabel);
		AllocationSiteLabel allocLabel = (AllocationSiteLabel) l;
		return alloc2objects.get(allocLabel.h);
	}

	@Override
	public void initAllPasses() {
		super.initAllPasses();
		Emap = instrumentor.getEmap();
	}
	
	@Override
	public void donePass() {
		super.donePass();
		object2labels.forEachEntry(new TIntObjectProcedure<Set<Label>>() {
			// @Override
			public boolean execute(int arg0, Set<Label> arg1) {
				System.out.println("Object is: " + arg0);
				System.out.println("Labels are: ");
				if (arg1 != null) {
					for (Label l : arg1) {
						assert (l instanceof AllocationSiteLabel);
						AllocationSiteLabel allocLabel = (AllocationSiteLabel) l;
						String s = Emap.get(allocLabel.h);
						if (s == null) {
							System.out.println("\t" + allocLabel.h);
						} else {
							System.out.println("\t" + s);
						}
					}
				}
				return true;
			}
		});
	}
}

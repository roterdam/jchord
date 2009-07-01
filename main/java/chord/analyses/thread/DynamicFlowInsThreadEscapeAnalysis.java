package chord.analyses.thread;

import java.util.List;
import java.util.ArrayList;

import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.util.Assertions;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;

@Chord(
	name = "dynamic-flowins-thresc-java",
    producedNames = { "escE" },
    namesOfSigns = { "escE" },
    signs = { "E0:E0" }
)
public class DynamicFlowInsThreadEscapeAnalysis extends DynamicFlowSenThreadEscapeAnalysis {
    // map from the index in domain H of each alloc site not yet known
	// to be flow-ins. thread-escaping to the list of indices in
	// domain E of instance field/array deref sites that should become
    // flow-ins. thread-escaping if this alloc site becomes flow-ins.
	// thread-escaping
    // invariant: isHidxEsc[h] = true => HidxToPendingEidxs[h] == null
    private TIntArrayList[] HidxToPendingEidxs;
    // isHidxEsc[h] == true iff alloc site having index h in domain H
	// is flow-ins. thread-escaping
	private boolean[] isHidxEsc;
	private TIntIntHashMap objToHidx;

	private int numH;

	public void init() {
		if (isFirst) {
			numH = Hmap.size();
			HidxToPendingEidxs = new TIntArrayList[numH];
			isHidxEsc = new boolean[numH];
		} else {
			for (int i = 0; i < numH; i++)
				HidxToPendingEidxs[i] = null;
			for (int i = 0; i < numH; i++)
				isHidxEsc[i] = false;
		}
		objToHidx = new TIntIntHashMap();
		super.init();
	}
	public void processObjValAsgnInst(int hIdx, int o) {
		if (o != 0) {
			super.processObjValAsgnInst(hIdx, o);
			objToHidx.remove(o);
			if (hIdx != -1) {
				objToHidx.put(o, hIdx);
			}
		}
	}
	public void processHeapRd(int eIdx, int b) {
		if (eIdx != -1 && b != 0) {
			isEidxVisited[eIdx] = true;
			if (!isEidxEsc[eIdx]) {
				if (objToHidx.containsKey(b)) {
					int hIdx = objToHidx.get(b);
					if (isHidxEsc[hIdx]) {
						isEidxEsc[eIdx] = true;
					} else {
						TIntArrayList l = HidxToPendingEidxs[hIdx];
						if (l == null) {
							l = new TIntArrayList();
							HidxToPendingEidxs[hIdx] = l;
							l.add(eIdx);
						} else if (!l.contains(eIdx)) {
							l.add(eIdx);
						}
					}
				}
			}
		}
	}
	public void markHesc(int hIdx) {
		if (!isHidxEsc[hIdx]) {
			isHidxEsc[hIdx] = true;
			TIntArrayList l = HidxToPendingEidxs[hIdx];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int eIdx = l.get(i);
					isEidxEsc[eIdx] = true;
				}
				HidxToPendingEidxs[hIdx] = null;
			}
		}
	}
    public void markAndPropEsc(int o) {
        if (escObjs.add(o)) {
			if (objToHidx.containsKey(o)) {
				int hIdx = objToHidx.get(o);
				markHesc(hIdx);
			}
			List l = (List) objToFldObjs.get(o);
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					FldObj fo = (FldObj) l.get(i);
					markAndPropEsc(fo.o);
				}
			}
		}
	}
}

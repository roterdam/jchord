package chord.analyses.thread;

import java.util.List;
import java.util.ArrayList;

import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.util.Assertions;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

@Chord(
	name = "dyn-thresc-compare-java"
)
public class DynamicThreadEscapeAnalysisComparator extends DynamicAnalysis {
	// map from each object to a list containing each non-null-valued
	// instance field of reference type along with that value
	private TIntObjectHashMap objToFldObjs;
    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToHidx; 
    // set of all currently escaping objects
    private TIntHashSet escObjs;

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
    // isEidxFlowSenEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-sen. thread-escaping
	private boolean[] isEidxFlowSenEsc;
    // isEidxFlowInsEsc[e] == true iff instance field/array deref
	// site having index e in domain E is flow-ins. thread-escaping
	private boolean[] isEidxFlowInsEsc;
	// isEidxVisited[e] == true iff instance field/array deref site
	// having index e in domain E is visited during the execution
	private boolean[] isEidxVisited;

	public boolean handlesObjValAsgnInst() { return true; }
	public boolean handlesInstFldRdInst() { return true; }
	public boolean handlesInstFldWrInst() { return true; }
	public boolean handlesAryElemRdInst() { return true; }
	public boolean handlesAryElemWrInst() { return true; }
	public boolean handlesStatFldWrInst() { return true; }
	public boolean handlesForkHeadInst() { return true; }

	private boolean isFirst = true;
	private int numE;
	private int numH;

	public void init() {
		if (isFirst) {
			isFirst = false;
 			escObjs = new TIntHashSet();
			numE = Emap.size();
			numH = Hmap.size();
			isEidxFlowSenEsc = new boolean[numE];
			isEidxFlowInsEsc = new boolean[numE];
			isEidxVisited = new boolean[numE];
			HidxToPendingEidxs = new TIntArrayList[numH];
			isHidxEsc = new boolean[numH];
		} else {
			printStats();
			escObjs.clear();
			for (int i = 0; i < numH; i++)
				HidxToPendingEidxs[i] = null;
			for (int i = 0; i < numH; i++)
				isHidxEsc[i] = false;
		}
		objToFldObjs = new TIntObjectHashMap();
		objToHidx = new TIntIntHashMap();
	}
	public void done() {
		printStats();
		System.out.println("VISITED");
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i])
				System.out.println(Emap.get(i));
		}
		System.out.println("FLOWSEN");
		for (int i = 0; i < numE; i++) {
			if (isEidxFlowSenEsc[i])
				System.out.println(Emap.get(i));
		}
		System.out.println("FLOWINS");
		for (int i = 0; i < numE; i++) {
			if (isEidxFlowInsEsc[i])
				System.out.println(Emap.get(i));
		}
	}

	private void printStats() {
		System.out.println("***** STATS *****");
		int numVisited = 0, numFlowInsEsc = 0, numFlowSenEsc = 0, numAllocEsc = 0;
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i])
				numVisited++;
			if (isEidxFlowSenEsc[i]) {
				numFlowSenEsc++;
				isEidxFlowInsEsc[i] = true;
			}
			if (isEidxFlowInsEsc[i])
				numFlowInsEsc++;
		}
		for (int i = 0; i < numH; i++) {
			if (isHidxEsc[i])
				numAllocEsc++;
		}
		System.out.println("numVisited: " + numVisited +
			" numFlowSenEsc: " + numFlowSenEsc +
			" numFlowInsEsc: " + numFlowInsEsc +
			" numAllocEsc: " + numAllocEsc);
	}

	public void processObjValAsgnInst(int hIdx, int o) {
		if (o != 0) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
			objToHidx.remove(o);
			if (hIdx != -1)
				objToHidx.put(o, hIdx);
		}
	}
	public void processInstFldRdInst(int eIdx, int b, int fIdx) { 
		processHeapRd(eIdx, b);
	}
	public void processInstFldWrInst(int eIdx, int b, int fIdx, int r) {
		processHeapWr(eIdx, b, fIdx, r);
	}
	public void processStatFldWrInst(int fIdx, int r) { 
		if (r != 0) {
			markAndPropEsc(r);
		}
	}
	public void processAryElemRdInst(int eIdx, int b, int idx) { 
		processHeapRd(eIdx, b);
	}
	public void processAryElemWrInst(int eIdx, int b, int idx, int r) {
		processHeapWr(eIdx, b, idx, r);
	}
	public void processForkHeadInst(int o) { 
		if (o != 0) {
			markAndPropEsc(o);
		}
	}
	private void processHeapRd(int eIdx, int b) {
		if (eIdx != -1 && b != 0) {
			isEidxVisited[eIdx] = true;
			if (!isEidxFlowSenEsc[eIdx] && escObjs.contains(b)) {
				isEidxFlowSenEsc[eIdx] = true;
			}
			if (!isEidxFlowInsEsc[eIdx]) {
				if (objToHidx.containsKey(b)) {
					int hIdx = objToHidx.get(b);
					if (isHidxEsc[hIdx]) {
						isEidxFlowInsEsc[eIdx] = true;
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
	private void processHeapWr(int eIdx, int b, int fIdx, int r) {
		processHeapRd(eIdx, b);
		if (b != 0 && fIdx != -1) {
			if (r == 0) {
				// remove field fIdx if it is there
				List l = (List) objToFldObjs.get(b);
				if (l != null) {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = (FldObj) l.get(i);
						if (fo.f == fIdx) {
							l.remove(i);
							return;
						}
					}
				}
			} else {
				List l = (List) objToFldObjs.get(b);
				if (l == null) {
					l = new ArrayList();
					objToFldObjs.put(b, l);
				} else {
					int n = l.size();
					for (int i = 0; i < n; i++) {
						FldObj fo = (FldObj) l.get(i);
						if (fo.f == fIdx) {
							fo.o = r;
							return;
						}
					}
				}
				l.add(new FldObj(fIdx, r));
				if (escObjs.contains(b))
					markAndPropEsc(r);
			}
		}
	}
	private void markHesc(int hIdx) {
		if (!isHidxEsc[hIdx]) {
			isHidxEsc[hIdx] = true;
			TIntArrayList l = HidxToPendingEidxs[hIdx];
			if (l != null) {
				int n = l.size();
				for (int i = 0; i < n; i++) {
					int eIdx = l.get(i);
					isEidxFlowInsEsc[eIdx] = true;
				}
				HidxToPendingEidxs[hIdx] = null;
			}
		}
	}
    private void markAndPropEsc(int o) {
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

package chord.analyses.thread;

import java.util.List;
import java.util.ArrayList;

import chord.project.Chord;
import chord.project.ProgramRel;
import chord.project.DynamicAnalysis;
import chord.project.Project;
import chord.util.Assertions;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;

@Chord(
    name = "flowsen-dyn-thresc-java",
    producedNames = { "escE", "visitedE" },
    namesOfSigns = { "escE", "visitedE" },
    signs = { "E0:E0", "E0:E0" }
)
public class FlowSenDynamicThreadEscapeAnalysis extends DynamicAnalysis {
	// map from each object to a list containing each non-null-valued
	// instance field of reference type along with that value
	protected TIntObjectHashMap/*<List>*/ objToFldObjs;
    // set of all currently escaping objects
    protected TIntHashSet escObjs;

    // isEidxEsc[e] == true iff instance field/array deref
	// site having index e in domain E is deemed thread-escaping
	protected boolean[] isEidxEsc;
	// isEidxVisited[e] == true iff instance field/array deref
	// site having index e in domain E is visited at least once
	protected boolean[] isEidxVisited;

    public boolean handlesObjValAsgnInst() { return true; }
	public boolean handlesInstFldRdInst() { return true; }
	public boolean handlesInstFldWrInst() { return true; }
	public boolean handlesAryElemRdInst() { return true; }
	public boolean handlesAryElemWrInst() { return true; }
	public boolean handlesStatFldWrInst() { return true; }
	public boolean handlesForkHeadInst() { return true; }

	protected boolean isFirst = true;
	protected int numE;

	protected ProgramRel relEscE;
	protected ProgramRel relVisitedE;

	public void init() {
		if (isFirst) {
			isFirst = false;
 			escObjs = new TIntHashSet();
			numE = Emap.size();
			isEidxEsc = new boolean[numE];
			isEidxVisited = new boolean[numE];
			relEscE = (ProgramRel) Project.getTrgt("escE");
			relVisitedE = (ProgramRel) Project.getTrgt("visitedE");
		} else {
			escObjs.clear();
			printNumEsc();
		}
		objToFldObjs = new TIntObjectHashMap();
	}
	public void done() {
		if (convert) {
			relEscE.zero();
			relVisitedE.zero();
			for (int i = 0; i < numE; i++) {
				if (isEidxVisited[i])
					relVisitedE.add(i);
				if (isEidxEsc[i])
					relEscE.add(i);
			}
			relEscE.save();
			relVisitedE.save();
		}
		printNumEsc();
	}
	private void printNumEsc() {
		int visited = 0, esc = 0;
		for (int i = 0; i < numE; i++) {
			if (isEidxVisited[i])
				visited++;
			if (isEidxEsc[i])
				esc++;
		}
		System.out.println("NUM VISITED: " + visited);
		System.out.println("NUM ESC: " + esc);
	}

    public void processObjValAsgnInst(int hIdx, int o) {
		if (o != 0) {
			objToFldObjs.remove(o);
			escObjs.remove(o);
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
    public void processHeapWr(int eIdx, int b, int fIdx, int r) {
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
    public void processHeapRd(int eIdx, int b) {
        if (eIdx != -1 && b != 0) {
			isEidxVisited[eIdx] = true;
            if (!isEidxEsc[eIdx] && escObjs.contains(b)) {
                isEidxEsc[eIdx] = true;
            }
        }
    }
    public void markAndPropEsc(int o) {
        if (escObjs.add(o)) {
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

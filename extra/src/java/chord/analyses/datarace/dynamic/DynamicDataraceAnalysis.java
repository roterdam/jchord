/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.datarace.dynamic;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

import chord.instr.InstrScheme;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.CSObj;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alias.DomC;
import chord.analyses.alias.DomO;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.util.Execution;
import chord.analyses.thread.DomA;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomL;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.Config;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.ArraySet;
import chord.util.SetUtils;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Dynamic datarace analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="dynamic-datarace-java"
)
public class DynamicDataraceAnalysis extends DynamicAnalysis {
    // set of IDs of currently escaping concrete/abstract objects
    private TIntHashSet escObjs;

    // map from each object to a list of each non-null instance field
    // of reference type along with its value
    private TIntObjectHashMap<List<FldObj>> objToFldObjs;

    // map from each object to the index in domain H of its alloc site
    private TIntIntHashMap objToHidx;

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

    // isEidxVisited[e] == true iff instance field/array deref site
    // having index e in domain E is visited during the execution
    private boolean[] isEidxVisited;

    // isEidxEsc[e] == true iff:
    // 1. kind is flowSen and instance field/array deref site having
    //    index e in domain E is flow-sen. thread-escaping
    // 2. kind is flowIns and instance field/array deref site having
    //    index e in domain E is flow-ins. thread-escaping
    private boolean[] isEidxEsc;

    private int numE;
    private int numH;
    private boolean isFlowIns;

	private ProgramRel relVisitedE;
	private ProgramRel relEscE;

    private InstrScheme instrScheme;

	private DomM domM;
	private DomI domI;
	private DomF domF;
	private DomE domE;
	private DomA domA;
	private DomH domH;
	private DomC domC;
	private DomL domL;
    private CSAliasAnalysis hybridAnalysis;
    private ThrSenAbbrCSCGAnalysis thrSenAbbrCSCGAnalysis;

	private void init() {
        domM = (DomM) ClassicProject.g().getTrgt("M");
        domI = (DomI) ClassicProject.g().getTrgt("I");
        domF = (DomF) ClassicProject.g().getTrgt("F");
        domE = (DomE) ClassicProject.g().getTrgt("E");
        domA = (DomA) ClassicProject.g().getTrgt("A");
        domH = (DomH) ClassicProject.g().getTrgt("H");
        domC = (DomC) ClassicProject.g().getTrgt("C");
        domL = (DomL) ClassicProject.g().getTrgt("L");
		hybridAnalysis = (CSAliasAnalysis) ClassicProject.g().getTrgt("cs-alias-java");
        thrSenAbbrCSCGAnalysis = (ThrSenAbbrCSCGAnalysis)
            ClassicProject.g().getTrgt("thrsen-abbr-cscg-java");
	}

    @Override
    public InstrScheme getInstrScheme() {
        if (instrScheme != null)
            return instrScheme;
        instrScheme = new InstrScheme();
        instrScheme.setNewAndNewArrayEvent(true, false, true);
        instrScheme.setPutstaticReferenceEvent(false, false, false, false, true);
        instrScheme.setThreadStartEvent(false, false, true);

        instrScheme.setGetfieldPrimitiveEvent(true, false, true, false);
        instrScheme.setPutfieldPrimitiveEvent(true, false, true, false);
        instrScheme.setAloadPrimitiveEvent(true, false, true, false);
        instrScheme.setAstorePrimitiveEvent(true, false, true, false);

        instrScheme.setGetfieldReferenceEvent(true, false, true, false, false);
        instrScheme.setPutfieldReferenceEvent(true, false, true, true, true);
        instrScheme.setAloadReferenceEvent(true, false, true, false, false);
        instrScheme.setAstoreReferenceEvent(true, false, true, true, true);

        return instrScheme;
    }


    @Override
    public void initAllPasses() {
		ClassicProject.g().runTask("ctxts-java");
		ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());
		ClassicProject.g().runTask("datarace-prologue-dlog");

        isFlowIns = System.getProperty(
			"chord.escape.dynamic.flowins", "false").equals("true");
        escObjs = new TIntHashSet();
        objToFldObjs = new TIntObjectHashMap<List<FldObj>>();
        DomE domE = (DomE) ClassicProject.g().getTrgt("E");
        ClassicProject.g().runTask(domE);
        numE = domE.size();
        isEidxVisited = new boolean[numE];
        isEidxEsc = new boolean[numE];
        relVisitedE = (ProgramRel) ClassicProject.g().getTrgt("visitedE");
        relEscE = (ProgramRel) ClassicProject.g().getTrgt("escE");
        if (isFlowIns) {
            DomH domH = (DomH) ClassicProject.g().getTrgt("H");
            ClassicProject.g().runTask(domH);
            numH = domH.size();
            HidxToPendingEidxs = new TIntArrayList[numH];
            isHidxEsc = new boolean[numH];
            objToHidx = new TIntIntHashMap();
        }
    }


    @Override
    public void initPass() {
        escObjs.clear();
        objToFldObjs.clear();
        if (isFlowIns) {
            for (int i = 0; i < numH; i++) {
                HidxToPendingEidxs[i] = null;
            }
            for (int i = 0; i < numH; i++)
                isHidxEsc[i] = false;
            objToHidx.clear();
        }
    }

    @Override
    public void donePass() {
        System.out.println("***** STATS *****");
        int numAllocEsc = 0;
        int numVisitedE = 0;
        int numEscE = 0;
        for (int i = 0; i < numE; i++) {
            if (isEidxVisited[i]) {
                numVisitedE++;
                if (isEidxEsc[i])
                    numEscE++;
            }
        }
        if (isFlowIns) {
            for (int i = 0; i < numH; i++) {
                if (isHidxEsc[i])
                    numAllocEsc++;
            }
        }
        System.out.println("numAllocEsc: " + numAllocEsc);
        System.out.println("numVisitedE: " + numVisitedE +
            " numEscE: " + numEscE);
    }


    @Override
    public void doneAllPasses() {
        relVisitedE.zero();
        relEscE.zero();
        for (int i = 0; i < numE; i++) {
            if (isEidxVisited[i]) {
                relVisitedE.add(i);
                if (isEidxEsc[i])
                    relEscE.add(i);
            }
        }
        relVisitedE.save();
        relEscE.save();

        DomE domE = (DomE) ClassicProject.g().getTrgt("E");
        Program program = Program.g();
        PrintWriter writer1 =
             OutDirUtils.newPrintWriter("dynamic_visitedE.txt");
        PrintWriter writer2 =
            OutDirUtils.newPrintWriter("dynamic_escE.txt");
        for (int i = 0; i < numE; i++) {
            if (isEidxVisited[i]) {
                Quad q = domE.get(i);
                String s = q.toVerboseStr();
                writer1.println(s);
                if (isEidxEsc[i])
                    writer2.println(s);
            }
        }
        writer1.close();
        writer2.close();
    }

    public void processNewOrNewArray(int h, int t, int o) {
        if (o == 0)
            return;
        objToFldObjs.remove(o);
        escObjs.remove(o);
        if (isFlowIns) {
            if (h >= 0)
                objToHidx.put(o, h);
            else
                objToHidx.remove(o);
        }
    }

    public void processGetfieldPrimitive(int e, int t, int b, int f) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processAloadPrimitive(int e, int t, int b, int i) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processGetfieldReference(int e, int t, int b, int f, int o) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processAloadReference(int e, int t, int b, int i, int o) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processPutfieldPrimitive(int e, int t, int b, int f) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processAstorePrimitive(int e, int t, int b, int i) {
        if (e >= 0)
            processHeapRd(e, b);
    }

    public void processPutfieldReference(int e, int t, int b, int f, int o) {
        if (e >= 0)
            processHeapWr(e, b, f, o);
    }

    public void processAstoreReference(int e, int t, int b, int i, int o) {
        if (e >= 0)
            processHeapWr(e, b, i, o);
    }

    public void processPutstaticReference(int e, int t, int b, int f, int o) {
        if (o != 0)
            markAndPropEsc(o);
    }
    public void processThreadStart(int p, int t, int o) {
        if (o != 0)
            markAndPropEsc(o);
    }

    private void processHeapRd(int e, int b) {
        if (isEidxEsc[e])
            return;
        isEidxVisited[e] = true;
        if (b == 0)
            return;
        if (isFlowIns) {
            if (objToHidx.containsKey(b)) {
                int h = objToHidx.get(b);
                if (isHidxEsc[h]) {
                    isEidxEsc[e] = true;
                } else {
                    TIntArrayList list = HidxToPendingEidxs[h];
                    if (list == null) {
                        list = new TIntArrayList();
                        HidxToPendingEidxs[h] = list;
                        list.add(e);
                    } else if (!list.contains(e)) {
                        list.add(e);
                    }
                }
            }
        } else {
            if (escObjs.contains(b))
                isEidxEsc[e] = true;
        }
    }

    private void processHeapWr(int e, int b, int fIdx, int r) {
        processHeapRd(e, b);
        if (b == 0 || fIdx < 0)
            return;
        List<FldObj> l = objToFldObjs.get(b);
        if (r == 0) {
            // this is a strong update; so remove field fIdx if it is there
            if (l != null) {
                int n = l.size();
                for (int i = 0; i < n; i++) {
                    FldObj fo = l.get(i);
                    if (fo.f == fIdx) {
                        l.remove(i);
                        break;
                    }
                }
            }
            return;
        }
        boolean added = false;
        if (l == null) {
            l = new ArrayList<FldObj>();
            objToFldObjs.put(b, l);
        } else {
            for (FldObj fo : l) {
                if (fo.f == fIdx) {
                    fo.o = r;
                    added = true;
                    break;
                }
            }
        }
        if (!added)
            l.add(new FldObj(fIdx, r));
        if (escObjs.contains(b))
            markAndPropEsc(r);
    }


    private void markAndPropEsc(int o) {
        if (escObjs.add(o)) {
            List<FldObj> l = objToFldObjs.get(o);
            if (l != null) {
                for (FldObj fo : l)
                    markAndPropEsc(fo.o);
            }
            if (isFlowIns) {
                if (objToHidx.containsKey(o)) {
                    int h = objToHidx.get(o);
                    markHesc(h);
                }
            }
        }
    }

    private void markHesc(int h) {
        if (!isHidxEsc[h]) {
            isHidxEsc[h] = true;
            TIntArrayList l = HidxToPendingEidxs[h];
            if (l != null) {
                int n = l.size();
                for (int i = 0; i < n; i++) {
                    int e = l.get(i);
                    isEidxEsc[e] = true;
                }
                HidxToPendingEidxs[h] = null;
            }
        }
    }

	private void printResults() {
		ClassicProject.g().runTask(hybridAnalysis);
		ClassicProject.g().runTask(thrSenAbbrCSCGAnalysis);
	    final ICSCG thrSenAbbrCSCG = thrSenAbbrCSCGAnalysis.getCallGraph();
		ClassicProject.g().runTask("datarace-epilogue-dlog");
		final ProgramDom<Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> domTCE =
			new ProgramDom<Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>>();
		domTCE.setName("TCE");
		final DomO domO = new DomO();
		domO.setName("O");

		PrintWriter out;

		out = OutDirUtils.newPrintWriter("dataracelist.xml");
		out.println("<dataracelist>");
		final ProgramRel relDatarace = (ProgramRel) ClassicProject.g().getTrgt("datarace");
		relDatarace.load();
		final ProgramRel relRaceCEC = (ProgramRel) ClassicProject.g().getTrgt("raceCEC");
		relRaceCEC.load();
		final Iterable<Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>> tuples = relDatarace.getAry6ValTuples();
		for (Hext<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad,
				  Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tuple : tuples) {
			int tce1 = domTCE.getOrAdd(new Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>(
				tuple.val0, tuple.val1, tuple.val2));
			int tce2 = domTCE.getOrAdd(new Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad>(
				tuple.val3, tuple.val4, tuple.val5));
			RelView view = relRaceCEC.getView();
			view.selectAndDelete(0, tuple.val1);
			view.selectAndDelete(1, tuple.val2);
			view.selectAndDelete(2, tuple.val4);
			view.selectAndDelete(3, tuple.val5);
			Set<Ctxt> pts = new ArraySet<Ctxt>(view.size());
			Iterable<Ctxt> res = view.getAry1ValTuples();
			for (Ctxt ctxt : res) {
				pts.add(ctxt);
			}
			view.free();
			int p = domO.getOrAdd(new CSObj(pts));
			jq_Field fld = tuple.val2.getField();
			int f = domF.indexOf(fld);
			out.println("<datarace Oid=\"O" + p +
				"\" Fid=\"F" + f + "\" " +
				"TCE1id=\"TCE" + tce1 + "\" "  +
				"TCE2id=\"TCE" + tce2 + "\"/>");
		}
		relDatarace.close();
		relRaceCEC.close();
		out.println("</dataracelist>");
		out.close();

		ClassicProject.g().runTask("LI");
		ClassicProject.g().runTask("LE");
		ClassicProject.g().runTask("syncCLC-dlog");
		final ProgramRel relLI = (ProgramRel) ClassicProject.g().getTrgt("LI");
		final ProgramRel relLE = (ProgramRel) ClassicProject.g().getTrgt("LE");
		final ProgramRel relSyncCLC = (ProgramRel) ClassicProject.g().getTrgt("syncCLC");
		relLI.load();
		relLE.load();
		relSyncCLC.load();

		final Map<Pair<Ctxt, jq_Method>, ShortestPathBuilder> srcNodeToSPB =
			new HashMap<Pair<Ctxt, jq_Method>, ShortestPathBuilder>();

		final IPathVisitor<Pair<Ctxt, jq_Method>> visitor =
			new IPathVisitor<Pair<Ctxt, jq_Method>>() {
				public String visit(Pair<Ctxt, jq_Method> origNode,
						Pair<Ctxt, jq_Method> destNode) {
					Set<Quad> insts = thrSenAbbrCSCG.getLabels(origNode, destNode);
					jq_Method srcM = origNode.val1;
					int mIdx = domM.indexOf(srcM);
					Ctxt srcC = origNode.val0;
					int cIdx = domC.indexOf(srcC);
					String lockStr = "";
					Quad inst = insts.iterator().next();
					int iIdx = domI.indexOf(inst);
					RelView view = relLI.getView();
					view.selectAndDelete(1, iIdx);
					Iterable<Inst> locks = view.getAry1ValTuples();
					for (Inst lock : locks) {
						int lIdx = domL.indexOf(lock);
						RelView view2 = relSyncCLC.getView();
						view2.selectAndDelete(0, cIdx);
						view2.selectAndDelete(1, lIdx);
						Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
						Set<Ctxt> pts = SetUtils.newSet(view2.size());
						for (Ctxt ctxt : ctxts)
							pts.add(ctxt);
						int oIdx = domO.getOrAdd(new CSObj(pts));
						view2.free();
						lockStr += "<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
							mIdx + "\" Oid=\"O" + oIdx + "\"/>";
					}
					view.free();
					return lockStr + "<elem Cid=\"C" + cIdx + "\" " +
						"Iid=\"I" + iIdx + "\"/>";
				}
			};

		out = OutDirUtils.newPrintWriter("TCElist.xml");
		out.println("<TCElist>");
		for (Trio<Trio<Ctxt, Ctxt, jq_Method>, Ctxt, Quad> tce : domTCE) {
			Trio<Ctxt, Ctxt, jq_Method> srcOCM = tce.val0;
			Ctxt methCtxt = tce.val1;
			Quad heapInst = tce.val2;
			int cIdx = domC.indexOf(methCtxt);
			int eIdx = domE.indexOf(heapInst);
			out.println("<TCE id=\"TCE" + domTCE.indexOf(tce) + "\" " +
				"Tid=\"A" + domA.indexOf(srcOCM)    + "\" " +
				"Cid=\"C" + cIdx + "\" " +
				"Eid=\"E" + eIdx + "\">");
			jq_Method dstM = heapInst.getMethod();
			int mIdx = domM.indexOf(dstM);
			RelView view = relLE.getView();
			view.selectAndDelete(1, eIdx);
			Iterable<Inst> locks = view.getAry1ValTuples();
			for (Inst lock : locks) {
				int lIdx = domL.indexOf(lock);
				RelView view2 = relSyncCLC.getView();
				view2.selectAndDelete(0, cIdx);
				view2.selectAndDelete(1, lIdx);
				Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
				Set<Ctxt> pts = SetUtils.newSet(view2.size());
				for (Ctxt ctxt : ctxts)
					pts.add(ctxt);
				int oIdx = domO.getOrAdd(new CSObj(pts));
				view2.free();
				out.println("<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
					mIdx + "\" Oid=\"O" + oIdx + "\"/>");
			}
			view.free();
			Pair<Ctxt, jq_Method> srcCM =
				new Pair<Ctxt, jq_Method>(srcOCM.val1, srcOCM.val2);
			ShortestPathBuilder spb = srcNodeToSPB.get(srcCM);
			if (spb == null) {
				spb = new ShortestPathBuilder(thrSenAbbrCSCG, srcCM, visitor);
				srcNodeToSPB.put(srcCM, spb);
			}
			Pair<Ctxt, jq_Method> dstCM =
				new Pair<Ctxt, jq_Method>(methCtxt, dstM);
			String path = spb.getShortestPathTo(dstCM);
			out.println("<path>");
			out.println(path);
			out.println("</path>");
			out.println("</TCE>");
		}
		out.println("</TCElist>");
		out.close();

		relLI.close();
		relLE.close();
		relSyncCLC.close();

		domO.saveToXMLFile();
		domC.saveToXMLFile();
		domA.saveToXMLFile();
		domH.saveToXMLFile();
		domI.saveToXMLFile();
		domM.saveToXMLFile();
		domE.saveToXMLFile();
		domF.saveToXMLFile();
		domL.saveToXMLFile();

		OutDirUtils.copyFileFromMainDir("src/web/Olist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Clist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Alist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Hlist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Ilist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Mlist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Elist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Flist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/Llist.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/style.css");
		OutDirUtils.copyFileFromMainDir("src/web/misc.xsl");
		OutDirUtils.copyFileFromMainDir("src/web/datarace/results.dtd");
		OutDirUtils.copyFileFromMainDir("src/web/datarace/results.xml");
		OutDirUtils.copyFileFromMainDir("src/web/datarace/group.xsl");
		OutDirUtils.copyFileFromMainDir("src/web/datarace/paths.xsl");
		OutDirUtils.copyFileFromMainDir("src/web/datarace/races.xsl");

		OutDirUtils.runSaxon("results.xml", "group.xsl");
		OutDirUtils.runSaxon("results.xml", "paths.xsl");
		OutDirUtils.runSaxon("results.xml", "races.xsl");

		Program.g().HTMLizeJavaSrcFiles();
	}
}


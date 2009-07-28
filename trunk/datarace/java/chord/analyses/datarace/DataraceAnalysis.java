/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.datarace;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;

import chord.util.ArraySet;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.Obj;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.bddbddb.Rel.RelView;
import chord.project.JavaAnalysis;
import chord.doms.DomA;
import chord.doms.DomL;
import chord.doms.DomO;
import chord.doms.DomC;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Program;
import chord.project.ProgramRel;
import chord.project.ProgramDom;
import chord.project.Project;
import chord.project.Properties;
import chord.util.FileUtils;
import chord.util.PropertyUtils;
import chord.util.SetUtils;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.Assertions;

/**
 * Datarace analysis.
 * <p>
 * Outputs relation <tt>datarace</tt> containing each tuple
 * <tt>(a1,c1,e1,a2,c2,e2)</tt> denoting a possible race between
 * abstract threads <tt>a1</tt> and <tt>a2</tt> executing
 * accesses <tt>e1</tt> and <tt>e2</tt>, respectively, in
 * abstract contexts <tt>c1</tt> and <tt>c2</tt> of their
 * containing methods, respectively.
 * <p>
 * Recognized system properties:
 * <ul>
 * <li><tt>chord.max.iters</tt> (default is 0)</li>
 * <li><tt>chord.include.escaping</tt> (default is true).</li>
 * <li><tt>chord.include.parallel</tt> (default is true).</li>
 * <li><tt>chord.include.nongrded</tt> (default is true).</li>
 * <li><tt>chord.print.results</tt> (default is true).</li>
 * <li>All system properties recognized by abstract contexts analysis
 * (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="datarace-java"
)
public class DataraceAnalysis extends JavaAnalysis {
	private ProgramRel relRefineH;
	private ProgramRel relRefineM;
	private ProgramRel relRefineV;
	private ProgramRel relRefineI;
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
		relRefineH = (ProgramRel) Project.getTrgt("refineH");
		relRefineM = (ProgramRel) Project.getTrgt("refineM");
		relRefineV = (ProgramRel) Project.getTrgt("refineV");
		relRefineI = (ProgramRel) Project.getTrgt("refineI");
		domM = (DomM) Project.getTrgt("M");
		domI = (DomI) Project.getTrgt("I");
		domF = (DomF) Project.getTrgt("F");
		domE = (DomE) Project.getTrgt("E");
		domA = (DomA) Project.getTrgt("A");
		domH = (DomH) Project.getTrgt("H");
		domC = (DomC) Project.getTrgt("C");
		domL = (DomL) Project.getTrgt("L");
		hybridAnalysis = (CSAliasAnalysis) Project.getTrgt("cs-alias-java");
	    thrSenAbbrCSCGAnalysis = (ThrSenAbbrCSCGAnalysis)
			Project.getTrgt("thrsen-abbr-cscg-java");
	}

	public void run() {
		int maxIters = PropertyUtils.getIntProperty("chord.max.iters", 0);
		Assertions.Assert(maxIters >= 0);

		boolean includeParallel = PropertyUtils.getBoolProperty(
			"chord.include.parallel", true);
		boolean includeEscaping = PropertyUtils.getBoolProperty(
			"chord.include.escaping", true);
		boolean includeNongrded = PropertyUtils.getBoolProperty(
			"chord.include.nongrded", true);
		boolean printResults = PropertyUtils.getBoolProperty(
			"chord.print.results", true);

		init();

		for (int numIters = 0; true; numIters++) {
			Project.runTask("datarace-prologue-dlog");
			if (includeParallel)
				Project.runTask("datarace-parallel-include-dlog");
			else
				Project.runTask("datarace-parallel-exclude-dlog");
			if (includeEscaping)
				Project.runTask("datarace-escaping-include-dlog");
			else
				Project.runTask("datarace-escaping-exclude-dlog");
			if (includeNongrded)
				Project.runTask("datarace-nongrded-include-dlog");
			else
				Project.runTask("datarace-nongrded-exclude-dlog");
			Project.runTask("datarace-dlog");
			Project.runTask("datarace-stats-dlog");
			if (numIters == maxIters)
				break;
			Project.runTask("datarace-feedback-dlog");
			Project.runTask("refine-hybrid-dlog");
			relRefineH.load();
			int numRefineH = relRefineH.size();
			relRefineH.close();
			relRefineM.load();
			int numRefineM = relRefineM.size();
			relRefineM.close();
			relRefineV.load();
			int numRefineV = relRefineV.size();
			relRefineV.close();
			relRefineI.load();
			int numRefineI = relRefineI.size();
			relRefineI.close();
			if (numRefineH == 0 && numRefineM == 0 &&
				numRefineV == 0 && numRefineI == 0)
				break;
			Project.resetTaskDone("ctxts-java");
		}

		if (printResults)
			printResults();
	}

	private void printResults() {
		Project.runTask(hybridAnalysis);
		Project.runTask(thrSenAbbrCSCGAnalysis);
	    final ICSCG thrSenAbbrCSCG = thrSenAbbrCSCGAnalysis.getCallGraph();
		Project.runTask("datarace-epilogue-dlog");
		final ProgramDom<Trio<Pair<Ctxt, jq_Method>, Ctxt, Quad>> domTCE =
			new ProgramDom<Trio<Pair<Ctxt, jq_Method>, Ctxt, Quad>>();
		domTCE.setName("TCE");
		final DomO domO = new DomO();
		domO.setName("O");

		PrintWriter out;

		String outDirName = Properties.outDirName;
		
		out = FileUtils.newPrintWriter(
			(new File(outDirName, "dataracelist.xml")).getAbsolutePath());
		out.println("<dataracelist>");
		final ProgramRel relDatarace = (ProgramRel) Project.getTrgt("datarace");
		relDatarace.load();
		final ProgramRel relRaceCEC = (ProgramRel) Project.getTrgt("raceCEC");
		relRaceCEC.load();
		final Iterable<Hext<Pair<Ctxt, jq_Method>, Ctxt, Quad,
				Pair<Ctxt, jq_Method>, Ctxt, Quad>> tuples =
			relDatarace.getAry6ValTuples();
		for (Hext<Pair<Ctxt, jq_Method>, Ctxt, Quad,
				  Pair<Ctxt, jq_Method>, Ctxt, Quad> tuple : tuples) {
			int tce1 = domTCE.set(new Trio<Pair<Ctxt, jq_Method>, Ctxt, Quad>(
				tuple.val0, tuple.val1, tuple.val2));
			int tce2 = domTCE.set(new Trio<Pair<Ctxt, jq_Method>, Ctxt, Quad>(
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
			int p = domO.set(new Obj(pts));
			jq_Field fld = Program.getField(tuple.val2);
			int f = domF.get(fld);
			out.println("<datarace Oid=\"O" + p +
				"\" Fid=\"F" + f + "\" " +
				"TCE1id=\"TCE" + tce1 + "\" "  +
				"TCE2id=\"TCE" + tce2 + "\"/>");
		}
		relDatarace.close();
		relRaceCEC.close();
		out.println("</dataracelist>");
		out.close();

		final ProgramRel relLI = (ProgramRel) Project.getTrgt("LI");
		final ProgramRel relLE = (ProgramRel) Project.getTrgt("LE");
		final ProgramRel relSyncCLC = (ProgramRel) Project.getTrgt("syncCLC");
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
					int mIdx = domM.get(srcM);
					Ctxt srcC = origNode.val0;
					int cIdx = domC.get(srcC);
					String lockStr = "";
					Quad inst = insts.iterator().next();
					int iIdx = domI.get(inst);
					RelView view = relLI.getView();
					view.selectAndDelete(1, iIdx);
					Iterable<Inst> locks = view.getAry1ValTuples();
					for (Inst lock : locks) {
						int lIdx = domL.get(lock);
						RelView view2 = relSyncCLC.getView();
						view2.selectAndDelete(0, cIdx);
						view2.selectAndDelete(1, lIdx);
						Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
						Set<Ctxt> pts = SetUtils.newSet(view2.size());
						for (Ctxt ctxt : ctxts)
							pts.add(ctxt);
						int oIdx = domO.set(new Obj(pts));
						view2.free();
						lockStr += "<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
							mIdx + "\" Oid=\"O" + oIdx + "\"/>";
					}
					view.free();
					return lockStr + "<elem Cid=\"C" + cIdx + "\" " +
						"Iid=\"I" + iIdx + "\"/>";
				}
			};

		out = FileUtils.newPrintWriter(
			(new File(outDirName, "TCElist.xml")).getAbsolutePath());
		out.println("<TCElist>");
		for (Trio<Pair<Ctxt, jq_Method>, Ctxt, Quad> tce : domTCE) {
			Pair<Ctxt, jq_Method> srcCM = tce.val0;
			Ctxt methCtxt = tce.val1;
			Quad heapInst = tce.val2;
			int cIdx = domC.get(methCtxt);
			int eIdx = domE.get(heapInst);
			out.println("<TCE id=\"TCE" + domTCE.get(tce) + "\" " +
				"Tid=\"A" + domA.get(srcCM)    + "\" " +
				"Cid=\"C" + cIdx + "\" " +
				"Eid=\"E" + eIdx + "\">");
			jq_Method dstM = Program.getMethod(heapInst);
			int mIdx = domM.get(dstM);
			RelView view = relLE.getView();
			view.selectAndDelete(1, eIdx);
			Iterable<Inst> locks = view.getAry1ValTuples();
			for (Inst lock : locks) {
				int lIdx = domL.get(lock);
				RelView view2 = relSyncCLC.getView();
				view2.selectAndDelete(0, cIdx);
				view2.selectAndDelete(1, lIdx);
				Iterable<Ctxt> ctxts = view2.getAry1ValTuples();
				Set<Ctxt> pts = SetUtils.newSet(view2.size());
				for (Ctxt ctxt : ctxts)
					pts.add(ctxt);
				int oIdx = domO.set(new Obj(pts));
				view2.free();
				out.println("<lock Lid=\"L" + lIdx + "\" Mid=\"M" +
					mIdx + "\" Oid=\"O" + oIdx + "\"/>");
			}
			view.free();
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

		Project.copyFile("datarace/web/results.dtd");
		Project.copyFile("main/web/Olist.dtd");
		Project.copyFile("main/web/Clist.dtd");
		Project.copyFile("main/web/Alist.dtd");
		Project.copyFile("main/web/Hlist.dtd");
		Project.copyFile("main/web/Ilist.dtd");
		Project.copyFile("main/web/Mlist.dtd");
		Project.copyFile("main/web/Elist.dtd");
		Project.copyFile("main/web/Flist.dtd");
		Project.copyFile("main/web/Llist.dtd");
		Project.copyFile("datarace/web/results.xml");
		Project.copyFile("main/web/style.css");
		Project.copyFile("datarace/web/group.xsl");
		Project.copyFile("datarace/web/paths.xsl");
		Project.copyFile("datarace/web/races.xsl");
		Project.copyFile("main/web/misc.xsl");

		Project.runSaxon("results.xml", "group.xsl");
		Project.runSaxon("results.xml", "paths.xsl");
		Project.runSaxon("results.xml", "races.xsl");

		Program.HTMLizeJavaSrcFiles();
	}
}

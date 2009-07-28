/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.deadlock;

import java.io.File;
import java.io.PrintWriter;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.project.Program;
import chord.project.Project;
import chord.project.Chord;
import chord.project.ProgramDom;
import chord.project.ProgramRel;
import chord.project.JavaAnalysis;
import chord.project.Properties;
import chord.project.Utils;

import chord.util.ArraySet;
import chord.util.graph.IPathVisitor;
import chord.util.graph.ShortestPathBuilder;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.Obj;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomA;
import chord.doms.DomL;
import chord.doms.DomO;
import chord.doms.DomC;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.util.FileUtils;
import chord.util.SetUtils;
import chord.util.tuple.object.Pair;
import chord.util.Assertions;

/**
 * Deadlock analysis.
 * <p>
 * Outputs relation <tt>deadlock</tt> containing each tuple
 * <tt>(a1,l1,l2,a2,l3,l4)</tt> denoting a possible deadlock between
 * abstract thread <tt>a1</tt>, which acquires abstract lock
 * <tt>l1</tt> followed by abstract lock <tt>l2</tt>, and
 * abstract thread <tt>a2</tt>, which acquires abstract lock
 * <tt>l3</tt> followed by abstract lock <tt>l4</tt>.
 * <p>
 * Recognized system properties:
 * <ul>
 * <li><tt>chord.max.iters</tt> (default is 0)</li>
 * <li><tt>chord.include.escaping</tt> (default is true).</li>
 * <li><tt>chord.include.parallel</tt> (default is true).</li>
 * <li><tt>chord.include.nonreent</tt> (default is true).</li>
 * <li><tt>chord.include.nongrded</tt> (default is true).</li>
 * <li><tt>chord.print.results</tt> (default is true).</li>
 * <li>All system properties recognized by abstract contexts analysis
 * (see {@link chord.analyses.alias.CtxtsAnalysis}).</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name="deadlock-java"
)
public class DeadlockAnalysis extends JavaAnalysis {
    private DomM domM;
    private DomI domI;
    private DomA domA;
    private DomH domH;
    private DomC domC;
    private DomL domL;
    ProgramDom<Pair<Ctxt, Inst>> domN;
    
    private ProgramRel relRefineH;
    private ProgramRel relRefineM;
    private ProgramRel relRefineV;
    private ProgramRel relRefineI;
	private ThrSenAbbrCSCGAnalysis thrSenAbbrCSCGAnalysis;
	private ProgramRel relNC;
	private ProgramRel relNL;
	private ProgramRel relDeadlock;
	private ProgramRel relSyncCLC;
	
	private ICSCG thrSenAbbrCSCG;
	private final Map<CM, Set<CM>> CMCMMap =
		new HashMap<CM, Set<CM>>();

	private void init() {
		relRefineH = (ProgramRel) Project.getTrgt("refineH");
		relRefineM = (ProgramRel) Project.getTrgt("refineM");
		relRefineV = (ProgramRel) Project.getTrgt("refineV");
		relRefineI = (ProgramRel) Project.getTrgt("refineI");
		domM = (DomM) Project.getTrgt("M");
		domI = (DomI) Project.getTrgt("I");
		domA = (DomA) Project.getTrgt("A");
		domH = (DomH) Project.getTrgt("H");
		domC = (DomC) Project.getTrgt("C");
		domL = (DomL) Project.getTrgt("L");
		domN = (ProgramDom) Project.getTrgt("N");
		thrSenAbbrCSCGAnalysis = (ThrSenAbbrCSCGAnalysis)
			Project.getTrgt("thrsen-abbr-cscg-java");
		relNC = (ProgramRel) Project.getTrgt("NC");
		relNL = (ProgramRel) Project.getTrgt("NL");
		relDeadlock = (ProgramRel) Project.getTrgt("deadlock");
		relSyncCLC = (ProgramRel) Project.getTrgt("syncCLC");
	}
	
	public void run() {
		int maxIters = Integer.getInteger("chord.max.iters", 0);
		Assertions.Assert(maxIters >= 0);

		boolean excludeParallel = Boolean.getBoolean(
			"chord.exclude.parallel");
		boolean excludeEscaping = Boolean.getBoolean(
			"chord.exclude.escaping");
		boolean excludeNonreent = Boolean.getBoolean(
			"chord.exclude.nonreent");
		boolean excludeNongrded = Boolean.getBoolean(
			"chord.exclude.nongrded");
		boolean printResults = System.getProperty(
			"chord.print.results", "true").equals("true");

		init();
		
		Project.runTask(domL);

		for (int numIters = 0; true; numIters++) {
			Project.runTask(thrSenAbbrCSCGAnalysis);
			thrSenAbbrCSCG = thrSenAbbrCSCGAnalysis.getCallGraph();
			domN.clear();
			for (Inst i : domL) {
				jq_Method m = Program.getMethod(i);
				Set<Ctxt> cs = thrSenAbbrCSCG.getContexts(m);
				for (Ctxt c : cs) {
					domN.set(new Pair<Ctxt, Inst>(c, i));
				}
			}
			domN.save();

			relNC.zero();
			relNL.zero();
			for (Pair<Ctxt, Inst> cm : domN) {
				int n = domN.get(cm);
				int c = domC.get(cm.val0);
				int l = domL.get(cm.val1);
				relNC.add(n, c);
				relNL.add(n, l);
			}
			relNC.save();
			relNL.save();

			if (excludeParallel)
				Project.runTask("deadlock-parallel-exclude-dlog");
			else
				Project.runTask("deadlock-parallel-include-dlog");
			if (excludeEscaping)
				Project.runTask("deadlock-escaping-exclude-dlog");
			else
				Project.runTask("deadlock-escaping-include-dlog");
			if (excludeNonreent)
				Project.runTask("deadlock-nonreent-exclude-dlog");
			else
				Project.runTask("deadlock-nonreent-include-dlog");
			if (excludeNongrded)
				Project.runTask("deadlock-nongrded-exclude-dlog");
			else
				Project.runTask("deadlock-nongrded-include-dlog");
			Project.runTask("deadlock-dlog");
			Project.runTask("deadlock-stats-dlog");
			if (numIters == maxIters)
				break;
			Project.runTask("deadlock-feedback-dlog");
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

	private Obj getPointsTo(int cIdx, int lIdx) {
		RelView view = relSyncCLC.getView();
		view.selectAndDelete(0, cIdx);
		view.selectAndDelete(1, lIdx);
		Iterable<Ctxt> ctxts = view.getAry1ValTuples();
		Set<Ctxt> pts = SetUtils.newSet(view.size());
		for (Ctxt ctxt : ctxts)
			pts.add(ctxt);
		view.free();
		return new Obj(pts);
	}
	
	private void printResults() {
        final DomO domO = new DomO();
        domO.setName("O");
        
        PrintWriter out;

        String outDirName = Properties.outDirName;

		relDeadlock.load();
		relSyncCLC.load();

		out = FileUtils.newPrintWriter(
			(new File(outDirName, "deadlocklist.xml")).getAbsolutePath());
		out.println("<deadlocklist>");
		for (Object[] tuple : relDeadlock.getAryNValTuples()) {
			Pair<Ctxt, jq_Method> t1Val = (Pair) tuple[0];
			Pair<Ctxt, Inst> n1Val = (Pair) tuple[1];
			Ctxt c1Val = n1Val.val0;
			Inst l1Val = n1Val.val1;
			Pair<Ctxt, Inst> n2Val = (Pair) tuple[2];
			Ctxt c2Val = n2Val.val0;
			Inst l2Val = n2Val.val1;
			Pair<Ctxt, jq_Method> t2Val = (Pair) tuple[3];
			Pair<Ctxt, Inst> n3Val = (Pair) tuple[4];
			Ctxt c3Val = n3Val.val0;
			Inst l3Val = n3Val.val1;
			Pair<Ctxt, Inst> n4Val = (Pair) tuple[5];
			Ctxt c4Val = n4Val.val0;
			Inst l4Val = n4Val.val1;
			int l1 = domL.get(l1Val);
			int l2 = domL.get(l2Val);
			int l3 = domL.get(l3Val);
			int l4 = domL.get(l4Val);
			// require l1,l2 <= l3,l4 and if not switch
			if (l1 > l3 || (l1 == l3 && l2 > l4)) {
				{
					int tmp;
					tmp = l1; l1 = l3; l3 = tmp;
					tmp = l2; l2 = l4; l4 = tmp;
				}
				{
					Inst tmp;
					tmp = l1Val; l1Val = l3Val; l3Val = tmp;
					tmp = l2Val; l2Val = l4Val; l4Val = tmp;
				}
				{
					Ctxt tmp;
					tmp = c1Val; c1Val = c3Val; c3Val = tmp; 
					tmp = c2Val; c2Val = c4Val; c4Val = tmp;
				}
				{
					Pair<Ctxt, jq_Method> tmp;
					tmp = t1Val; t1Val = t2Val; t2Val = tmp;
				}
			}
			int t1 = domA.get(t1Val);
			int t2 = domA.get(t2Val);
			int c1 = domC.get(c1Val);
			int c2 = domC.get(c2Val);
			int c3 = domC.get(c3Val);
			int c4 = domC.get(c4Val);
			Ctxt t1cVal = t1Val.val0;
			Ctxt t2cVal = t2Val.val0;
			int t1c = domC.get(t1cVal);
			int t2c = domC.get(t2cVal);
			jq_Method t1mVal = t1Val.val1;
			jq_Method t2mVal = t2Val.val1;
			int t1m = domM.get(t1mVal);
			int t2m = domM.get(t2mVal);
			jq_Method m1Val = Program.getMethod(l1Val);
			jq_Method m2Val = Program.getMethod(l2Val);
			jq_Method m3Val = Program.getMethod(l3Val);
			jq_Method m4Val = Program.getMethod(l4Val);
			int m1 = domM.get(m1Val);
			int m2 = domM.get(m2Val);
			int m3 = domM.get(m3Val);
			int m4 = domM.get(m4Val);
			Obj o1Val = getPointsTo(c1, l1);
			Obj o2Val = getPointsTo(c2, l2);
			Obj o3Val = getPointsTo(c3, l3);
			Obj o4Val = getPointsTo(c4, l4);
			int o1 = domO.set(o1Val);
			int o2 = domO.set(o2Val);
			int o3 = domO.set(o3Val);
			int o4 = domO.set(o4Val);
			addToCMCMMap(t1cVal, t1mVal, c1Val, m1Val);
			addToCMCMMap(t2cVal, t2mVal, c3Val, m3Val);
			addToCMCMMap(c1Val , m1Val , c2Val, m2Val);
			addToCMCMMap(c3Val , m3Val , c4Val, m4Val);
/*
			Type type1 = getLub(o1Val, o4Val);
			Type type2 = getLub(o2Val, o3Val);
			int type1id = domT.get(type1);
			int type2id = domT.get(type1);
*/
			out.println("<deadlock " +
				"group=\"" + l1 + "_" + l2 + "_" + l3 + "_" + l4 + "\" " +
				// "t1id=\"T" + type1id + "\" t2id=\"T" + type2id + "\" " +
				"T1Cid=\"C" + t1c + "\" T1Mid=\"M" + t1m + "\" " +
				"T2Cid=\"C" + t2c + "\" T2Mid=\"M" + t2m + "\" " +
				"C1id=\"C"  + c1 + "\" M1id=\"M" + m1 + "\" L1id=\"L" + l1 + "\" O1id=\"O" + o1 + "\" " +
				"C2id=\"C"  + c2 + "\" M2id=\"M" + m2 + "\" L2id=\"L" + l2 + "\" O2id=\"O" + o2 + "\" " +
				"C3id=\"C"  + c3 + "\" M3id=\"M" + m3 + "\" L3id=\"L" + l3 + "\" O3id=\"O" + o3 + "\" " +
				"C4id=\"C"  + c4 + "\" M4id=\"M" + m4 + "\" L4id=\"L" + l4 + "\" O4id=\"O" + o4 + "\"/>");
		}
		out.println("</deadlocklist>");
		out.close();

		relDeadlock.close();
		relSyncCLC.close();
		
        IPathVisitor<Pair<Ctxt, jq_Method>> visitor =
			new IPathVisitor<Pair<Ctxt, jq_Method>>() {
				public String visit(Pair<Ctxt, jq_Method> origNode,
						Pair<Ctxt, jq_Method> destNode) {
					Ctxt ctxt = origNode.val0;
					Set<Quad> insts = thrSenAbbrCSCG.getLabels(origNode, destNode);
					for (Quad inst : insts) {
						return "<elem Cid=\"C" + domC.get(ctxt) + "\" " +
							"Iid=\"I" + domI.get(inst) + "\"/>";
					}
					return "";
				}
			};

        out = FileUtils.newPrintWriter(
        	(new File(outDirName, "CMCMlist.xml")).getAbsolutePath());
        out.println("<CMCMlist>");
        
        for (CM cm1 : CMCMMap.keySet()) {
        	Ctxt ctxt1 = cm1.val0;
        	jq_Method meth1 = cm1.val1;
            int c1 = domC.get(ctxt1);
            int m1 = domM.get(meth1);
			Set<CM> cmSet = CMCMMap.get(cm1);
			ShortestPathBuilder<Pair<Ctxt, jq_Method>> builder =
				new ShortestPathBuilder(thrSenAbbrCSCG, cm1, visitor);
			for (CM cm2 : cmSet) {
				Ctxt ctxt2 = cm2.val0;
				jq_Method meth2 = cm2.val1;
				int c2 = domC.get(ctxt2);
				int m2 = domM.get(meth2);
				out.println("<CMCM C1id=\"C" + c1 + "\" M1id=\"M" + m1 +
					"\" C2id=\"C" + c2 + "\" M2id=\"M" + m2 + "\">");
           		String path = builder.getShortestPathTo(cm2);
				out.println("<path>");
				out.println(path);
				out.println("</path>");
				out.println("</CMCM>");
			}
        }
        out.println("</CMCMlist>");
        out.close();
		
		domO.saveToXMLFile();
        domC.saveToXMLFile();
        domA.saveToXMLFile();
        domH.saveToXMLFile();
        domI.saveToXMLFile();
        domM.saveToXMLFile();
        domL.saveToXMLFile();

        Utils.copyFile("deadlock/web/results.dtd");
        Utils.copyFile("main/web/Olist.dtd");
        Utils.copyFile("main/web/Clist.dtd");
        Utils.copyFile("main/web/Alist.dtd");
        Utils.copyFile("main/web/Hlist.dtd");
        Utils.copyFile("main/web/Ilist.dtd");
        Utils.copyFile("main/web/Mlist.dtd");
        Utils.copyFile("main/web/Llist.dtd");
        Utils.copyFile("deadlock/web/results.xml");
        Utils.copyFile("main/web/style.css");
        Utils.copyFile("deadlock/web/group.xsl");
        Utils.copyFile("deadlock/web/paths.xsl");
        Utils.copyFile("main/web/misc.xsl");

        Utils.runSaxon("results.xml", "group.xsl");
        Utils.runSaxon("results.xml", "paths.xsl");

        Program.HTMLizeJavaSrcFiles();
	}

	private class CM extends Pair<Ctxt, jq_Method> {
		public CM(Ctxt c, jq_Method m) {
			super(c, m);
		}
	};

	private void addToCMCMMap(Ctxt c1, jq_Method m1,
			Ctxt c2, jq_Method m2) {
		CM cm1 = new CM(c1, m1);
		Set<CM> s = CMCMMap.get(cm1);
		if (s == null) {
			s = new ArraySet<CM>();
			CMCMMap.put(cm1, s);
		}
		CM cm2 = new CM(c2, m2);
		s.add(cm2);
	}
}

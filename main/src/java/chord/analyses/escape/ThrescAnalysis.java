/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.datarace;

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
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.analyses.alias.DomO;
import chord.analyses.alias.DomC;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
import chord.analyses.thread.DomA;
import chord.doms.DomL;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.Project;
import chord.project.Properties;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.SetUtils;
import chord.util.tuple.object.Hext;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import chord.util.fig.Execution;
import chord.bddbddb.Rel.PairIterable;

/**
 * Adapated from DataraceAnalysis.java
 */
@Chord(name="thresc-java")
public class ThrescAnalysis extends JavaAnalysis {
	private DomM domM;
	private DomI domI;
	private DomF domF;
	private DomE domE;
	private DomA domA;
	private DomH domH;
	private DomC domC;
	private DomL domL;

  Execution X;

	private void init() {
		domM = (DomM) Project.getTrgt("M");
		domI = (DomI) Project.getTrgt("I");
		domF = (DomF) Project.getTrgt("F");
		domE = (DomE) Project.getTrgt("E");
		domA = (DomA) Project.getTrgt("A");
		domH = (DomH) Project.getTrgt("H");
		domC = (DomC) Project.getTrgt("C");
		domL = (DomL) Project.getTrgt("L");
	}

	public void run() {
    X = Execution.v("adaptive");
    X.addSaveFiles("inputs.dat", "outputs.dat");
    if (X.getBooleanArg("saveStrings", false))
      X.addSaveFiles("inputs.strings", "outputs.strings");

		init();

		Project.runTask(CtxtsAnalysis.getCspaKind());
		Project.runTask("flowins-thresc-dlog");

    PrintWriter datOut = OutDirUtils.newPrintWriter("outputs.dat");

		final ProgramRel rel = (ProgramRel) Project.getTrgt("flowinsEscE");
		rel.load();
		final Iterable<Inst> items = rel.getAry1ValTuples();
    int numEscaping = 0;
		for (Inst inst : items) {
			int e = domE.indexOf((Quad)inst);
      datOut.println(e);
      numEscaping++;
		}
		rel.close();

    datOut.close();
    X.output.put("numEscaping", numEscaping);

    PrintWriter strOut = OutDirUtils.newPrintWriter("outputs.strings");
    for (int e = 0; e < domE.size(); e++)
      strOut.println("E"+e + " " + estr(e));
    strOut.close();

    X.finish(null);
	}

  public String estr(int e) {
    if (e < 0) return "-";
    Quad quad = (Quad)domE.get(e);
    return Program.getProgram().toJavaPosStr(quad)+" "+Program.getProgram().toQuadStr(quad);
  }
}

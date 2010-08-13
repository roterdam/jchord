/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import java.io.*;
import java.util.*;

import joeq.Class.jq_Field;
import joeq.Class.jq_Type;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CSAliasAnalysis;
import chord.analyses.alias.CSObj;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alias.DomC;
import chord.analyses.alias.DomO;
import chord.analyses.alias.ICSCG;
import chord.analyses.alias.ThrSenAbbrCSCGAnalysis;
import chord.analyses.snapshot.Execution;
import chord.analyses.snapshot.StatFig;
import chord.analyses.thread.DomA;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomE;
import chord.doms.DomF;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomL;
import chord.doms.DomT;
import chord.doms.DomV;
import chord.doms.DomM;
import chord.doms.DomP;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.Project;
import chord.project.Config;
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
 * Extract examples from the output of a path analysis.
 * Each example contains the following information:
 *   - Input: program p, query e
 *   - Output: for each allocation site h, whether h is in the hint, that is, useful for proving e
 */
@Chord(name="featextract-java")
public class FeatureExtractionAnalysis extends JavaAnalysis {
	private Execution X;

	private DomE domE;
	private DomH domH;
	private DomT domT;
	private DomV domV;
	private DomP domP;

	private ProgramRel relHT;
	private ProgramRel relEV;
	private ProgramRel relVT;
	private ProgramRel relVH;
	private ProgramRel relHints;
	private ProgramRel relEsc;

  HashSet<Quad> esc = new HashSet();

  // Canonical
  Quad _H;
  jq_Type _T;
  Register _V;
  Inst _P;

  public void computedExcludedClasses() {
    String[] checkExcludedPrefixes = Config.toArray(Config.checkExcludeStr);
    Program program = Program.getProgram();
    for (jq_Reference r : program.getClasses()) {
      String rName = r.getName();
      for (String prefix : checkExcludedPrefixes) {
        if (rName.startsWith(prefix))
          excludedClasses.add(r);
      }
    }
  }
  private boolean computeStatementIsExcluded(Quad e) {
    jq_Class c = e.getMethod().getDeclaringClass();
    return excludedClasses.contains(c);
  }
  Set<jq_Reference> excludedClasses = new HashSet<jq_Reference>();
  HashSet<Quad> queryE = new HashSet(); // Set of all queries that we should answer

  int maxHintSize; // Don't output a hint larger than this

	private void init() {
    //Project.runTask("cipa-0cfa-dlog");
    Project.runTask("cspa-kcfa-dlog");
    Project.runTask("flowins-thresc-dlog");
    Project.runTask("hints-dlog");

    // Load domains
		domE = (DomE) Project.getTrgt("E"); Project.runTask(domE);
		domH = (DomH) Project.getTrgt("H"); Project.runTask(domH);
		domT = (DomT) Project.getTrgt("T"); Project.runTask(domT);
		domV = (DomV) Project.getTrgt("V"); Project.runTask(domV);
		domP = (DomP) Project.getTrgt("P"); Project.runTask(domP);

    // Load relations
		relHT = (ProgramRel) Project.getTrgt("HT"); Project.runTask(relHT); relHT.load();
		relEV = (ProgramRel) Project.getTrgt("EV"); Project.runTask(relEV); relEV.load();
		relVT = (ProgramRel) Project.getTrgt("VT"); Project.runTask(relVT); relVT.load();

		relVH = (ProgramRel) Project.getTrgt("refinedVH"); relVH.load();
		relHints = (ProgramRel) Project.getTrgt("hints"); relHints.load();
    relEsc = (ProgramRel) Project.getTrgt("esc"); relEsc.load();

    // Run flow-insensitive thread escape analysis to see if we can prove these queries local
    // Even if we can't, the hint might still be useful
    for (Object h : relEsc.getAry1ValTuples())
      esc.add((Quad)h);
    X.logs("Flow-insensitive thread-escape: %s escaping:", esc.size());
    for (Quad h : esc)
      X.logs("  ESCAPE: %s", hstr(h));

    _H = (Quad)domH.get(1);
    _T = domT.get(1);
    _V = domV.get(1);
    _P = domP.get(1);

    // Compute which queries we should answer in the whole program
    computedExcludedClasses();
    for (Quad e : domE) if (!computeStatementIsExcluded(e) && e2v(e) != null) queryE.add(e);
	}

  private void finish() {
    relHT.close();
    relEV.close();
    relVT.close();
    relVH.close();
    relHints.close();
    relEsc.close();
  }

	private <T> ArrayList<T> sel0(T x, ProgramRel rel, Object o0) {
		RelView view = rel.getView();
		view.selectAndDelete(0, o0);
		ArrayList<T> list = new ArrayList();
    for(Object o1 : view.getAry1ValTuples())
      list.add((T)o1);
    view.free();
    return list;
	}
	private <T> ArrayList<T> sel1(T x, ProgramRel rel, Object o1) {
		RelView view = rel.getView();
		view.selectAndDelete(1, o1);
		ArrayList<T> list = new ArrayList();
    for(Object o0 : view.getAry1ValTuples())
      list.add((T)o0);
    view.free();
    return list;
	}

  jq_Type h2t(Quad h) {
    for (jq_Type t : sel0(_T, relHT, h)) return t;
    throw new RuntimeException("Bad: "+h);
  }
  jq_Type v2t(Register v) {
    for (jq_Type t : sel0(_T, relVT, v)) return t;
    throw new RuntimeException("Bad: "+v);
  }
  Register e2v(Quad e) {
    for (Register v : sel0(_V, relEV, e)) return v;
    return null;
  }

  List<Quad> v2h(Register v) { return sel0(_H, relVH, v); }
  //List<Inst> succ(Inst p) { return sel0(_P, relPP, p); }

  List<Quad> e2hints(Quad e) { return sel0(_H, relHints, e); }

  String tstr(jq_Type t) { return String.format("%s[%s]", domT.indexOf(t), t); }
  String hstr(Quad h) { return String.format("%s[%s]", domH.indexOf(h), h.toVerboseStr()); }
  String estr(Quad e) { return String.format("%s[%s]", domE.indexOf(e), e.toVerboseStr()); }
  String vstr(Register v) { return String.format("%s[%s]", domV.indexOf(v), v); }
  String pstr(Inst p) { return String.format("%s[%s]", domP.indexOf(p), p.toVerboseStr()); }

  StatFig excessFig = new StatFig(); // Number of extra allocation sites added

  class Query {
    Quad e0; // This is the access which we're trying to prove thread local
    HashSet<Quad> trueHints;
    HashSet<Quad> predHints;
    boolean flowinsLocal = true;

    public Query(Quad e0) {
      this.e0 = e0;
    }

    void extract() {
      X.logs("=== Query %s", estr(e0));
      Register v0 = e2v(e0);
      //jq_Type t0 = v2t(v0);
      //X.logs("  VAR %s has type %s", vstr(v0), tstr(t0));

      // What the flowins anaysis does
      for (Quad h : v2h(v0)) {
        if (esc.contains(h)) {
          flowinsLocal = false;
          X.logs("  NONLOCAL because %s -> %s -> %s", estr(e0), vstr(v0), hstr(h));
        }
      }
      X.logs("  flowins analysis: %s", flowinsLocal ? "local" : "escape");
      predHints = new HashSet();
      for (Quad h : e2hints(e0)) { predHints.add(h); }

      // True hints
      if (trueHints != null) {
        for (Quad h : trueHints) {
          X.logs("  TRUE: %s has type %s", hstr(h), tstr(h2t(h)));
          if (!predHints.contains(h))
            X.logs("    UNSOUND ERROR: not covered by prediction (flowins must be broken)");
        }
      }

      // Predicted hints
      for (Quad h : predHints) {
        X.logs("  PRED: %s has type %s", hstr(h), tstr(h2t(h)));
      }

      // Evaluate excess (how many more sites we put in the hint compared to the path true)
      if (trueHints != null) {
        int excess = predHints.size() - trueHints.size();
        X.logs("  excess = %s - %s = %s", predHints.size(), trueHints.size(), excess);
        excessFig.add(excess);
      }

      X.logs("");
    }

    public void addHint(Quad h) {
      if (trueHints == null) trueHints = new HashSet();
      trueHints.add(h);
    }
  }

	public void run() {
    X = Execution.v("hints");		
    X.addSaveFiles("hints.txt");
    java.util.HashMap<Object,Object> options = new java.util.LinkedHashMap<Object,Object>();
    options.put("version", 2);
    options.put("program", System.getProperty("chord.work.dir"));
    options.put("k", System.getProperty("chord.kcfa.k"));
    options.put("maxHintSize", maxHintSize = X.getIntArg("maxHintSize", 10));
    options.put("hintsPath", X.getStringArg("hintsPath", null));
    options.put("hintsType", X.getStringArg("hintsType", null));
    X.writeMap("options.map", options);

    init();

    // Read queries and hints
    HashMap<Quad,Query> queries = new HashMap();
    if (X.getBooleanArg("onlyConsiderQueriesInHints", true)) {
      String hintsPath = X.getStringArg("hintsPath", null);
      // Get the queries from just the hints of the path program analysis
      try {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(hintsPath)));
        String line;
        while ((line = in.readLine()) != null) {
          String[] tokens = line.split("\\s+");
          Quad e = domE.get(Integer.parseInt(tokens[0]));
          Quad h = (Quad)domH.get(Integer.parseInt(tokens[1]));
          Query q = queries.get(e);
          if (q == null) queries.put(e, q = new Query(e));
          q.addHint(h);
        }
        in.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      X.logs("Read %s queries from %s (these were proven on the path program)", queries.size(), hintsPath);
    }
    else {
      // Include all queries
      for (Quad e : queryE) queries.put(e, new Query(e));
      X.logs("Including all %s queries (which are not excluded)", queries.size());
    }

    // Compute our hints
    for (Query q : queries.values()) q.extract();

    // What the flowins analysis can do
    int numLocal = 0;
    for (Query q : queries.values())
      if (q.flowinsLocal) numLocal++;

    // Output hints
    PrintWriter out = OutDirUtils.newPrintWriter("hints.txt");
    for (Query q : queries.values()) {
      if (q.predHints.size() == 0) // Don't need any hints (how is this possible?)
        out.println(domE.indexOf(q.e0) + " -1"); // Query trivially true (but still want to keep it)
      if (q.predHints.size() <= maxHintSize) {
        for (Quad h : q.predHints)
          out.println(domE.indexOf(q.e0) + " " + domH.indexOf(h));
      }
      else
        out.println(domE.indexOf(q.e0) + " 0"); // We can't prove this query!
    }
    out.close();

    // Print hint statistics
    int sumHintSize = 0;
    int maxHintSize = 0;
    for (Query q : queries.values()) {
      int size = q.predHints.size();
      sumHintSize += size;
      maxHintSize = Math.max(maxHintSize, size);
    }
    X.output.put("avgHintSize", 1.0*sumHintSize/queries.size());
    X.output.put("maxHintSize", maxHintSize);

    X.output.put("numLocal", numLocal);
    X.output.put("numEscaping", queries.size()-numLocal);
    X.output.put("flowinsFracProven", numLocal/queries.size());
    X.output.put("numQueries", queries.size());
    X.output.put("avgExcess", excessFig.mean());
    X.logs("Aggregate excess: %s", excessFig);

    finish();
    X.finish(null);
	}
}

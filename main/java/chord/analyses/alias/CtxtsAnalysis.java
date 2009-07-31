/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.ArraySet;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomC;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.doms.DomV;
import chord.project.Chord;
import chord.project.JavaAnalysis;
import chord.project.Program;
import chord.project.ProgramRel;
import chord.project.Project;

/**
 * Abstract contexts analysis.
 * <p>
 * The goal of this analysis is to translate client-specified inputs
 * concerning the kind of context sensitivity desired into relations
 * that are subsequently consumed by context-sensitive may alias and
 * call graph analyses.
 * <p>
 * This analysis allows:
 * <ul>
 * <li>each method to be analyzed using a different kind of context
 * sensitivity (one of context insensitivity, k-CFA,
 * k-object-sensitivity, and copy-context-sensitivity),</li>
 * <li>each local variable to be analyzed context sensitively or
 * insensitively, and</li>
 * <li>a different 'k' value to be used for each object allocation
 * statement and method invocation statement in the program.</li>
 * </ul>
 * This analysis can be called multiple times and in each invocation
 * it can incorporate feedback from a client to adjust the
 * precision of the points-to information and call graph computed
 * subsequently by the may alias and call graph analyses.  Clients
 * can indicate in each invocation:
 * <ul>
 * <li>Which methods must be analyzed context sensitively (in
 * addition to those already being analyzed context sensitively in
 * the previous invocation of this analysis) and using what kind
 * of context sensitivity; the remaining methods will be analyzed
 * context insensitively (that is, in the lone <tt>epsilon</tt>
 * context)
 * </li>
 * <li>Which local variables of reference type must be analyzed
 * context sensitively (in addition to those already being analyzed
 * context sensitively in the previous invocation of this analysis);
 * the remaining ones will be analyzed context insensitively
 * (that is, their points-to information will be maintained in the
 * lone <tt>epsilon</tt context).
 * </li>
 * <li>The object alocation statements and method invocation
 * statements in the program whose 'k' values must be incremented
 * (over those used in the previous invocation of this analysis).
 * </li>
 * </ul>
 * Recognized system properties:
 * <ul>
 * <li><tt>chord.ctxt.kind</tt> which specifies the kind of context
 * sensitivity to be used for each method (and all its local
 * variables) in the program.
 * It may be one of
 * <tt>ci</tt> (context insensitive) or
 * <tt>cs</tt> (k-CFA).
 * </li>
 * <li><tt>chord.inst.ctxt.kind</tt> which specifies the kind of
 * context sensitivity to be used for each instance method (and all
 * its local variables) in the program.
 * It may be one of
 * <tt>ci</tt> (context insensitive),
 * <tt>cs</tt> (k-CFA), or
 * <tt>co</tt> (k-object-sensitive).
 * </li>
 * <li><tt>chord.stat.ctxt.kind</tt> which specifies the kind of
 * context sensitivity to be used for each static method (and all
 * its local variables) in the program.
 * It may be one of
 * <tt>ci</tt> (context insensitive),
 * <tt>cs</tt> (k-CFA), or
 * <tt>cc</tt> (copy-context-sensitive).
 * </li>
 * <li><tt>chord.kobj.k</tt> and <tt>chord.kcfa.k</tt> which specify
 * the 'k' value to be used each object allocation statement and for
 * each method invocation statement, respectively, in the
 * program.</li>
 * </ul>
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "ctxts-java",
	consumedNames = { "IM", "VH" },
	producedNames = { "C", "CC", "CH", "CI",
		"epsilonV", "epsilonM", "kcfaSenM", "kobjSenM", "ctxtCpyM",
		"refinableCH", "refinableCI", "refinableM", "refinableV" },
	namesOfTypes = { "C" },
	types = { DomC.class }
)
public class CtxtsAnalysis extends JavaAnalysis {
	private static final Set<Ctxt> emptyCtxtSet =
		Collections.emptySet();
	private static final Set<jq_Method> emptyMethSet =
		Collections.emptySet();
	private static final Quad[] emptyElems = new Quad[0];

	// includes all methods in domain
	private Map<jq_Method, Set<Ctxt>> methToCtxtsMap;
	
	// ctxt kind is KCFASEN
	private Map<jq_Method, List<Quad>> methToClrSitesMap;
    // ctxt kind is KOBJSEN
    private Map<jq_Method, List<Quad>> methToRcvSitesMap;
	// ctxt kind is CTXTCPY
	private Map<jq_Method, Set<jq_Method>> methToClrMethsMap;
	
	private Set<Ctxt> epsilonCtxtSet;

    public static final int CTXTINS = 0;  // abbr ci; must be 0
    public static final int KOBJSEN = 1;  // abbr co
    public static final int KCFASEN = 2;  // abbr cs
    public static final int CTXTCPY = 3;  // abbr cc

	private jq_Method mainMeth;
	private boolean[] isCtxtSenI;	// indexed by domI
	private boolean[] isCtxtSenV;	// indexed by domV
	private int[] methKind;         // indexed by domM
	private int[] kobjValue;        // indexed by domH
	private int[] kcfaValue;        // indexed by domI

	private int kobjK;
	private int kcfaK;
    private int instCtxtKind;
    private int statCtxtKind;
    private int maxIters;
	private int currIter;

	private DomV domV;
	private DomM domM;
	private DomI domI;
	private DomH domH;
	private DomC domC;

	private ProgramRel relIM;
	private ProgramRel relVH;

	private ProgramRel relCC;
	private ProgramRel relCH;
	private ProgramRel relCI;

	private ProgramRel relRefineH;
    private ProgramRel relRefineM;
    private ProgramRel relRefineI;
    private ProgramRel relRefineV;

    private ProgramRel relRefinableM;
	private ProgramRel relRefinableV;
	private ProgramRel relRefinableCI;
	private ProgramRel relRefinableCH;

	private ProgramRel relEpsilonM;
	private ProgramRel relKcfaSenM;
	private ProgramRel relKobjSenM;
	private ProgramRel relCtxtCpyM;
	private ProgramRel relEpsilonV;

	private void init() {
		domV = (DomV) Project.getTrgt("V");
		domI = (DomI) Project.getTrgt("I");
		domM = (DomM) Project.getTrgt("M");
		domH = (DomH) Project.getTrgt("H");
		domC = (DomC) Project.getTrgt("C");

		relIM = (ProgramRel) Project.getTrgt("IM");
		relVH = (ProgramRel) Project.getTrgt("VH");
		
		relRefineH = (ProgramRel) Project.getTrgt("refineH");
        relRefineI = (ProgramRel) Project.getTrgt("refineI");
        relRefineM = (ProgramRel) Project.getTrgt("refineM");
        relRefineV = (ProgramRel) Project.getTrgt("refineV");
        relRefinableM = (ProgramRel) Project.getTrgt("refinableM");
        relRefinableV = (ProgramRel) Project.getTrgt("refinableV");
		relRefinableCH = (ProgramRel) Project.getTrgt("refinableCH");
		relRefinableCI = (ProgramRel) Project.getTrgt("refinableCI");
        
		relCC = (ProgramRel) Project.getTrgt("CC");
		relCH = (ProgramRel) Project.getTrgt("CH");
		relCI = (ProgramRel) Project.getTrgt("CI");
		relEpsilonM = (ProgramRel) Project.getTrgt("epsilonM");
		relKcfaSenM = (ProgramRel) Project.getTrgt("kcfaSenM");
		relKobjSenM = (ProgramRel) Project.getTrgt("kobjSenM");
		relCtxtCpyM = (ProgramRel) Project.getTrgt("ctxtCpyM");
		relEpsilonV = (ProgramRel) Project.getTrgt("epsilonV");

		mainMeth = Program.getMainMethod();
		
        maxIters = Integer.getInteger("chord.max.iters", 0);

        String ctxtKindStr = System.getProperty("chord.ctxt.kind", "ci");
        String instCtxtKindStr = System.getProperty(
        	"chord.inst.ctxt.kind", ctxtKindStr);
        String statCtxtKindStr = System.getProperty(
        	"chord.stat.ctxt.kind", ctxtKindStr);
        if (instCtxtKindStr.equals("ci")) {
        	instCtxtKind = CTXTINS;
        } else if (instCtxtKindStr.equals("cs")) {
        	instCtxtKind = KCFASEN;
        } else if (instCtxtKindStr.equals("co")) {
        	instCtxtKind = KOBJSEN;
        } else
        	assert false;
        if (statCtxtKindStr.equals("ci")) {
        	statCtxtKind = CTXTINS;
        } else if (statCtxtKindStr.equals("cs")) {
        	statCtxtKind = KCFASEN;
        } else if (statCtxtKindStr.equals("cc")) {
        	statCtxtKind = CTXTCPY;
        } else
        	assert false;

		kobjK = Integer.getInteger("chord.kobj.k", 1);
		assert (kobjK > 0);
		kcfaK = Integer.getInteger("chord.kcfa.k", 1);
		assert (kcfaK > 0);

		if (maxIters > 0) {
			assert (instCtxtKind == KOBJSEN ||
				instCtxtKind == KCFASEN ||
				statCtxtKind == KCFASEN);
		}
	}
	
	private int getCtxtKind(jq_Method m) {
		if (m == mainMeth || m instanceof jq_ClassInitializer ||
				m.isAbstract())
			return CTXTINS;
        return m.isStatic() ? statCtxtKind : instCtxtKind;
	}

	public void run() {
		if (currIter == 0) {
			init();
			int numV = domV.size();
			isCtxtSenV = new boolean[numV];
			int numM = domM.size();
			methKind = new int[numM];
			for (int mIdx = 0; mIdx < numM; mIdx++) {
				jq_Method mVal = domM.get(mIdx);
                methKind[mIdx] = (maxIters > 0) ? CTXTINS : getCtxtKind(mVal);
			}
			for (int mIdx = 0; mIdx < numM; mIdx++) {
				if (methKind[mIdx] != CTXTINS) {
					jq_Method m = domM.get(mIdx);
					ControlFlowGraph cfg = Program.getCFG(m);
					RegisterFactory rf = cfg.getRegisterFactory();
					for (Object o : rf) {
						Register v = (Register) o;
						if (v.getType().isReferenceType()) {
							int vIdx = domV.get(v);
							isCtxtSenV[vIdx] = true;
						}
					}
				}
			}
			kobjValue = new int[domH.size()];
			for (Quad inst : domH) {
				int h = domH.get(inst);
				kobjValue[h] = kobjK;
			}
			kcfaValue = new int[domI.size()];
			for (Quad inst : domI) {
				int i = domI.get(inst);
				kcfaValue[i] = kcfaK;
			}
		} else {
            relRefineH.load();
            Iterable<Quad> heapInsts =
                relRefineH.getAry1ValTuples();
            for (Quad inst : heapInsts) {
                int hIdx = domH.get(inst);
                kobjValue[hIdx]++;
            }
            relRefineH.close();
            relRefineI.load();
            Iterable<Quad> invkInsts =
                relRefineI.getAry1ValTuples();
            for (Quad inst : invkInsts) {
                int iIdx = domI.get(inst);
                kcfaValue[iIdx]++;
            }
            relRefineI.close();
            relRefineV.load();
            Iterable<Register> vars = relRefineV.getAry1ValTuples();
            for (Register var : vars) {
                int v = domV.get(var);
                assert (!isCtxtSenV[v]);
                isCtxtSenV[v] = true;
            }
            relRefineV.close();
            relRefineM.load();
            Iterable<jq_Method> meths = relRefineM.getAry1ValTuples();
            for (jq_Method meth : meths) {
                int m = domM.get(meth);
                assert (methKind[m] == CTXTINS);
                methKind[m] = getCtxtKind(meth);
                assert (methKind[m] != CTXTINS);
            }
            relRefineM.close();
		}

		validate();

		relIM.load();
		relVH.load();

		Ctxt epsilon = domC.setCtxt(emptyElems);
		epsilonCtxtSet = new ArraySet<Ctxt>(1);
		epsilonCtxtSet.add(epsilon);

		methToCtxtsMap = new HashMap<jq_Method, Set<Ctxt>>();
		int numI = domI.size();
		int numH = domH.size();

		isCtxtSenI = new boolean[numI];
		methToClrSitesMap = new HashMap<jq_Method, List<Quad>>();
        methToRcvSitesMap = new HashMap<jq_Method, List<Quad>>();
		methToClrMethsMap = new HashMap<jq_Method, Set<jq_Method>>();

		if (maxIters > 0) {
			int[] histogramI = new int[maxIters + 1];
			for (int i = 0; i < numI; i++) {
				histogramI[kcfaValue[i]]++;
			}
			for (int i = 0; i <= maxIters; i++) {
				System.out.println("I " + i + " " + histogramI[i]);
			}
		
			int[] histogramH = new int[maxIters + 1];
			for (int i = 0; i < numH; i++) {
				histogramH[kobjValue[i]]++;
			}
			for (int i = 0; i <= maxIters; i++) {
				System.out.println("H " + i + " " + histogramH[i]);
			}
		}

		doAnalysis();

		relIM.close();
		relVH.close();

		for (int iIdx = 0; iIdx < numI; iIdx++) {
			if (!isCtxtSenI[iIdx])
				continue;
			Quad invk = domI.get(iIdx);
			jq_Method meth = Program.getMethod(invk);
			Set<Ctxt> ctxts = methToCtxtsMap.get(meth);
			int k = kcfaValue[iIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, invk, oldElems);
				domC.setCtxt(newElems);
			}
		}
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad inst = domH.get(hIdx);
			jq_Method meth = Program.getMethod(inst);
			Set<Ctxt> ctxts = methToCtxtsMap.get(meth);
			int k = kobjValue[hIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, inst, oldElems);
				domC.setCtxt(newElems);
			}
		}

		domC.save();

		int numC = domC.size();

		boolean isLastIter = (currIter == maxIters);
		
		relCC.zero();
		relCI.zero();
		if (!isLastIter)
			relRefinableCI.zero();
		for (int iIdx = 0; iIdx < numI; iIdx++) {
			if (!isCtxtSenI[iIdx])
				continue;
			Quad invk = domI.get(iIdx);
			jq_Method meth = Program.getMethod(invk);
			Set<Ctxt> ctxts = methToCtxtsMap.get(meth);
			int k = kcfaValue[iIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, invk, oldElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCC.add(oldCtxt, newCtxt);
				if (!isLastIter &&
					newElems.length < oldElems.length + 1 &&
					!contains(oldElems, invk)) {
                    relRefinableCI.add(oldCtxt, invk);
				}
				relCI.add(newCtxt, invk);
			}
		}
		relCI.save();
		if (!isLastIter)
			relRefinableCI.save();

		assert (domC.size() == numC);

		relCH.zero();
		if (!isLastIter)
			relRefinableCH.zero();
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad inst = domH.get(hIdx);
			jq_Method meth = Program.getMethod(inst);
			Set<Ctxt> ctxts = methToCtxtsMap.get(meth);
			int k = kobjValue[hIdx];
			for (Ctxt oldCtxt : ctxts) {
				Quad[] oldElems = oldCtxt.getElems();
				Quad[] newElems = combine(k, inst, oldElems);
				Ctxt newCtxt = domC.setCtxt(newElems);
				relCC.add(oldCtxt, newCtxt);
                if (!isLastIter &&
                	newElems.length < oldElems.length + 1 &&
					!contains(oldElems, inst)) {
                    relRefinableCH.add(oldCtxt, inst);
				}
				relCH.add(newCtxt, inst);
			}
		}
		relCH.save();
		if (!isLastIter)
			relRefinableCH.save();

		assert (domC.size() == numC);

		relCC.save();

        relEpsilonM.zero();
        relKcfaSenM.zero();
        relKobjSenM.zero();
        relCtxtCpyM.zero();
        int numM = domM.size();
        for (int mIdx = 0; mIdx < numM; mIdx++) {
            int kind = methKind[mIdx];
            switch (kind) {
            case CTXTINS:
                relEpsilonM.add(mIdx);
                break;
            case KOBJSEN:
                relKobjSenM.add(mIdx);
                break;
            case KCFASEN:
                relKcfaSenM.add(mIdx);
                break;
            case CTXTCPY:
                relCtxtCpyM.add(mIdx);
                break;
            default:
                assert false;
            }
        }
        relEpsilonM.save();
        relKcfaSenM.save();
        relKobjSenM.save();
        relCtxtCpyM.save();

		relEpsilonV.zero();
		int numV = domV.size();
		for (int v = 0; v < numV; v++) {
			if (!isCtxtSenV[v])
				relEpsilonV.add(v);
		}
		relEpsilonV.save();

		relRefinableV.zero();
		for (int v = 0; v < numV; v++) {
			if (!isCtxtSenV[v]) {
				Register var = domV.get(v);
				jq_Method meth = domV.getMethod(var);
				if (getCtxtKind(meth) != CTXTINS) {
					relRefinableV.add(var);
				}
			}
		}
		relRefinableV.save();
		
		relRefinableM.zero();
		for (int m = 0; m < numM; m++) {
			if (methKind[m] == CTXTINS) {
				jq_Method meth = domM.get(m);
				if (getCtxtKind(meth) != CTXTINS) {
					relRefinableM.add(m);
				}
			}
		}
		relRefinableM.save();

		currIter++;
	}

	private static boolean contains(Quad[] elems, Quad q) {
		for (Quad e : elems) {
			if (e == q)
				return true;
		}
		return false;
	}

	private void validate() {
		// check that the main jq_Method and each class initializer method
		// and each method without a body is not asked to be analyzed
		// context sensitively.
		int numM = domM.size();
		for (int m = 0; m < numM; m++) {
			int kind = methKind[m];
			if (kind != CTXTINS) {
				jq_Method meth = domM.get(m);
				assert (meth != mainMeth);
				assert (!(meth instanceof jq_ClassInitializer));
				assert (Program.getCFG(meth) != null);
				if (kind == KOBJSEN) {
					assert (!meth.isStatic());
				} else if (kind == CTXTCPY) {
					assert (meth.isStatic());
				}
			}
		}
		// check that each variable in a context insensitive method is
		// not asked to be treated context sensitively.
		int numV = domV.size();
		for (int v = 0; v < numV; v++) {
			if (isCtxtSenV[v]) {
				Register var = domV.get(v);
				jq_Method meth = domV.getMethod(var);
				int m = domM.get(meth);
				int kind = methKind[m];
				assert (kind != CTXTINS);
			}
		}
	}

	private void doAnalysis() {
		Set<jq_Method> roots = new HashSet<jq_Method>();
		Map<jq_Method, Set<jq_Method>> methToPredsMap =
			new HashMap<jq_Method, Set<jq_Method>>();
		for (int mIdx = 0; mIdx < domM.size(); mIdx++) {
			jq_Method meth = domM.get(mIdx);
			int kind = methKind[mIdx];
			switch (kind) {
			case CTXTINS:
			{
				roots.add(meth);
				methToPredsMap.put(meth, emptyMethSet);
				methToCtxtsMap.put(meth, epsilonCtxtSet);
				break;
			}
            case KCFASEN:
            {
                Set<jq_Method> predMeths = new HashSet<jq_Method>();
                List<Quad> clrSites = new ArrayList<Quad>();
                for (Quad invk : getCallers(meth)) {
                    int iIdx = domI.get(invk);
                    isCtxtSenI[iIdx] = true;
                    predMeths.add(Program.getMethod(invk));
                    clrSites.add(invk);
                }
                methToClrSitesMap.put(meth, clrSites);
                methToPredsMap.put(meth, predMeths);
                methToCtxtsMap.put(meth, emptyCtxtSet);
                break;
            }
			case KOBJSEN:
            {
            	Set<jq_Method> predMeths = new HashSet<jq_Method>();
                List<Quad> rcvSites = new ArrayList<Quad>();
				ControlFlowGraph cfg = Program.getCFG(meth);
                Register thisVar = cfg.getRegisterFactory().get(0);
                Iterable<Quad> pts = getPointsTo(thisVar);
                for (Quad inst : pts) {
                    predMeths.add(Program.getMethod(inst));
                    rcvSites.add(inst);
                }
                methToRcvSitesMap.put(meth, rcvSites);
                methToPredsMap.put(meth, predMeths);
                methToCtxtsMap.put(meth, emptyCtxtSet);
                break;
			}
			case CTXTCPY:
			{
            	System.out.println("REACHED CPY: " + meth);
				Set<jq_Method> predMeths = new HashSet<jq_Method>();
				for (Quad invk : getCallers(meth)) {
					predMeths.add(Program.getMethod(invk));
				}
				methToClrMethsMap.put(meth, predMeths);
				methToPredsMap.put(meth, predMeths);
				methToCtxtsMap.put(meth, emptyCtxtSet);
				break;
			}
			default:
				assert false;
			}
		}
		process(roots, methToPredsMap);
	}

	private void process(Set<jq_Method> roots,
			Map<jq_Method, Set<jq_Method>> methToPredsMap) {
		IGraph<jq_Method> graph =
			new MutableGraph<jq_Method>(roots, methToPredsMap, null);
		List<Set<jq_Method>> sccList = graph.getTopSortedSCCs();
		System.out.println("numSCCs: " + sccList.size());
		for (int i = 0; i < sccList.size(); i++) {
			Set<jq_Method> scc = sccList.get(i);
			System.out.println("SCC: " + i + " size: " + scc.size());
			if (scc.size() == 1) {
				jq_Method cle = scc.iterator().next();
				if (roots.contains(cle))
					continue;
				if (!graph.hasEdge(cle, cle)) {
					methToCtxtsMap.put(cle, getNewCtxts(cle));
					continue;
				}
			}
			for (jq_Method cle : scc) {
				assert (!roots.contains(cle));
			}
			boolean changed = true;
			for (int count = 0; changed; count++) {
				System.out.println("\tcount: " + count);
				changed = false;
				for (jq_Method cle : scc) {
					Set<Ctxt> newCtxts = getNewCtxts(cle);
					if (!changed) {
						Set<Ctxt> oldCtxts =
							methToCtxtsMap.get(cle);
						for (Ctxt ctxt : newCtxts) {
							if (!oldCtxts.contains(ctxt)) {
								changed = true;
								break;
							}
						}
					}
					methToCtxtsMap.put(cle, newCtxts);
				}
			}
		}
		System.out.println("DONE");
	}

	private Iterable<Quad> getPointsTo(Register var) {
		RelView view = relVH.getView();
		view.selectAndDelete(0, var);
		return view.getAry1ValTuples();
	}

	private Iterable<Quad> getCallers(jq_Method meth) {
		RelView view = relIM.getView();
		view.selectAndDelete(1, meth);
		return view.getAry1ValTuples();
	}

	private Quad[] combine(int k, Quad inst, Quad[] elems) {
        int oldLen = elems.length;
        int newLen = Math.min(k - 1, oldLen) + 1;
        Quad[] newElems = new Quad[newLen];
        newElems[0] = inst;
		if (newLen > 1)
        	System.arraycopy(elems, 0, newElems, 1, newLen - 1);
		return newElems;
	}

	private Set<Ctxt> getNewCtxts(jq_Method cle) {
		Set<Ctxt> newCtxts = new HashSet<Ctxt>();
		int mIdx = domM.get(cle);
		int kind = methKind[mIdx];
		switch (kind) {
        case KCFASEN:
		{
			List<Quad> invks = methToClrSitesMap.get(cle);
            for (Quad invk : invks) {
                int k = kcfaValue[domI.get(invk)];
                jq_Method clr = Program.getMethod(invk);
                Set<Ctxt> clrCtxts = methToCtxtsMap.get(clr);
                for (Ctxt oldCtxt : clrCtxts) {
					Quad[] oldElems = oldCtxt.getElems();
					Quad[] newElems = combine(k, invk, oldElems);
                    Ctxt newCtxt = domC.setCtxt(newElems);
                    newCtxts.add(newCtxt);
                }
            }
            break;
		}
        case KOBJSEN:
		{
			List<Quad> rcvs = methToRcvSitesMap.get(cle);
            for (Quad rcv : rcvs) {
                int k = kobjValue[domH.get(rcv)];
                jq_Method clr = Program.getMethod(rcv);
                Set<Ctxt> rcvCtxts = methToCtxtsMap.get(clr);
                for (Ctxt oldCtxt : rcvCtxts) {
					Quad[] oldElems = oldCtxt.getElems();
					Quad[] newElems = combine(k, rcv, oldElems);
                    Ctxt newCtxt = domC.setCtxt(newElems);
                    newCtxts.add(newCtxt);
                }
            }
            break;
		}
		case CTXTCPY:
		{
			Set<jq_Method> clrs = methToClrMethsMap.get(cle);
			for (jq_Method clr : clrs) {
				Set<Ctxt> clrCtxts = methToCtxtsMap.get(clr);
				newCtxts.addAll(clrCtxts);
			}
			break;
		}
		default:
			assert false;
		}
		return newCtxts;
	}
}

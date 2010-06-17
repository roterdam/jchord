/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.slicer;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import joeq.Compiler.Quad.Quad;
import joeq.Class.jq_Method;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(name = "dynamic-cg-java")
public class DynamicCallGraphAnalysis extends DynamicAnalysis {
	private final Set<jq_Method> emptySet = Collections.emptySet();
	private InstrScheme instrScheme;
	private final Map<Quad, Set<jq_Method>> invkToTrgts =
		new HashMap<Quad, Set<jq_Method>>();


	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMethodEvent(true, false);
		instrScheme.setMethodCallEvent(true, false, false, true, false);
		return instrScheme;
	}
	
	public void processEnterMethod(int m, int t) {
		if (m >= 0) {
			// TODO
		}
	}
    public void processMethodCallBef(int i, int t, int o) {
		if (i >= 0) {
			// TODO
		}
	}
    
	public Set<jq_Method> getTargets(Quad invk) {
		Set<jq_Method> targets = invkToTrgts.get(invk);
		return (targets == null) ? emptySet : targets;
	}
}

/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Project;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.doms.DomB;

/**
 * @author omertripp
 *
 */
@Chord(name="dynamic-loop-java")
public class LoopTest extends DynamicAnalysis {

	private DomB domB;
	private InstrScheme scheme;
	
	@Override
	public InstrScheme getInstrScheme() {
		if (scheme != null) {
			return scheme;
		}
		scheme = new InstrScheme();
		scheme.setEnterAndLeaveLoopEvent();
		return scheme;
	}
	
	@Override
	public void initAllPasses() {
		super.initAllPasses();
		domB = (DomB) Project.getTrgt("B");
	}
	
	@Override
	public void processEnterLoop(int w, int t) {
		String s = domB.toUniqueString(w);
		if (s.contains("V@T")) {
			Messages.log("Entered loop: " + s);
			Messages.log("Loop id: " + w);
		}
	}
	
	@Override
	public void processLeaveLoop(int w, int t) {
		String s = domB.toUniqueString(w);
		if (s.contains("V@T")) {
			Messages.log("Exited loop: " + s);
		}
	}
	
	@Override
	public void processLoopIteration(int w, int t) {
		String s = domB.toUniqueString(w);
		if (s.contains("V@T")) {
			Messages.log("Loop iteration began: " + s);
		}
	}
}

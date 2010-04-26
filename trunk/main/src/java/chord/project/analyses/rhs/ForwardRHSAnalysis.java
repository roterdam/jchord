/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project.analyses.rhs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Return;

import chord.util.tuple.object.Pair;
import chord.program.Program;
import chord.program.Location;
import chord.analyses.alias.ICICG;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Project;
import chord.project.analyses.JavaAnalysis;
import chord.util.ArraySet;

/**
 * Implementation of the Reps-Horwitz-Sagiv algorithm for
 * summary-based forward dataflow analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class ForwardRHSAnalysis<PE extends IEdge, SE extends IEdge>
		extends RHSAnalysis<PE, SE> {
	@Override
	public boolean isForward() { return true; }
}


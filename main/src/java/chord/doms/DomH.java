/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.doms;

import java.util.List;

import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Util.Templates.ListIterator;

import chord.util.tuple.object.Pair;
import chord.project.Chord;
import chord.project.Config;
import chord.project.ClassicProject;
import chord.program.PhantomObjVal;
import chord.program.PhantomClsVal;
import chord.project.analyses.ProgramDom;
import chord.program.Reflect;
import chord.program.Program;

/**
 * Domain of object allocation statements.
 * <p>		
 * The 0th element of this domain (null) is a distinguished		
 * hypothetical object allocation statement that may be used		
 * for various purposes.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author omertripp (omertrip@post.tau.ac.il)
 */
@Chord(
	name = "H",
	consumes = { "M" }
)
public class DomH extends ProgramDom<Object> {
	protected DomM domM;
	protected int lastRealIdx;
	public int getLastRealIdx() {
		return lastRealIdx;
	}
/*
	protected int lastPhantomObjIdx;
	public int getLastPhantomObjIdx() {
		return lastPhantomObjIdx;
	}
*/
	public void init() {
		domM = (DomM) (Config.classic ? ClassicProject.g().getTrgt("M") : consumes[0]);
	}
	public void fill() {
		int numM = domM.size();
		add(null);	
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
					it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
					Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof New || op instanceof NewArray ||
							op instanceof MultiNewArray) 
						add(q);
				}
			}
		}
		lastRealIdx = size() - 1;
		Reflect reflect = Program.g().getReflect();
		processResolvedNewInstSites(reflect.getResolvedObjNewInstSites());
		processResolvedNewInstSites(reflect.getResolvedConNewInstSites());
		processResolvedNewInstSites(reflect.getResolvedAryNewInstSites());
/*
		for (jq_Reference r : program.getReflectInfo().getReflectClasses()) {
			add(new PhantomObjVal(r));
		}
		lastPhantomObjIdx = size() - 1;
		for (jq_Reference r : program.getClasses()) {
			add(new PhantomClsVal(r));
		}
*/
	}
	private void processResolvedNewInstSites(List<Pair<Quad, List<jq_Reference>>> l) {
		for (Pair<Quad, List<jq_Reference>> p : l)
			add(p.val0);
	}
	public String toUniqueString(Object o) {
		if (o instanceof Quad) {
			Quad q = (Quad) o;
			return q.toByteLocStr();
		}
/*
		if (o instanceof PhantomObjVal) {
			jq_Reference r = ((PhantomObjVal) o).r;
			return r.getName() + "@phantom_obj";
		}
		if (o instanceof PhantomClsVal) {
			jq_Reference r = ((PhantomClsVal) o).r;
			return r.getName() + "@phantom_cls";
		}
*/
		assert (o == null);
		return "null";
	}
	public String toXMLAttrsString(Object o) {
		if (o instanceof Quad) {
			Quad q = (Quad) o;
			Operator op = q.getOperator();
			TypeOperand to;
			if (op instanceof New) 
				to = New.getType(q);
			else if (op instanceof NewArray) 
				to = NewArray.getType(q);
			else if (op instanceof MultiNewArray)
				to = MultiNewArray.getType(q);
			else {
				assert (op instanceof Invoke);
				to = null;
			}
			String type = (to != null) ? to.getType().getName() : "null";
			jq_Method m = q.getMethod();
			String file = m.getDeclaringClass().getSourceFileName();
			int line = q.getLineNumber();
			int mIdx = domM.indexOf(m);
			return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
				"Mid=\"M" + mIdx + "\"" + " type=\"" + type + "\"";
		}
		return "";
	}
}

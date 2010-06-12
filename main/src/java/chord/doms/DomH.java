/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.doms;

import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Util.Templates.ListIterator;
import chord.program.ClassHierarchyBuilder;
import chord.project.Chord;
import chord.project.Project;
import chord.project.Properties;
import chord.project.analyses.ProgramDom;
import chord.program.Program;
import chord.util.IndexSet;

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
	consumedNames = { "M", "T" }
)
public class DomH extends ProgramDom<Object> {
	protected DomM domM;
	protected DomT domT;
	protected jq_Method ctnrMethod;
	protected boolean handleReflection;
	protected IndexSet<jq_Class> reflectAllocTypes;
	protected ClassHierarchyBuilder chb;
	protected int lastRealHidx;
	public IndexSet<jq_Class> getReflectAllocTypes() {
		return reflectAllocTypes;
	}
	public int getLastRealHidx() {
		return lastRealHidx;
	}
	@Override
	public void init() {		
		domM = (DomM) Project.getTrgt("M");
		domT = (DomT) Project.getTrgt("T");
		getOrAdd(null);	
		handleReflection = Properties.scopeKind.equals("rta_reflect");
 		if (handleReflection) {
			reflectAllocTypes = new IndexSet<jq_Class>();
			chb = Program.v().getClassHierarchy();
		}
	}
	@Override
	public void fill() {
		int numM = domM.size();
		for (int mIdx = 0; mIdx < numM; mIdx++) {
			jq_Method m = domM.get(mIdx);
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			ctnrMethod = m;
			for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
					it.hasNext();) {
				BasicBlock bb = it.nextBasicBlock();
				for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
					Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof New || op instanceof NewArray) {
						getOrAdd(q);
					} else if (handleReflection && op instanceof CheckCast) {
						jq_Type type = CheckCast.getType(q).getType();
						if (type instanceof jq_Class) {
							String cName = type.getName();
							jq_Class c = (jq_Class) type;
							Set<String> concreteSubs = c.isInterface() ?
								chb.getConcreteImplementors(cName) :
								chb.getConcreteSubclasses(cName);
							if (concreteSubs == null)
								continue;
							for (String dName : concreteSubs) {
								jq_Class d = (jq_Class) jq_Type.parseType(dName);
								reflectAllocTypes.add(d);
							}
						}
					}
				}
			}
		}
		lastRealHidx = size() - 1;
		if (handleReflection) {
			for (jq_Class c : reflectAllocTypes)
				getOrAdd(c);
		}
	}
	@Override
	public int getOrAdd(Object o) {
		if (o instanceof Quad) {
			assert (ctnrMethod != null);
			Quad q = (Quad) o;
			Program.v().mapInstToMethod(q, ctnrMethod);
		}
		return super.getOrAdd(o);
	}
	@Override
	public String toUniqueString(Object o) {
		if (o instanceof Quad) {
			Quad q = (Quad) o;
			return Program.v().toBytePosStr(q);
		}
		if (o instanceof jq_Class) {
			jq_Class c = (jq_Class) o;
			return c.getName();
		}
		assert (o == null);
		return "null";
	}
	@Override
	public String toXMLAttrsString(Object o) {
		if (o instanceof Quad) {
			Quad q = (Quad) o;
			Operator op = q.getOperator();
			String type = (op instanceof New) ? New.getType(q).getType().getName() :
				(op instanceof NewArray) ? NewArray.getType(q).getType().getName() : "unknown";
			jq_Method m = Program.v().getMethod(q);
			String file = Program.getSourceFileName(m.getDeclaringClass());
			int line = Program.getLineNumber(q, m);
			int mIdx = domM.indexOf(m);
			return "file=\"" + file + "\" " + "line=\"" + line + "\" " +
				"Mid=\"M" + mIdx + "\"" + " type=\"" + type + "\"";
		}
		return "";
	}
}

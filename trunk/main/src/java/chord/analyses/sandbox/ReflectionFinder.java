/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.sandbox;

import java.util.Set;
import java.util.HashSet;
import java.util.List;

import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Util.Templates.ListIterator;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;

import chord.program.Program;
import chord.project.Chord;
import chord.project.Project;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;

/**
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "reflect-finder-java"
)
public class ReflectionFinder extends JavaAnalysis {
	public void run() {
		Project.runTask("cipa-0cfa-dlog");
		ProgramRel relReachableM = (ProgramRel) Project.getTrgt("reachableM");
		relReachableM.load();
		Iterable<jq_Method> methods = relReachableM.getAry1ValTuples();
		for (jq_Method m : methods) {
			if (m.isAbstract())
				continue;
			ControlFlowGraph cfg = m.getCFG();
			boolean first = true;
            for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
                BasicBlock bb = it.nextBasicBlock();
                for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
                    Quad q = it2.nextQuad();
					Operator op = q.getOperator();
					if (op instanceof Invoke) {
						jq_Method n = Invoke.getMethod(q).getMethod();
						jq_Class c = n.getDeclaringClass();
						boolean found = false;
            			if (c.getName().equals("java.lang.Class")) {	
							String nName = n.getName().toString();
							String nDesc = n.getDesc().toString();
					    	if (nName.equals("newInstance") &&
									nDesc.equals("()Ljava/lang/Object;")) 
								found = true;
							else if (nName.equals("forName"))
								found = true;
						}
						if (found) {
							if (first) {
								System.out.println("Method: " + m);
								first = false;
							}
							System.out.println("\t" + q.toJavaLocStr());
						}
				}
                }
			}
		}
	}
}


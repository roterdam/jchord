/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.rels;

import joeq.Class.jq_Class;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Initializer;
import joeq.Class.jq_NameAndDesc;

import java.util.Set;
import chord.doms.DomM;
import chord.program.Program;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.IndexSet;

/**
 * Relation containing each tuple (m1,t,m2) such that method m2 is the
 * resolved method of an invokevirtual or invokeinterface call with
 * resolved method m1 on an object of concrete class t.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
	name = "cha",
	sign = "M1,T1,M0:M0xM1_T1"
)
public class RelCHA extends ProgramRel {
	public void fill() {
		DomM domM = (DomM) doms[0];
		IndexSet<jq_Class> classes = Program.v().getPreparedClasses();
		for (jq_Class c : classes) {
			for (jq_InstanceMethod m : c.getDeclaredInstanceMethods()) {
				if (m.isPrivate())
					continue;
				if (m instanceof jq_Initializer)
					continue;
				if (!domM.contains(m))
					continue;
				jq_NameAndDesc nd = m.getNameAndDesc();
				if (c.isInterface()) {
					for (jq_Class d : classes) {
						if (d.isInterface() || d.isAbstract())
							continue;
						if (d.implementsInterface(c)) {
							jq_InstanceMethod n = d.getVirtualMethod(nd);
							assert (n != null);
							if (domM.contains(n))
								add(m, d, n);
						}
					}
				} else {
					for (jq_Class d : classes) {
						if (d.isInterface() || d.isAbstract())
							continue;
						if (d.extendsClass(c)) {
							jq_InstanceMethod n = d.getVirtualMethod(nd);
							assert (n != null);
							if (domM.contains(n))
								add(m, d, n);
						}
					}
				}
			}
		}
	}
}

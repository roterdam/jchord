/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import chord.util.ArraySet;
import chord.util.IndexHashSet;
import chord.util.IndexSet;

import joeq.Class.jq_Class;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_NameAndDesc;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class RTA extends AbstractBootstrapper {
	private static final IndexSet<jq_InstanceMethod> emptySet =
		new ArraySet<jq_InstanceMethod>(0);
	private IndexHashSet<jq_Class> reachableAllocClasses =
		new IndexHashSet<jq_Class>();
	public IndexSet<jq_InstanceMethod> getTargetsOfVirtualCall(jq_InstanceMethod m) {
		IndexSet<jq_InstanceMethod> targets = null;
		jq_Class c = m.getDeclaringClass();
		jq_NameAndDesc nd = m.getNameAndDesc();
		if (c.isInterface()) {
			for (jq_Class d : reachableAllocClasses) {
				assert (!d.isInterface());
				assert (!d.isAbstract());
				if (d.implementsInterface(c)) {
					jq_InstanceMethod n = d.getVirtualMethod(nd);
					assert (n != null);
					if (targets == null)
						targets = new ArraySet<jq_InstanceMethod>();
					targets.add(n);
				}
			}
		} else {
			for (jq_Class d : reachableAllocClasses) {
				assert (!d.isInterface());
				assert (!d.isAbstract());
				if (d.extendsClass(c)) {
					jq_InstanceMethod n = d.getVirtualMethod(nd);
					assert (n != null);
					if (targets == null)
						targets = new ArraySet<jq_InstanceMethod>();
					targets.add(n);
				}
			}
		}
		return (targets != null) ? targets : emptySet;
	}
	protected void processNew(jq_Class c) {
		if (reachableAllocClasses.add(c)) {
			if (DEBUG) System.out.println("Setting repeat");
			repeat = true;
		}
	}
}

/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.util.List;

import chord.util.IndexSet;
 
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DynamicScope extends Scope {
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Reference> getNewInstancedClasses() {
		return null;
	}
	public IndexSet<jq_Method> getMethods() {
		return methods;
	}
	public void build() {
		if (isBuilt)
			return;
		List<String> classNames = Program.getDynamicallyLoadedClasses();
		IndexSet<jq_Reference> classes = Program.loadClasses(classNames);
		methods = new IndexSet<jq_Method>();
		for (jq_Reference r : classes) {
			if (r instanceof jq_Array)
				continue;
			jq_Class c = (jq_Class) r;
			for (jq_Method m : c.getDeclaredStaticMethods()) 
				methods.add(m);
			for (jq_Method m : c.getDeclaredInstanceMethods()) 
				methods.add(m);
		}
		isBuilt = true;
	}
}

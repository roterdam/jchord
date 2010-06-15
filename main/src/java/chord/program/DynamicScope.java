/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.util.List;

import chord.util.IndexSet;
 
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DynamicScope implements IScope {
	private boolean isBuilt = false;
	private IndexSet<jq_Class> classes;
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Class> getClasses() {
		return classes;
	}
	public IndexSet<jq_Class> getNewInstancedClasses() {
		return null;
	}
	public IndexSet<jq_Method> getMethods() {
		return methods;
	}
	public void build() {
		if (isBuilt)
			return;
		List<String> classNames = Program.getDynamicallyLoadedClasses();
		classes = Program.loadClasses(classNames);
		methods = new IndexSet<jq_Method>();
		for (jq_Class c : classes) {
			for (jq_Method m : c.getDeclaredStaticMethods()) 
				methods.add(m);
			for (jq_Method m : c.getDeclaredInstanceMethods()) 
				methods.add(m);
		}
		isBuilt = true;
	}
}

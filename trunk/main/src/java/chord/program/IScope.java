/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import chord.util.IndexSet;

/**
 * Generic interface for algorithms computing analysis scope
 * (reachable classes and methods).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IScope {
	public IndexSet<jq_Method> getMethods();
	public IndexSet<jq_Reference> getReflectClasses();
}

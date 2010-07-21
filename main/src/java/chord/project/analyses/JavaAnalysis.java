/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses;

/**
 * Generic implementation of a Java task (a program analysis
 * expressed in Java).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class JavaAnalysis implements ITask {
    protected String name;
    public void setName(String name) {
        assert (name != null);
        assert (this.name == null);
        this.name = name;
    }
    public String getName() {
        return name;
    }
	public void run() {
		throw new RuntimeException("Analysis '" + name +
			"' must override method run().");
	}
	public String toString() {
		return name;
	}
}

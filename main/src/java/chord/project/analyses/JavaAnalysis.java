/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses;

import chord.project.IStepCollection;
import chord.project.Messages;
import chord.project.ITask;

/**
 * Generic implementation of a Java task (a program analysis
 * expressed in Java).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class JavaAnalysis implements ITask {
	private static final String ERROR = "Analysis '%s' must override method '%s'";
    protected String name;
    protected Object[] consumes;
    protected Object[] produces;
    protected Object[] controls;
	@Override
    public void setName(String name) {
        assert (name != null);
        assert (this.name == null);
        this.name = name;
    }
	@Override
    public String getName() {
        return name;
    }
	@Override
	public void run() {
		Messages.fatal(ERROR, name, "run()");
	}
	@Override
	public void run(Object ctrl, IStepCollection sc) {
		// TODO
		Messages.fatal(ERROR, name, "run(Object, IStepCollection)");
	}
	@Override
	public String toString() {
		return name;
	}
}

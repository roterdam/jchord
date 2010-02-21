/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project.analyses;

import chord.project.Project;

/**
 * Specification of a program analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface ITask {
	/**
	 * Sets the name of this program analysis.
	 * 
	 * @param	name	A name unique across all program analyses
	 *			included in a Chord project.
	 */
    public void setName(String name);
    /**
     * Provides the name of this program analysis.
     * 
     * @return	The name of this program analysis.
     */
    public String getName();
	/**
	 * Executes this program analysis.
	 * 
	 * This method must usually not be called directly from another
	 * program analysis that wishes to execute this program analysis.
	 * The correct way to achieve this is to call
	 * {@link chord.project.Project#runTask(String)} or
	 * {@link chord.project.Project#runTask(ITask)}, providing this
	 * program analysis either by its name or its object.
	 */
	public void run();
}

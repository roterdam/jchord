/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project;

import java.util.List;

import CnCHJ.api.ItemCollection;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IStepCollection {
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

    public void run(Object ctrl);
 
	public void setConsumedDataCollections(List<ItemCollection> c);

	public List<ItemCollection> getConsumedDataCollections();

	public void setProducedDataCollections(List<ItemCollection> c);

	public List<ItemCollection> getProducedDataCollections();

	public void setProducedCtrlCollections(List<ICtrlCollection> c);

	public List<ICtrlCollection> getProducedCtrlCollections();

	public void setPrescribingCollection(ICtrlCollection c);

	public ICtrlCollection getPrescribingCollection();
}

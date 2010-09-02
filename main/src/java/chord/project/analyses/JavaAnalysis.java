/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses;

import java.util.List;

import CnCHJ.api.ItemCollection;

import chord.project.ICtrlCollection;
import chord.project.IDataCollection;
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
	private static final String UNDEFINED_RUN = "ERRROR: Analysis '%s' must override method 'run()'";
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
		Messages.fatal(UNDEFINED_RUN, name);
	}
	@Override
	public void run(Object ctrl, IStepCollection sc) {
        List<IDataCollection> cdcList = sc.getConsumedDataCollections();
        int n = cdcList.size();
        consumes = new Object[n];
        for (int i = 0; i < n; i++) {
            IDataCollection cdc = cdcList.get(i);
            ItemCollection cic = cdc.getItemCollection();
            consumes[i] = cic.Get(ctrl);
        }
        run();
        List<IDataCollection> pdcList = sc.getProducedDataCollections();
        int m = pdcList.size();
        for (int i = 0; i < m; i++) {
        	IDataCollection pdc = pdcList.get(i);
        	ItemCollection pic = pdc.getItemCollection();
        	Object o = produces[i];
        	if (o != null)
        		pic.Put(ctrl, o);
        }
        List<ICtrlCollection> pccList = sc.getProducedCtrlCollections();
        int k = pccList.size();
        for (int i = 0; i < k; i++) {
        	ICtrlCollection pcc = pccList.get(i);
        	Object o = controls[i];
        	if (o != null)
        		pcc.Put(o);
        }
	}
	@Override
	public String toString() {
		return name;
	}
}

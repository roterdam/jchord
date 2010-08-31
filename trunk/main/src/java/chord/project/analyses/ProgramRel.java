/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses;

import java.util.List;

import chord.bddbddb.Rel;
import chord.program.visitors.IClassVisitor;
import chord.project.Config;
import chord.project.ICtrlCollection;
import chord.project.IStepCollection;
import chord.project.VisitorHandler;
import chord.util.ChordRuntimeException;
import chord.project.Messages;
import chord.project.ITask;

import CnCHJ.api.ItemCollection;

/**
 * Generic implementation of a program relation (a specialized kind
 * of Java task).
 * <p>
 * A program relation is a relation over one or more program domains.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ProgramRel extends Rel implements ITask {
	private static final String SKIP_TUPLE =
		"WARN: Skipping a tuple from relation '%s' as element '%s' was not found in domain '%s'.";
	protected Object[] consumes;
	@Override
	public void run() {
		zero();
		init();
		fill();
		save();
	}
	@Override
	public void run(Object ctrl, IStepCollection sc) {
		List<ItemCollection> cdcList = sc.getConsumedDataCollections();
		int n = cdcList.size();
		consumes = new Object[n];
		for (int i = 0; i < n; i++) {
			ItemCollection cdc = cdcList.get(i);
			consumes[i] = cdc.Get(ctrl);
		}
		// TODO: initialize this rel (call setSign and setDoms)
		run();
		List<ICtrlCollection> pccList = sc.getProducedCtrlCollections();
		assert (pccList.size() == 0);
		List<ItemCollection> pdcList = sc.getProducedDataCollections();
		assert (pdcList.size() == 1);
		ItemCollection pdc = pdcList.get(0);
		pdc.Put(ctrl, this);
	}
	public void init() { }
	public void save() {
		if (Config.verbose > 1)
			System.out.println("SAVING rel " + name + " size: " + size());
		super.save(Config.bddbddbWorkDirName);
	}
	public void load() {
		super.load(Config.bddbddbWorkDirName);
	}
	public void fill() {
		if (this instanceof IClassVisitor) {
			VisitorHandler vh = new VisitorHandler(this);
			vh.visitProgram();
		} else {
			throw new ChordRuntimeException("Relation '" + name +
				"' must override method fill().");
		}
	}
	public void print() {
		super.print(Config.outDirName);
	}
	public String toString() {
		return name;
	}
	public void skip(Object elem, ProgramDom dom) {
		Messages.log(SKIP_TUPLE, getClass().getName(), elem, dom.getClass().getName());
	}
}

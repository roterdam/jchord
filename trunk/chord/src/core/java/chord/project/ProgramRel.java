/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import chord.bddbddb.Rel;
import chord.visitors.IClassVisitor;

/**
 * Generic implementation of a program relation (a specialized kind
 * of Java task).
 * <p>
 * A program relation is a relation over one or more program domains.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ProgramRel extends Rel implements ITask {
	public void run() {
		zero();
		init();
		fill();
		save();
	}
	public void init() { }
	public void save() {
		System.out.println("SAVING rel " + name + " size: " + size());
		super.save(Properties.outDirName);
		Project.setTrgtDone(this);
	}
	public void load() {
		super.load(Properties.outDirName);
	}
	public void fill() {
		if (this instanceof IClassVisitor) {
			VisitorHandler vh = new VisitorHandler(this);
			vh.visitProgram();
		} else {
			throw new RuntimeException("Relation '" + name +
				"' must override method fill().");
		}
	}
	public String toString() {
		return name;
	}
}

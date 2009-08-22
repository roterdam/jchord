/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.List;
import java.util.ArrayList;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class Executor {
	private final List<Thread> tasks;
	private final boolean serial;
	public Executor(boolean serial) {
		this.serial = serial;
		tasks = serial ? null : new ArrayList<Thread>();
	}
	public void execute(Runnable task) {
		if (tasks == null)
			task.run();
		else {
			Thread t = new Thread(task);
			tasks.add(t);
			t.start();
		}
	}
	public void waitForCompletion() throws InterruptedException {
		if (tasks == null)
			return;
		for (Thread t : tasks)
			t.join();
		tasks.clear();
	}
}

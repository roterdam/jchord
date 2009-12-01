/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class Executor { // TODO: MIGHT BE REPLACED BY USING java.util.concurrent, WHICH ALLOWS MUCH BETTER CONCURRENCY CONTROL MUCH MORE SCALABLE LOCKING.

	/**
	 * Submitted tasks. This field is {@code null} if the executor is serial.
	 */
	private final List<Thread> tasks;

	/**
	 * A flag indicating that submitted tasks will be executed serially.
	 */
	private final boolean serial;

	/**
	 * Creates a new executor.
	 *
	 * @param serial a flag indicating that submitted tasks will be executed serially.
	 */
	public Executor(final boolean serial) {
		this.serial = serial;
		tasks = serial ? null : new ArrayList<Thread>();
	}

	/**
	 * Executes given runnable task. If the executor is serial, this method blocks
	 * until the submitted task is completed. If the executor is not serial, this
	 * method only starts a new thread for the submitted task and returns immediately.
	 *
	 * @param task a task to be executed.
	 * @throws IllegalArgumentException if {@code task} is {@code null}.
	 */
	public synchronized void execute(final Runnable task) {
		if (task == null) {
			throw new IllegalArgumentException();
		}
		if (serial)
			task.run();
		else {
			final Thread t = new Thread(task);
			tasks.add(t);
			t.start();
		}
	}

	/**
	 * Waits for completion of all submitted tasks and returns afterwards.
	 *
	 * @throws InterruptedException if any occurs during waiting for tasks completion.
	 */
	public synchronized void waitForCompletion() throws InterruptedException {
		if (!serial) {
			try {
				for (final Thread t : tasks) {
					t.join();
				}
			} finally {
				tasks.clear();
			}
		}
	}

}

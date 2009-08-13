/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.project;

import chord.util.IntBuffer;
import java.io.IOException;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Runtime {
	private static int maxCount;
	private static boolean[][] traceStk;
	private static boolean[] traceStkTop;
	private static IntBuffer buffer;
	private static boolean trace = false;
	public static int[] threadObjs;
	public static int numThreads;
	public static int[][] numCallsToMeth;
	public static int[][] numItersOfLoop;
	public static final int NUM_INIT_THREADS = 10;
	public synchronized static void createThread(int tObj) {
		if (trace) {
			trace = false;
			if (numThreads == threadObjs.length) {
				int[] newThreadObjs = new int[2 * numThreads];
				System.arraycopy(threadObjs, 0, newThreadObjs, 0, numThreads);
				threadObjs = newThreadObjs;
				threadObjs[numThreads] = tObj;
				numThreads++;
			}
			trace = true;
		}
	}
	public synchronized static void methodEnter(int mIdx) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			try {
				buffer.put(EventKind.METHOD_ENTER);
				buffer.put(tIdx);
				buffer.put(mIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}

	public synchronized static void methodLeave(int mIdx) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			try {
				buffer.put(EventKind.METHOD_LEAVE);
				buffer.put(tIdx);
				buffer.put(mIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}

	public synchronized static void befNew(int hIdx) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			try {
				buffer.put(EventKind.BEF_NEW);
				buffer.put(tIdx);
				buffer.put(hIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aftNew(int hIdx, Object o) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(EventKind.AFT_NEW);
				buffer.put(tIdx);
				buffer.put(hIdx);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void newArray(int hIdx, Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(EventKind.NEW_ARRAY);
				buffer.put(hIdx);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void instFldRd(int eIdx, Object b,
			int fIdx) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			try {
				buffer.put(EventKind.INST_FLD_RD);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(fIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void instFldWr(int eIdx, Object b,
			int fIdx, Object r) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(EventKind.INST_FLD_WR);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(fIdx);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aryElemRd(int eIdx, Object b,
			int idx) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			try {
				buffer.put(EventKind.ARY_ELEM_RD);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(idx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aryElemWr(int eIdx, Object b,
			int idx, Object r) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(EventKind.ARY_ELEM_WR);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(idx);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void statFldWr(Object r) {
		if (trace) {
			trace = false;
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(EventKind.STAT_FLD_WR);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void acqLock(int lIdx, Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(EventKind.ACQ_LOCK);
				buffer.put(lIdx);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadStart(Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(EventKind.THREAD_START);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadSpawn(Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(EventKind.THREAD_SPAWN);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	private static int getTid() {
		Thread t = Thread.currentThread();
		int tObj = System.identityHashCode(t);
		for (int i = 0; i < numThreads; i++) {
			int tObj2 = threadObjs[i];
			if (tObj2 == tObj)
				return i;
		}
		throw new RuntimeException();
	}
	public synchronized static void methodEnterCheck(int mIdx) {
		if (trace) {
			trace = false;
			int tId = getTid();

			trace = true;
		}
	}
	public synchronized static void methodLeaveCheck(int mIdx) {
		if (trace) {
			trace = false;
			int tId = getTid();
			
			trace = true;
		}
	}
	public synchronized static void LoopEnterCheck(int wIdx) {
		if (trace) {
			trace = false;
			int tId = getTid();
			trace = true;
		}
	}
	public synchronized static void LoopLeaveCheck(int wIdx) {
		if (trace) {
			trace = false;
			int tId = getTid();
			trace = true;
		}
	}
	public synchronized static void open(String fileName,
			int numMeths, int numLoops, int c) {
		try {
			buffer = new IntBuffer(1024, fileName, false);
			threadObjs = new int[NUM_INIT_THREADS];
			numCallsToMeth = new int[numMeths][];
			numItersOfLoop = new int[numLoops][];
			maxCount = c;
		} catch (IOException ex) { throw new RuntimeException(ex); }
		trace = true;
	}
	public synchronized static void close() {
		trace = false;
		try {
			buffer.flush();
		} catch (IOException ex) { throw new RuntimeException(ex); }
	}
}


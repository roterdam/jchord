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
	private static IntBuffer buffer;
	private static boolean trace = false;

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
	public synchronized static void open(String fileName) {
		try {
			buffer = new IntBuffer(1024, fileName, false);
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


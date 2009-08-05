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

	public synchronized static void methodEnter() {
		if (trace) {
			trace = false;
		}
		trace = true;
	}

	public synchronized static void methodLeave() {
		if (trace) {
			trace = false;
		}
		trace = true;
	}

	public synchronized static void befNewInst(int hIdx) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			try {
				buffer.put(InstKind.BEF_NEW_INST);
				buffer.put(tIdx);
				buffer.put(hIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aftNewInst(int hIdx, Object o) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			int tIdx = System.identityHashCode(t);
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(InstKind.AFT_NEW_INST);
				buffer.put(tIdx);
				buffer.put(hIdx);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void newArrayInst(int hIdx, Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(InstKind.NEW_ARRAY_INST);
				buffer.put(hIdx);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void instFldRdInst(int eIdx, Object b,
			int fIdx) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			try {
				buffer.put(InstKind.INST_FLD_RD_INST);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(fIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void instFldWrInst(int eIdx, Object b,
			int fIdx, Object r) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(InstKind.INST_FLD_WR_INST);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(fIdx);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aryElemRdInst(int eIdx, Object b,
			int idx) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			try {
				buffer.put(InstKind.ARY_ELEM_RD_INST);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(idx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aryElemWrInst(int eIdx, Object b,
			int idx, Object r) {
		if (trace) {
			trace = false;
			int bIdx = System.identityHashCode(b);
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(InstKind.ARY_ELEM_WR_INST);
				buffer.put(eIdx);
				buffer.put(bIdx);
				buffer.put(idx);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void statFldWrInst(int fIdx, Object r) {
		if (trace) {
			trace = false;
			int rIdx = System.identityHashCode(r);
			try {
				buffer.put(InstKind.STAT_FLD_WR_INST);
				buffer.put(fIdx);
				buffer.put(rIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void acqLockInst(int lIdx, Object o) {
		if (trace) {
			trace = false;
			int oIdx = System.identityHashCode(o);
			try {
				buffer.put(InstKind.ACQ_LOCK_INST);
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
				buffer.put(InstKind.THREAD_START_INST);
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
				buffer.put(InstKind.THREAD_SPAWN_INST);
				buffer.put(oIdx);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public static void open(String fileName) {
		try {
			buffer = new IntBuffer(1024, fileName, false);
		} catch (IOException ex) { throw new RuntimeException(ex); }
		trace = true;
	}
	public static void close() {
		trace = false;
		try {
			buffer.flush();
		} catch (IOException ex) { throw new RuntimeException(ex); }
	}
}


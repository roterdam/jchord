/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.project;

import chord.instr.EventKind;
import chord.instr.InstrScheme;
import chord.instr.InstrScheme.EventFormat;
import chord.util.ByteBufferedFile;
import chord.util.WeakIdentityHashMap;

import java.io.IOException;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Runtime {
/*
	private static int instrBound;
	private static boolean[][] traceStk;
	private static boolean[] traceStkTop;
	public static int[] threadObjs;
	public static int numThreads;
	public static int[][] numCallsToMeth;
	public static int[][] numItersOfLoop;
	public static final int NUM_INIT_THREADS = 10;
*/
	private static ByteBufferedFile buffer;
	// note: use currentId == 0 for null and currentId == 1 for hypothetical
	// lone object of a hypothetical class all of whose instance fields are
	// static fields in other real classes.
	private static InstrScheme scheme;
    private static int currentId = 2;
    private static WeakIdentityHashMap objmap;
	private static boolean trace = false;
/*
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
*/
    private static int getObjectId(Object o) {
    	if (o == null)
    		return 0;
        Object val = objmap.get(o);
        if (val == null) {
            val = currentId++;
            objmap.put(o, val);
        }
        return (Integer) val;
    }
    public static long getPrimitiveId(int oId, int fId) {
        // We must add 1 below so that we never assign to a field an
        // identifier smaller than (1 << 32).
    	long l = oId + 1;
    	l = l << 32;
    	return l + fId;
    }
	public synchronized static void enterMethod(int mId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				buffer.putByte(EventKind.ENTER_METHOD);
				if (ef.hasMid())
					buffer.putInt(mId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void leaveMethod(int mId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				buffer.putByte(EventKind.LEAVE_METHOD);
				if (ef.hasMid())
					buffer.putInt(mId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void befNew(int hId) {
		if (trace) {
			trace = false;
			try {
				buffer.putByte(EventKind.BEF_NEW);
				buffer.putInt(hId);
				int tId = getObjectId(Thread.currentThread());
				buffer.putInt(tId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aftNew(int hId, Object o) {
		if (trace) {
			trace = false;
			try {
				buffer.putByte(EventKind.AFT_NEW);
				buffer.putInt(hId);
				int tId = getObjectId(Thread.currentThread());
				buffer.putInt(tId);
				int oId = getObjectId(o);
				buffer.putInt(oId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void newArray(int hId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				buffer.putByte(EventKind.NEW_ARRAY);
				if (hId != -1)
					buffer.putInt(hId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getstaticPrimitive(int eId, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
				buffer.putByte(EventKind.GETSTATIC_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != -1)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getstaticReference(int eId, int fId,
			Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
				buffer.putByte(EventKind.GETSTATIC_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != -1)
					buffer.putInt(fId);
				if (ef.hasOid()) {
					int tId = getObjectId(o);
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putstaticPrimitive(int eId, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
				buffer.putByte(EventKind.PUTSTATIC_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != -1)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putstaticReference(int eId, int fId,
			Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
				buffer.putByte(EventKind.PUTSTATIC_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != -1)
					buffer.putInt(fId);
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getfieldPrimitive(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
				buffer.putByte(EventKind.GETFIELD_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != -1)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getfieldReference(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
				buffer.putByte(EventKind.GETFIELD_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != -1)
					buffer.putInt(fId);
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putfieldPrimitive(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
				buffer.putByte(EventKind.PUTFIELD_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != -1)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putfieldReference(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
				buffer.putByte(EventKind.PUTFIELD_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != -1)
					buffer.putInt(fId);
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aloadPrimitive(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
				buffer.putByte(EventKind.ALOAD_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIid())
					buffer.putInt(iId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aloadReference(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
				buffer.putByte(EventKind.ALOAD_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIid())
					buffer.putInt(iId);
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void astorePrimitive(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
				buffer.putByte(EventKind.ASTORE_PRIMITIVE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIid())
					buffer.putInt(iId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void astoreReference(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
				buffer.putByte(EventKind.ASTORE_REFERENCE);
				if (eId != -1)
					buffer.putInt(eId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBid()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIid())
					buffer.putInt(iId);
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadStart(int pId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
				buffer.putByte(EventKind.THREAD_START);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadJoin(int pId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
				buffer.putByte(EventKind.THREAD_JOIN);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasOid()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void acquireLock(int pId, Object l) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
				buffer.putByte(EventKind.ACQUIRE_LOCK);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasLid()) {
					int lId = getObjectId(l);
					buffer.putInt(lId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void releaseLock(int pId, Object l) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
				buffer.putByte(EventKind.RELEASE_LOCK);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasLid()) {
					int lId = getObjectId(l);
					buffer.putInt(lId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void wait(int pId, Object l) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
				buffer.putByte(EventKind.WAIT);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasLid()) {
					int lId = getObjectId(l);
					buffer.putInt(lId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void notify(int pId, Object l) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
				buffer.putByte(EventKind.NOTIFY);
				if (pId != -1)
					buffer.putInt(pId);
				if (ef.hasTid()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasLid()) {
					int lId = getObjectId(l);
					buffer.putInt(lId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void enterBasicBlock(int bId) {
		if (trace) {
			trace = false;
			try {
				buffer.putByte(EventKind.ENTER_BB);
				buffer.putInt(bId);
				int tId = getObjectId(Thread.currentThread());
				buffer.putInt(tId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	
	}
	public synchronized static void test() { }
	public synchronized static void enterMethodCheck(int mId) {
		if (trace) {
			trace = false;
			trace = true;
		}
	}
	public synchronized static void leaveMethodCheck(int mId) {
		if (trace) {
			trace = false;
			trace = true;
		}
	}
	public synchronized static void enterLoopCheck(int wId) {
		if (trace) {
			trace = false;
			trace = true;
		}
	}
	public synchronized static void leaveLoopCheck(int wId) {
		if (trace) {
			trace = false;
			trace = true;
		}
	}
	public static void setInstrScheme(InstrScheme s) {
		scheme = s;
	}
	public synchronized static void open(String traceFileName,
			String instrSchemeFileName, int numMeths, int numLoops, int ib) {
		try {
			buffer = new ByteBufferedFile(1024, traceFileName, false);
		    objmap = new WeakIdentityHashMap();
			scheme = InstrScheme.load(instrSchemeFileName);
			// threadObjs = new int[NUM_INIT_THREADS];
			// numCallsToMeth = new int[numMeths][];
			// numItersOfLoop = new int[numLoops][];
			// instrBound = ib;
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

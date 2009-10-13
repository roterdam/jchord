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
	static class LoopFrame {
		int wId;		// unique loop id (across all loops)
		// number of calls to containing method of this loop
		// at the time this frame was created
		int numCalls;
		// current number of iterations of this loop; stops
		// incrementing beyond itersBound
		int numIters;
		public LoopFrame(int wId, int numCalls, int numIters) {
			this.wId = wId;
			this.numCalls = numCalls;
			this.numIters = numIters;
		}
	}
	public static final int MISSING_FIELD_VAL = -1;
	public static final int UNKNOWN_FIELD_VAL = -2;
	private static int callsBound;
	private static int itersBound;
	private static int numMeths;
	private static boolean alwaysInstr;
	private static class ThreadInfo {
		private final static int INITIAL_STK_SIZE = 100; 
		public int numCallsToMeth[];
		public int numMethsExceedingCallsBound;
		public boolean noInstr;
		public LoopFrame stk[];
		private int stkSize;
		private int stkTopPos;
		public ThreadInfo() {
			numCallsToMeth = new int[numMeths];
			numMethsExceedingCallsBound = 0;
			noInstr = false;
			stk = new LoopFrame[INITIAL_STK_SIZE];
			stkSize = INITIAL_STK_SIZE;
			stkTopPos = -1;
		}
		public void pop() {
			if (stkTopPos < 0) throw new RuntimeException();
			--stkTopPos;
		}
		public LoopFrame top() {
			return stkTopPos == -1 ? null : stk[stkTopPos];
		}
		public void push(int wId, int numCalls) {
			LoopFrame frame = new LoopFrame(wId, numCalls, 0);
			if (stkTopPos == stkSize - 1) {
				int newStkSize = stkSize * 2;
				LoopFrame newStk[] = new LoopFrame[newStkSize];
				System.arraycopy(stk, 0, newStk, 0, stkSize);
				stk = newStk;
				stkSize = newStkSize;
			}
			stk[++stkTopPos] = frame;
		}
	}
	private static ByteBufferedFile buffer;
	// note: use currentId == 0 for null and currentId == 1 for hypothetical
	// lone object of a hypothetical class all of whose instance fields are
	// static fields in other real classes.
	private static InstrScheme scheme;
    private static int currentId = 2;
    private static WeakIdentityHashMap objmap;
	private static WeakIdentityHashMap thrmap;
	private static boolean trace = false;

	private static boolean hasEnterAndLeaveMethodEvent;
	private static boolean hasEnterAndLeaveLoopEvent;

    public static int getObjectId(Object o) {
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
	// befNew event is present => h,t,o present
	public synchronized static void befNewEvent(int hId) {
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
	// aftNew event is present => h,t,o present
	public synchronized static void aftNewEvent(int hId, Object o) {
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
	public synchronized static void newEvent(int hId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				buffer.putByte(EventKind.NEW);
				if (hId != MISSING_FIELD_VAL)
					buffer.putInt(hId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void newArrayEvent(int hId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				buffer.putByte(EventKind.NEW_ARRAY);
				if (hId != MISSING_FIELD_VAL)
					buffer.putInt(hId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getstaticPrimitiveEvent(int eId, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
				buffer.putByte(EventKind.GETSTATIC_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getstaticReferenceEvent(int eId, int fId,
			Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
				buffer.putByte(EventKind.GETSTATIC_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putstaticPrimitiveEvent(int eId, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
				buffer.putByte(EventKind.PUTSTATIC_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putstaticReferenceEvent(int eId, int fId,
			Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
				buffer.putByte(EventKind.PUTSTATIC_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getfieldPrimitiveEvent(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
				buffer.putByte(EventKind.GETFIELD_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void getfieldReferenceEvent(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
				buffer.putByte(EventKind.GETFIELD_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putfieldPrimitiveEvent(int eId,
			Object b, int fId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
				buffer.putByte(EventKind.PUTFIELD_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void putfieldReferenceEvent(int eId,
			Object b, int fId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
				buffer.putByte(EventKind.PUTFIELD_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (fId != MISSING_FIELD_VAL)
					buffer.putInt(fId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aloadPrimitiveEvent(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
				buffer.putByte(EventKind.ALOAD_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIdx())
					buffer.putInt(iId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void aloadReferenceEvent(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
				buffer.putByte(EventKind.ALOAD_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIdx())
					buffer.putInt(iId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void astorePrimitiveEvent(int eId,
			Object b, int iId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
				buffer.putByte(EventKind.ASTORE_PRIMITIVE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIdx())
					buffer.putInt(iId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void astoreReferenceEvent(int eId,
			Object b, int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
				buffer.putByte(EventKind.ASTORE_REFERENCE);
				if (eId != MISSING_FIELD_VAL)
					buffer.putInt(eId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasBaseObj()) {
					int bId = getObjectId(b);
					buffer.putInt(bId);
				}
				if (ef.hasIdx())
					buffer.putInt(iId);
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadStartEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
				buffer.putByte(EventKind.THREAD_START);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void threadJoinEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
				buffer.putByte(EventKind.THREAD_JOIN);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void acquireLockEvent(int lId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
				buffer.putByte(EventKind.ACQUIRE_LOCK);
				if (lId != MISSING_FIELD_VAL) {
					buffer.putInt(lId);
				}
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void releaseLockEvent(int rId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
				buffer.putByte(EventKind.RELEASE_LOCK);
				if (rId != MISSING_FIELD_VAL) {
					buffer.putInt(rId);
				}
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void waitEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
				buffer.putByte(EventKind.WAIT);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void notifyEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
				buffer.putByte(EventKind.NOTIFY);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void methodCallBefEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				buffer.putByte(EventKind.METHOD_CALL_BEF);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void methodCallAftEvent(int iId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				buffer.putByte(EventKind.METHOD_CALL_AFT);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void returnPrimitiveEvent(int pId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_PRIMITIVE);
				buffer.putByte(EventKind.RETURN_PRIMITIVE);
				if (pId != MISSING_FIELD_VAL)
					buffer.putInt(pId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	
	}
	public synchronized static void returnReferenceEvent(int pId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_REFERENCE);
				buffer.putByte(EventKind.RETURN_REFERENCE);
				if (pId != MISSING_FIELD_VAL)
					buffer.putInt(pId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	
	}
	public synchronized static void explicitThrowEvent(int pId, Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.EXPLICIT_THROW);
				buffer.putByte(EventKind.EXPLICIT_THROW);
				if (pId != MISSING_FIELD_VAL)
					buffer.putInt(pId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	
	}
	public synchronized static void implicitThrowEvent(Object o) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.IMPLICIT_THROW);
				buffer.putByte(EventKind.IMPLICIT_THROW);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
				if (ef.hasObj()) {
					int oId = getObjectId(o);
					buffer.putInt(oId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void quadEvent(int pId) {
		if (trace) {
			trace = false;
			try {
				if (!alwaysInstr) {
					Object t = Thread.currentThread();
					ThreadInfo info = (ThreadInfo) thrmap.get(t);
					if (info == null || info.noInstr) {
						trace = true;
						return;
					}
				}
				buffer.putByte(EventKind.QUAD);
				buffer.putInt(pId);
				int tId = getObjectId(Thread.currentThread());
				buffer.putInt(tId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void basicBlockEvent(int bId) {
		if (trace) {
			trace = false;
			try {
				if (!alwaysInstr) {
					Object t = Thread.currentThread();
					ThreadInfo info = (ThreadInfo) thrmap.get(t);
					if (info == null || info.noInstr) {
						trace = true;
						return;
					}
				}
				buffer.putByte(EventKind.BASIC_BLOCK);
				buffer.putInt(bId);
				int tId = getObjectId(Thread.currentThread());
				buffer.putInt(tId);
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void enterMethodEvent(int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = null;
			if (thrmap != null) {
				info = (ThreadInfo) thrmap.get(t);
				if (info == null) {
					info = new ThreadInfo();
					thrmap.put(t, info);
				}
			}
			boolean genEvent = false;
			if (callsBound == 0) {
				assert (hasEnterAndLeaveMethodEvent);
				// if bound was 0 but still landed here means event must be
				// generated unconditionally
				genEvent = true;
			} else {
				if (info.numCallsToMeth[mId]++ == callsBound) {
					info.numMethsExceedingCallsBound++;
					info.noInstr = true;
				} else if (hasEnterAndLeaveMethodEvent && !info.noInstr)
					genEvent = true;
			}
			if (genEvent) {
				try {
					buffer.putByte(EventKind.ENTER_METHOD);
					buffer.putInt(mId);
					int tId = getObjectId(t);
					buffer.putInt(tId);
				} catch (IOException ex) { throw new RuntimeException(ex); }
			}
			trace = true;
		}
	}
	public synchronized static void leaveMethodEvent(int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			boolean genEvent = false;
			if (callsBound == 0) {
				assert (hasEnterAndLeaveMethodEvent);
				// if bound was 0 but still landed here means event must be
				// generated unconditionally
				genEvent = true;
			} else {
				ThreadInfo info = (ThreadInfo) thrmap.get(t);
				if (--info.numCallsToMeth[mId] == callsBound) {
					if (--info.numMethsExceedingCallsBound == 0) {
						assert (info.noInstr);
						info.noInstr = false;
					}
				} else if (hasEnterAndLeaveMethodEvent && !info.noInstr)
					genEvent = true;
			}
			if (genEvent) {
				try {
					buffer.putByte(EventKind.LEAVE_METHOD);
					buffer.putInt(mId);
					int tId = getObjectId(t);
					buffer.putInt(tId);
				} catch (IOException ex) { throw new RuntimeException(ex); }
			}
			trace = true;
		}
	}
	public synchronized static void enterLoopEvent(int wId, int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info == null) {
				trace = true;
				return;
			}
			boolean genEvent = false;
			if (itersBound == 0) {
				assert (hasEnterAndLeaveLoopEvent);
				int n = info.numCallsToMeth[mId];
				LoopFrame topFrame = info.top();
				if (topFrame == null || topFrame.wId != wId || topFrame.numCalls != n) {
					// entering loop from outside instead of from back edge
					info.push(wId, n);
					genEvent = true;
				}
			} else {
				if (info.noInstr) {
					trace = true;
					return;
				}
				int n = info.numCallsToMeth[mId];
				LoopFrame topFrame = info.top();
				if (topFrame != null && topFrame.wId == wId && topFrame.numCalls == n) {
					// entering loop from back edge instead of from outside
					if (++topFrame.numIters == itersBound)
						info.noInstr = true;
				} else {
					// entering loop from outside instead of from back edge
					info.push(wId, n);
					if (hasEnterAndLeaveLoopEvent)
						genEvent = true;
				}
			}
			if (genEvent) {
				try {
					buffer.putByte(EventKind.ENTER_LOOP);
					buffer.putInt(wId);
					int tId = getObjectId(t);
					buffer.putInt(tId);
				} catch (IOException ex) { throw new RuntimeException(ex); }
			}
			trace = true;
		}
	}
	public synchronized static void leaveLoopEvent(int wId, int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info == null) {
				trace = true;
				return;
			}
			boolean genEvent = false;
			if (itersBound == 0) {
				assert (hasEnterAndLeaveLoopEvent);
				int n = info.numCallsToMeth[mId];
				LoopFrame topFrame = info.top();
				if (topFrame == null || topFrame.wId != wId || topFrame.numCalls != n) {
					trace = true;
					return;
				}
				info.pop();
				genEvent = true;
			} else {
				int n = info.numCallsToMeth[mId];
				LoopFrame topFrame = info.top();
				if (topFrame == null || topFrame.wId != wId || topFrame.numCalls != n) {
					trace = true;
					return;
				}
				info.noInstr = false;
				info.pop();
				if (hasEnterAndLeaveLoopEvent)
					genEvent = true;
			}
			if (genEvent) {
				try {
					buffer.putByte(EventKind.LEAVE_LOOP);
					buffer.putInt(wId);
					int tId = getObjectId(t);
					buffer.putInt(tId);
				} catch (IOException ex) { throw new RuntimeException(ex); }
			}
			trace = true;
		}
	}
	public synchronized static void open(int fileBlockSize,
			String traceFileName, String instrSchemeFileName, int numMeths,
			int callsBound, int itersBound) {
		try {
			buffer = new ByteBufferedFile(fileBlockSize, traceFileName, false);
		    objmap = new WeakIdentityHashMap();
			scheme = InstrScheme.load(instrSchemeFileName);
			hasEnterAndLeaveMethodEvent = scheme.hasEnterAndLeaveMethodEvent();
			hasEnterAndLeaveLoopEvent = scheme.hasEnterAndLeaveLoopEvent();
			Runtime.callsBound = callsBound;
			Runtime.itersBound = itersBound;
			Runtime.numMeths = numMeths;
			alwaysInstr = (callsBound > 0 || itersBound > 0) ? false : true;
			if (!alwaysInstr || hasEnterAndLeaveLoopEvent)
				thrmap = new WeakIdentityHashMap();
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


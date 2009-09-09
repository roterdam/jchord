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
	public static final int MISSING_FIELD_VAL = -1;
	public static final int UNKNOWN_FIELD_VAL = -2;
	private static int instrBound;
	private static int numMeths;
	private static int numLoops;
	private static class ThreadInfo {
		private final static int INITIAL_STK_SIZE = 100; 
		public int numCallsToMeth[];
		public int numItersOfLoop[];
		public boolean doInstr;
		public boolean stk[];
		public int stkSize;
		public int stkTop;
		public ThreadInfo() {
			numCallsToMeth = new int[numMeths];
			numItersOfLoop = new int[numLoops];
			doInstr = true;
			stk = new boolean[INITIAL_STK_SIZE];
			stkSize = INITIAL_STK_SIZE;
			stkTop = -1;
		}
		public boolean pop() {
			assert (stkTop >= 0);
			return stk[stkTop--];
		}
		public void push(boolean val) {
			if (stkTop == stkSize - 1) {
				int newStkSize = stkSize * 2;
				boolean newStk[] = new boolean[newStkSize];
				System.arraycopy(stk, 0, newStk, 0, stkSize);
				stk = newStk;
				stkSize = newStkSize;
			}
			stk[++stkTop] = val;
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
	public synchronized static void enterMethodEvent(int mId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				buffer.putByte(EventKind.ENTER_METHOD);
				if (mId != MISSING_FIELD_VAL)
					buffer.putInt(mId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void leaveMethodEvent(int mId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				buffer.putByte(EventKind.LEAVE_METHOD);
				if (mId != MISSING_FIELD_VAL)
					buffer.putInt(mId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void enterLoopEvent(int wId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
				buffer.putByte(EventKind.ENTER_LOOP);
				if (wId != MISSING_FIELD_VAL)
					buffer.putInt(wId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
	}
	public synchronized static void leaveLoopEvent(int wId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
				buffer.putByte(EventKind.LEAVE_LOOP);
				if (wId != MISSING_FIELD_VAL)
					buffer.putInt(wId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
				}
			} catch (IOException ex) { throw new RuntimeException(ex); }
			trace = true;
		}
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
	public synchronized static void methodCallEvent(int iId) {
		if (trace) {
			trace = false;
			try {
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				buffer.putByte(EventKind.METHOD_CALL);
				if (iId != MISSING_FIELD_VAL)
					buffer.putInt(iId);
				if (ef.hasThr()) {
					int tId = getObjectId(Thread.currentThread());
					buffer.putInt(tId);
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
				if (thrmap != null) {
					Object t = Thread.currentThread();
					ThreadInfo info = (ThreadInfo) thrmap.get(t);
					if (!info.doInstr) {
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
				if (thrmap != null) {
					Object t = Thread.currentThread();
					ThreadInfo info = (ThreadInfo) thrmap.get(t);
					if (!info.doInstr) {
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
	public synchronized static void enterMethodCheckEvent(int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info == null) {
				info = new ThreadInfo();
				thrmap.put(t, info);
			}
			info.numCallsToMeth[mId]++;
			if (info.numCallsToMeth[mId] == instrBound) {
				info.push(info.doInstr);
				info.doInstr = false;
			} else {
				if (hasEnterAndLeaveMethodEvent && info.doInstr) {
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					try {
						buffer.putByte(EventKind.ENTER_METHOD);
						if (ef.hasLoc())
							buffer.putInt(mId);
						if (ef.hasThr()) {
							int tId = getObjectId(t);
							buffer.putInt(tId);
						}
					} catch (IOException ex) { throw new RuntimeException(ex); }
				}
			}
			trace = true;
		}
	}
	public synchronized static void leaveMethodCheckEvent(int mId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info.doInstr) {
				if (hasEnterAndLeaveMethodEvent) {
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					try {
						buffer.putByte(EventKind.LEAVE_METHOD);
						if (ef.hasLoc())
							buffer.putInt(mId);
						if (ef.hasThr()) {
							int tId = getObjectId(t);
							buffer.putInt(tId);
						}
					} catch (IOException ex) { throw new RuntimeException(ex); }
				}
			} else {
				if (info.numCallsToMeth[mId] == instrBound)
					info.doInstr = info.pop();
			}
			info.numCallsToMeth[mId]--;
			trace = true;
		}
	}
	public synchronized static void enterLoopCheckEvent(int wId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info.numItersOfLoop[wId] == 0) {
				// entering loop from outside instead of from back edge
				info.push(info.doInstr);
				if (hasEnterAndLeaveLoopEvent && info.doInstr) {
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
					try {
						buffer.putByte(EventKind.ENTER_LOOP);
						if (wId != MISSING_FIELD_VAL)
							buffer.putInt(wId);
						if (ef.hasThr()) {
							int tId = getObjectId(t);
							buffer.putInt(tId);
						}
					} catch (IOException ex) { throw new RuntimeException(ex); }
				}
			} else {
				// entering loop from back edge instead of from outside
				if (info.doInstr) {
					info.numItersOfLoop[wId]++;
					if (info.numItersOfLoop[wId] == instrBound)
						info.doInstr = false;
				}
			}
			trace = true;
		}
	}
	public synchronized static void leaveLoopCheckEvent(int wId) {
		if (trace) {
			trace = false;
			Thread t = Thread.currentThread();
			ThreadInfo info = (ThreadInfo) thrmap.get(t);
			if (info.numItersOfLoop[wId] > 0) {
            	info.numItersOfLoop[wId] = 0;
				info.doInstr = info.pop();
				if (hasEnterAndLeaveLoopEvent && info.doInstr) {
					EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
					try {
						buffer.putByte(EventKind.LEAVE_LOOP);
						if (wId != MISSING_FIELD_VAL)
							buffer.putInt(wId);
						if (ef.hasThr()) {
							int tId = getObjectId(t);
							buffer.putInt(tId);
						}
					} catch (IOException ex) { throw new RuntimeException(ex); }
				}
			}
			trace = true;
		}
	}
	public synchronized static void open(String traceFileName,
			String instrSchemeFileName, int numMeths, int numLoops,
			int instrBound) {
		try {
			buffer = new ByteBufferedFile(1024, traceFileName, false);
		    objmap = new WeakIdentityHashMap();
			scheme = InstrScheme.load(instrSchemeFileName);
			{ 
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				hasEnterAndLeaveMethodEvent = ef.present();
			}
			if (instrBound > 0) {
				thrmap = new WeakIdentityHashMap();
				Runtime.instrBound = instrBound;
				Runtime.numMeths = numMeths;
				Runtime.numLoops = numLoops;
			}
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


/*
Three kinds of basic blocks:

1. METHOD ENTRY BASIC BLOCK e:
   num_nested_levs[M]++;
   if (num_nested_levs[M] == max) {
       push(do_instrument);
       do_instrument = false;
   }
   /else/ if (do_instrument)
       print e

2. METHOD EXIT BASIC BLOCK e:
   if (do_instrument)
       print e
   /else/ if (num_nested_levs[M] == max)
       do_instrument = pop();
   num_nested_levs[M]--;

3. BASIC BLOCK p other than METHOD ENTRY or EXIT:
    assert that p is not both the head and exit of same loop
    if (p is exit of loop L1) {
        if (entering from inside loop) { // figured by test last_iters_of_loop[L1] > 0
            last_iters_of_loop[L] = 0;
            do_instrument = pop();
        }
    }
    if (p is exit of loop L2) {
        if (entering from inside loop) {
            ...
        }
    }
    NOTE: head test below MUST come after all loop tests above, as we might be
    leaving one loop and starting another, in which case we want do_instrument
    to be set correctly
    if (p is head of loop L) {
        if (entering from outside loop) // figured by test last_iters_of_loop[L] == 0
            push(do_instrument);
        else if (do_instrument) {
            last_iters_of_loop[L]++
            if (last_iters_of_loop[L] == max)
                do_instrument = false;
        }
    }
    if (do_instrument)
        print p;


INVARIANTS:
1. METHOD ENTRY or EXIT basic blocks cannot be loop head or loop exit of any loop
2. basic block besides METHOD ENTRY or EXIT can be loop head of 1 loop,
   and loop exit of 1 or more loops

INSTRUMENTIATION:
1. instrument METHOD ENTRY and EXIT basic blocks using method.insertBefore and
   method.insertAfter/addCatch
2. instrument each basic block with (loop exits + loop head + print) check above

*/

/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.instr;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import chord.util.ChordRuntimeException;

/**
 * Scheme specifying the kind and format of events to be generated
 * during an instrumented program's execution.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class InstrScheme implements Serializable {
	public static final int NEW_AND_NEWARRAY = 0;

	public static final int GETSTATIC_PRIMITIVE = 1;
	public static final int GETSTATIC_REFERENCE = 2;
	public static final int PUTSTATIC_PRIMITIVE = 3;
	public static final int PUTSTATIC_REFERENCE = 4;

	public static final int GETFIELD_PRIMITIVE = 5;
	public static final int GETFIELD_REFERENCE = 6;
	public static final int PUTFIELD_PRIMITIVE = 7;
	public static final int PUTFIELD_REFERENCE = 8;

	public static final int ALOAD_PRIMITIVE = 9;
	public static final int ALOAD_REFERENCE = 10;
	public static final int ASTORE_PRIMITIVE = 11;
	public static final int ASTORE_REFERENCE = 12;

    public static final int METHOD_CALL = 13;
    public static final int RETURN_PRIMITIVE = 14;
    public static final int RETURN_REFERENCE = 15;
    public static final int EXPLICIT_THROW = 16;
    public static final int IMPLICIT_THROW = 17;

    public static final int THREAD_START = 18;
    public static final int THREAD_JOIN = 19;
    public static final int ACQUIRE_LOCK = 20;
    public static final int RELEASE_LOCK = 21;
    public static final int WAIT = 22;
    public static final int NOTIFY = 23;
    
	public static final int MAX_NUM_EVENT_FORMATS = 24;

	public class EventFormat implements Serializable {
		private boolean present;
		private boolean hasLoc;
		private boolean hasThr;
		private boolean hasFldOrIdx;
		private boolean hasObj;
		private boolean hasBaseObj;
		private boolean isBef;
		private boolean isAft;
		private int size;
		public void setPresent() { present = true; }
		public boolean present() { return present; }
		public int size() { return size; }
		public boolean hasLoc() { return hasLoc; }
		public boolean hasThr() { return hasThr; }
		public boolean hasFld() { return hasFldOrIdx; }
		public boolean hasIdx() { return hasFldOrIdx; }
		public boolean hasObj() { return hasObj; }
		public boolean hasBaseObj() { return hasBaseObj; }
		public void setBef() { isBef = true; }
		public void setAft() { isAft = true; }
		public boolean isBef() { return isBef; }
		public boolean isAft() { return isAft; }
		public void setLoc() {
			if (!hasLoc) {
				hasLoc = true; 
				size += 4;
			}
		}
		public void setThr() {
			if (!hasThr) {
				hasThr = true; 
				size += 4;
			}
		}
		public void setFld() {
			if (!hasFldOrIdx) {
				hasFldOrIdx = true; 
				size += 4;
			}
		}
		public void setIdx() {
			if (!hasFldOrIdx) {
				hasFldOrIdx = true; 
				size += 4;
			}
		}
		public void setObj() {
			if (!hasObj) {
				hasObj = true; 
				size += 4;
			}
		}
		public void setBaseObj() {
			if (!hasBaseObj) {
				hasBaseObj = true; 
				size += 4;
			}
		}
	}

	private boolean hasEnterAndLeaveMethodEvent;
	private boolean hasEnterAndLeaveLoopEvent;
	private boolean hasFinalizeEvent;
	private boolean hasBasicBlockEvent;
	private boolean hasQuadEvent;
	private final EventFormat[] events;

	public InstrScheme() {
		events = new EventFormat[MAX_NUM_EVENT_FORMATS];
		for (int i = 0; i < MAX_NUM_EVENT_FORMATS; i++)
			events[i] = new EventFormat();
	}

	public void setEnterAndLeaveMethodEvent() {
		hasEnterAndLeaveMethodEvent = true;
	}

	public boolean hasEnterAndLeaveMethodEvent() {
		return hasEnterAndLeaveMethodEvent;
	}

	public void setEnterAndLeaveLoopEvent() {
		hasEnterAndLeaveLoopEvent = true;
	}

	public boolean hasEnterAndLeaveLoopEvent() {
		return hasEnterAndLeaveLoopEvent;
	}

	public void setBasicBlockEvent() {
		hasBasicBlockEvent = true;
	}

	public boolean hasBasicBlockEvent() {
		return hasBasicBlockEvent;
	}

	public void setQuadEvent() {
		hasQuadEvent = true;
	}

	public boolean hasQuadEvent() {
		return hasQuadEvent;
	}

	public boolean hasFinalizeEvent() {
		return hasFinalizeEvent;
	}

	public EventFormat getEvent(int eventId) {
		return events[eventId];
	}

	public void setNewAndNewArrayEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[NEW_AND_NEWARRAY];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setGetstaticPrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld) {
		EventFormat e = events[GETSTATIC_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
	}

	public void setGetstaticReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld, boolean hasObj) {
		EventFormat e = events[GETSTATIC_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
		if (hasObj) e.setObj();
	}

	public void setPutstaticPrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld) {
		EventFormat e = events[PUTSTATIC_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
	}

	public void setPutstaticReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld, boolean hasObj) {
		EventFormat e = events[PUTSTATIC_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
		if (hasObj) e.setObj();
	}

	public void setGetfieldPrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld) {
		EventFormat e = events[GETFIELD_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
	}

	public void setGetfieldReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld, boolean hasObj) {
		EventFormat e = events[GETFIELD_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
		if (hasObj) e.setObj();
	}

	public void setPutfieldPrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld) {
		EventFormat e = events[PUTFIELD_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
	}

	public void setPutfieldReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasFld, boolean hasObj) {
		EventFormat e = events[PUTFIELD_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasFld) e.setFld();
		if (hasObj) e.setObj();
	}

	public void setAloadPrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasIdx) {
		EventFormat e = events[ALOAD_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasIdx) e.setIdx();
	}

	public void setAloadReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasIdx, boolean hasObj) {
		EventFormat e = events[ALOAD_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasIdx) e.setIdx();
		if (hasObj) e.setObj();
	}

	public void setAstorePrimitiveEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasIdx) {
		EventFormat e = events[ASTORE_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasIdx) e.setIdx();
	}

	public void setAstoreReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasBaseObj, boolean hasIdx, boolean hasObj) {
		EventFormat e = events[ASTORE_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasBaseObj) e.setBaseObj();
		if (hasIdx) e.setIdx();
		if (hasObj) e.setObj();
	}

	public void setMethodCallEvent(boolean hasLoc, boolean hasThr, boolean hasObj,
			boolean isBef, boolean isAft) {
		if (!isBef && !isAft)
			return;
		EventFormat e = events[METHOD_CALL];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
		if (isBef) e.setBef();
		if (isAft) e.setAft();
	}

	public void setReturnPrimitiveEvent(boolean hasLoc, boolean hasThr) {
		EventFormat e = events[RETURN_PRIMITIVE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
	}

	public void setReturnReferenceEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[RETURN_REFERENCE];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setExplicitThrowEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[EXPLICIT_THROW];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setImplicitThrowEvent(boolean hasThr, boolean hasObj) {
		EventFormat e = events[IMPLICIT_THROW];
		e.setPresent();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setThreadStartEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[THREAD_START];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setThreadJoinEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[THREAD_JOIN];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setAcquireLockEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[ACQUIRE_LOCK];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setReleaseLockEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[RELEASE_LOCK];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setWaitEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[WAIT];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}

	public void setNotifyEvent(boolean hasLoc, boolean hasThr,
			boolean hasObj) {
		EventFormat e = events[NOTIFY];
		e.setPresent();
		if (hasLoc) e.setLoc();
		if (hasThr) e.setThr();
		if (hasObj) e.setObj();
	}
	
	public void setFinalizeEvent() {
		hasFinalizeEvent = true;
	}

	public boolean hasFieldEvent() {
		return hasGetfieldEvent() || hasPutfieldEvent();
	}

	public boolean hasStaticEvent() {
		return hasGetstaticEvent() || hasPutstaticEvent();
	}

	public boolean hasArrayEvent() {
		return hasAloadEvent() || hasAstoreEvent();
	}

	public boolean hasGetstaticEvent() {
		return events[GETSTATIC_PRIMITIVE].present() ||
			events[GETSTATIC_REFERENCE].present();
	}

	public boolean hasPutstaticEvent() {
		return events[PUTSTATIC_PRIMITIVE].present() ||
			events[PUTSTATIC_REFERENCE].present();
	}

	public boolean hasGetfieldEvent() {
		return events[GETFIELD_PRIMITIVE].present() ||
			events[GETFIELD_REFERENCE].present();
	}

	public boolean hasPutfieldEvent() {
		return events[PUTFIELD_PRIMITIVE].present() ||
			events[PUTFIELD_REFERENCE].present();
	}

	public boolean hasAloadEvent() {
		return events[ALOAD_PRIMITIVE].present() ||
			events[ALOAD_REFERENCE].present();
	}

	public boolean hasAstoreEvent() {
		return events[ASTORE_PRIMITIVE].present() ||
			events[ASTORE_REFERENCE].present();
	}

	public boolean needsMmap() {
		return hasEnterAndLeaveMethodEvent || hasEnterAndLeaveLoopEvent;
	}

	public boolean needsHmap() {
		EventFormat e = events[NEW_AND_NEWARRAY];
		return e.hasLoc() || e.hasObj();
	}

	public boolean needsEmap() {
		return
			events[GETSTATIC_PRIMITIVE].hasLoc() ||
			events[GETSTATIC_REFERENCE].hasLoc() ||
			events[PUTSTATIC_PRIMITIVE].hasLoc() ||
			events[PUTSTATIC_REFERENCE].hasLoc() ||
			events[GETFIELD_PRIMITIVE].hasLoc() ||
			events[GETFIELD_REFERENCE].hasLoc() ||
			events[PUTFIELD_PRIMITIVE].hasLoc() ||
			events[PUTFIELD_REFERENCE].hasLoc() ||
			events[ALOAD_PRIMITIVE].hasLoc() ||
			events[ALOAD_REFERENCE].hasLoc() ||
			events[ASTORE_PRIMITIVE].hasLoc() ||
			events[ASTORE_REFERENCE].hasLoc();
	}

	public boolean needsFmap() {
		return
			events[GETSTATIC_PRIMITIVE].hasFld() ||
			events[GETSTATIC_REFERENCE].hasFld() ||
			events[PUTSTATIC_PRIMITIVE].hasFld() ||
			events[PUTSTATIC_REFERENCE].hasFld() ||
			events[GETFIELD_PRIMITIVE].hasFld() ||
			events[GETFIELD_REFERENCE].hasFld() ||
			events[PUTFIELD_PRIMITIVE].hasFld() ||
			events[PUTFIELD_REFERENCE].hasFld();
	}

	public boolean needsImap() {
		return
			events[METHOD_CALL].hasLoc() ||
			events[THREAD_START].hasLoc() ||
			events[THREAD_JOIN].hasLoc() ||
			events[WAIT].hasLoc() ||
			events[NOTIFY].hasLoc();
	}

	public boolean needsPmap() {
 		return hasQuadEvent;
	}

	public boolean needsLmap() {
		return events[ACQUIRE_LOCK].hasLoc();
	}

	public boolean needsRmap() {
		return events[RELEASE_LOCK].hasLoc();
	}

	public boolean needsBmap() {
		return hasBasicBlockEvent;
	}

	public boolean needsTraceTransform() {
		return events[NEW_AND_NEWARRAY].hasObj();
	}

	public static InstrScheme load(String fileName) {
		InstrScheme scheme;
		try {
			ObjectInputStream stream = new ObjectInputStream(
				new FileInputStream(fileName));
			scheme = (InstrScheme) stream.readObject();
			stream.close();
		} catch (ClassNotFoundException ex) {
			throw new ChordRuntimeException(ex);
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
		return scheme;
	}

	public void save(String fileName) {
		try {
			ObjectOutputStream stream = new ObjectOutputStream(
				new FileOutputStream(fileName));
			stream.writeObject(this);
			stream.close();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
}

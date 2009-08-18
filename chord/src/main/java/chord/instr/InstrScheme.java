package chord.instr;

public class InstrScheme {
	private static final InstrScheme instance = new InstrScheme();
	public static final InstrScheme v() { return instance; }
	/**
	 * ENTER_AND_LEAVE_METHOD:
	 * Controls generation of the following events after/before
	 * thread t enters/leaves method m:
	 * ENTER_METHOD m t
	 * LEAVE_METHOD m t
	 * 
	 * NEW_AND_NEW_ARRAY:
	 * Controls generation of the following event before thread t
	 * executes a NEW instruction at program point p:
	 * BEF_NEW p t
	 * <p>
	 * Controls generation of the following event after thread t
	 * executes a NEW instruction at program point p and allocates
	 * new object o:
	 * AFT_NEW p t o
	 * <p>
	 * Note: Both above events are visible only in the crude trace
	 * and not in the final trace.  Moreover, they are needed only
	 * if instrOid is true.
	 * <p>
	 * Controls generation of the following events after thread t
	 * executes a NEW or NEWARRAY instruction at program point p
	 * and allocates new object o:
	 * NEW p t o
	 * NEW_ARRAY p t o
	 *
	 * GETSTATIC_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * reads primitive-typed static field f at program point p:
	 * GETSTATIC_PRIMITIVE p t f
	 * 
	 * GETSTATIC_REFERENCE:
	 * Controls generation of the following event after thread t
	 * reads object o from reference-typed static field f at
	 * program point p:
	 * GETSTATIC_REFERENCE p t f o
	 *
	 * PUTSTATIC_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * writes primitive-typed static field f at program point p:
	 * PUTSTATIC_PRIMITIVE p t f
     *
     * PUTSTATIC_REFERENCE:
	 * Controls generation of the following event after thread t
	 * writes object o to reference-typed static field f at
	 * program point p:
	 * PUTSTATIC_REFERENCE p t f o
	 * 
	 * GETFIELD_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * reads primitive-typed instance field f of object b at
	 * program point p:
	 * GETFIELD_PRIMITIVE p t b f
	 *
	 * GETFIELD_REFERENCE:
	 * Controls generation of the following event after thread t
	 * reads object o from reference-typed instance field f of
	 * object b at program point p:
	 * GETFIELD_REFERENCE p t b f o
	 * 
	 * PUTFIELD_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * writes primitive-typed instance field f of object b at
	 * program point p:
	 * PUTFIELD_PRIMITIVE p t b f
	 *
	 * PUTFIELD_REFERENCE:
	 * Controls generation of the following event after thread t
	 * writes object o to reference-typed instance field f of
	 * object b at program point p:
	 * PUTFIELD_REFERENCE p t b f o
	 *
	 * ALOAD_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * reads the primitive-typed i^th element of array object b at
	 * program point p:
	 * ALOAD_PRIMITIVE p t b i
	 * 
	 * ALOAD_REFERENCE:
	 * Controls generation of the following event after thread t
	 * reads object o from the reference-typed i^th element of
	 * array object b at program point p:
	 * ALOAD_REFERENCE p t b i o
	 * 
	 * ASTORE_PRIMITIVE:
	 * Controls generation of the following event after thread t
	 * writes the primitive-typed i^th element of array object b at
	 * program point p:
	 * ASTORE_PRIMITIVE p t b i
	 * 
	 * ASTORE_REFERENCE:
	 * Controls generation of the following event after thread t
	 * writes object o to the reference-typed i^th element of array
	 * object b at program point p:
	 * ASTORE_REFERENCE p t b i o
	 * 
	 * THREAD_START:
	 * Controls generation of the following event before thread t
	 * calls the <tt>start()</tt> method of <tt>java.lang.Thread</tt>
	 * at program point p and spawns a thread o:
	 * THREAD_START p t o
	 * 
	 * THREAD_JOIN:
	 * Controls generation of the following event before thread t
	 * calls the <tt>join()</tt>, <tt>join(int)</tt>, or
	 * <tt>join(int,int)</tt> method of <tt>java.lang.Thread</tt>
	 * at program point p to join with thread o:
	 * THREAD_JOIN p t o
	 * 
	 * ACQUIRE_LOCK:
	 * Controls generation of the following event after thread t
	 * executes a statement of the form monitorenter l or enters
	 * a method synchronized on l at program point p:
	 * ACQUIRE_LOCK p t l
	 * 
	 * RELEASE_LOCK:
	 * Controls generation of the following event before thread t
	 * executes a statement of the form monitorexit l or leaves
	 * a method synchronized on l:
	 * RELEASE_LOCK p t l
	 * 
	 * WAIT:
	 * Controls generation of the following event before thread t
	 * calls the <tt>wait()</tt>, <tt>wait(long)</tt>, or
	 * <tt>wait(long,int)</tt> method of <tt>java.lang.Object</tt>
	 * at program point p on object l:
	 * WAIT p t l
	 * 
	 * NOTIFY:
	 * Controls generation of the following event before thread t
	 * calls the <tt>notify()</tt> or <tt>notifyAll()</tt> method of
	 * <tt>java.lang.Object</tt> at program point p on object l:
	 * NOTIFY p t l
	 */
    public static final int ENTER_AND_LEAVE_METHOD = 0;
    public static final int NEW_AND_NEWARRAY = 1;
    public static final int GETSTATIC_PRIMITIVE = 2;
    public static final int GETSTATIC_REFERENCE = 3;
    public static final int PUTSTATIC_PRIMITIVE = 4;
    public static final int PUTSTATIC_REFERENCE = 5;
    public static final int GETFIELD_PRIMITIVE = 6;
    public static final int GETFIELD_REFERENCE = 7;
    public static final int PUTFIELD_PRIMITIVE = 8;
    public static final int PUTFIELD_REFERENCE = 9;
    public static final int ALOAD_PRIMITIVE = 10;
    public static final int ALOAD_REFERENCE = 11;
    public static final int ASTORE_PRIMITIVE = 12;
    public static final int ASTORE_REFERENCE = 13;
    public static final int THREAD_START = 14;
    public static final int THREAD_JOIN = 15;
    public static final int ACQUIRE_LOCK = 16;
    public static final int RELEASE_LOCK = 17;
    public static final int WAIT = 18;
    public static final int NOTIFY = 19;

	public static final int MAX_NUM_EVENT_FORMATS = 19;

	public class EventFormat {
		private boolean hasMorPorHorE;
		private boolean hasT;
		private boolean hasForI;
		private boolean hasOorL;
		private boolean hasB;
		private int size;
		public boolean present() { return size > 0; }
		public int size() { return size; }
		public boolean hasMid() { return hasMorPorHorE; }
		public boolean hasPid() { return hasMorPorHorE; }
		public boolean hasEid() { return hasMorPorHorE; }
		public boolean hasHid() { return hasMorPorHorE; }
		public boolean hasTid() { return hasT; }
		public boolean hasFid() { return hasForI; }
		public boolean hasIid() { return hasForI; }
		public boolean hasOid() { return hasOorL; }
		public boolean hasLid() { return hasOorL; }
		public boolean hasBid() { return hasB; }
		public void setM() {
			if (!hasMorPorHorE) {
				hasMorPorHorE = true; 
				size += 4;
			}
		}
		public void setP() {
			if (!hasMorPorHorE) {
				hasMorPorHorE = true; 
				size += 4;
			}
		}
		public void setH() {
			if (!hasMorPorHorE) {
				hasMorPorHorE = true; 
				size += 4;
			}
		}
		public void setE() {
			if (!hasMorPorHorE) {
				hasMorPorHorE = true; 
				size += 4;
			}
		}
		public void setT() {
			if (!hasT) {
				hasT = true; 
				size += 4;
			}
		}
		public void setF() {
			if (!hasForI) {
				hasForI = true; 
				size += 4;
			}
		}
		public void setI() {
			if (!hasForI) {
				hasForI = true; 
				size += 4;
			}
		}
		public void setO() {
			if (!hasOorL) {
				hasOorL = true; 
				size += 4;
			}
		}
		public void setL() {
			if (!hasOorL) {
				hasOorL = true; 
				size += 4;
			}
		}
		public void setB() {
			if (!hasB) {
				hasB = true; 
				size += 4;
			}
		}
	}

	private int instrMethodAndLoopBound;
	private final EventFormat[] events =
		new EventFormat[MAX_NUM_EVENT_FORMATS];

	public void setInstrMethodAndLoopBound(int n) {
		assert (n >= 0);
		instrMethodAndLoopBound = n;
	}
	public int getInstrMethodAndLoopBound() {
		return instrMethodAndLoopBound;
	}

	public EventFormat getEvent(int eventId) {
		return events[eventId];
	}
	public void setEnterAndLeaveMethodEvent(
			boolean hasM, boolean hasT) {
		EventFormat e = events[ENTER_AND_LEAVE_METHOD];
		if (hasM) e.setM();
		if (hasT) e.setT();
	}
	public void setNewAndNewArrayEvent(
			boolean hasH, boolean hasT, boolean hasO) {
		EventFormat e = events[NEW_AND_NEWARRAY];
		if (hasH) e.setH();
		if (hasT) e.setT();
		if (hasO) e.setO();
	}
	public void setGetstaticPrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasF) {
		EventFormat e = events[GETSTATIC_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasF) e.setF();
	}
	public void setGetstaticReferenceEvent(
			boolean hasE, boolean hasT, boolean hasF, boolean hasO) {
		EventFormat e = events[GETSTATIC_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasF) e.setF();
		if (hasO) e.setO();
	}
	public void setPutstaticPrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasF) {
		EventFormat e = events[PUTSTATIC_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasF) e.setF();
	}
	public void setPutstaticReferenceEvent(
			boolean hasE, boolean hasT, boolean hasF, boolean hasO) {
		EventFormat e = events[PUTSTATIC_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasF) e.setF();
		if (hasO) e.setO();
	}
	public void setGetfieldPrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasF) {
		EventFormat e = events[GETFIELD_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasF) e.setF();
	}
	public void setGetfieldReferenceEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasF, boolean hasO) {
		EventFormat e = events[GETFIELD_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasF) e.setF();
		if (hasO) e.setO();
	}
	public void setPutfieldPrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasF) {
		EventFormat e = events[PUTFIELD_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasF) e.setF();
	}
	public void setPutfieldReferenceEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasF, boolean hasO) {
		EventFormat e = events[PUTFIELD_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasF) e.setF();
		if (hasO) e.setO();
	}
	public void setAloadPrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasI) {
		EventFormat e = events[ALOAD_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasI) e.setI();
	}
	public void setAloadReferenceEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasI, boolean hasO) {
		EventFormat e = events[ALOAD_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasI) e.setI();
		if (hasO) e.setO();
	}
	public void setAstorePrimitiveEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasI) {
		EventFormat e = events[ASTORE_PRIMITIVE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasI) e.setI();
	}
	public void setAstoreReferenceEvent(
			boolean hasE, boolean hasT, boolean hasB, boolean hasI, boolean hasO) {
		EventFormat e = events[ASTORE_REFERENCE];
		if (hasE) e.setE();
		if (hasT) e.setT();
		if (hasB) e.setB();
		if (hasI) e.setI();
		if (hasO) e.setO();
	}
	public void setThreadStartEvent(
			boolean hasP, boolean hasT, boolean hasO) {
		EventFormat e = events[THREAD_START];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasO) e.setO();
	}
	public void setThreadJoinEvent(
			boolean hasP, boolean hasT, boolean hasO) {
		EventFormat e = events[THREAD_JOIN];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasO) e.setO();
	}
	public void setAcquireLockEvent(
			boolean hasP, boolean hasT, boolean hasL) {
		EventFormat e = events[ACQUIRE_LOCK];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasL) e.setL();
	}
	public void setReleaseLockEvent(
			boolean hasP, boolean hasT, boolean hasL) {
		EventFormat e = events[RELEASE_LOCK];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasL) e.setL();
	}
	public void setWaitEvent(
			boolean hasP, boolean hasT, boolean hasL) {
		EventFormat e = events[WAIT];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasL) e.setL();
	}
	public void setNotifyEvent(
			boolean hasP, boolean hasT, boolean hasL) {
		EventFormat e = events[NOTIFY];
		if (hasP) e.setP();
		if (hasT) e.setT();
		if (hasL) e.setL();
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
		return events[ENTER_AND_LEAVE_METHOD].hasMid() ||
			instrMethodAndLoopBound > 0;
	}
	public boolean needsHmap() {
		return events[NEW_AND_NEWARRAY].hasHid();
	}
	public boolean needsEmap() {
		return
			events[GETSTATIC_PRIMITIVE].hasEid() || events[GETSTATIC_REFERENCE].hasEid() ||
			events[PUTSTATIC_PRIMITIVE].hasEid() || events[PUTSTATIC_REFERENCE].hasEid() ||
			events[GETFIELD_PRIMITIVE].hasEid() || events[GETFIELD_REFERENCE].hasEid() ||
			events[PUTFIELD_PRIMITIVE].hasEid() || events[PUTFIELD_REFERENCE].hasEid() ||
			events[ALOAD_PRIMITIVE].hasEid() || events[ALOAD_REFERENCE].hasEid() ||
			events[ASTORE_PRIMITIVE].hasEid() || events[ASTORE_REFERENCE].hasEid();
	}
	public boolean needsFmap() {
		return
			events[GETSTATIC_PRIMITIVE].hasFid() || events[GETSTATIC_REFERENCE].hasFid() ||
			events[PUTSTATIC_PRIMITIVE].hasFid() || events[PUTSTATIC_REFERENCE].hasFid() ||
			events[GETFIELD_PRIMITIVE].hasFid() || events[GETFIELD_REFERENCE].hasFid() ||
			events[PUTFIELD_PRIMITIVE].hasFid() || events[PUTFIELD_REFERENCE].hasFid();
	}
	public boolean needsPmap() {
		return
			events[THREAD_START].hasPid() || events[THREAD_JOIN].hasPid() ||
			events[ACQUIRE_LOCK].hasPid() || events[RELEASE_LOCK].hasPid() ||
			events[WAIT].hasPid() || events[NOTIFY].hasPid();
	}
	public boolean needsTraceTransform() {
		return events[NEW_AND_NEWARRAY].hasOid();
	}
	public void setAllEvents() {
		setEnterAndLeaveMethodEvent(true, true);
		setNewAndNewArrayEvent(true, true, true);
		setGetstaticPrimitiveEvent(true, true, true);
		setPutstaticPrimitiveEvent(true, true, true);
		setGetstaticReferenceEvent(true, true, true, true);
		setPutstaticReferenceEvent(true, true, true, true);
		setGetfieldPrimitiveEvent(true, true, true, true);
		setPutfieldPrimitiveEvent(true, true, true, true);
		setGetfieldReferenceEvent(true, true, true, true, true);
		setPutfieldReferenceEvent(true, true, true, true, true);
		setThreadStartEvent(true, true, true);
		setThreadJoinEvent(true, true, true);
		setAcquireLockEvent(true, true, true);
		setReleaseLockEvent(true, true, true);
		setWaitEvent(true, true, true);
		setNotifyEvent(true, true, true);
	}
}

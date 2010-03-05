/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the BSD License; see COPYING for details.
 */
package chord.runtime;

import chord.instr.InstrScheme;
import chord.util.WeakIdentityHashMap;

/**
 * Abstract online or offline handler of events generated during an
 * instrumented program's execution.
 * 
 * Dynamic program analyses must specify a concrete event handler
 * via system property <tt>chord.runtime.class</tt>.
 * <p>
 * The default event handler is {@link chord.runtime.Runtime} and
 * should suffice for offline dynamic program analyses (i.e. those that
 * handle the events in a separate JVM, either during or after the
 * instrumented program's execution, depending upon whether the value
 * of system property <tt>chord.trace.pipe</tt> is true or false,
 * respectively).
 * <p>
 * Online analyses (i.e. those that handle events during the
 * instrumented program's execution in the same JVM) must subclass this
 * class and define the relevant event handling methods, i.e. static
 * methods named ".*Event", e.g. {@link #acquireLockEvent(int, Object)}.
 * Which methods are relevant depends upon the instrumentation scheme
 * chosen by the dynamic program analysis;
 * see {@link chord.project.analyses.DynamicAnalysis#getInstrScheme()}.
 *   
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class Runtime {
	public static final int MISSING_FIELD_VAL = -1;
	public static final int UNKNOWN_FIELD_VAL = -2;

	protected static InstrScheme scheme;

	// note: use currentId == 0 for null and currentId == 1 for hypothetical
	// lone object of a hypothetical class all of whose instance fields are
	// static fields in other real classes.
    protected static int currentId = 2;
    protected static WeakIdentityHashMap objmap;

	// NOTE: CALLER MUST SYNCHRONIZE!
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

	private static void doDefault() {
		// do nothing
	}

    public static void befNewEvent(int hId) {
		doDefault();
	}
    public static void aftNewEvent(int hId, Object o) {
		doDefault();
	}
	public static void newEvent(int hId) {
		doDefault();
	}
	public static void newArrayEvent(int hId, Object o) {
		doDefault();
	}
	public static void getstaticPrimitiveEvent(int eId, Object b, int fId) {
		doDefault();
	}
	public static void getstaticReferenceEvent(int eId, Object b, int fId, Object o) {
		doDefault();
	}
	public static void putstaticPrimitiveEvent(int eId, Object b, int fId) {
		doDefault();
	}
	public static void putstaticReferenceEvent(int eId, Object b, int fId, Object o) {
		doDefault();
	}
	public static void getfieldPrimitiveEvent(int eId, Object b, int fId) {
		doDefault();
	}
	public static void getfieldReferenceEvent(int eId, Object b, int fId, Object o) {
		doDefault();
	}
	public static void putfieldPrimitiveEvent(int eId, Object b, int fId) {
		doDefault();
	}
	public static void putfieldReferenceEvent(int eId, Object b, int fId, Object o) {
		doDefault();
	}
	public static void aloadPrimitiveEvent(int eId, Object b, int iId) {
		doDefault();
	}
	public static void aloadReferenceEvent(int eId, Object b, int iId, Object o) {
		doDefault();
	}
	public static void astorePrimitiveEvent(int eId, Object b, int iId) {
		doDefault();
	}
	public static void astoreReferenceEvent(int eId, Object b, int iId, Object o) {
		doDefault();
	}
	public static void threadStartEvent(int iId, Object o) {
		doDefault();
	}
	public static void threadJoinEvent(int iId, Object o) {
		doDefault();
	}
	public static void acquireLockEvent(int lId, Object o) {
		doDefault();
	}
	public static void releaseLockEvent(int rId, Object o) {
		doDefault();
	}
	public static void waitEvent(int iId, Object o) {
		doDefault();
	}
	public static void notifyEvent(int iId, Object o) {
		doDefault();
	}
	public static void notifyAllEvent(int iId, Object o) {
		doDefault();
	}
	public static void methodCallBefEvent(int iId, Object o) {
		doDefault();
	}
	public static void methodCallAftEvent(int iId, Object o) {
		doDefault();
	}
	public static void returnPrimitiveEvent(int pId) {
		doDefault();
	}
	public static void returnReferenceEvent(int pId, Object o) {
		doDefault();
	}
	public static void explicitThrowEvent(int pId, Object o) {
		doDefault();
	}
	public static void implicitThrowEvent(Object o) {
		doDefault();
	}
	public static void quadEvent(int pId) {
		doDefault();
	}
	public static void basicBlockEvent(int bId) {
		doDefault();
	}
	public static void enterMethodEvent(int mId) {
		doDefault();
	}
	public static void leaveMethodEvent(int mId) {
		doDefault();
	}
	public static void enterLoopEvent(int wId, int mId) {
		doDefault();
	}
	public static void leaveLoopEvent(int wId, int mId) {
		doDefault();
	}
	// called during VMInit JVMTI event
	public static void open(String args) {
		String[] a = chord.project.analyses.DynamicAnalysis.agentOptions.split("=");
        String instrSchemeFileName = null;
		for (int i = 0; i < a.length; i += 2) {
			if (a[i].equals("instr_scheme_file_name")) {
				instrSchemeFileName = a[i+1];
				break;
			}
		}
		assert (instrSchemeFileName != null);
		scheme = InstrScheme.load(instrSchemeFileName);
		objmap = new WeakIdentityHashMap();
	}
	// called during VMDeath JVMTI event
	public static void close() {

	}
}


/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.instr;

/**
 * The kind of an event generated during an instrumented
 * program's execution.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class EventKind {
	public static final byte ENTER_MAIN_METHOD = 0;

	public static final byte ENTER_METHOD = 1;
	public static final byte LEAVE_METHOD = 2;

	public static final byte BEF_NEW = 3;
	public static final byte AFT_NEW = 4;
	public static final byte NEW = 5;
	public static final byte NEW_ARRAY = 6;

	public static final byte GETSTATIC_PRIMITIVE = 7;
	public static final byte GETSTATIC_REFERENCE = 8;
	public static final byte PUTSTATIC_PRIMITIVE = 9;
	public static final byte PUTSTATIC_REFERENCE = 10;

	public static final byte GETFIELD_PRIMITIVE = 11;
	public static final byte GETFIELD_REFERENCE = 12;
	public static final byte PUTFIELD_PRIMITIVE = 13;
	public static final byte PUTFIELD_REFERENCE = 14;

	public static final byte ALOAD_PRIMITIVE = 15;
	public static final byte ALOAD_REFERENCE = 16;
	public static final byte ASTORE_PRIMITIVE = 17; 
	public static final byte ASTORE_REFERENCE = 18; 

	public static final byte METHOD_CALL_BEF = 19;
	public static final byte METHOD_CALL_AFT = 20;
	public static final byte RETURN_PRIMITIVE = 21;
	public static final byte RETURN_REFERENCE = 22;
	public static final byte EXPLICIT_THROW = 23;
	public static final byte IMPLICIT_THROW = 24;

	public static final byte QUAD = 25;
	public static final byte BASIC_BLOCK = 26;

	public static final byte THREAD_START = 27;
	public static final byte THREAD_JOIN = 28;
	public static final byte ACQUIRE_LOCK = 29;
	public static final byte RELEASE_LOCK = 30;
	public static final byte WAIT = 31;
	public static final byte NOTIFY = 32;
	public static final byte NOTIFY_ALL = 33;

	public static final byte FINALIZE = 34;
}

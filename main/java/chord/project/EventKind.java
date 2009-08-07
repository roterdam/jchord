/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

/**
 * The kind of an event.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class EventKind {
	static final int BEF_NEW = 0;
	static final int AFT_NEW = 1;
	static final int NEW = 2;
	static final int NEW_ARRAY = 3;
	static final int INST_FLD_RD = 4;
	static final int INST_FLD_WR = 5;
	static final int STAT_FLD_WR = 6;
	static final int ARY_ELEM_RD = 7;
	static final int ARY_ELEM_WR = 8; 
	static final int ACQ_LOCK = 9;
	static final int THREAD_START = 10;
	static final int THREAD_SPAWN = 11;
	static final int METHOD_ENTER = 12;
	static final int METHOD_LEAVE = 13;
}

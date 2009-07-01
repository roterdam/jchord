/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

/**
 * The kind of an instruction.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class InstKind {
	static final int BEF_NEW_INST = 0;
	static final int AFT_NEW_INST = 1;
	static final int NEW_INST = 2;
	static final int NEW_ARRAY_INST = 3;
	static final int INST_FLD_RD_INST = 4;
	static final int INST_FLD_WR_INST = 5;
	static final int STAT_FLD_WR_INST = 7;
	static final int ARY_ELEM_RD_INST = 8;
	static final int ARY_ELEM_WR_INST = 9; 
	static final int ACQ_LOCK_INST = 10;
	static final int FORK_HEAD_INST = 11;
}

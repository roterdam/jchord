/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ChordRuntimeException extends RuntimeException {
	public ChordRuntimeException() { }
	public ChordRuntimeException(String msg) {
		super(msg);
	}
	public ChordRuntimeException(Throwable ex) {
		super(ex);
	}
}


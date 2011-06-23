/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.argret;

import chord.project.Chord;
import chord.project.analyses.ProgramDom;

/**
 * Domain of integers from 0 to chord.domK.size - 1 in order.
 */
@Chord(name = "K")
public class DomK extends ProgramDom<Integer> {
	public static final int MAXZ = Integer.getInteger("chord.domK.size", 32);
	@Override
	public void fill() {
		for (int i = 0; i < MAXZ; i++)
			getOrAdd(new Integer(i));  
	}
}

/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.doms;

import chord.project.Chord;
import chord.project.analyses.ProgramDom;
/**
 * Another domain of integers. Unlike domZ, has fixed size, unconnected
 * to max_args.
 *
 */
@Chord(
	name = "ZZ"
  )
public class DomZZ extends ProgramDom<Integer> {
  public static final int MAXZ = 32;
  
  @Override
  public void fill() {
	for (int i = 0; i < MAXZ; i++)
	  getOrAdd(new Integer(i));  
  }
}

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
public class MethodSign {
	public final String mName;
	public final String mDesc;
	public final String cName;
	public MethodSign(String mName, String mDesc, String cName) {
		this.mName = mName;
		this.mDesc = mDesc;
		this.cName = cName;
	}
	// s is of the form mName:mDesc@cName
	public static MethodSign parse(String s) {
		int colonIdx = s.indexOf(':');
        int atIdx = s.indexOf('@');
        String mName = s.substring(0, colonIdx);
        String mDesc = s.substring(colonIdx + 1, atIdx);
        String cName = s.substring(atIdx + 1);
        return new MethodSign(mName, mDesc, cName);
    }
}

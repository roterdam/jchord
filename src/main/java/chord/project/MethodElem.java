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
public class MethodElem extends MethodSign {
	public final int num;
	public MethodElem(int num, String mName, String mDesc, String cName) {
		super(mName, mDesc, cName);
		this.num = num;
	}
    // s is of the form num!mName:mDesc@cName
	public static MethodElem parse(String s) {
        int exclIdx = s.indexOf('!');
        int colonIdx = s.indexOf(':');
        int atIdx = s.indexOf('@');
        int num = Integer.parseInt(s.substring(0, exclIdx));
        String mName = s.substring(exclIdx + 1, colonIdx);
        String mDesc = s.substring(colonIdx + 1, atIdx);
        String cName = s.substring(atIdx + 1);
        return new MethodElem(num, mName, mDesc, cName);
	}
}

/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.alias.common;

import java.util.Set;
import java.io.Serializable;

/**
 * Representation of an abstract object.
 * <p>
 * An abstract object (also called points-to set) is a set of
 * abstract contexts (see {@link chord.alias.common.Ctxt}).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Obj implements Serializable {
	public final Set<Ctxt> pts;
	public Obj(Set<Ctxt> pts) {
		assert (pts != null);
		this.pts = pts;
	}
	/**
	 * Determines whether this abstract object may alias with
	 * a given abstract object.
	 * 
	 * @param	that	An abstract object.
	 * 
	 * @return	true iff this abstract object may alias with
	 * 			the given abstract object.
	 */
	public boolean mayAlias(Obj that) {
		for (Ctxt e : pts) {
			if (that.pts.contains(e))
				return true;
		}
		return false;
	}
	public int hashCode() {
		return pts.hashCode();
	}
	public boolean equals(Object that) {
		if (that instanceof Obj)
			return pts.equals(((Obj) that).pts);
		return false;
	}
	public String toString() {
		String s = "[";
		for (Ctxt e : pts) {
			s += " " + e;
		}
		return s + " ]";
	}
}

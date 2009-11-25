/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import chord.project.Program;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import java.io.Serializable;

/**
 * Representation of an abstract context of a method.
 * <p>
 * Each abstract context is a possibly empty sequence of the form
 * <tt>[e1,...,en]</tt> where each <tt>ei</tt> is either an object
 * allocation statement or a method invocation statement in
 * decreasing order of significance.
 * <p>
 * The abstract context corresponding to the empty sequence, called
 * <tt>epsilon</tt>, is the lone context of methods that are
 * analyzed context insensitively.  These include the main method,
 * all class initializer methods, and any additional user-specified
 * methods (see {@link chord.analyses.alias.CtxtsAnalysis}).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Ctxt implements Serializable {
	/**
	 * The sequence of statements comprising the abstract context,
	 * in decreasing order of significance.
	 */
	private final Quad[] elems;
	/**
	 * Constructor.
	 * 
	 * @param	elems	The sequence of statements comprising this
	 * 			abstract context.
	 */
	public Ctxt(Quad[] elems) {
		this.elems = elems;
	}
	/**
	 * Provides the sequence of statements comprising this abstract
	 * context.
	 * 
	 * @return	The sequence of statements comprising this abstract
	 * 			context.
	 */
	public Quad[] getElems() {
		return elems;
	}
	/**
	 * Determines whether this abstract context contains a given
	 * statement.
	 * 
	 * @param	inst	A statement.
	 * 
	 * @return	true iff this abstract context contains the given
	 * 			statement.
	 */
	public boolean contains(Quad inst) {
		for (int i = 0; i < elems.length; i++) {
			if (elems[i] == inst)
				return true;
		}
		return false;
	}
	public int hashCode() {
		int i = 0;
		for (Quad inst : elems)
			i += inst.hashCode();
		return i;
	}
	public boolean equals(Object o) {
        if (!(o instanceof Ctxt))
            return false;
        Ctxt that = (Ctxt) o;
        Quad[] thisElems = this.elems;
        Quad[] thatElems = that.elems;
        int n = thisElems.length;
        if (thatElems.length != n)
            return false;
        for (int i = 0; i < n; i++) {
            Quad inst = thisElems[i];
            if (inst != thatElems[i])
                return false;
        }
        return true;
    }
	public String toString() {
		String s = "[";
		int n = elems.length;
		for (int i = 0; i < n; i++) {
			Quad q = elems[i];
			Operator op = q.getOperator();
			s += (op instanceof Invoke) ?
				Program.v().toStringInvokeInst(q) : Program.v().toStringNewInst(q);
			if (i < n - 1)
				s += ",";
		}
		return s + "]";
	}
}

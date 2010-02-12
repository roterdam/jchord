/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.alias;

import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

import chord.util.graph.ILabeledGraph;
import chord.util.tuple.object.Pair;

/**
 * Specification of a context-sensitive call graph.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface ICSCG extends ILabeledGraph<Pair<Ctxt, jq_Method>, Quad> {
	/**
	 * Provides all abstract contexts in which a given jq_Method
	 * may be reachable.
	 * 
	 * @param	meth	A jq_Method.
	 * 
	 * @return	All abstract contexts in which the jq_Method may
	 * 			be reachable.
	 */
	public Set<Ctxt> getContexts(jq_Method meth);
    /**
     * Provides each jq_Method along with each abstract context in
     * which it may be called by a given jq_Method invocation site
     * in a given abstract context.
     * 
     * @param   ctxt    An abstract context.
     * @param   invk    A jq_Method invocation site.
     * 
     * @return  All (<tt>ctxt2</tt>, <tt>meth</tt>) pairs such that
     * 			jq_Method <tt>meth</tt> may be called in abstract
     *			context <tt>ctxt2</tt> from jq_Method invocation site
     * 			<tt>invk</tt> in abstract context <tt>ctxt</tt>.
     */
	public Set<Pair<Ctxt, jq_Method>>
		getTargets(Ctxt ctxt, Quad invk);
	/**
	 * Provides each jq_Method invocation site along with each abstract
	 * context from which it may call a given jq_Method in a given
	 * abstract context.
	 * 
	 * @param   ctxt    An abstract context.
	 * @param   meth    A jq_Method.
	 * 
	 * @return  All (<tt>ctxt2</tt>, <tt>invk</tt>) pairs such that
	 * 			jq_Method invocation site <tt>invk</tt> in abstract
	 * 			context <tt>ctxt2</tt> may call jq_Method <tt>meth</tt>
	 * 			in abstract context <tt>ctxt</tt>.
	 */
	public Set<Pair<Ctxt, Quad>>
		getCallers(Ctxt ctxt, jq_Method meth);
	/**
    /**
     * Determines whether a given jq_Method invocation site in a given
     * abstract context may call a given jq_Method in a given abstract
     * context.
     * 
	 * @param	ctxt1	An abstract context.
	 * @param	invk	A jq_Method invocation site.
	 * @param	ctxt2	An abstract context.
	 * @param	meth	A jq_Method.
	 * 
	 * @return	true iff jq_Method invocation site <tt>invk</tt> in
	 * 			abstract context <tt>ctxt1</tt> may call jq_Method
	 * 			<tt>meth</tt> in abstract context <tt>ctxt2</tt>.
	 */
	public boolean calls(Ctxt ctxt1, Quad invk,
		Ctxt ctxt2, jq_Method meth);
}

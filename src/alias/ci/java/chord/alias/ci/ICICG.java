/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.alias.ci;

import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

import chord.util.graph.ILabeledGraph;

/**
 * Specification of a context-insensitive call graph.
 * 
 * @author Mayur Naik <mhn@cs.stanford.edu>
 */
public interface ICICG extends ILabeledGraph<jq_Method, Quad> {
    /**
     * Provides all jq_Methods that may be called by a given jq_Method
     * invocation site.
     * 
     * @param   invk    A jq_Method invocation site.
     * 
     * @return  All jq_Methods that may be called by the given jq_Method
     * 			invocation site.
     */
    public Set<jq_Method> getTargets(Quad invk);
    /**
     * Provides all jq_Method invocation sites that may call a given
     * jq_Method.
     * 
     * @param   meth    A jq_Method.
     * 
     * @return  All jq_Method invocation sites that may call the
     * 			given jq_Method.
     */
    public Set<Quad> getCallers(jq_Method meth);
    /**
     * Determines whether a given jq_Method invocation site may call
     * a given jq_Method.
     * 
     * @param	invk	A jq_Method invocation site.
     * @param	meth	A jq_Method.
     * 
     * @return	true iff the given jq_Method invocation site may call
     * 			the given jq_Method.
     */
    public boolean calls(Quad invk, jq_Method meth);
}

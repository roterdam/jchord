/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import java.util.Set;
import java.util.Collections;

import joeq.Compiler.Quad.Quad;
import joeq.Class.jq_Reference;

import chord.util.ArraySet;
import chord.util.tuple.object.Pair;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class ReflectInfo {
	private final Set<jq_Reference> reflectClasses;
	private final Set<Pair<Quad, Set<jq_Reference>>> resolvedForNameSites;
	private final Set<Pair<Quad, Set<jq_Reference>>> resolvedNewInstSites;
	public ReflectInfo(Set<jq_Reference> _reflectClasses,
			Set<Pair<Quad, Set<jq_Reference>>> _resolvedForNameSites,
			Set<Pair<Quad, Set<jq_Reference>>> _resolvedNewInstSites) {
		reflectClasses = _reflectClasses;
		resolvedForNameSites = _resolvedForNameSites;
		resolvedNewInstSites = _resolvedNewInstSites;
	}
	public ReflectInfo() {
		reflectClasses = new ArraySet<jq_Reference>();
		resolvedForNameSites = new ArraySet<Pair<Quad, Set<jq_Reference>>>();
		resolvedNewInstSites = new ArraySet<Pair<Quad, Set<jq_Reference>>>();
	}
    /**
     * Provides all classes whose objects may be reflectively loaded.
     */
    public Set<jq_Reference> getReflectClasses() {
		return reflectClasses;
    }
    /**
     * Provides a map from each java.lang.Class.forName(s) call site,
	 * whose argument s could be resolved, to the names of all classes
     * to which it was resolved.
     */
    public Set<Pair<Quad, Set<jq_Reference>>> getResolvedForNameSites() {
		return resolvedForNameSites;
    }
    /**
     * Provides each java.lang.Object.newInstance() call site whose
	 * receiver's class could be resolved.
     */
    public Set<Pair<Quad, Set<jq_Reference>>> getResolvedNewInstSites() {
        return resolvedNewInstSites;
    }
	public boolean addReflectClass(jq_Reference c) {
		return reflectClasses.add(c);
	}
	public boolean addResolvedNewInstSite(Quad q, jq_Reference c) {
		for (Pair<Quad, Set<jq_Reference>> p : resolvedNewInstSites) {
			if (p.val0 == q) {
				Set<jq_Reference> s = p.val1;
				return s.add(c);
			}
		}
		Set<jq_Reference> s = new ArraySet<jq_Reference>(2);
		s.add(c);
		resolvedNewInstSites.add(new Pair<Quad, Set<jq_Reference>>(q, s));
		return true;
	}
	public boolean addResolvedForNameSite(Quad q, jq_Reference c) {
		for (Pair<Quad, Set<jq_Reference>> p : resolvedForNameSites) {
			if (p.val0 == q) {
				Set<jq_Reference> s = p.val1;
				return s.add(c);
			}
		}
		Set<jq_Reference> s = new ArraySet<jq_Reference>(2);
		s.add(c);
		resolvedForNameSites.add(new Pair<Quad, Set<jq_Reference>>(q, s));
		return true;
	}
}

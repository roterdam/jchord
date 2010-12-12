/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.program;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import joeq.Compiler.Quad.Quad;
import joeq.Class.jq_Reference;

import chord.util.tuple.object.Pair;

/**
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Reflect {
	private final List<Pair<Quad, List<jq_Reference>>> resolvedClsForNameSites;
	private final List<Pair<Quad, List<jq_Reference>>> resolvedObjNewInstSites;
	private final List<Pair<Quad, List<jq_Reference>>> resolvedConNewInstSites;
	private final List<Pair<Quad, List<jq_Reference>>> resolvedAryNewInstSites;

	public Reflect(
			List<Pair<Quad, List<jq_Reference>>> _resolvedClsForNameSites,
			List<Pair<Quad, List<jq_Reference>>> _resolvedObjNewInstSites,
			List<Pair<Quad, List<jq_Reference>>> _resolvedConNewInstSites,
			List<Pair<Quad, List<jq_Reference>>> _resolvedAryNewInstSites) {
		resolvedClsForNameSites = _resolvedClsForNameSites;
		resolvedObjNewInstSites = _resolvedObjNewInstSites;
		resolvedConNewInstSites = _resolvedConNewInstSites;
		resolvedAryNewInstSites = _resolvedAryNewInstSites;
	}
	public Reflect() {
		resolvedClsForNameSites = new ArrayList<Pair<Quad, List<jq_Reference>>>();
		resolvedObjNewInstSites = new ArrayList<Pair<Quad, List<jq_Reference>>>();
		resolvedConNewInstSites = new ArrayList<Pair<Quad, List<jq_Reference>>>();
		resolvedAryNewInstSites = new ArrayList<Pair<Quad, List<jq_Reference>>>();
	}
	/**
	 * Provides a map from each java.lang.Class.forName(s) call site,
	 * whose argument s could be resolved, to the names of all classes
	 * to which it was resolved.
	 */
	public List<Pair<Quad, List<jq_Reference>>> getResolvedClsForNameSites() {
		return resolvedClsForNameSites;
	}
	/**
	 * Provides each java.lang.Object.newInstance() call site whose
	 * receiver's class could be resolved.
	 */
	public List<Pair<Quad, List<jq_Reference>>> getResolvedObjNewInstSites() {
		return resolvedObjNewInstSites;
	}
	/**
	 */
	public List<Pair<Quad, List<jq_Reference>>> getResolvedConNewInstSites() {
		return resolvedConNewInstSites;
	}
	/**
	 */
	public List<Pair<Quad, List<jq_Reference>>> getResolvedAryNewInstSites() {
		return resolvedAryNewInstSites;
	}
	public boolean addResolvedClsForNameSite(Quad q, jq_Reference c) {
		return add(resolvedClsForNameSites, q, c);
	}
	public boolean addResolvedObjNewInstSite(Quad q, jq_Reference c) {
		return add(resolvedObjNewInstSites, q, c);
	}
	public boolean addResolvedConNewInstSite(Quad q, jq_Reference c) {
		return add(resolvedConNewInstSites, q, c);
	}
	public boolean addResolvedAryNewInstSite(Quad q, jq_Reference c) {
		return add(resolvedAryNewInstSites, q, c);
	}
	private static boolean add(List<Pair<Quad, List<jq_Reference>>> l, Quad q, jq_Reference c) {
		for (Pair<Quad, List<jq_Reference>> p : l) {
			if (p.val0 == q) {
				List<jq_Reference> s = p.val1;
				if (s.contains(c))
					return false;
				s.add(c);
				return true;
			}
		}
		List<jq_Reference> s = new ArrayList<jq_Reference>(2);
		s.add(c);
		l.add(new Pair<Quad, List<jq_Reference>>(q, s));
		return true;
	}
}

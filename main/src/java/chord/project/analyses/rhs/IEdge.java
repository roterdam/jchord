/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project.analyses.rhs;

/**
 * Specification of a path or summary edge in the Reps-Horwitz-Sagiv
 * algorithm for summary-based dataflow analysis.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IEdge {
	/**
	 * Determines whether the source node of a given path or
	 * summary edge is identical to the source node of this edge.
	 *
	 * @param edge	A path or summary edge.
	 *
	 * @returns		true iff the source node of <code>edge</code>
	 *				is identical to the source node of this edge.
	 */
	public boolean matchesSrcNodeOf(IEdge edge);
	/**
	 * Merges the given path or summary edge with this edge.
	 * This edge is mutated but the given edge is not.  The source
	 * nodes of the given edge and this edge are guaranteed to be
	 * identical.
	 *
	 * @param edge	A path or summary edge.
	 *
	 * @returns		true iff this edge changes due to the merge.
	 */
	public boolean mergeWith(IEdge edge);
}


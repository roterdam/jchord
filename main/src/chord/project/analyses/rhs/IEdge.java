/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.project.analyses.rhs;

/**
 * Specification of a path edge or a summary edge in the Reps-Horwitz-Sagiv
 * algorithm for context-sensitive dataflow analysis.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IEdge {
	/**
	 * Determines whether the given path or summary edge can merge with this
	 * edge. The two edges can merge iff they satisfies one of the following
	 * conditions:
	 * <ol> 
	 * <li>RHSAnalysis.mustMerge returns true</li>
	 * <li>RHSAnalysis.mustMerge returns false but RHSAnalysis.mayMerge returns
	 * true; the source nodes of the two edges are identical; one of the target
	 * node subsumes another</li>
	 * </ol>
	 * @param edge
	 *            A path or summary edge.
	 * 
	 * @return <ol>
	 * <li>-1 if these two edges cannot be merged</li>
	 * <li>0 if these two edges are identical</li>
	 * <li>1 if these two edges can be merged, and the return value would be identical to this.</li>
	 * <li>2 if these two edges can be merged, and the return value would be identical to the parameter</li>
	 * <li>3 if these two edges can be merged, but there is no info about the return value</li>
	 * </ol>
	 * 
	 */
	public int canMerge(IEdge edge);

	/**
	 * Merges the given path or summary edge with this edge. This edge is
	 * mutated but the given edge is not. The source nodes of the given edge and
	 * this edge are guaranteed to be identical.
	 * 
	 * @param edge
	 *            A path or summary edge.
	 * 
	 * @return true iff this edge changes due to the merge.
	 */
	public boolean mergeWith(IEdge edge);
	
	public boolean matchSourse(IEdge edge);
}

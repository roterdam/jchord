/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util.graph;

/**
 * Specification of a visitor over entities of a directed graph.
 * <p>
 * An entity may be, for instance, a strongly-connected component of
 * the graph or a path in the graph.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IGraphEntityVisitor<Node> {
	/**
	 * Method called just before starting to visit any node of
	 * an entity.
	 */
	public void prologue();
	/**
	 * Method called while visiting each node of an entity.
	 * 
	 * @param	node	The visited node.
	 */
	public void visit(Node node);
	/**
	 * Method called just after finishing visiting all nodes of
	 * an entity.
	 */
	public void epilogue();
}

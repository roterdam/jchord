package chord.project;

import joeq.Compiler.Quad.ControlFlowGraph;

public interface ICFGVisitor<T> {
	public T visit(ControlFlowGraph cfg);
}

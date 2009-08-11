package chord.project;

import joeq.Compiler.Quad.ControlFlowGraph;

public interface ICFGVisitor {
	public void visit(ControlFlowGraph cfg);
}

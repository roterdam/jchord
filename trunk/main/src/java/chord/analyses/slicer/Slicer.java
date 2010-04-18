package chord.analyses.slicer;

import chord.project.analyses.JavaAnalysis;
import chord.project.Chord;
import chord.program.Program;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Dominators;
import joeq.Compiler.Quad.Dominators.DominatorNode;
import jwutil.collections.Pair;

// import chord.project.analyses.PathProgramAnalysis;

@Chord(
	name="slicer-java"
)
public class Slicer extends JavaAnalysis {
	public void run() {
		// take as input a set of seeds: pairs of (P,F)
		// build dynamic call graph
        jq_Method m = Program.v().getMainMethod();
        ControlFlowGraph cfg = m.getCFG();
		Dominators dom = new Dominators();
		dom.visitCFG(cfg);
		DominatorNode n = dom.computeTree();
		n.dumpTree();
	}
}

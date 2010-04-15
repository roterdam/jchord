package chord.analyses.localvar;

import java.util.Map;

import chord.project.analyses.JavaAnalysis;
import chord.program.Program;
import chord.project.Chord;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import jwutil.collections.Pair;
import joeq.Class.jq_LocalVarTableEntry;

@Chord(
	name="localvar-java"
)
public class LocalVarAnalysis extends JavaAnalysis {
	public void run() {
		jq_Method m = Program.v().getMainMethod();
		ControlFlowGraph cfg = m.getCFG();
		RegisterFactory rf = cfg.getRegisterFactory();
		Map<Pair,Register> localNumberingMap = rf.getLocalNumberingMap();
		Map<Pair,Register> stackNumberingMap = rf.getStackNumberingMap();
		System.out.println("local:");
		for (Map.Entry<Pair,Register> e : localNumberingMap.entrySet()) {
			System.out.println(e);
		}
		System.out.println("stack:");
		for (Map.Entry<Pair,Register> e : stackNumberingMap.entrySet()) {
			System.out.println(e);
		}
		System.out.println("table:");
		jq_LocalVarTableEntry[] table = m.getLocalTable();
		for (jq_LocalVarTableEntry entry : table)
			System.out.println("entry: " + entry);
	}
}


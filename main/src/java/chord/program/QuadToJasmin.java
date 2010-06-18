package chord.program;

import chord.program.Program;
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.QuadVisitor.EmptyVisitor;

import chord.util.IndexSet;

public class QuadToJasmin {
	private static final JasminQuadVisitor visitor = new JasminQuadVisitor();
	public static void main(String[] args) {
		Program program = Program.getProgram();
		IndexSet<jq_Reference> classes = program.getClasses();
		for (jq_Reference r : classes) {
			if (r instanceof jq_Array)
				continue;
			jq_Class c = (jq_Class) r;
			String fileName = Program.getSourceFileName(c);
			if (fileName != null)
				System.out.println(".source " + fileName);
			// TODO: read joeq.Class.jq_Class to get things like
			// access modifier, superclass, implemented interfaces, fields, etc.
			// superclass will be null iff c == javalangobject
			for (jq_Method m : c.getDeclaredStaticMethods()) {
				processMethod(m);
            }
            for (jq_Method m : c.getDeclaredInstanceMethods()) {
				processMethod(m);
            }
		}
	}
	private static void processMethod(jq_Method m) {
		// TODO: output signature etc.
		if (m.isAbstract())
			return;
		ControlFlowGraph cfg = m.getCFG();
		RegisterFactory rf = cfg.getRegisterFactory();
		// see src/java/chord/analyses/sandbox/LocalVarAnalysis.java if you want
		// to distinguish local vars from stack vars 
		// see src/java/chord/rels/RelMmethArg.java to see how to distinguish
		// formal arguments from temporary variables
        for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
                it.hasNext();) {
            BasicBlock bb = it.nextBasicBlock();
			// TODO: generate label of bb
            for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
                Quad q = it2.nextQuad();
				q.accept(visitor);
			}
		}
	}

}

class JasminQuadVisitor extends EmptyVisitor {
	// see src/joeq/Compiler/Quad/QuadVisitor.java
	public void visitAload(Quad q) {
		// TODO: generate jasmin code for quad q
	}
	// TODO: add all other visit* methods from EmptyVisitor
}

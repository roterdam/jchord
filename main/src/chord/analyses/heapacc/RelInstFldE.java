package chord.analyses.heapacc;

import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Putstatic;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing each quad that accesses (reads or writes) an instance field
 * (as opposed to a static field or an array element).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
@Chord(
    name = "instFldE",
    sign = "E0"
)
public class RelInstFldE extends ProgramRel {
    public void fill() {
        DomE domE = (DomE) doms[0];
        int numE = domE.size();
        for (int eIdx = 0; eIdx < numE; eIdx++) {
            Quad e = (Quad) domE.get(eIdx);
            Operator op = e.getOperator();
            if (!(op instanceof Getstatic) && !(op instanceof Putstatic) && !(op instanceof ALoad) && !(op instanceof AStore))
                add(eIdx);
        }
    }
}

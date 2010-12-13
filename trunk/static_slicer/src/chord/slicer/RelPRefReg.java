package chord.slicer;

import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Util.Templates.List;
import chord.analyses.point.DomP;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

/**
 * Relation containing tuples (p, u) such that a register u is
 * referred at a program point p.
 * @author sangmin
 *
 */
@Chord(
		name = "PRefReg",
		sign = "P0,U0:P0_U0"
)
public class RelPRefReg extends ProgramRel {
	public void fill() {
		DomP domP = (DomP) doms[0];
		int numP = domP.size();
		for (int j=0; j < numP; j++) {
			Inst inst = domP.get(j);
			if (inst instanceof Quad) {
				Quad q = (Quad) inst;
				List.RegisterOperand l = q.getUsedRegisters();
				int numRegs = l.size();
				for (int i=0; i < numRegs; i++) {
					RegisterOperand ro = l.getRegisterOperand(i);
					if (ro != null)
						add(inst, ro.getRegister());
				}
			}
		}
	}

}

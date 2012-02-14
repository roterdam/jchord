package chord.analyses.libanalysis;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import chord.analyses.alloc.DomH;
import chord.analyses.method.DomM;
import chord.analyses.type.DomT;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.program.Reflect;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.ProgramRel;

@Chord(
		name = "allVH",
		sign = "V0,H0:V0_H0"
		)
public class RelAllVH extends ProgramRel {
	private DomH domH;
	private DomV domV;

	@Override
	public void fill() {
		DomV domV = (DomV) doms[0];
		DomH domH = (DomH) doms[1];
		for(int vIdx = 0; vIdx < domV.size(); vIdx++)
			for(int hIdx = 0; hIdx < domH.size(); hIdx++){
				add(vIdx, hIdx);
			}
	}
}

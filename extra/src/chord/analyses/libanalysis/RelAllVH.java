package chord.analyses.libanalysis;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.method.DomM;
import chord.analyses.type.DomT;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.program.Reflect;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

@Chord(
		name = "allVH",
		consumes = { "allV", "allH" },
		sign = "V0,H0:V0_H0"
		)
public class RelAllVH extends ProgramRel {
	private DomH domH;
	private DomV domV;
	private ProgramRel relAllV;
	private ProgramRel relAllH;
	String trackedPkgs;
	String[] trackedPkgsArr;

	@Override
	public void fill() {
		domV = (DomV) doms[0];
		domH = (DomH) doms[1];
		trackedPkgs = System.getProperty("chord.tracked.packages",null);
		trackedPkgsArr = Utils.toArray(trackedPkgs);

		if(trackedPkgs == null){
			for(int vIdx = 0; vIdx < domV.size(); vIdx++)
				for(int hIdx = 0; hIdx < domH.size(); hIdx++){
					add(vIdx, hIdx);
				}
		}else{
			relAllH   = (ProgramRel) ClassicProject.g().getTrgt("allH");
			relAllV   = (ProgramRel) ClassicProject.g().getTrgt("allV");
			relAllH.load();
			relAllV.load();
			
			Iterable<Register> itr1 = relAllV.getAry1ValTuples();
			Iterable<Quad> itr2 = relAllH.getAry1ValTuples();
			
			for(Register i1 : itr1)
				for(Quad i2 : itr2){
					add(i1,i2);
			}
/*			for(int vIdx = 0; vIdx < domV.size(); vIdx++)
				for(int hIdx = 1; hIdx < domH.size(); hIdx++){
					Quad q = (Quad) domH.get(hIdx);
					jq_Method m1 = q.getMethod();
					Register r = (Register) domV.get(vIdx);
					jq_Method m2 = domV.getMethod(r);
					if(isTracked(m1, m2)){
						add(vIdx, hIdx);
					}

				}
				*/
		}
	}

/*	private boolean isTracked(jq_Method m1, jq_Method m2){
		String cName1 = m1.getDeclaringClass().getName();
		String cName2 = m2.getDeclaringClass().getName();
		boolean isInc1 = false;
		boolean isInc2 = false;

		for (String c : trackedPkgsArr) {
			if (cName1.startsWith(c))
				isInc1 = true;
			if (cName2.startsWith(c))
				isInc2 = true;
		}

		return (isInc1 & isInc2);
	}
*/	
}

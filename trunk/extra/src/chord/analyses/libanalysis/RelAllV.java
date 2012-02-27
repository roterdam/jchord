package chord.analyses.libanalysis;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

@Chord(
		name = "allV",
		sign = "V0"
		)
public class RelAllV extends ProgramRel {
	private DomV domV;
	String trackedPkgs;
	String[] trackedPkgsArr;

	@Override
	public void fill() {		
		domV = (DomV) doms[0];
		trackedPkgs = System.getProperty("chord.tracked.packages",null);
		trackedPkgsArr = Utils.toArray(trackedPkgs);

		if(trackedPkgs == null){
			for(int vIdx = 0; vIdx < domV.size(); vIdx++)
				add(vIdx);
		}else{
			for(int vIdx = 0; vIdx < domV.size(); vIdx++){
				Register r = (Register) domV.get(vIdx);
				jq_Method m = domV.getMethod(r);
				if(isTracked(m)){
					add(vIdx);

				}
			}
		}
	}

	private boolean isTracked(jq_Method m){
		String cName = m.getDeclaringClass().getName();
		boolean isInc = false;

		for (String c : trackedPkgsArr) {
			if (cName.startsWith(c))
				isInc = true;
		}

		return (isInc);
	}
}
package chord.analyses.libanalysis;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

@Chord(
		name = "allH",
		sign = "H0"
		)
public class RelAllH extends ProgramRel {
	private DomH domH;
	String trackedPkgs;
	String[] trackedPkgsArr;

	@Override
	public void fill() {
		domH = (DomH) doms[0];
		trackedPkgs = System.getProperty("chord.tracked.packages",null);
		trackedPkgsArr = Utils.toArray(trackedPkgs);

		if(trackedPkgs == null){
			for(int hIdx = 0; hIdx < domH.size(); hIdx++)
				add(hIdx);
		}else{
			add(0);
			for(int hIdx = 1; hIdx < domH.size(); hIdx++){
				Quad q = (Quad) domH.get(hIdx);
				jq_Method m = q.getMethod();
				if(isTracked(m)){
					add(hIdx);

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

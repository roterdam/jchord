package chord.analyses.libanalysis;

import chord.analyses.alloc.DomH;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
		name = "allV",
		sign = "V0"
		)
public class RelAllV extends ProgramRel {
	private DomV domV;

	@Override
	public void fill() {
		DomV domV = (DomV) doms[0];
		for(int vIdx = 0; vIdx < domV.size(); vIdx++)
			add(vIdx);
	}
}
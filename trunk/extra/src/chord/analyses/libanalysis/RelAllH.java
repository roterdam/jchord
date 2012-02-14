package chord.analyses.libanalysis;

import chord.analyses.alloc.DomH;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
		name = "allH",
		sign = "H0"
		)
public class RelAllH extends ProgramRel {
	private DomH domH;

	@Override
	public void fill() {
		DomH domH = (DomH) doms[0];
		for(int hIdx = 0; hIdx < domH.size(); hIdx++)
			add(hIdx);
	}
}

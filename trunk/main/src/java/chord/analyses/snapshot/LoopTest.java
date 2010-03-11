/**
 * 
 */
package chord.analyses.snapshot;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.OutDirUtils;
import chord.project.analyses.DynamicAnalysis;
import chord.util.IndexMap;

/**
 * @author omert
 *
 */
@Chord(name="dynamic-loop-java")
public class LoopTest extends DynamicAnalysis {

	private IndexMap<String> Wmap;
	private InstrScheme scheme;
	
	@Override
	public InstrScheme getInstrScheme() {
		if (scheme != null) {
			return scheme;
		}
		scheme = new InstrScheme();
		scheme.setEnterAndLeaveLoopEvent();
		return scheme;
	}
	
	@Override
	public void initAllPasses() {
		super.initAllPasses();
		Wmap = instrumentor.getWmap();
	}
	
	@Override
	public void processEnterLoop(int w, int t) {
		String s = Wmap.get(w);
		if (s.contains("V@T")) {
			OutDirUtils.logOut("%s", "Entered loop: " + s);
			OutDirUtils.logOut("%s", "Loop id: " + w);
		}
	}
	
	@Override
	public void processLeaveLoop(int w, int t) {
		String s = Wmap.get(w);
		if (s.contains("V@T")) {
			OutDirUtils.logOut("%s", "Exited loop: " + s);
		}
	}
	
	@Override
	public void processLoopIteration(int w, int t) {
		String s = Wmap.get(w);
		if (s.contains("V@T")) {
			OutDirUtils.logOut("%s", "Loop iteration began: " + s);
		}
	}
}

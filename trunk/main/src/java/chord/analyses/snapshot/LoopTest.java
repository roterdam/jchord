/**
 * 
 */
package chord.analyses.snapshot;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.util.IndexMap;

/**
 * @author omertripp
 *
 */
@Chord(name="dynamic-loop-java")
public class LoopTest extends DynamicAnalysis {

	private IndexMap<String> Bmap;
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
		Bmap = instrumentor.getBmap();
	}
	
	@Override
	public void processEnterLoop(int w, int t) {
		String s = Bmap.get(w);
		if (s.contains("V@T")) {
			Messages.logAnon("Entered loop: " + s);
			Messages.logAnon("Loop id: " + w);
		}
	}
	
	@Override
	public void processLeaveLoop(int w, int t) {
		String s = Bmap.get(w);
		if (s.contains("V@T")) {
			Messages.logAnon("Exited loop: " + s);
		}
	}
	
	@Override
	public void processLoopIteration(int w, int t) {
		String s = Bmap.get(w);
		if (s.contains("V@T")) {
			Messages.logAnon("Loop iteration began: " + s);
		}
	}
}

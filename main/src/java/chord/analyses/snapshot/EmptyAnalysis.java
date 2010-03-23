/**
 * Do nothing.
 * Used to collect the trace.
 */
package chord.analyses.snapshot;

import chord.project.Chord;

@Chord(name = "ss-empty")
public class EmptyAnalysis extends SnapshotAnalysis {
	@Override public String propertyName() { return "empty"; }
  @Override public void abstractionChanged(int o, Object a) { }
}

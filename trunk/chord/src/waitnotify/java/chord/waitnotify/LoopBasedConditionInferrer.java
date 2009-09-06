package chord.waitnotify;

import chord.instr.InstrScheme;
import chord.project.DynamicAnalysis;

public class LoopBasedConditionInferrer extends DynamicAnalysis {
    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
		instrScheme.setConvert();

    	instrScheme.setGetstaticPrimitiveEvent(true, true, true);
    	instrScheme.setPutstaticPrimitiveEvent(true, true, true);
    	instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setAloadPrimitiveEvent(true, true, true, true);
    	instrScheme.setAstorePrimitiveEvent(true, true, true, true);

    	instrScheme.setGetstaticReferenceEvent(true, true, false, true);
    	instrScheme.setPutstaticReferenceEvent(true, true, false, true);
    	instrScheme.setGetfieldReferenceEvent(true, true, false, false, true);
    	instrScheme.setPutfieldReferenceEvent(true, true, false, false, true);
    	instrScheme.setAloadReferenceEvent(true, true, false, false, true);
    	instrScheme.setAstoreReferenceEvent(true, true, false, false, true);

    	instrScheme.setThreadStartEvent(true, true, true);
    	instrScheme.setThreadJoinEvent(true, true, true);
    	instrScheme.setAcquireLockEvent(true, true, true);
    	instrScheme.setReleaseLockEvent(true, true, true);
    	instrScheme.setWaitEvent(true, true, true);
    	instrScheme.setNotifyEvent(true, true, true);
    	return instrScheme;
    }
	public void initPass() {
		// do nothing for now
	}
	public void donePass() {
		// do nothing for now
	}
	public void doneAllPasses() {
		// TODO: print all wait's
	}
}

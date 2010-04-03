/**
 * 
 */
package chord.analyses.stats;

import java.util.HashSet;
import java.util.Set;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import chord.doms.DomM;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;
import chord.project.Project;

/**
 * @author omert
 *
 */
@Chord(name = "dynamic-visitcnt-java")
public class VisitedCountAnalysis extends DynamicAnalysis {
	
	private final static String[] LIB_PREFIXES = new String[] {
		"sun.",
		"com.sun.",
		"com.ibm.jvm.",
		"com.ibm.oti.",
		"com.ibm.misc.",
		"org.apache.harmony.",
		"joeq.",
		"jwutil.",
		"java.",
		"javax."
	};
	
	private InstrScheme instrScheme;
	private DomM domM;
	private final Set<jq_Method> visitedMethods = new HashSet<jq_Method>(16);
	private final Set<jq_Method> visitedAppMethods = new HashSet<jq_Method>(16);
	private final Set<jq_Class> visitedClasses = new HashSet<jq_Class>(16);
	private final Set<jq_Class> visitedAppClasses = new HashSet<jq_Class>(16);
	private long totalBytecode = 0;
	private long totalAppBytecode = 0;

	public InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterAndLeaveMethodEvent();
		return instrScheme;
	}
	
	public void initAllPasses() {
    super.initAllPasses();
    domM = (DomM)Project.getTrgt("M"); Project.runTask(domM);
	}
	
  @Override public void processEnterMethod(int m, int t) {
		if (m >= 0) {
			jq_Method mthd = domM.get(m);
			jq_Class klass = mthd.getDeclaringClass();
			visitedClasses.add(klass);
			byte[] bytecode = mthd.getBytecode();
			if (visitedMethods.add(mthd)) {
				totalBytecode += bytecode.length;
			}
			if (isAppMethod(mthd)) {
				visitedAppClasses.add(klass);
				if (visitedAppMethods.add(mthd)) {
					totalAppBytecode += bytecode.length;
				}
			}
		}
  }
    
    public void processLeaveMethod(int m, int t) {
    	/* Do nothing. */
    }
    
    private boolean isAppMethod(jq_Method m) {
    	String className = m.getDeclaringClass().getName();
//    	Messages.logAnon("%s", className);
    	for (String pref : LIB_PREFIXES) {
    		if (className.startsWith(pref)) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public void doneAllPasses() {
    	Messages.logAnon("=== # of visited classes: %d ===", visitedClasses.size());
		Messages.logAnon("=== # of visited app. classes: %d ===", visitedAppClasses.size());
		Messages.logAnon("=== # of visited methods: %d ===", visitedMethods.size());
		Messages.logAnon("=== # of visited app. methods: %d ===", visitedAppMethods.size());
		Messages.logAnon("=== Total bytecode: %d ===", totalBytecode);
		Messages.logAnon("=== Total app. bytecode: %d ===", totalAppBytecode);
	}
}

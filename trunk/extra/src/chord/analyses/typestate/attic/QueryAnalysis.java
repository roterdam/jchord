@Chord(name="query-java",
	produces={ "queryIHS", "S" },
	namesOfTypes = { "queryIHS", "S" },
	types = { ProgramRel.class, ProgramDom.class },
	namesOfSigns = { "queryIHS" },
	signs = { "I0,H0,S0:I0_H0_S0" }
)
public class QueryAnalysis extends JavaAnalysis {
    @Override
    public void run() {
		Step 1: parse typestate file
        // type to be tracked (e.g. Lock)
        // initial state (e.g. UNLOCKED)
        // list of methods, transitions (e.g., acqLock: UNLOCKED -> LOCKED)
        // list of asserts (e.g., acqLock: UNLOCKED)
		Step 2: Build domain S of all type states; save it after building
        Step 3: Build relation queryIHS to collect all queries; save it after building
		a query is a pair consisting of an assert point i in domain I and an allocation site h in domain H
        that 0-cfa pointer analysis says might be pointed to by the this
        argument of call site at i.  Add each such (i, h) to relation queryIH
        // Will need all reachable methods via Program.g().getMethods() and
		// pointer analysis:
		// runTask CIPAAnalysis and then use method "public CIObj pointsTo(Register var)" in CIPAAnalysis
		// to obtain h that is pointed to by this variable of i
	}
}


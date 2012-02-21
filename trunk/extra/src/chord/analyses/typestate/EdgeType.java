package chord.analyses.typestate;
public enum EdgeType {
	NULL,  // <null, null, null>
	ALLOC, // <null, h, null|X>
    SUMMARY  // <X, null, X'>
}

package chord.analyses.typestate.metaback;

public enum EdgeKind {
    NULL,  // <null, null, null>
    ALLOC, // <null, h, null> or <null, h, AS>
    FULL   // <AS, h, AS'>
}

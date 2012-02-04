@Chord(
    name = "typestate-java", consumes = "queryIHS"
)
public class TypestateAnalysis extends RHSAnalysis<Edge, Edge> {
	public void run() {
		Set<Trio<Quad, Quad, ???>> queries;
		for (each tuple in queryIHS) {
			queries.add(tuple);
		}
	}
    @Override
    public ICICG getCallGraph() {
        if (cicg == null) {
            CICGAnalysis cicgAnalysis =
                (CICGAnalysis) ClassicProject.g().getTrgt("cicg-java");
            ClassicProject.g().runTask(cicgAnalysis);
            cicg = cicgAnalysis.getCallGraph();
        }
        return cicg;
    }
}

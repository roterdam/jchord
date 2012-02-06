package chord.analyses.typestate;

import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.ICICG;
import chord.program.Location;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

@Chord(
    name = "typestate-java", consumes = "queryIHS"
)
public class TypestateAnalysis extends RHSAnalysis<Edge, Edge> {
	
	private ICICG cicg;
	
	static{
		String stateSpecFile = System.getProperty("chord.typestate.specfile", "typestatespec.txt");
		if(!TypeStateParser.parseStateSpec(stateSpecFile)){
			Messages.fatal("Problem occured while parsing state spec file:"+stateSpecFile+",Make sure that its in the required format");
		}
		
	}
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
	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Edge getInitPathEdge(Quad q, jq_Method m, Edge pe) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Edge getCopy(Edge pe) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean mayMerge() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean mustMerge() {
		// TODO Auto-generated method stub
		return false;
	}
}

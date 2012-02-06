package chord.analyses.typestate;

import java.util.Set;

import jdd.util.Array;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.ICICG;
import chord.program.Location;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Messages;
import chord.project.analyses.rhs.RHSAnalysis;
import chord.util.ArraySet;
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
	
	public boolean needToAnalyze(jq_Method m){
		boolean ret= false;
		/* here we need to write logic to check if 'm' calls any method 
		 * directly or transitively that changes the state (as given in the state spec)
		 */
		return ret;
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
    
    private Edge getRootPathEdge(jq_Method m) {
    	//Need to be more specific on what need to be stored
    	return new Edge(new ArraySet<AbstractState>(), new ArraySet<AbstractState>());
    }
    
	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		Set<jq_Method> roots = cicg.getRoots();
		Set<Pair<Location, Edge>> initPEs =
			new ArraySet<Pair<Location, Edge>>(roots.size());
		for (jq_Method m : roots) {
			if(needToAnalyze(m))
			{		
				Edge pe = getRootPathEdge(m);
				BasicBlock bb = m.getCFG().entry();
				Location loc = new Location(m, bb, -1, null);
				Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
				initPEs.add(pair);
			}
		}
		return initPEs;
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

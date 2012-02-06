package chord.analyses.typestate;

import java.util.Set;

import com.sun.org.apache.bcel.internal.generic.Type;

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
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;

@Chord(
    name = "typestate-java", consumes = "queryIHS"
)
public class TypestateAnalysis extends RHSAnalysis<Edge, Edge> {
	
	private ICICG cicg;
	private static DomM domM;
	private static DomI domI;
	
	static{
		String stateSpecFile = System.getProperty("chord.typestate.specfile", "typestatespec.txt");
		if(!TypeStateParser.parseStateSpec(stateSpecFile)){
			Messages.fatal("Problem occured while parsing state spec file:"+stateSpecFile+",Make sure that its in the required format");
		}
		
	}
	public void run() {
		domI = (DomI) ClassicProject.g().getTrgt("I");
        ClassicProject.g().runTask(domI);
        domM = (DomM) ClassicProject.g().getTrgt("M");
        ClassicProject.g().runTask(domM);
		Set<Trio<Quad, Quad, ???>> queries;
		for (each tuple in queryIHS) {
			queries.add(tuple);
		}
	}
	
	public boolean needToAnalyze(jq_Method m){
		boolean ret= false;
		/* here we need to write logic to check if 'm' calls any method 
		 * directly or transitively that changes the state (as given in the state spec)
		 * and we also validate if method m is on of the method that cases transition in which case
		 * we don't need to analyze it. 
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
    	//Need to be more specific on what needs to be stored here
    	return new Edge(new ArraySet<AbstractState>(), new ArraySet<AbstractState>());
    }
    
	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		Set<jq_Method> roots = cicg.getRoots();
		Set<Pair<Location, Edge>> initPEs =
			new ArraySet<Pair<Location, Edge>>();
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
		//How to handle new?
		Edge pe2=null;
		if(needToAnalyze(m)){
			//if the method invocation is for the method that could possibly change the 
			//state of object ( of the required type) then analyze it
			
		}
		return pe2;
	}
	
	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		//Keep track of state depending on the type of operation suggested by quad 
		//if its a move(or assignment) then do strong update..
		return null;
	}
	
	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		Edge pe2 = clrPE;
		//How to handle New ?
		if(needToAnalyze(m)){
			//if this method is of interest then try to apply summary
		} else if(TypeStateSpec.isMethodOfInterest(m.getName().toString())){
			//here depending on the possible state transitions
			//of method m, set the state of the object pointed by q
			//either to one of the target state or error state (if asserts fail)
		}		
		return pe2;
	}
	@Override
	public Edge getCopy(Edge pe) {		
		return new Edge(pe.srcNode,pe.dstNode);
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
		return true;
	}
}

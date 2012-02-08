package chord.analyses.typestate;

import java.util.Set;

import org.hsqldb.lib.StringConverter;

import soot.JastAddJ.Access;

import com.sun.org.apache.bcel.internal.generic.Type;

import jdd.util.Array;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.UTF.Utf8;
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
import chord.analyses.var.DomV;

//This is required to determine if the quad is allocating object of desired type
import chord.analyses.alloc.DomH;
import chord.analyses.alloc.RelHT;
import chord.analyses.escape.hybrid.full.DstNode;

import chord.project.analyses.ProgramRel;


@Chord(
    name = "typestate-java", consumes = "queryIHS"
)
public class TypestateAnalysis extends RHSAnalysis<Edge, Edge> {
	
	private ICICG cicg;
	private static DomM domM;
	private static DomI domI;
	private static DomV domV;
	private static DomH domH;
	private TypeStateSpec sp;
	
	private MyQuadVisitor qv = new MyQuadVisitor();
	
	public void run() {
		String stateSpecFile = System.getProperty("chord.typestate.specfile", "typestatespec.txt");
		
		if( (sp = TypeStateParser.parseStateSpec(stateSpecFile))==null){
			Messages.fatal("Problem occured while parsing state spec file:"+stateSpecFile+",Make sure that its in the required format");
		}
		qv.sp = sp;
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
		} else if(sp.isMethodOfInterest(m.getName().toString())){
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
class MyQuadVisitor extends QuadVisitor.EmptyVisitor {
	
	ArraySet<AbstractState> istate;
	ArraySet<AbstractState> ostate;
	TypeStateSpec sp;
	
	@Override
	public void visitMove(Quad q) {
		//TODO:
		//Need to take care of inheritance
		//Like moving from child class to base class where base class object can access
		//functions that cause state transitions
		
		ostate = istate;
		assert(istate!=null);
		RegisterOperand d = Move.getDest(q);
				
		//Check if target register is in the must access set of the abstract state access path and remove it
		ostate = removeRegisterFromMustSet(d, ostate);
		
		Operand s = Move.getSrc(q);
		if(s instanceof RegisterOperand){
			ostate = addRegisterToTheMustSet((RegisterOperand)s,d,ostate);
		}		
	}
	 
	@Override
	public void visitInvoke(Quad q) {
		ostate = istate;
				
		Utf8 s = Invoke.getMethod(q).getMethod().getName();

		//TODO:
		//1.Get the source register.. how? //usedRegisters(0) ???
				
		//2.Check whether the register has any tuple in pointsto of object of the specific type
			//2.a if the method called is one of stateTransitions method then
				//2.b Get the pointsto set for this register
				//2.c For each abstract state for each of the pointsto relation...
					//2.c.a update the typestate if this register is not in must not set and register is either in must set or may bit is true
		
		//3. else analyse the method in called context ?
		
		if(sp.isMethodOfInterest(s.toString())){
			
		}
		
		
		
	}
	
	@Override
    public void visitNew(Quad q) {
		RegisterOperand d = New.getDest(q);
		//TODO:
		//1. Check if the object allocated is the object of interest
		//2. create a new abstract state (AS) with mustset containing only this register and may bit to false
		//3. check if any AbstractState has this quad(in case of loop)
			//3.a if there is one already..then remove this register for its must set and add to mustnot set
					//Set the unique bit of this state and AS to false 
			//2.a.b else...Set the unique bit of AS to true 
    }
    
	@Override
    public void visitNewArray(Quad q) {
    	
    }
	
	@Override
	public void visitPutfield(Quad q) {
		
	}
	
	@Override
	public void visitGetfield(Quad q) {
		
	}
	
	private ArraySet<AbstractState> addRegisterToTheMustSet(RegisterOperand r,RegisterOperand d,ArraySet<AbstractState> state){
		
		ArraySet<AbstractState> outState = state;
		for(int i=0;i<state.size();i++){
			if(isRegisterInAccessPath(r, state.get(i).mustSet)){
				outState.remove(i);
				outState.add(i, copyAbsState(state.get(i)));
				//TODO: add code to add the register 'd' to the access path
				
				//Because register can be present in the must set of at most one
				//access path so break here
				break;
			}
		}
		return outState;
	}
	
	private ArraySet<AbstractState> removeRegisterFromMustSet(RegisterOperand r,ArraySet<AbstractState> state){
		ArraySet<AbstractState> outState = state;
		for(int i=0;i<state.size();i++){
			if(isRegisterInAccessPath(r, state.get(i).mustSet)){
				outState.remove(i);
				outState.add(i, copyAbsState(state.get(i)));				
				//TODO: add code to remove the register from the access path ns.mustSet
				
				//Because register can be present in the must set of at most one
				//access path so break here
				break;
			}
		}
		return outState;		
	}
	
	private ArraySet<AccessPath> copyAccessPath(ArraySet<AccessPath> ap){
		ArraySet<AccessPath> out = new ArraySet<AccessPath>();
		out.addAll(ap);
		return out;
	}
	
	private AbstractState copyAbsState(AbstractState abs){
		AbstractState ns= new AbstractState(abs.alloc, abs.ts, abs.mustSet);
		return ns;
	}
	
	private Boolean isRegisterInAccessPath(RegisterOperand r,ArraySet<AccessPath> ap){
		Boolean isPresent = false;
		//TODO:code to check if the access path contains the register
		return isPresent;
	}
}



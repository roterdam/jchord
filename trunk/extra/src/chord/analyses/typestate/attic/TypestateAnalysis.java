package chord.analyses.typestate;
import hj.array.lang.booleanArray;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.ObjectOutputStream.PutField;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hsqldb.lib.StringConverter;

import soot.JastAddJ.Access;
import soot.dava.internal.javaRep.DStaticFieldRef;
import soot.jimple.parser.node.AAbstractModifier;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.bcel.internal.generic.Type;

import jdd.util.Array;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Return.RETURN_A;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.UTF.Utf8;
import chord.analyses.alias.CICG;
import chord.analyses.alias.CICGAnalysis;
import chord.analyses.alias.CIPAAnalysis;
import chord.analyses.alias.ICICG;
import chord.program.Location;
import chord.program.Program;
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

import chord.project.analyses.ProgramRel;
import chord.project.analyses.rhs.IEdge;

@Chord(name = "typestate-java")
public class TypestateAnalysis extends RHSAnalysis<Edge, Edge> {

	private ICICG cicg;
	private static DomM domM;
	private static DomI domI;
	private static DomV domV;
	private static DomH domH;
	private TypeStateSpec sp;
	private CIPAAnalysis cipa;
	private MyQuadVisitor qv = new MyQuadVisitor();
	BufferedWriter out;
	BufferedWriter out2;
	PrintWriter wri;
	

	public void run() {
		String stateSpecFile = System.getProperty("chord.typestate.specfile",
				"typestatespec.txt");

		if ((sp = TypeStateParser.parseStateSpec(stateSpecFile)) == null) {
			Messages.fatal("Problem occured while parsing state spec file:"
					+ stateSpecFile
					+ ",Make sure that its in the required format");
		}

		cipa = (CIPAAnalysis) ClassicProject.g().getTrgt("cipa-java");
		ClassicProject.g().runTask(cipa);
		RHSAnalysis.DEBUG = true;
		qv.sp = sp;
		qv.cipa = cipa;
		try {
			wri = new PrintWriter(new FileWriter("SummaryExplore.txt"));
		} catch (Exception e) {
			// TODO: handle exception
		}
		init();
		runPass();
		wri.close();
	}

	public boolean needToAnalyze(jq_Method m) {
		boolean ret = false;
		/*
		 * here we need to write logic to check if 'm' calls any method directly
		 * or transitively that changes the state (as given in the state spec)
		 * and we also validate if method m is on of the method that cases
		 * transition in which case we don't need to analyze it.
		 */

		return ret;
	}

	@Override
	public ICICG getCallGraph() {
		wri.println("Called getCallGraph:");
		if (cicg == null) {
			CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g()
					.getTrgt("cicg-java");
			ClassicProject.g().runTask(cicgAnalysis);
			cicg = cicgAnalysis.getCallGraph();
		}
		return cicg;
	}

	private Edge getRootPathEdge(jq_Method m) {
		// Need to be more specific on what needs to be stored here
		return new Edge(new ArraySet<AbstractState>(),
				new ArraySet<AbstractState>());
	}

	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		Set<jq_Method> roots = cicg.getRoots();
		Set<Pair<Location, Edge>> initPEs = new ArraySet<Pair<Location, Edge>>();
		for (jq_Method m : roots) {
			if (m == Program.g().getMainMethod()) {
				Edge pe = getRootPathEdge(m);
				BasicBlock bb = m.getCFG().entry();
				Location loc = new Location(m, bb, -1, null);
				System.out.println("Added:" + loc + ",With PE:" + pe);
				Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
				initPEs.add(pair);
			}
		}
		return initPEs;
	}

	
	@Override
	public Edge getInitPathEdge(Quad q, jq_Method m, Edge pe) {
		System.out.println("Get Init Path Edge Called For:" + q.toString());
		System.out.println("For Method:" + m.toString());
		System.out.println("With Edge:");
		printEdge(pe);
		// How to handle new?
		ArraySet<AbstractState> srcState = new ArraySet<AbstractState>();
		ArraySet<AbstractState> dstState = new ArraySet<AbstractState>();
		
		ParamListOperand args = Invoke.getParamList(q);
		
		for(AbstractState ab:pe.dstNode){
			AbstractState targetState = null;
			ArraySet<AccessPath> newMustSet = new ArraySet<AccessPath>();
			addAllGlobalAccessPath(newMustSet,ab.mustSet);
			for(int i=0;i<args.length();i++){
				RegisterOperand op = args.get(i);
				if(getIndexInAP(ab.mustSet, op.getRegister()) >= 0){
					Register target=getParameterRegister(m.getLiveRefVars(), i);
					if(target != null){
						RegisterAccessPath newPath = new RegisterAccessPath(target);
						newMustSet.add(newPath);
						boolean methHasSt = methodHasTransition(m);
						if(methHasSt){
							//TODO: add code here to handle transition from one state to another of the object
							//passed as this
							//TODO: make sure that we add the transition only to the 'this argument', i.e R0
						}
						else{
							targetState = new AbstractState(ab.alloc, ab.ts, newMustSet,new ArrayList<Utf8>());
						}
					}
				}
			}
			if(targetState == null && (hasAnyGlobalAccessPath(ab))){
				targetState = new AbstractState(ab.alloc, ab.ts, newMustSet,new ArrayList<Utf8>());
			}
			if(targetState != null){
				srcState.add(new AbstractState(ab.alloc, ab.ts, targetState.mustSet,ab.stateTransitions));
				dstState.add(targetState);
			}
		}
		
		Edge pe2 = new Edge(srcState,dstState);
		return pe2;
	}

	public void printEdge(Edge pe) {
		wri.println("Edge:");
		wri.println("Src Node Size:" + pe.srcNode.size());
		for (AbstractState s : pe.srcNode) {
			wri.println("New Alloc Site:" + s.alloc.toString() + ":");
			wri.println("Must Set Start");
			for (AccessPath p : s.mustSet) {
				if (p instanceof RegisterAccessPath) {
					wri.println("Register:"
							+ ((RegisterAccessPath) p).getRootRegister()
									.toString());
				}
			}
			wri.println("End Must Set");
		}

		wri.println("Dst Node Size:" + pe.dstNode.size());
		for (AbstractState s : pe.dstNode) {
			wri.println("New Alloc Site:" + s.alloc.toString() + ":");
			wri.println("Must Set Start");
			for (AccessPath p : s.mustSet) {
				if (p instanceof RegisterAccessPath) {
					wri.println("Register:"
							+ ((RegisterAccessPath) p).getRootRegister()
									.toString());
				}
			}
			wri.println("End Must Set");
		}
		wri.flush();
	}

	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		// Keep track of state depending on the type of operation suggested by
		// quad
		// if its a move(or assignment) then do strong update..
		wri.println("Called getMiscPathEdge with:" + q.toString());
		wri.println("With Edge:");
		printEdge(pe);
		qv.istate = pe.dstNode;
		qv.ostate = pe.dstNode;
		q.accept(qv);
		return new Edge(pe.srcNode, qv.ostate);
	}

	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		
		ParamListOperand args = Invoke.getParamList(q);
		
		ArraySet<AbstractState> destNode = new ArraySet<AbstractState>();
		
		//Handling the input arguments
		for(int i=0;i<args.length();i++){
			Register arg = args.get(i).getRegister();
			Register localReg = getParameterRegister(m.getLiveRefVars(), i);
			AbstractState targetAb = null;
			AbstractState sourceAb = null;
			for(AbstractState ab:tgtSE.srcNode){
				if(getIndexInAP(ab.mustSet, localReg) >= 0){
					sourceAb = ab;
					for(AbstractState ab1:tgtSE.dstNode){
						if(ab1.alloc == ab.alloc){
							targetAb = ab1;
							break;
						}
					}
					
				}
				if(targetAb != null){
					break;
				}
			}
			if(targetAb != null && sourceAb!=null){
				for(AbstractState clrAb:clrPE.dstNode){
					if(getIndexInAP(clrAb.mustSet, arg)>=0){
						ArrayList<Utf8> newTransitions = new ArrayList<Utf8>();
						TypeState targetTypeState = clrAb.ts;
						for(Utf8 transition:targetAb.stateTransitions){
							if(targetTypeState!= sp.getErrorState()){
								if(sp.getMethodAssertions(transition).contains(targetTypeState)){
									targetTypeState = sp.getTargetState(transition, targetTypeState);
									newTransitions.add(transition);
								}
								else{
									targetTypeState = sp.getErrorState();
								}
								continue;
							}
							break;
						}
						ArrayList<Utf8> newTargetTransitions = new ArrayList<Utf8>(clrAb.stateTransitions);
						newTargetTransitions.addAll(newTransitions);
						ArraySet<AccessPath> newTargetAccessPath = fixAccessPath(sourceAb.mustSet,targetAb.mustSet,clrAb.mustSet);
						AbstractState finalAbstractState = new AbstractState(clrAb.alloc, targetTypeState, newTargetAccessPath);
						finalAbstractState.stateTransitions.addAll(newTargetTransitions);
						destNode.add(finalAbstractState);
					}
				}
			}
			
		}
		
		
		//Handle the return value
		jq_Type retType = m.getReturnType();
		if(retType.getName().equals(sp.getObjecttype())){
			RegisterOperand reg = Invoke.getDest(q);
			if(reg != null){
				Register returnRegister = reg.getRegister();
				AbstractState targetRet= null;
				for(AbstractState ab:tgtSE.dstNode){
					if(ab.isReturn){
						targetRet=ab;
					}
				}
				AbstractState retStateInDest = null;
				//TODO:
				//Check if the state is already present in dest node, if yes then
				//remove it,modify and add a new one
			}
		}
		
		//Propogate the caller edge node to the PE
		for(AbstractState ab:clrPE.dstNode){
			boolean isPresent = false;
			for(AbstractState ab1:destNode){
				if(ab.alloc==ab1.alloc){
					isPresent = true;
					break;
				}
			}
			if(!isPresent){
				destNode.add(ab);
			}
		}
		
		wri.println("Called getInvkPathEdge for quad:" + q.toString());
		wri.println("For Method:" + m.toString());
		wri.println("With clrEdge:");
		printEdge(clrPE);
		wri.println("With tgtSE:");
		printEdge(tgtSE);
		return new Edge(clrPE.srcNode,destNode);
	}

	
	

	@Override
	public Edge getCopy(Edge pe) {
		return new Edge(new ArraySet<AbstractState>(pe.srcNode), new ArraySet<AbstractState>(pe.dstNode));
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		wri.println("Called getSummaryEdge:" + m.toString());
		wri.println("With Edge:");
		printEdge(pe);
		// TODO Auto-generated method stub
		return new Edge(pe.srcNode, pe.dstNode);
	}

	@Override
	public boolean mayMerge() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean mustMerge() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
	private boolean methodHasTransition(jq_Method m){
		boolean hasTransition = false;
		if(m!=null){
			hasTransition = sp.isMethodOfInterest(m.getName());
		}
		return hasTransition;
	}


	public static int getIndexInAP(ArraySet<AccessPath> asSet, Register r) {
		int index = -1;
		for (AccessPath ap : asSet) {
			if (ap instanceof RegisterAccessPath) {
				if (((RegisterAccessPath) ap).getRootRegister().equals(r)) {
					// OPTIMIZATION: a register can only present in must set of
					// atmost one heap object
					index = asSet.indexOf(ap);
					break;
				}
				// TODO: Need to handle access path
			}
			if (ap instanceof GlobalAccessPath) {
				// TODO: need to handle things here
			}
		}
		return index;
	}
	
	private Register getParameterRegister(List<Register> listParams,int i){
		Register targetRegister= null;
		for(Register r:listParams){
			if(r.toString().equalsIgnoreCase("R"+i)){
				targetRegister = r;
				break;
			}
		}
		return targetRegister;
	}
	
	private boolean hasAnyGlobalAccessPath(AbstractState ab){
		boolean canAccessGlobally = false;
		for(AccessPath ap:ab.mustSet){
			if(ap instanceof GlobalAccessPath){
				canAccessGlobally = true;
				break;
			}
		}
		return canAccessGlobally;
	}
	private void addAllGlobalAccessPath(ArraySet<AccessPath> newMustSet,ArraySet<AccessPath> oldMustSet){
		for(AccessPath ap:oldMustSet){
			if(ap instanceof GlobalAccessPath){
				newMustSet.add(ap);
			}
		}
	}
	
	private ArraySet<AccessPath> fixAccessPath(ArraySet<AccessPath> sourceMustset,
			ArraySet<AccessPath> destMustSet, ArraySet<AccessPath> targetMustSet) {
		
		//TODO: find the global access path difference between sourceMustSet and destMustSet
		//fix up the targetMust Set with the delta Must set..this is to ensure that any GlobalAccess changes
		//to the input arguments are updated in the targetMustSet
		
		ArraySet<AccessPath> accessPathsRemoved = new ArraySet<AccessPath>(sourceMustset);
		accessPathsRemoved.removeAll(destMustSet);
		
		ArraySet<AccessPath> accessPathsAdded = new ArraySet<AccessPath>(destMustSet);
		accessPathsAdded.removeAll(sourceMustset);
		
		ArraySet<AccessPath> fixedAccessPath = new ArraySet<AccessPath>(targetMustSet);
		
		for(AccessPath ap:accessPathsRemoved){
			if(ap instanceof GlobalAccessPath){
				removeAccessPath((GlobalAccessPath)ap, fixedAccessPath);
			}
		}
		
		for(AccessPath ap:accessPathsAdded){
			if(ap instanceof GlobalAccessPath){
				fixedAccessPath.add(ap);
			}
		}
		
		return fixedAccessPath;
	}
	
	private void removeAccessPath(GlobalAccessPath targetAP,ArraySet<AccessPath> pathList){
		for(AccessPath ap:pathList){
			if(ap instanceof GlobalAccessPath){
				//TODO: remove the access Path pointed by targetAP from pathList
			}
		}
	}
}

class MyQuadVisitor extends QuadVisitor.EmptyVisitor {

	ArraySet<AbstractState> istate;
	ArraySet<AbstractState> ostate;
	TypeStateSpec sp;
	CIPAAnalysis cipa;

	@Override
	public void visitMove(Quad q) {
		// TODO:
		// Need to take care of inheritance
		// Like moving from child class to base class where base class object
		// can access
		// functions that cause state transitions

		// 1.Remove Destination Register from any mustset
		Register destR = null;
		if (Move.getDest(q) instanceof RegisterOperand) {
			destR = Move.getDest(q).getRegister();
		}
		Register srcR = null;
		if (Move.getSrc(q) instanceof RegisterOperand) {
			srcR = ((RegisterOperand) Move.getSrc(q)).getRegister();
		}
		
		ostate = new ArraySet<AbstractState>();
		if (destR == null) {
			ostate.addAll(istate);
		} else {
			for (AbstractState ab : istate) {
				AbstractState newAbstractState = ab;
				ArraySet<AccessPath> newAccessPath = new ArraySet<AccessPath>(
						ab.mustSet);
				if (cipa.pointsTo(destR).pts.contains(ab.alloc)) {
					int index;
					while((index = TypestateAnalysis.getIndexInAP(newAccessPath, destR)) >= 0) {
						newAccessPath.remove(index);						
					}
					newAbstractState = new AbstractState(ab.alloc, ab.ts,
							newAccessPath);
				}				
				ostate.add(newAbstractState);
			}
		}

		// 2.Handle Source Register

		if (srcR != null) {
			for (AbstractState ab : istate) {
				if (cipa.pointsTo(srcR).pts.contains(ab.alloc)) {
					int index;
					if ((index = TypestateAnalysis.getIndexInAP(ab.mustSet, srcR)) >= 0) {
						ArraySet<AccessPath> oldMustSet = new ArraySet<AccessPath>(
								ab.mustSet);
						AbstractState newAbstractState = new AbstractState(
								ab.alloc, ab.ts, oldMustSet);
						RegisterAccessPath newPath = new RegisterAccessPath(
								destR);
						oldMustSet.add(newPath);
						ostate.remove(ab);
						ostate.add(newAbstractState);
					}
				}
			}

		}

	}

	@Override
	public void visitNew(Quad q) {
		
		Register destR = New.getDest(q).getRegister();
		ostate = new ArraySet<AbstractState>();
		if (isTypeInteresting(New.getType(q).getType())) {
			for (AbstractState ab : istate) {
				AbstractState newAbstractState = ab;
				ArraySet<AccessPath> newAccessPath = new ArraySet<AccessPath>(
						ab.mustSet);
				if (cipa.pointsTo(destR).pts.contains(ab.alloc)) {
					int index;
					while((index = TypestateAnalysis.getIndexInAP(newAccessPath, destR)) >= 0) {
						newAccessPath.remove(index);
					}
					newAbstractState = new AbstractState(ab.alloc, ab.ts,
							newAccessPath);
				}				
				ostate.add(newAbstractState);
			}
			ArraySet<AccessPath> newPath = new ArraySet<AccessPath>();
			newPath.add(new RegisterAccessPath(destR));
			ostate.add(getNewState(q, newPath));
		} else {
			ostate=istate;
		}
	}

	private AbstractState getNewState(Quad q, ArraySet<AccessPath> accesPath) {
		return new AbstractState(q, sp.getInitialState(), accesPath);
	}

	@Override
	public void visitNewArray(Quad q) {

	}

	@Override
	public void visitGetstatic(Quad q) {
		jq_Field srcField= Getstatic.getField(q).getField();
		Register destR = null;
		if(Getstatic.getDest(q) != null)
		{
			destR = Getstatic.getDest(q).getRegister();
		}		
	}
	
	@Override
	 public void visitPutstatic(Quad q) {
		jq_Field destField= Putstatic.getField(q).getField();
		Register srcR = null;
		if(Putstatic.getSrc(q) instanceof RegisterOperand)
		{
			srcR = Getstatic.getDest(q).getRegister();
		}		
	}
	
	@Override
	public void visitPutfield(Quad q) {
		Register destR = null;
		jq_Field destF = null;
		Register srcR = null;
		if(Putfield.getBase(q) instanceof RegisterOperand){
			destR = ((RegisterOperand)Putfield.getBase(q)).getRegister();
		}
		destF = Putfield.getField(q).getField();
		if(Putfield.getSrc(q) instanceof RegisterOperand){
			srcR = ((RegisterOperand)Putfield.getSrc(q)).getRegister();
		}
		
		ostate = new ArraySet<AbstractState>();
		if(destR == null || srcR==null){
			ostate=istate;
		} else{
			//TODO:Remove destination access path from must set
			//TODO: add destination access path to the source must set
		}
		
	}

	@Override
	public void visitGetfield(Quad q) {

	}

	@Override
	 public void visitReturn(Quad q){
		 //TODO: get the correct return register
		//Set the flag i.e isReturn to true of the corresponding abstract state
	 }
	
	private boolean isTypeInteresting(jq_Type type){
		if(type != null){
			return type.getName().equals(sp.getObjecttype());
		}
		return false;
	}
}

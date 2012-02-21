package chord.analyses.typestate;
import hj.array.lang.booleanArray;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream.PutField;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.accessibility.AccessibleSelection;

import org.hsqldb.lib.StringConverter;

import soot.JastAddJ.Access;
import soot.dava.internal.javaRep.DStaticFieldRef;
import soot.dava.toolkits.base.AST.structuredAnalysis.MustMayInitialize;
import soot.jimple.parser.node.AAbstractModifier;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import com.sun.org.apache.bcel.internal.generic.Type;

import jdd.util.Array;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeVirtual;
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
public class TypeStateAnalysis extends RHSAnalysis<Edge, Edge> {

	private ICICG cicg;
	private TypeStateSpec sp;
	private CIPAAnalysis cipa;
	private MyQuadVisitor qv = new MyQuadVisitor();
	BufferedWriter out;
	BufferedWriter out2;
	private DomM domM;
	private DomH domH;
	PrintStream wri;
	int maxFieldDepth;
	int maxAPLength = 0;
	private Edge nullEdge;
	protected static boolean DEBUG = false;

	public void run() {
		String stateSpecFile = System.getProperty("chord.typestate.specfile",
				"typestatespec.txt");

		if ((sp = TypeStateParser.parseStateSpec(stateSpecFile)) == null) {
			Messages.fatal("Problem occured while parsing state spec file:"
					+ stateSpecFile
					+ ",Make sure that its in the required format");
		}

		String temp = System.getProperty("chord.typestate.maxaplen", "8");

		try {
			maxAPLength = Integer.parseInt(temp);
		} catch (NumberFormatException e) {
			Messages.fatal("Problem occured while parsing max access path width:"
					+ temp + ",Make sure that its an integer");
		}

		temp = System.getProperty("chord.typestate.maxfdepth", "6");

		try {
			maxFieldDepth = Integer.parseInt(temp);
		} catch (NumberFormatException e) {
			Messages.fatal("Problem occured while parsing max access path depth:"
					+ maxFieldDepth + ",Make sure that its an integer");
		}

		cipa = (CIPAAnalysis) ClassicProject.g().getTrgt("cipa-java");
		ClassicProject.g().runTask(cipa);
		RHSAnalysis.DEBUG = DEBUG;
		qv.sp = sp;
		qv.cipa = cipa;
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
		domH = (DomH) ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);

		nullEdge = new Edge(null, null, EdgeType.NULL);
		try {
			wri = new PrintStream(new FileOutputStream("SummaryExplore.txt"));
		} catch (Exception e) {
			// TODO: handle exception
		}

		init();
		runPass();
		printSummaries();
		wri.close();
	}

	@Override
	public boolean useBFS() {
		return false;
	}

	@Override
	public ICICG getCallGraph() {
		if (DEBUG) {
			wri.println("Called getCallGraph:");
		}
		if (cicg == null) {
			CICGAnalysis cicgAnalysis = (CICGAnalysis) ClassicProject.g()
					.getTrgt("cicg-java");
			ClassicProject.g().runTask(cicgAnalysis);
			cicg = cicgAnalysis.getCallGraph();
		}
		return cicg;
	}

	@Override
	public Set<Pair<Location, Edge>> getInitPathEdges() {
		Set<jq_Method> roots = cicg.getRoots();
		Set<Pair<Location, Edge>> initPEs = new ArraySet<Pair<Location, Edge>>();
		for (jq_Method m : roots) {
			BasicBlock bb = m.getCFG().entry();
			Location loc = new Location(m, bb, -1, null);
			// First: Add null Edge
			Edge pe = nullEdge;
			Pair<Location, Edge> pair = new Pair<Location, Edge>(loc, pe);
			initPEs.add(pair);
			if (DEBUG) {
				wri.println("Added:" + loc + ",With PE:" + pe);
			}

			// get the number of allocation sites in the method
			ArraySet<Edge> allocEdges = getAllocSites(m);
			// Add the required number of alloc edges to the start of the method
			for (Edge locE : allocEdges) {
				pair = new Pair<Location, Edge>(loc, locE);
				initPEs.add(pair);
			}
		}
		return initPEs;
	}

	@Override
	public Edge getInitPathEdge(Quad q, jq_Method m, Edge pe) {
		if (DEBUG) {
			wri.println("Get Init Path Edge Called For:" + q.toString());
			wri.println("For Method:" + m.toString());
			wri.println("With Edge:" + q.toVerboseStr());
		}
		switch (pe.type) {
		case NULL:
			// Check if there is any return and assign the return register
			// appropriately.
			return pe;
		case ALLOC:
			if (pe.dstNode == null) {
				return nullEdge;
			}
			break;
		}
		AbstractState newSrc = pe.dstNode;
		AbstractState newDest = pe.dstNode;
		TypeState newTypeState = null;
		boolean isthis = false;

		if (newDest != null) {
			newTypeState = newDest.ts;
			ArraySet<AccessPath> newMustSet = new ArraySet<AccessPath>();
			addAllGlobalAccessPath(newMustSet, newDest.mustSet);
			ParamListOperand args = Invoke.getParamList(q);
			for (int i = 0; i < args.length(); i++) {
				RegisterOperand op = args.get(i);
				if (Helper.getIndexInAP(newDest.mustSet, op.getRegister()) >= 0) {
					Register target = getParameterRegister(m.getLiveRefVars(),
							i);
					if (target != null) {
						if (i == 0) {
							isthis = true;
						}
						newMustSet.add(new RegisterAccessPath(target));
					}
				}
			}
			// Do the Typestate change depending on whether the method in
			// interesting or not
			jq_Method targetMethod = Invoke.getMethod(q).getMethod();
			// Need to handle the case when we can have static methods of the
			// same name
			// Change the type state of only that object on which this is
			// invoked
			if (sp.isMethodOfInterest(targetMethod.getName()) && isthis) {
				newTypeState = sp.getTargetState(targetMethod.getName(),
						pe.dstNode.ts);
				if (DEBUG) {
					wri.println("\nState Transition To:" + newTypeState + "\n");
				}
			} else {
				if (DEBUG) {
					wri.println("\nNot Doing State Transition for:"
							+ targetMethod.getName() + " and this is:"
							+ (isthis ? "true" : "false"));
				}

			}
			newSrc = new AbstractState(pe.dstNode.alloc, newTypeState,
					newMustSet);
			newDest = newSrc;

			return new Edge(newSrc, newDest, EdgeType.SUMMARY);
		}
		return pe;
	}

	@Override
	public Edge getMiscPathEdge(Quad q, Edge pe) {
		if (DEBUG) {
			wri.println("Called getMiscPathEdge with:" + q.toString());
			wri.println("With Edge:" + pe);
		}
		qv.istate = pe.dstNode;
		qv.ostate = pe.dstNode;
		qv.etype = pe.type;
		qv.targetAlloc = pe.targetAlloc;
		if (pe.type != EdgeType.NULL) {
			q.accept(qv);
		}
		return qv.ostate == pe.dstNode ? pe : new Edge(pe.srcNode, qv.ostate,
				pe.type);
	}

	@Override
	public Edge getInvkPathEdge(Quad q, Edge clrPE, jq_Method m, Edge tgtSE) {
		switch (clrPE.type) {
		case NULL:
			switch (tgtSE.type) {
			case NULL:
				return nullEdge;
			case SUMMARY:
				return null;
			case ALLOC:
				if (tgtSE.dstNode == null || (!tgtSE.dstNode.canReturn)
						|| (!Helper.hasAnyGlobalAccessPath(tgtSE.dstNode))) {
					return null;
				}
			}
			break;
		case ALLOC:
			switch (tgtSE.type) {
			case NULL:
				if (clrPE.dstNode == null) {
					return clrPE;
				}
				return null;
			default:
				if (clrPE.dstNode == null || tgtSE.dstNode == null
						|| clrPE.dstNode.alloc != tgtSE.dstNode.alloc) {
					return null;
				}
			}
			break;
		case SUMMARY:
			switch (tgtSE.type) {
			case SUMMARY:
				if (tgtSE.dstNode == null
						|| tgtSE.dstNode.alloc != clrPE.dstNode.alloc) {
					return null;
				}
				break;
			default:
				return null;
			}
		}

		Register targetReturnRegister = null;
		if (Invoke.getDest(q) != null) {
			targetReturnRegister = Invoke.getDest(q).getRegister();
		}

		AbstractState newDest = clrPE.dstNode;
		ArraySet<AccessPath> newAccessPath = new ArraySet<AccessPath>();
		Quad targetAlloc = null;

		switch (clrPE.type) {
		case ALLOC:
		case SUMMARY:
			targetAlloc = clrPE.dstNode.alloc;
			addAllLocalAccessPath(newAccessPath, clrPE.dstNode.mustSet);
		case NULL:
			if (targetReturnRegister != null) {
				newAccessPath.add(new RegisterAccessPath(targetReturnRegister));
			}
			addAllGlobalAccessPath(newAccessPath, tgtSE.dstNode.mustSet);
			if (targetAlloc != null) {
				targetAlloc = tgtSE.dstNode.alloc;
			}
			break;
		}
		newDest = new AbstractState(targetAlloc, tgtSE.dstNode.ts,
				newAccessPath);

		EdgeType targetType = clrPE.type == EdgeType.NULL ? EdgeType.ALLOC
				: clrPE.type;
		return new Edge(clrPE.srcNode, newDest, targetType);

	}

	@Override
	public Edge getCopy(Edge pe) {
		Edge ret = new Edge(pe.srcNode, pe.dstNode, pe.type);
		ret.targetAlloc = pe.targetAlloc;
		return ret;
	}

	@Override
	public Edge getSummaryEdge(jq_Method m, Edge pe) {
		if (DEBUG) {
			wri.println("Called Get Summary Edge:");
			wri.println("For Method:" + m.toString());
			wri.println("With Edge:" + pe);
		}
		switch (pe.type) {
		case ALLOC:
		case SUMMARY:
			if (pe.dstNode != null) {
				ArraySet<AccessPath> newMustSet = new ArraySet<AccessPath>();
				addAllGlobalAccessPath(newMustSet, pe.dstNode.mustSet);
				AbstractState newState = new AbstractState(pe.dstNode.alloc,
						pe.dstNode.ts, newMustSet, pe.dstNode.canReturn);
				return new Edge(pe.srcNode, newState, pe.type);
			}
			return pe;
		case NULL:
		default:
			return pe;
		}
	}

	@Override
	public boolean mayMerge() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean mustMerge() {
		// TODO Auto-generated method stub
		return false;
	}

	public void printSummaries() {
		wri.println("Starting Summary Edges:");
		for (jq_Method m : summEdges.keySet()) {
			wri.println("Summaray Edges of " + m + " Start");
			Set<Edge> seSet = summEdges.get(m);
			if (seSet != null) {
				for (Edge se : seSet)
					wri.println("\tSE " + se);
			}
			wri.println("Summaray Edges of " + m + " End");
		}
		wri.println("Starting Path Edges:");
		for (Inst i : pathEdges.keySet()) {
			wri.println("PE of " + i);
			Set<Edge> peSet = pathEdges.get(i);
			if (peSet != null) {
				for (Edge pe : peSet)
					wri.println("\tPE " + pe);
			}
		}
	}

	private Register getParameterRegister(List<Register> listParams, int i) {
		Register targetRegister = null;
		for (Register r : listParams) {
			if (r.toString().equalsIgnoreCase("R" + i)) {
				targetRegister = r;
				break;
			}
		}
		return targetRegister;
	}

	private void addAllGlobalAccessPath(ArraySet<AccessPath> newMustSet,
			ArraySet<AccessPath> oldMustSet) {
		for (AccessPath ap : oldMustSet) {
			if (ap instanceof GlobalAccessPath) {
				newMustSet.add(ap);
			}
		}
	}

	private boolean isTypeInteresting(jq_Type type) {
		if (type != null) {
			return type.getName().equals(sp.getObjecttype());
		}
		return false;
	}

	private void addAllLocalAccessPath(ArraySet<AccessPath> newAccessPath,
			ArraySet<AccessPath> mustSet) {
		for (AccessPath ap : mustSet) {
			if (ap instanceof RegisterAccessPath) {
				newAccessPath.add(ap);
			}
		}
	}

	private ArraySet<Edge> getAllocSites(jq_Method targetM) {
		ArraySet<Edge> allocEdges = new ArraySet<Edge>();
		int numH = domH.getLastI() + 1;
		for (int hIdx = 1; hIdx < numH; hIdx++) {
			Quad q = (Quad) domH.get(hIdx);
			if (!isTypeInteresting(New.getType(q).getType())) {
				continue;
			}
			jq_Method m = q.getMethod();
			if (m == targetM) {
				allocEdges.add(new Edge(null, null, EdgeType.ALLOC, q));
			}
		}
		return allocEdges;
	}

}

class MyQuadVisitor extends QuadVisitor.EmptyVisitor {

	AbstractState istate;
	AbstractState ostate;
	TypeStateSpec sp;
	CIPAAnalysis cipa;
	EdgeType etype;
	Quad targetAlloc;

	@Override
	public void visitMove(Quad q) {
		ostate = istate;
		if (etype != EdgeType.NULL) {
			Register destR = null;
			if (Move.getDest(q) instanceof RegisterOperand) {
				destR = Move.getDest(q).getRegister();
			}
			Register srcR = null;
			if (Move.getSrc(q) instanceof RegisterOperand) {
				srcR = ((RegisterOperand) Move.getSrc(q)).getRegister();
			}
			if (destR != null && istate != null) {
				int index = -1;
				ArraySet<AccessPath> newAccessPath = null;
				while ((index = Helper.getIndexInAP(istate.mustSet, destR,
						index)) >= 0) {
					if (newAccessPath == null) {
						newAccessPath = new ArraySet<AccessPath>(istate.mustSet);
					}
					newAccessPath.remove(istate.mustSet.get(index));
				}
				if (srcR != null) {
					index = -1;
					while ((index = Helper.getIndexInAP(istate.mustSet, srcR,
							index)) >= 0) {
						if (newAccessPath == null) {
							newAccessPath = new ArraySet<AccessPath>(
									istate.mustSet);
						}
						ArrayList<jq_Field> fieldList = new ArrayList<jq_Field>();
						fieldList.addAll(istate.mustSet.get(index).fields);
						newAccessPath.add(new RegisterAccessPath(destR,
								fieldList));
					}
				}
				if (newAccessPath != null) {
					ostate = new AbstractState(istate.alloc, istate.ts,
							newAccessPath);
				}
			}
		}

	}

	@Override
	public void visitNew(Quad q) {
		ostate = istate;
		if (etype == EdgeType.ALLOC && istate == null && targetAlloc == q
				&& isTypeInteresting(New.getType(q).getType())) {
			ArraySet<AccessPath> accessPath = new ArraySet<AccessPath>();
			Register dest = New.getDest(q).getRegister();
			accessPath.add(new RegisterAccessPath(dest));
			ostate = getNewState(q, accessPath);
		}

		// This is to remove reference of register R from the must set
		// of incoming edge.
		if (etype != EdgeType.NULL && istate != null) {
			Register dest = New.getDest(q).getRegister();
			ArraySet<AccessPath> newAccessPath = removeReference(
					istate.mustSet, dest);
			if (newAccessPath != null) {
				ostate = new AbstractState(istate.alloc, istate.ts,
						newAccessPath);
				ostate.canReturn = istate.canReturn;
			}
		}
	}

	private ArraySet<AccessPath> removeReference(ArraySet<AccessPath> mustSet,
			Register r) {
		int index = -1;
		ArraySet<AccessPath> newAccessPath = null;
		while ((index = Helper.getIndexInAP(mustSet, r, index)) >= 0) {
			if (newAccessPath == null) {
				newAccessPath = new ArraySet<AccessPath>(mustSet);
			}
			newAccessPath.remove(mustSet.get(index));
		}
		return newAccessPath;
	}

	@Override
	public void visitNewArray(Quad q) {

	}

	@Override
	public void visitGetstatic(Quad q) {
		if (etype != EdgeType.NULL) {
			jq_Field srcField = Getstatic.getField(q).getField();
			Register destR = null;
			ostate = istate;
			if (Getstatic.getDest(q) != null) {
				destR = Getstatic.getDest(q).getRegister();
			}
			if (destR != null && istate != null) {
				int index = -1;
				ArraySet<AccessPath> newMustSet = null;
				while ((index = Helper.getIndexInAP(istate.mustSet, destR,
						index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					newMustSet.remove(istate.mustSet.get(index));
				}

				while ((index = Helper.getIndexInAP(istate.mustSet, srcField,
						index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					ArrayList<jq_Field> fieldList = new ArrayList<jq_Field>();
					fieldList.addAll(istate.mustSet.get(index).fields);
					RegisterAccessPath newPath = new RegisterAccessPath(destR,
							fieldList);
					newMustSet.add(newPath);
				}
				if (newMustSet != null) {
					ostate = new AbstractState(istate.alloc, istate.ts,
							newMustSet);
				}
			}
		}
	}

	@Override
	public void visitPutstatic(Quad q) {
		ostate = istate;
		if (etype != EdgeType.NULL) {
			jq_Field destField = Putstatic.getField(q).getField();
			Register srcR = null;
			ostate = istate;
			if (Putstatic.getSrc(q) instanceof RegisterOperand) {
				srcR = Getstatic.getDest(q).getRegister();
			}
			if (srcR != null && istate != null) {
				int index = -1;
				ArraySet<AccessPath> newMustSet = null;
				while ((index = Helper.getIndexInAP(istate.mustSet, destField,
						index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					newMustSet.remove(istate.mustSet.get(index));
				}

				while ((index = Helper
						.getIndexInAP(istate.mustSet, srcR, index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					ArrayList<jq_Field> fieldList = new ArrayList<jq_Field>();
					fieldList.addAll(istate.mustSet.get(index).fields);
					GlobalAccessPath newPath = new GlobalAccessPath(destField,
							fieldList);
					newMustSet.add(newPath);
				}
				if (newMustSet != null) {
					ostate = new AbstractState(istate.alloc, istate.ts,
							newMustSet);
				}
			}
		}
	}

	@Override
	public void visitPutfield(Quad q) {
		ostate = istate;
		if (etype != EdgeType.NULL) {
			Register destR = null;
			jq_Field destF = null;
			Register srcR = null;
			if (Putfield.getBase(q) instanceof RegisterOperand) {
				destR = ((RegisterOperand) Putfield.getBase(q)).getRegister();
			}
			destF = Putfield.getField(q).getField();
			if (Putfield.getSrc(q) instanceof RegisterOperand) {
				srcR = ((RegisterOperand) Putfield.getSrc(q)).getRegister();
			}
			if (istate != null) {
				int index = -1;
				ArraySet<AccessPath> newMustSet = null;

				while ((index = Helper.getIndexInAP(istate.mustSet, destR,
						destF, index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					newMustSet.remove(istate.mustSet.get(index));
				}
				while ((index = Helper
						.getIndexInAP(istate.mustSet, srcR, index)) >= 0) {
					ArrayList<jq_Field> fieldList = new ArrayList<jq_Field>(
							istate.mustSet.get(index).fields);
					fieldList.add(destF);
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>(istate.mustSet);
					}
					newMustSet.add(new RegisterAccessPath(destR, fieldList));
				}
				if (newMustSet != null) {
					ostate = new AbstractState(istate.alloc, istate.ts,
							newMustSet);
				}
			}
		}
	}

	@Override
	public void visitGetfield(Quad q) {
		ostate = istate;
		if (etype != EdgeType.NULL) {
			Register srcR = null;
			jq_Field srcF = null;
			Register destR = null;
			if (Getfield.getBase(q) instanceof RegisterOperand) {
				srcR = ((RegisterOperand) Getfield.getBase(q)).getRegister();
			}
			srcF = Getfield.getField(q).getField();
			if (Getfield.getDest(q) instanceof RegisterOperand) {
				destR = ((RegisterOperand) Getfield.getDest(q)).getRegister();
			}
			ostate = istate;
			if (istate != null) {
				int index = -1;
				ArraySet<AccessPath> newMustSet = null;
				while ((index = Helper.getIndexInAP(istate.mustSet, destR,
						index)) >= 0) {
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>();
						newMustSet.addAll(istate.mustSet);
					}
					newMustSet.remove(istate.mustSet.get(index));
				}
				index = -1;
				ArraySet<AccessPath> targetSet = newMustSet == null ? istate.mustSet
						: newMustSet;
				while ((index = Helper.getIndexInAP(targetSet, srcR, srcF,
						index)) >= 0) {
					ArrayList<jq_Field> fieldList = new ArrayList<jq_Field>(
							targetSet.get(index).fields);
					fieldList.addAll(fieldList);
					if (newMustSet == null) {
						newMustSet = new ArraySet<AccessPath>();
						newMustSet.addAll(istate.mustSet);
					}
					newMustSet.add(new RegisterAccessPath(destR, fieldList));
				}
				if (newMustSet != null) {
					ostate = new AbstractState(istate.alloc, istate.ts,
							newMustSet);
				}
			}
		}

	}

	@Override
	public void visitReturn(Quad q) {
		ostate = istate;
		if (etype != EdgeType.NULL) {
			jq_Method targetMethod = q.getMethod();
			jq_Type returnType = targetMethod.getReturnType();
			if (istate != null) {
				if (isTypeInteresting(returnType)) {
					if (Return.getSrc(q) instanceof RegisterOperand) {
						Register targetRegister = ((RegisterOperand) (Return
								.getSrc(q))).getRegister();
						// Add details of may points to
						if (Helper.getIndexInAP(istate.mustSet, targetRegister) >= 0) {
							ostate = new AbstractState(istate.alloc, istate.ts,
									new ArraySet<AccessPath>(istate.mustSet),
									true);
						}
					}
				}
			}
		}
	}

	// Helper Methods

	private AbstractState getNewState(Quad q, ArraySet<AccessPath> accesPath) {
		return new AbstractState(q, sp.getInitialState(), accesPath);
	}

	private boolean isTypeInteresting(jq_Type type) {
		if (type != null) {
			return type.getName().equals(sp.getObjecttype());
		}
		return false;
	}
}

class Helper {
	public static boolean isTypeInteresting(jq_Type type, TypeStateSpec sp) {
		if (type != null) {
			return type.getName().equals(sp.getObjecttype());
		}
		return false;
	}

	public static boolean hasAnyGlobalAccessPath(AbstractState ab) {
		boolean canAccessGlobally = false;
		for (AccessPath ap : ab.mustSet) {
			if (ap instanceof GlobalAccessPath) {
				canAccessGlobally = true;
				break;
			}
		}
		return canAccessGlobally;
	}

	public static int getIndexInAP(ArraySet<AccessPath> mustSet, Register r,
			jq_Field f, int index) {
		int currIn = 0;
		for (AccessPath ap : mustSet) {
			if (ap instanceof RegisterAccessPath) {
				RegisterAccessPath regAP = (RegisterAccessPath) ap;
				if (regAP.getRootRegister().equals(r)) {
					if (!regAP.fields.isEmpty()
							&& (regAP.fields.get(0).equals(f))
							&& currIn > index) {
						return currIn;
					}
				}
			}
			currIn++;
		}
		return -1;
	}

	public static int getIndexInAP(ArraySet<AccessPath> asSet, Register r) {
		int index = -1;
		for (AccessPath ap : asSet) {
			if (ap instanceof RegisterAccessPath) {
				if (((RegisterAccessPath) ap).getRootRegister().equals(r)) {
					index = asSet.indexOf(ap);
					break;
				}
			}
		}
		return index;
	}

	public static int getIndexInAP(ArraySet<AccessPath> asSet, Register r,
			int minIndex) {
		int index = -1;
		int currIndex = 0;
		for (AccessPath ap : asSet) {
			if (ap instanceof RegisterAccessPath) {
				if (((RegisterAccessPath) ap).getRootRegister().equals(r)
						&& (currIndex > minIndex)) {
					return currIndex;
				}
			}
			currIndex++;
		}
		return index;
	}

	public static int getIndexInAP(ArraySet<AccessPath> asSet, jq_Field f,
			int minIndex) {
		int index = -1;
		int currIndex = 0;
		for (AccessPath ap : asSet) {
			if (ap instanceof GlobalAccessPath) {
				if (((GlobalAccessPath) ap).global == f && currIndex > minIndex) {
					return currIndex;
				}
			}
			currIndex++;
		}
		return index;
	}

	public static int getIndexInAP(ArraySet<AccessPath> asSet,
			jq_Field staticField, jq_Field accessField, int minIndex) {
		int index = -1;
		int currIndex = 0;
		for (AccessPath ap : asSet) {
			if (ap instanceof GlobalAccessPath) {
				if (((GlobalAccessPath) ap).global == staticField) {
					if (!ap.fields.isEmpty()
							&& ap.fields.get(0).equals(accessField)
							&& (currIndex > minIndex))
						return currIndex;
				}
			}
			currIndex++;
		}
		return index;
	}
}

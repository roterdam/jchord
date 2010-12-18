/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.analyses.snapshot;

import gnu.trove.TIntLongHashMap;
import gnu.trove.TIntLongProcedure;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TLongArrayList;

import java.util.Stack;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.BasicBlockVisitor;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.basicblock.DomB;
import chord.analyses.method.DomM;
import chord.instr.InstrScheme;
import chord.project.ClassicProject;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.analyses.DynamicAnalysis;

/**
 * @author omertripp (omertrip@post.tau.ac.il)
 *
 */
@Chord(name = "dynamic-shape")
public class DynamicShapeAnalysis extends DynamicAnalysis {

	private static class MethodExecutionState {
		protected final int m;
		/* 
		 * We need to use <code>Long</code>, instead of <code>Integer</code>, since we have to guarantee fresh fake addresses in case
		 * we miss <code>NEW_OR_NEWARRAY</code> events. 
		 */
		protected final TIntLongHashMap env = new TIntLongHashMap();
		protected int b = -1;
		protected int ii = -1;
		
		public MethodExecutionState(int m) {
			this.m = m;
		}
	}
	
	private static enum InstructionType {
		INVOKE,
		MOVE_R,
		NEW_OR_NEW_ARRAY,
		ASTORE_P,
		ALOAD_P,
		ASTORE_R,
		ALOAD_R,
		PUTFIELD_P,
		PUTFIELD_R,
		GETFIELD_P,
		GETFIELD_R,
		PUTSTATIC_R,
		GETSTATIC_R,
		OTHER
	}
	
	private static final int DEBUG_LEVEL = 0;
	private static final boolean HALT_ON_MISMATCH = false;
	private static final boolean PRINT_CFG = false;
	
	/* 
	 * In practice, there is a discrepancy between the <code>Operator.New</code> quads in the CFG and their
	 * corresponding <code>NEW_OR_NEWARRAY</code> trace events. A temporary solution is to synthesize a heap address, and use
	 * it until the actual address becomes available as part of the <code>NEW_OR_NEWARRAY</code> event. For that, we need a per-thread
	 * stack of all the fake IDs that are currently alive. (In practice, we use a least, as that's cheaper.)
	 *    
	 */
	private final TIntObjectHashMap<TLongArrayList> t2fakeHeapID = new TIntObjectHashMap<TLongArrayList>();
	private final TIntObjectHashMap<Stack<MethodExecutionState>> t2m = new TIntObjectHashMap<Stack<MethodExecutionState>>();
	private final TIntObjectHashMap<TIntLongHashMap> thr2formalBindings = new TIntObjectHashMap<TIntLongHashMap>(); 
	private InstrScheme instrScheme;
	private DomM M;
	private DomB B;

	@Override
	public chord.instr.InstrScheme getInstrScheme() {
		if (instrScheme != null) return instrScheme;
		instrScheme = new InstrScheme();
		instrScheme.setEnterMethodEvent(true, true);
		instrScheme.setLeaveMethodEvent(true, true);
		instrScheme.setBasicBlockEvent();
		instrScheme.setMethodCallEvent(true, true, true, true, true);
		instrScheme.setNewEvent(true, true, true, true, false);
		instrScheme.setNewArrayEvent(true, true, true);
		instrScheme.setAloadPrimitiveEvent(false, true, true, false);
		instrScheme.setAloadReferenceEvent(false, true, true, false, true);
		instrScheme.setAstorePrimitiveEvent(false, true, true, false);
		instrScheme.setAstoreReferenceEvent(false, true, true, false, true);
		instrScheme.setGetfieldPrimitiveEvent(false, true, true, true);
		instrScheme.setGetfieldReferenceEvent(false, true, true, true, true);
		instrScheme.setPutfieldPrimitiveEvent(false, true, true, true);
		instrScheme.setPutfieldReferenceEvent(false, true, true, true, true);
		instrScheme.setGetstaticReferenceEvent(false, true, true, true, true);
		instrScheme.setGetstaticPrimitiveEvent(false, true, false, true);
		instrScheme.setPutstaticReferenceEvent(false, true, true, true, true);
		instrScheme.setPutstaticPrimitiveEvent(false, true, false, true);
		return instrScheme;
	}

	private void updateAbstraction(int t, InstructionType instrType, long ... args) {
		// Check if an allocation site we missed is now reported.
		if ((instrType == InstructionType.NEW_OR_NEW_ARRAY) && (t2fakeHeapID.get(t) != null) && (!t2fakeHeapID.get(t).isEmpty())) {
			adjustPointsToOnDelayedNew(t, args[0]);
		} else {
			L: do {
				Quad q = nextQuad(t);
				if (q == null) {
					break L;
				}
				if (DEBUG_LEVEL >= 2) {
					Messages.log("About to process quad " + q + ".");
				}
				Operator o = q.getOperator();
				if ((o instanceof Operator.New) ||
						(o instanceof Operator.NewArray) ||
						(o instanceof Operator.ALoad) ||
						(o instanceof Operator.AStore) ||
						(o instanceof Operator.Getfield) ||
						(o instanceof Operator.Putfield) ||
						(o instanceof Operator.Getstatic.GETSTATIC_A) ||
						(o instanceof Operator.Putstatic.PUTSTATIC_A) ||
						(o instanceof Operator.Invoke)) {
					// First, check if we've missed an allocation site.
					if ((o instanceof Operator.New || o instanceof Operator.NewArray) && (instrType != InstructionType.NEW_OR_NEW_ARRAY)) {
						if (DEBUG_LEVEL >= 2) {
							MethodExecutionState execState = t2m.get(t).peek();
							jq_Method mthd = M.get(execState.m);
							Messages.log("WARN: Handling mismatch between instruction-type " + instrType + " and quad " + q + 
									" when analyzing method " + mthd.getDeclaringClass().getName() + "." + mthd.getNameAndDesc().toString() + ".");
						}
						updatePointsTo(t, q, InstructionType.NEW_OR_NEW_ARRAY, fakeHeapID(t));
						continue L;
					}
					// Otherwise we must guarantee a match.
					if (verifyMatch(o, instrType)) {
						updatePointsTo(t, q, instrType, args);
						break L;
					} else {
						if (DEBUG_LEVEL >= 0) {
							MethodExecutionState execState = t2m.get(t).peek();
							jq_Method mthd = M.get(execState.m);
							if (PRINT_CFG) {
								printCFG(mthd.getCFG());
							}
							if (HALT_ON_MISMATCH) {
								assert (false) : "ERROR: Mismatch between instruction type and quad when running " + 
										mthd.getDeclaringClass().getName() + "." + mthd.getNameAndDesc() + ": " + instrType + " - " + q + ".";
							} else {
								Messages.log("ERROR: Mismatch between instruction type and quad when running " +
										mthd.getDeclaringClass().getName() + "." + mthd.getNameAndDesc() + ": " + instrType + " - " + q + ".");
							}
						}
					}
				} else if ((o instanceof Operator.Move.MOVE_A)) {
					updatePointsTo(t, q, InstructionType.MOVE_R);
				} else {
					updatePointsTo(t, q, InstructionType.OTHER);
				}
			} while (true);
		}
	}
	
	private void adjustPointsToOnDelayedNew(int t, long l) {
		TLongArrayList fakeIDs = t2fakeHeapID.get(t);
		final long lastFakeAddress = fakeIDs.remove(fakeIDs.size()-1);
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		final int[] var = new int[] { -1 };
		MethodExecutionState execState = thrStack.peek();
		execState.env.forEachEntry(new TIntLongProcedure() {
			public boolean execute(int arg0, long arg1) {
				if (arg1 == lastFakeAddress) {
					var[0] = arg0;
					return false;
				} else {
					return true;
				}
			}
		});
		if ((var[0] == -1)) {
			if (DEBUG_LEVEL >= 2) {
				Messages.log("INFO: Attempted to replace fake heap ID with real one, but failed to find fake ID in environment.");
				int m = t2m.get(t).peek().m;
				printCFG(M.get(m).getCFG());
				Messages.log("INFO: Fake address is: " + lastFakeAddress + ".");
				Messages.log("INFO: Real address is: " + l + ".");
				execState.env.forEachEntry(new TIntLongProcedure() {
					public boolean execute(int arg0, long arg1) {
						Messages.log("INFO: " + arg0 + " ---> " + arg1);
						return true;
					}
				});
			}
		} else {			
			execState.env.put(var[0], l);
		}
	}

	private long fakeHeapID(int t) {
		TLongArrayList fakeIDs = t2fakeHeapID.get(t);
		if (fakeIDs == null) {
			t2fakeHeapID.put(t, fakeIDs = new TLongArrayList());
		}
		if (fakeIDs.isEmpty()) {
			fakeIDs.add(Long.MAX_VALUE);
			return Long.MAX_VALUE;
		} else {
			long last = fakeIDs.get(fakeIDs.size()-1);
			fakeIDs.add(last-1);
			return (last-1);
		}
	}

	private boolean verifyMatch(Operator o, InstructionType instrType) {
		if (!((o instanceof Operator.New && instrType == InstructionType.NEW_OR_NEW_ARRAY) || 
				(o instanceof Operator.NewArray && instrType == InstructionType.NEW_OR_NEW_ARRAY) || 
				(o instanceof Operator.Getfield && instrType == InstructionType.GETFIELD_R) ||
				(o instanceof Operator.Getfield && instrType == InstructionType.GETFIELD_P) ||
				(o instanceof Operator.Putfield && instrType == InstructionType.PUTFIELD_R) ||
				(o instanceof Operator.Putfield && instrType == InstructionType.PUTFIELD_P) ||
				(o instanceof Operator.AStore && instrType == InstructionType.ASTORE_R) ||
				(o instanceof Operator.AStore && instrType == InstructionType.ASTORE_P) ||
				(o instanceof Operator.ALoad && instrType == InstructionType.ALOAD_R) ||
				(o instanceof Operator.ALoad && instrType == InstructionType.ALOAD_P) ||
				(o instanceof Operator.Getstatic && instrType == InstructionType.GETSTATIC_R) ||
				(o instanceof Operator.Putstatic && instrType == InstructionType.PUTSTATIC_R) ||
				(o instanceof Operator.Invoke && instrType == InstructionType.INVOKE))) {
			return false;
		} else {
			return true;
		}
	}

	private void updatePointsTo(int t, Quad q, InstructionType instrType, long ... args) {
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		if (thrStack.isEmpty()) {
			if (DEBUG_LEVEL >= 0) {
				Messages.log("ERROR: Attempted to update points-to information with empty stack!");
			}
		}
		TIntLongHashMap pts = thrStack.peek().env;
		switch (instrType) {
		case MOVE_R:
			Operand moprnd = Operator.Move.getSrc(q);
			if (moprnd instanceof RegisterOperand) {
				RegisterOperand src = (RegisterOperand) moprnd;
				int srcRegNum = src.getRegister().getNumber();
				if (pts.containsKey(srcRegNum)) {
					long heapVal = pts.get(srcRegNum);
					pts.remove(srcRegNum);
					RegisterOperand dest = Operator.Move.getDest(q);
					pts.put(dest.getRegister().getNumber(), heapVal);
				} else {
					if (DEBUG_LEVEL >= 2) {
						Messages.log("WARN: Cannot find heap value pointed-to by source register!");
					}
				}
			}
			break;
			
		case ALOAD_R:
			RegisterOperand def = Operator.ALoad.getDest(q);
			long ld = args[1];
			pts.put(def.getRegister().getNumber(), ld);
		case ALOAD_P:
			Operand alpoprnd = Operator.ALoad.getBase(q);
			if (alpoprnd instanceof RegisterOperand) {
				RegisterOperand lbase = (RegisterOperand) alpoprnd;
				long lb = args[0];
				pts.put(lbase.getRegister().getNumber(), lb);
			}
			break;
			
		case ASTORE_R:
			Operand asroprnd = Operator.AStore.getValue(q);
			if (asroprnd instanceof RegisterOperand) {
				RegisterOperand val = (RegisterOperand) asroprnd;
				long sv = args[1];
				pts.put(val.getRegister().getNumber(), sv);
			}
		case ASTORE_P:
			Operand aspoprnd = Operator.AStore.getBase(q);
			if (aspoprnd instanceof RegisterOperand) {
				RegisterOperand sbase = (RegisterOperand) aspoprnd;
				long sb = args[0];
				pts.put(sbase.getRegister().getNumber(), sb);
			}
			break;
		
		case GETFIELD_R:
			RegisterOperand gfval = Operator.Getfield.GETFIELD_A.getDest(q);
			long gfv = args[1];
			pts.put(gfval.getRegister().getNumber(), gfv);
		case GETFIELD_P:
			Operand gfpoprnd = Operator.Getfield.getBase(q);
			if (gfpoprnd instanceof RegisterOperand) {
				RegisterOperand gfref = (RegisterOperand) gfpoprnd;
				long gfr = args[0];
				pts.put(gfref.getRegister().getNumber(), gfr);
			}
			break;
		
		case PUTFIELD_R:
			Operand proprnd = Operator.Putfield.getSrc(q);
			if (proprnd instanceof RegisterOperand) {
				RegisterOperand pfval = (RegisterOperand) proprnd; 
				long pfv = args[1];
				pts.put(pfval.getRegister().getNumber(), pfv);
			}
		case PUTFIELD_P:
			Operand ppoprnd = Operator.Putfield.getBase(q);
			if (ppoprnd instanceof RegisterOperand) {
				RegisterOperand pfref = (RegisterOperand) ppoprnd;
				long pfr = args[0];
				pts.put(pfref.getRegister().getNumber(), pfr);
			}
			break;
			
		case GETSTATIC_R:
			RegisterOperand gsref = Operator.Getstatic.getDest(q);
			long gsr = args[0];
			pts.put(gsref.getRegister().getNumber(), gsr);
			break;

		case PUTSTATIC_R:
			Operand psoprnd = Operator.Putstatic.getSrc(q);
			if (psoprnd instanceof RegisterOperand) {
				RegisterOperand psref = (RegisterOperand) psoprnd;
				long psr = args[0];
				pts.put(psref.getRegister().getNumber(), psr);
			}
			break;
			
		case INVOKE:
			TIntLongHashMap F = thr2formalBindings.get(t);
			if (F == null) {
				thr2formalBindings.put(t, F = new TIntLongHashMap());
			}
			F.clear();
			ParamListOperand paramList = Invoke.getParamList(q);
			for (int i=0; i<paramList.length(); ++i) {
				RegisterOperand paramOperand = paramList.get(i);
				int paramRegNum = paramOperand.getRegister().getNumber();
				if (pts.containsKey(paramRegNum)) {
					F.put(i, pts.get(paramRegNum));
				}
			}
			break;
			
		case NEW_OR_NEW_ARRAY:
			Operator o = q.getOperator();
			if (o instanceof Operator.NewArray) {
				RegisterOperand ndef = Operator.NewArray.getDest(q);
				long nd = args[0];
				pts.put(ndef.getRegister().getNumber(), nd);
			} else {
				assert (o instanceof Operator.New);
				RegisterOperand ndef = Operator.New.getDest(q);
				long nd = args[0];
				pts.put(ndef.getRegister().getNumber(), nd);
			}
			break;
			
		case OTHER:
			if (DEBUG_LEVEL >= 3) {
				Messages.log("INFO: Ignoring quad " + q + " in points-to update.");
			}
		}
	}

	private Quad nextQuad(int t) {
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		if (thrStack == null || thrStack.isEmpty()) {
			if (DEBUG_LEVEL >= 0) {
				Messages.log("ERROR: Encountered a null/empty stack when running nextQuad with t=" + t + ".");
			}
			return null;
		} else {
			MethodExecutionState execState = thrStack.peek();
			BasicBlock currentBB = B.get(execState.b);
			if ((currentBB == null) || (currentBB.size() <= (incrementInstruction(t)))) {
				if (DEBUG_LEVEL >= 1) {
					Messages.log("ERROR: Instruction index out of bounds, where t=" + t + ".");
				}
				return null;
			} else {
				return currentBB.getQuad(execState.ii);
			}
		}
	}

	private int incrementInstruction(int t) {
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		assert (!thrStack.isEmpty());
		MethodExecutionState execState = thrStack.peek();
		++execState.ii;
		if (DEBUG_LEVEL >= 3) {
			Messages.log("INFO: Current II=" + execState.ii + ".");
		}
		return execState.ii;
	}

	@Override
	public void initAllPasses() {
		super.initAllPasses();
		M = (DomM) ClassicProject.g().getTrgt("M");
		B = (DomB) ClassicProject.g().getTrgt("B");
	}
	
	@Override
	public void initPass() {
		super.initPass();
		t2fakeHeapID.clear();
		t2m.clear();
		thr2formalBindings.clear();
	}
	
	@Override
	public void processBasicBlock(int b, int t) {
		BasicBlock currentBB = B.get(b);
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		if (!thrStack.isEmpty()) {
			MethodExecutionState top = thrStack.peek();
			top.b = b;
			top.ii = -1;
		} else {
			if (DEBUG_LEVEL >= 1) {
				Messages.log("ERROR: Encountered call to processBasicBlock with empty stack!");
			}
		}
		if (DEBUG_LEVEL >= 3) {
			Messages.log("INFO: Current BB="+currentBB.getID() + ".");
		}
	}
	
	@Override
	public void processEnterMethod(int m, int t) {
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		if (thrStack == null) {
			t2m.put(t, thrStack = new Stack<MethodExecutionState>());
		}
		if (DEBUG_LEVEL >= 2) {
			jq_Method mthd = M.get(m);
			Messages.log("INFO: Entering method " + mthd.getDeclaringClass().getName() + "." + mthd.getNameAndDesc() + ".");
			if (!thrStack.isEmpty()) {
				MethodExecutionState execState = thrStack.peek();
				jq_Method caller = M.get(execState.m);
				Messages.log("INFO: Caller method is " + caller.getDeclaringClass().getName() + "." + caller.getNameAndDesc() + 
						", b=" + B.get(execState.b).getID() + ", and ii=" + execState.ii + ".");
			} else {
				Messages.log("INFO: Stack is empty at point of call.");
			}
		}
		/* 
		 * We're here assuming that <code>processBasicBlock</code> is called *after*
		 * <code>processEnterMethod</code>. Otherwise we're in trouble.
		 * */
		if (!thrStack.isEmpty()) {
			updateAbstraction(t, InstructionType.INVOKE);
		}
		
		initEnv(t, m);
	}
	
	private void initEnv(int t, int m) {
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		final MethodExecutionState execState = new MethodExecutionState(m);
		thrStack.push(execState);
		TIntLongHashMap F = thr2formalBindings.get(t);
		if (F != null) {
			F.forEachEntry(new TIntLongProcedure() {
				// @Override commeting because jdk 1.5 fails due to this
				public boolean execute(int arg0, long arg1) {
					execState.env.put(arg0, arg1);
					return true;
				}
			});
		}
	}

	@Override
	public void processLeaveMethod(int m, int t) {
		if (DEBUG_LEVEL >= 2) {
			Messages.log("INFO: Leaving method " + M.get(m).getNameAndDesc() + ".");
		}
		Stack<MethodExecutionState> thrStack = t2m.get(t);
		if (!thrStack.isEmpty()) {
			thrStack.pop();
		} else {
			if (DEBUG_LEVEL >= 0) {
				Messages.log("ERROR: Encountered empty stack while leaving method " + M.get(m).getNameAndDesc() + ".");
			}
		}
		if (!thrStack.isEmpty()) {
			MethodExecutionState execState = thrStack.peek();
			if (DEBUG_LEVEL >= 2) {
				Messages.log("INFO: Returned to method " + M.get(execState.m).getNameAndDesc() +
						" with b=" + B.get(execState.b).getID() + " and ii=" + execState.ii + ".");
			}
		} else {
			if (DEBUG_LEVEL >= 2) {
				Messages.log("INFO: Execution stack is now empty.");
			}
		}
	}
	
	@Override
	public void processBefMethodCall(int i, int t, int o) {
		/* Do nothing 4 now. */
	}
	
	@Override
	public void processAftMethodCall(int i, int t, int o) {
		/* Do nothing 4 now. */
	}

	@Override
	public void processBefNew(int h, int t, int o) {
		updateAbstraction(t, InstructionType.NEW_OR_NEW_ARRAY, o);
	}
	
	@Override
	public void processNewArray(int h, int t, int o) {
		updateAbstraction(t, InstructionType.NEW_OR_NEW_ARRAY, o);
	}
	
	@Override
	public void processAloadPrimitive(int e, int t, int b, int i) {
		updateAbstraction(t, InstructionType.ALOAD_P, b);
	}
	
	@Override
	public void processAloadReference(int e, int t, int b, int i, int o) {
		updateAbstraction(t, InstructionType.ALOAD_R, b, o);
	}
	
	@Override
	public void processAstorePrimitive(int e, int t, int b, int i) {
		updateAbstraction(t, InstructionType.ASTORE_P, b);
	}
	
	@Override
	public void processAstoreReference(int e, int t, int b, int i, int o) {
		updateAbstraction(t, InstructionType.ASTORE_R, b, o);
	}
	
	@Override
	public void processPutfieldPrimitive(int e, int t, int b, int f) {
		updateAbstraction(t, InstructionType.PUTFIELD_P, b);
	}
	
	@Override
	public void processPutfieldReference(int e, int t, int b, int f, int o) {
		updateAbstraction(t, InstructionType.PUTFIELD_R, b, o);
	}
	
	@Override
	public void processGetfieldPrimitive(int e, int t, int b, int f) {
		updateAbstraction(t, InstructionType.GETFIELD_P, b);
	}
	
	@Override
	public void processGetfieldReference(int e, int t, int b, int f, int o) {
		updateAbstraction(t, InstructionType.GETFIELD_R, b, o);
	}
	
	@Override
	public void processGetstaticReference(int e, int t, int b, int f, int o) {
		updateAbstraction(t, InstructionType.GETSTATIC_R, o);
	}
	
	@Override
	public void processPutstaticReference(int e, int t, int b, int f, int o) {
		updateAbstraction(t, InstructionType.PUTSTATIC_R, o);
	}
	
	@Override
	public void processPutstaticPrimitive(int e, int t, int b, int f) {
		/* Do nothing 4 now. */
	}
	
	@Override
	public void processGetstaticPrimitive(int e, int t, int b, int f) {
		/* Do nothing 4 now. */
	}
	
	private void printCFG(ControlFlowGraph cfg) {
		final BasicBlock[] bbs = new BasicBlock[cfg.getNumberOfBasicBlocks()];
		cfg.visitBasicBlocks(new BasicBlockVisitor() {
			// @Override commeting because jdk 1.5 fails due to this
			public void visitBasicBlock(BasicBlock bb) {
				bbs[bb.getID()] = bb;
			}
		});
		System.out.println("CFG of method " + cfg.getMethod().getDeclaringClass().toString() + "." + cfg.getMethod().getNameAndDesc());
		for (int i=0; i<bbs.length; ++i) {
			System.out.println("Basic block " + i + ":");
			joeq.Util.Templates.ListIterator.Quad it = bbs[i].iterator();
			while (it.hasNext()) {
				System.out.println("\t" + it.next());
			}
			System.out.println();
			System.out.print("\tSuccessors: ");
			joeq.Util.Templates.List.BasicBlock successors = bbs[i].getSuccessors();
			if (successors.size() > 0) {
				System.out.print(((BasicBlock) successors.get(0)).getID());
				for (int j=1; j<successors.size(); ++j) {
					System.out.print(", ");
					System.out.print(((BasicBlock) successors.get(j)).getID());
				}
			}
			System.out.println(); System.out.println();
		}
	}
}

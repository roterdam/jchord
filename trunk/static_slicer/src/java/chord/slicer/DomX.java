package chord.slicer;

import java.util.HashMap;
import java.util.HashSet;

import joeq.Class.jq_Field;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.bddbddb.Rel.IntPairIterable;
import chord.bddbddb.Rel.IntTrioIterable;
import chord.doms.DomE;
import chord.doms.DomI;
import chord.doms.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.integer.IntTrio;
import chord.util.tuple.object.Trio;

/**
 * Any thing that can possibly be in actual-in, formal-in, actual-out, formal-out
 *  vertices of a system dependency graph
 *  Trio<e|f|u, i|method entry|method exit, 0|1>
 *  1st element - f for static field, e for instance field or array, u for register
 *  2nd element - i(quad for call site) for actual-in and actual-out, entry bb for formal-in, exit bb for formal-out
 *  3rd element - 0 for *-in, 1 for *-out 
 * @author sangmin
 *
 */

@Chord(
		name = "X",
		consumes = { "I", "M", "E", "U", "mods", "refs", "invkArg", "MArg", "invkRet", "MRet" }
)
public class DomX extends ProgramDom<Trio<Object,Inst,Integer>> {
	private static Integer ZERO = new Integer(0);
	private static Integer ONE = new Integer(1);

	public void fill() {
		DomI domI = (DomI) ClassicProject.g().getTrgt("I");
		DomM domM = (DomM) ClassicProject.g().getTrgt("M");
		DomE domE = (DomE) ClassicProject.g().getTrgt("E");
		DomU domU = (DomU) ClassicProject.g().getTrgt("U");

		ProgramRel relIM = (ProgramRel) ClassicProject.g().getTrgt("IM");
		relIM.load();		
		IntPairIterable tuplesIM = relIM.getAry2IntTuples();
		
		ProgramRel relMods = (ProgramRel) ClassicProject.g().getTrgt("mods");			
		relMods.load();
		
		ProgramRel relRefs = (ProgramRel) ClassicProject.g().getTrgt("refs");
		relRefs.load();
		
		int sizeIM = relIM.size();
		int done = 0;
		int fivePercents = sizeIM/20 + 1;		
		System.out.println("rel IM size : " + sizeIM + ", rel mods size : " + relMods.size() + ", rel refs size : " + relRefs.size());
				
		HashMap<Integer,HashSet<Integer>> mapMods = new HashMap<Integer,HashSet<Integer>>();
		HashMap<Integer,HashSet<Integer>> mapRefs = new HashMap<Integer,HashSet<Integer>>();
		int numFormalIn = 0;
		int numFormalOut = 0;
		HashSet<Integer> set = null;
		IntPairIterable tuplesMods = relMods.getAry2IntTuples();
		System.out.println("Start searching relation mods for Formal-in/out");
		
		for(IntPair tupleMods : tuplesMods){
			int mIdxMods = tupleMods.idx0;
			int eIdxMods = tupleMods.idx1;	
			if(mapMods.containsKey(mIdxMods)){
				set = mapMods.get(mIdxMods);
			} else {
				set = new HashSet<Integer>();
				mapMods.put(mIdxMods, set);
			}			
			set.add(eIdxMods);
			
			Quad q = domE.get(eIdxMods);
			Operator operator = q.getOperator();
			if(operator instanceof Operator.Putstatic){
				// formal-out for static field f modified in this method
				jq_Field f = Operator.Putstatic.getField(q).getField();			
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdxMods).getCFG().exit(),ONE));
				numFormalOut++;
				// for static fields, we also create formal-in to handle the case where
				// the static field may or may not be modified by the called method
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdxMods).getCFG().entry(),ZERO));
				numFormalIn++;
			} else if (operator instanceof Operator.Putfield || operator instanceof Operator.AStore){
				// formal-out for instance field or array written in this method				
				getOrAdd(new Trio<Object,Inst,Integer>(q, domM.get(mIdxMods).getCFG().exit(), ONE));
				numFormalOut++;
			}
		}
				
		System.out.println("Start searching relation refs for Formal-in");
		IntPairIterable tuplesRefs = relRefs.getAry2IntTuples();
		
		for(IntPair tupleRefs : tuplesRefs){
			int mIdxRefs = tupleRefs.idx0;
			int eIdxRefs = tupleRefs.idx1;			
			if(mapRefs.containsKey(mIdxRefs)){
				set = mapRefs.get(mIdxRefs);
			} else {
				set = new HashSet<Integer>();
				mapRefs.put(mIdxRefs, set);
			}			
			set.add(eIdxRefs);
			
			Quad q = domE.get(eIdxRefs);
			Operator operator = q.getOperator();
			if(operator instanceof Operator.Getstatic){
				// formal-in for static field f modified in this method
				jq_Field f = Operator.Putstatic.getField(q).getField();
				getOrAdd(new Trio<Object,Inst,Integer>(f, domM.get(mIdxRefs).getCFG().entry(), ZERO));
				numFormalIn++;
			} else if (operator instanceof Operator.Getfield || operator instanceof Operator.ALoad){
				// formal-in for instance field or array read in this method
				getOrAdd(new Trio<Object,Inst,Integer>(q, domM.get(mIdxRefs).getCFG().entry(), ZERO));
				numFormalIn++;
			}
		}
		System.out.println("Number of formal-in : " + numFormalIn);
		System.out.println("Number of formal-out : " + numFormalOut);
				
		System.out.println("Start searching Actual-in/out for static field and instance fields");
		int numActualIn = 0, numActualOut = 0;
		long starttime = System.currentTimeMillis();
		for(IntPair tupleIM : tuplesIM){
			int iIdxIM = tupleIM.idx0;
			int mIdxIM = tupleIM.idx1;
			
			HashSet<Integer> setE = mapMods.get(mIdxIM);
			if(setE != null){
				for(Integer eIdxMods : setE){
					Quad q = domE.get(eIdxMods);
					Operator operator = q.getOperator();
					if(operator instanceof Operator.Putstatic){
						// actual-out for static field f modified in this method
						jq_Field f = Operator.Putstatic.getField(q).getField();
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdxIM), ONE));
						numActualOut++;
						// actual-in for static field f modified in this method
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdxIM), ZERO));
						numActualIn++;
					} else if (operator instanceof Operator.Putfield || operator instanceof Operator.AStore){
						// actual-out for instance field or array written in this method
						getOrAdd(new Trio<Object,Inst,Integer>(q, domI.get(iIdxIM), ONE));
						numActualOut++;
					}					
				}
			}
			
			setE = mapRefs.get(mIdxIM);
			if(setE != null){
				for(Integer eIdxRefs : setE){
					Quad q = domE.get(eIdxRefs);
					Operator operator = q.getOperator();
					if(operator instanceof Operator.Getstatic){
						// actual-in for static field f modified in this method
						jq_Field f = Operator.Putstatic.getField(q).getField();
						getOrAdd(new Trio<Object,Inst,Integer>(f, domI.get(iIdxIM), ZERO));
						numActualIn++;
					} else if (operator instanceof Operator.Getfield || operator instanceof Operator.ALoad){
						// actual-in for instance field or array read in this method
						getOrAdd(new Trio<Object,Inst,Integer>(q, domI.get(iIdxIM), ZERO));
						numActualIn++;
					}	
				}
			}
			
			done++;
			if(done%fivePercents == 0){
				long time = System.currentTimeMillis() - starttime;
				System.out.println(((double)done/(double)sizeIM)*100 + " % done taking " + time + " ms.");
				//starttime = System.currentTimeMillis();
			}
		}
		System.out.println("End searching Actual-in/out for static field and instance fields");
		System.out.println("Number of actual-in : " + numActualIn);
		System.out.println("Number of actual-out : " + numActualOut);
		mapMods.clear();
		mapRefs.clear();
		if(set != null) set.clear();
				
		// actual-in for registers used as parameters
		ProgramRel relInvkArg = (ProgramRel) ClassicProject.g().getTrgt("invkArg");
		relInvkArg.load();
		IntTrioIterable tuplesInvkArg = relInvkArg.getAry3IntTuples();
		for(IntTrio tuple : tuplesInvkArg){
			int iIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domI.get(iIdx), ZERO));        	
		}

		// formal-in for registers used as arguments
		ProgramRel relMArg = (ProgramRel) ClassicProject.g().getTrgt("MArg");
		relMArg.load();
		IntTrioIterable tuplesMArg = relMArg.getAry3IntTuples();
		for(IntTrio tuple : tuplesMArg){
			int mIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domM.get(mIdx).getCFG().entry(), ZERO));      	
		}
		
		// actual-out for registers used as return values
		ProgramRel relInvkRet = (ProgramRel) ClassicProject.g().getTrgt("invkRet");
		relInvkRet.load();
		IntPairIterable tuplesInvkRet = relInvkRet.getAry2IntTuples();
		for(IntPair tuple : tuplesInvkRet){
			int iIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domI.get(iIdx), ONE));
		}
		
		// formal-out for registers used as return values
		ProgramRel relMRet = (ProgramRel) ClassicProject.g().getTrgt("MRet");
		relMRet.load();
		IntPairIterable tuplesMRet = relMRet.getAry2IntTuples();
		for(IntPair tuple : tuplesMRet){
			int mIdx = tuple.idx0;
			int uIdx = tuple.idx1;
			Register r = domU.get(uIdx);
			getOrAdd(new Trio<Object,Inst,Integer>(r, domM.get(mIdx).getCFG().exit(), ONE));
		}

	}

	public String toUniqueString(Trio<Object,Inst,Integer> x){
		
		String ret = super.toUniqueString(x);
		if(x.val1 instanceof BasicBlock){
			ret = "<" + x.val0 + ", " + x.val1 + ":" + ((BasicBlock)x.val1).getMethod() + ", " + x.val2 +">";
		}
		
		return ret;
	}

}

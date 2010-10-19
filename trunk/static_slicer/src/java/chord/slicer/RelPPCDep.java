package chord.slicer;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import chord.doms.DomB;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.object.Pair;

/**
 * Relation containing tuple (p1,p2) such that p1 has a control dependency on p2.
 * It includes cases that an exit basic block depends on return statements.
 * @author sangmin
 *
 */
@Chord(
		name = "PPCDep",
		sign = "P0,P1:P0_P1",
		consumes = { "B", "cdepBB", "MPtail" }
)
public class RelPPCDep extends ProgramRel{

	public void fill(){
		DomB domB = (DomB)ClassicProject.g().getTrgt("B");

		ProgramRel rel = (ProgramRel) ClassicProject.g().getTrgt("cdepBB");
		rel.load();
		IntPairIterable tuples = rel.getAry2IntTuples();
		for (IntPair tuple : tuples) {
			int b0Idx = tuple.idx0;
			int b1Idx = tuple.idx1;
			BasicBlock b0 = domB.get(b0Idx);
			BasicBlock b1 = domB.get(b1Idx);

			joeq.Util.Templates.ListIterator.Quad quadIter = b0.iterator();
			if(b1.isEntry() || b1.isExit()){

			} else {
				Quad last = b1.getLastQuad();
				while(quadIter.hasNext()){
					Quad q = quadIter.nextQuad();
					add(q,last);
				}
			}
		}
		
		ProgramRel relMPtail = (ProgramRel) ClassicProject.g().getTrgt("MPtail");
		relMPtail.load();
		PairIterable<jq_Method, BasicBlock> iter = relMPtail.getAry2ValTuples();
		for(Pair<jq_Method, BasicBlock> pair : iter){
			BasicBlock xb = pair.val1;
			assert xb.isExit() : xb.fullDump();
			joeq.Util.Templates.List.BasicBlock bblist = xb.getPredecessors();
			int size = bblist.size();
			for(int i=0; i < size; i++){
				BasicBlock bb = bblist.getBasicBlock(i);
				Quad q = bb.getLastQuad();
				assert q.getOperator() instanceof Operator.Return;
				add(xb,q);				
			}
			
		}
	}


}

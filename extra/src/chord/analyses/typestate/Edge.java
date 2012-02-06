package chord.analyses.typestate;


import joeq.Class.jq_Field;
import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

public class Edge implements IEdge {
	ArraySet<AbstractState> srcNode;
	ArraySet<AbstractState> dstNode;
	
	@Override
	public boolean canMerge(IEdge edge) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean mergeWith(IEdge edge) {
		// TODO Auto-generated method stub
		return false;
	}
}


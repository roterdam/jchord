package chord.analyses.typestate;

import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

public class Edge implements IEdge {
	final ArraySet<AbstractState> srcNode;
	ArraySet<AbstractState> dstNode;
	
	public Edge(ArraySet<AbstractState> srcNode,ArraySet<AbstractState> dstNode){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
	}
	
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


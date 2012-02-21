package chord.analyses.typestate;

import hj.array.lang.booleanArray;
import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

public class Edge implements IEdge {
	final AbstractState srcNode;
	AbstractState dstNode;
	EdgeType type=EdgeType.ALLOC;
	
	public Edge(AbstractState srcNode,AbstractState dstNode){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
	}
	
	public Edge(AbstractState srcNode,AbstractState dstNode,EdgeType edgeT){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		type = edgeT;
	}
	
	public boolean canMerge(IEdge edge) {
		return false;
	}
	@Override
	public boolean mergeWith(IEdge edge) {	
		return false;
	}
	
	@Override
	public String toString(){
		String ret="SrcState:\n";
		if(srcNode == null){
			ret = ret+"EMPTY";
		}
		else{
			ret =ret+srcNode;
		}
		ret += " => DestState:\n";
		if(dstNode == null){
			ret = ret+"EMPTY";
		}
		else{
			ret = ret+dstNode;
		}
		return ret;
	}
	
	/*
	@Override
	public int hashCode() {
		int hashCode=0;
		if(srcNode != null){
			hashCode = hashCode ^ srcNode.hashCode();
		}
		if(dstNode != null){
			hashCode = hashCode ^ dstNode.hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Edge))
			return false;
		Edge that = (Edge) obj;
		boolean areEqual = true;
		if(this.srcNode == null || that.srcNode == null)
		{
			areEqual = areEqual && this.srcNode == that.srcNode;
		}
		else{
			areEqual = areEqual && this.srcNode.equals(that.srcNode);
		}		
		if(this.dstNode == null || that.dstNode == null){
			areEqual = areEqual && this.dstNode == that.dstNode;
		}
		else{
			areEqual = areEqual && this.dstNode.equals(that.dstNode);
		}
		areEqual = areEqual && this.type == that.type;
		return areEqual;
	}*/
}


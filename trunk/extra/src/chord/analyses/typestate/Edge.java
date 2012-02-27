package chord.analyses.typestate;
import joeq.Compiler.Quad.Quad;
import hj.array.lang.booleanArray;
import chord.util.ArraySet;
import chord.project.analyses.rhs.IEdge;

public class Edge implements IEdge {
	// 3 cases: <srcNode, targetAlloc, dstNode>
    // <null, null, null> (NULL)
	// <null, h, null|X> (ALLOC)
	// <X, null, X> (SUMMARY)
	final AbstractState srcNode;
	final AbstractState dstNode;
	EdgeType type = EdgeType.ALLOC;
	Quad targetAlloc = null;
	public Edge(AbstractState srcNode,AbstractState dstNode){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
	}
	
	public Edge(AbstractState srcNode,AbstractState dstNode,EdgeType edgeT){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		type = edgeT;
	}
	
	public Edge(AbstractState srcNode,AbstractState dstNode,EdgeType edgeT,Quad allocQuad){
		this.srcNode = srcNode;
		this.dstNode = dstNode;
		assert(edgeT == EdgeType.ALLOC);
		type = edgeT;
		targetAlloc = allocQuad;
	}
	
	// TODO: move all this to equals, and also override hashCode
	public boolean canMerge(IEdge edge) {
		boolean canMerge=false;
		if(edge != null){
			Edge targetE = (Edge)edge;
			if(type == EdgeType.SUMMARY && targetE.type == EdgeType.SUMMARY){
				if(srcNode == null || targetE.srcNode == null){
					canMerge = srcNode == targetE.srcNode;
				}
				else{
					canMerge = srcNode.equals(targetE.srcNode);
				}
				if(dstNode == null || targetE.dstNode == null){
					canMerge = canMerge && (dstNode == targetE.dstNode);
				}
				else{
					canMerge = canMerge && (dstNode.equals(targetE.dstNode));
				}
				
			}
			if(type == EdgeType.NULL && targetE.type == EdgeType.NULL){
				canMerge = srcNode == targetE.srcNode && srcNode == null && dstNode == targetE.dstNode && dstNode == null;
			}
		}
		return canMerge;
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
		if(targetAlloc == null){
			ret = ret +" Alloc is empty";
		}
		else{
			ret = ret + " Alloc is:" + targetAlloc.toString();
		}
		return ret;
	}
}


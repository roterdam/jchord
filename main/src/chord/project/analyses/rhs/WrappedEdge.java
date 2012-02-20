package chord.project.analyses.rhs;

import joeq.Compiler.Quad.Inst;
import chord.project.analyses.myrhs.IEdge;
import chord.project.analyses.myrhs.WrappedEdge;
import chord.util.Utils;
import chord.util.tuple.object.Quad;

public class WrappedEdge<Edge extends IEdge> {
	/**
	 * inst: instruction after PE
	 * pe: the path edge
	 * Integer: the shortest path length
	 * WrappedEdge<PE>: the wrapped path edge predecessor
	 */
	public Inst inst;
	public Edge edge;
	public int pathLength;
	public WrappedEdge<Edge> provence;
	public WrappedEdge(Inst inst, Edge pe, Integer pathLength, WrappedEdge<Edge> provence){
		this.inst = inst;
		this.edge = pe;
		this.pathLength= pathLength;
		this.provence = provence;
	}
	
	public int canMerge(WrappedEdge<Edge> that){
		if(inst == null){
			if(that.inst!=null)
				return -1;
		}
		else
		if(!inst.equals(that.inst))
			return -1;
		return edge.canMerge(that.edge);
	}
	
	public boolean mergeWith(WrappedEdge<Edge> that){
		int canMerge = this.canMerge(that);
		if(canMerge < 0)
			throw new RuntimeException(this+" cannot be merged with "+that);
		boolean changed = edge.mergeWith(that.edge);
		if(canMerge == 0){
			if(pathLength > that.pathLength){
				pathLength = that.pathLength;
				provence = that.provence;
				changed = true;
			}
		}
		if(canMerge == 2){
			pathLength = that.pathLength;
			provence = that.provence;
			changed = true;
		}
		return changed;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inst == null) ? 0 : inst.hashCode());
		result = prime * result + pathLength;
		result = prime * result + ((edge == null) ? 0 : edge.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WrappedEdge other = (WrappedEdge) obj;
		if (inst == null) {
			if (other.inst != null)
				return false;
		} else if (!inst.equals(other.inst))
			return false;
		if (pathLength != other.pathLength)
			return false;
		if (edge == null) {
			if (other.edge != null)
				return false;
		} else if (!edge.equals(other.edge))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WrappedEdge [Inst=" + inst + ", PE="+edge+", path length = "+pathLength+"]";
	}

	
}

package chord.project.analyses.rhs;

import joeq.Compiler.Quad.Inst;
import chord.util.Utils;
import chord.util.tuple.object.Quad;

public class WrappedEdge<PE extends IEdge> {
	/**
	 * Inst: instruction after PE
	 * PE: the path edge
	 * Integer: the shortest trajactory length
	 * WrappedEdge<PE>: the wrapped path edge predecessor
	 */
	public Quad<Inst, PE,Integer,WrappedEdge<PE>> q;
	public WrappedEdge(Inst inst, PE pe, Integer trajLength, WrappedEdge<PE> provence){
		this.q = new Quad<Inst,PE,Integer, WrappedEdge<PE>>(inst,pe,trajLength,provence);
	}
	
	public int canMerge(WrappedEdge<PE> that){
		if(q.val0 == null){
			if(that.q.val0!=null)
				return -1;
		}
		else
		if(!q.val0.equals(that.q.val0))
			return -1;
		return q.val1.canMerge(that.q.val1);
	}
	
	public boolean mergeWith(WrappedEdge<PE> that){
		int canMerge = this.canMerge(that);
		if(canMerge < 0)
			throw new RuntimeException(this+" cannot be merged with "+that);
		boolean changed = q.val1.mergeWith(that.q.val1);
		if(canMerge == 0){
			if(q.val2 > that.q.val2){
				q.val2 = that.q.val2;
				q.val3 = that.q.val3;
				changed = true;
			}
		}
		if(canMerge == 2){
			q.val2 = that.q.val2;
			q.val3 = that.q.val3;
			changed = true;
		}
		return changed;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		if(q==null)
		return result;
		result += (q.val0==null?0:q.val0.hashCode())+(q.val1==null?0:q.val1.hashCode())+(q.val2==null?0:q.val2.hashCode());
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
		WrappedEdge that = (WrappedEdge) obj;
			return Utils.areEqual(this.q.val0, that.q.val0) &&
				   Utils.areEqual(this.q.val1, that.q.val1) &&
				   Utils.areEqual(this.q.val2, that.q.val2);
	}

	@Override
	public String toString() {
		return "WrappedEdge [Inst=" + q.val0 + ", PE="+q.val1+"]";
	}

	
}

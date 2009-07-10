package chord.analyses.thread.escape.hybrid;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;

public class PathEdge {
	final Quad q;	// q == null => empty basic block
	final BasicBlock bb;	// basic block of quad
	final SD sd;
	public PathEdge(Quad q, BasicBlock bb, SD sd) {
		this.q = q;
		this.bb = bb;
		this.sd = sd;
	}
	public int hashCode() {
		return sd.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof PathEdge))
			return false;
		PathEdge that = (PathEdge) o;
		return q == that.q && bb == that.bb &&
			sd.equals(that.sd);
	}
	public String toString() {
		return q + ";" + sd;
	}
}

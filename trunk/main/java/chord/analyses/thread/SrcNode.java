package chord.analyses.thread;

import java.util.Arrays;
import java.util.Set;

import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

public class SrcNode {
	final IntArraySet[] env;  // may contain null (i.e. escPts) elems
	final Set<IntTrio> heap;
	public SrcNode(IntArraySet[] e, Set<IntTrio> h) {
		env = e;
		heap = h;
	}
	public int hashCode() {
		return heap.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SrcNode))
			return false;
		SrcNode that = (SrcNode) o;
		return Arrays.equals(env, that.env) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "env@s=" + HybridThreadEscapeAnalysis.toString(env) +
			";heap@s=" + HybridThreadEscapeAnalysis.toString(heap);
	}
}

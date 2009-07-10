package chord.analyses.thread;

import java.util.Arrays;
import java.util.Set;

import chord.util.IntArraySet;
import chord.util.tuple.integer.IntTrio;

public class DstNode {
	final IntArraySet[] env;
	final Set<IntTrio> heap;
	final IntArraySet esc;
	public DstNode(IntArraySet[] e, Set<IntTrio> h, IntArraySet e2) {
		env = e;
		heap = h;
		esc = e2;
	}
	public int hashCode() {
		return heap.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof DstNode))
			return false;
		DstNode that = (DstNode) o;
		return Arrays.equals(env, that.env) &&
			esc.equals(that.esc) &&
			heap.equals(that.heap);
	}
	public String toString() {
		return "env@d=" + HybridThreadEscapeAnalysis.toString(env) +
			"; heap@d=" + HybridThreadEscapeAnalysis.toString(heap) +
			"; esc@d=" + HybridThreadEscapeAnalysis.toString(esc);
	}
}

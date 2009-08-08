package chord.analyses.thread.escape.hybrid;

public class SummEdge {
	final SrcNode srcNode;
	final RetNode retNode;
	public SummEdge(SrcNode s, RetNode r) {
		srcNode = s;
		retNode = r;
	}
	public int hashCode() {
		return srcNode.hashCode() + retNode.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SummEdge))
			return false;
		SummEdge that = (SummEdge) o;
		return srcNode.equals(that.srcNode) &&
			retNode.equals(that.retNode);
	}
	public String toString() {
		return srcNode + ";" + retNode;
	}
}

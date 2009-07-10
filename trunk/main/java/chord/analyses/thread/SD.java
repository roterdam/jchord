package chord.analyses.thread;

public class SD {
	final SrcNode srcNode;
	final DstNode dstNode;
	public SD(SrcNode s, DstNode d) {
		srcNode = s;
		dstNode = d;
	}
	public int hashCode() {
		return srcNode.hashCode() + dstNode.hashCode();
	}
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof SD))
			return false;
		SD that = (SD) o;
		return srcNode.equals(that.srcNode) &&
			dstNode.equals(that.dstNode);
	}
	public String toString() {
		return srcNode + ";" + dstNode;
	}
}

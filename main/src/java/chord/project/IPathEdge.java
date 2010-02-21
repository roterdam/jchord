package chord.project;

public interface IPathEdge {
	public boolean matchesSrcNodeOf(IPathEdge pe);
	public boolean mergeWith(IPathEdge pe);
}


package chord.analyses.slicer;

public class RetnExpr implements Expr {
	public static final RetnExpr instance = new RetnExpr();
	private RetnExpr() { }
	public boolean equals(Object o) {
		return o == instance;
	}
	public int hashCode() {
		return 0;
	}
}


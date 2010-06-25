package chord.analyses.slicer;

public class ConstrExpr implements Expr {
	public static final ConstrExpr instance = new ConstrExpr();
	private ConstrExpr() { }
	public boolean equals(Object o) {
		return o == instance;
	}
	public int hashCode() {
		return 0;
	}
}


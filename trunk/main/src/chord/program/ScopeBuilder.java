package chord.program;

import joeq.Class.jq_Method;
import chord.util.IndexSet;

public interface ScopeBuilder {

	public abstract IndexSet<jq_Method> getMethods();

	public abstract Reflect getReflect();

}

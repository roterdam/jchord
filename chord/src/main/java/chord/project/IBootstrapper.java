package chord.project;

import chord.util.IndexSet;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;

public interface IBootstrapper {
	public IndexSet<jq_Class> getPreparedClasses();
	public IndexSet<jq_Method> getReachableMethods();
}

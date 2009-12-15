package chord.project;

import chord.util.IndexSet;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;

/**
 * Generic interface for algorithms computing program scope
 * (reachable classes and methods).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IBootstrapper {
	public void run();
	public IndexSet<jq_Class> getPreparedClasses();
	public IndexSet<jq_Method> getReachableMethods();
}

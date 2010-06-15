package chord.program;

import chord.util.IndexSet;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;

/**
 * Generic interface for algorithms computing analysis scope
 * (i.e., reachable classes and methods).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public interface IScope {
	public void build();
	public IndexSet<jq_Class> getClasses();
	public IndexSet<jq_Class> getNewInstancedClasses();
	public IndexSet<jq_Method> getMethods();
}

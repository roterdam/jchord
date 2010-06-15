package chord.program;

import java.util.Set;
import chord.util.IndexSet;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;
import joeq.Compiler.Quad.Quad;
import chord.util.tuple.object.Pair;

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

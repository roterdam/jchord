package chord.project;

import java.util.List;

import chord.util.IndexSet;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;

public interface IBootstrapper {
	public void run(List<MethodSign> rootMethodSigns);
	/**
	 * 
	 * @return
	 */
	public IndexSet<jq_Class> getPreparedClasses();
	/**
	 * 
	 * @return
	 */
	public IndexSet<jq_Method> getReachableMethods();
}

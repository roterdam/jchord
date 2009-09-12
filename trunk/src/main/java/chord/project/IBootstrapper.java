package chord.project;

import chord.util.IndexSet;
import joeq.Class.jq_InstanceMethod;
import joeq.Class.jq_Method;
import joeq.Class.jq_Class;

public interface IBootstrapper {
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
	/**
	 * 
	 * @param m
	 * @return
	 */
	public IndexSet<jq_InstanceMethod> getTargetsOfVirtualCall(jq_InstanceMethod m);
}

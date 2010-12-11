package joeq.Compiler.Quad;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;

public class EntryOrExitBasicBlock extends BasicBlock {
	private final jq_Method method;
	public EntryOrExitBasicBlock(jq_Method m) {
		method = m;
	}
	public EntryOrExitBasicBlock(jq_Method m, int numOfPredecessors) {
		super(numOfPredecessors);
		method = m;
	}
    /** Creates new entry node. Only to be called by ControlFlowGraph. */
    static EntryOrExitBasicBlock createStartNode(jq_Method m) {
        return new EntryOrExitBasicBlock(m);
    }
    /** Creates new exit node */
    static EntryOrExitBasicBlock createEndNode(jq_Method m, int numOfPredecessors) {
        return new EntryOrExitBasicBlock(m, numOfPredecessors);
    }
	public jq_Method getMethod() { return method; }
    public int getLineNumber() {
		return 0;	// TODO
	}
	public String toByteLocStr() {
		String bci;
		if (isEntry())
			bci = "E";
		else {
			assert (isExit());
			bci = "X";
		}
        String mName = method.getName().toString();
        String mDesc = method.getDesc().toString();
        String cName = method.getDeclaringClass().getName();
        return bci + "!" + mName + ":" + mDesc + "@" + cName;
	}
    public String toJavaLocStr() {
        jq_Class c = method.getDeclaringClass();
        String fileName = c.getSourceFileName();
        int lineNumber = method.getLineNumber(0);
        return fileName + ":" + lineNumber;
	}
    public String toLocStr() {
        return toByteLocStr() + " (" + toJavaLocStr() + ")";
	}
    public String toVerboseStr() {
        return toByteLocStr() + " (" + toJavaLocStr() + ") [" + toString() + "]";
	}
}

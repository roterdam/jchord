package chord.project;

public abstract class InstrFormat {
	public static int instrMethodAndLoopBound;
	/**
	 * Controls generation of following events:
	 * METHOD_ENTER <thread object> <mIdx>
	 * METHOD_LEAVE <thread object> <mIdx>
	 */
	public static boolean instrMethodEnterAndLeave;
	/**
	 * Controls generation of following events:
	 * THREAD_SPAWN <thread object>
	 * THREAD_START <thread object>
	 */
	public static boolean instrThreadSpawnAndStart;
	/**
	 * Controls generation of following events:
	 * INST_FLD_RD <eIdx> <base object> <fIdx>
	 * INST_FLD_WR <eIdx> <base object> <fIdx> <rhs object>
	 */
	public static boolean instrInstFldInst;
	/**
	 * Controls generation of following events:
	 * ARY_ELEM_RD <eIdx> <base object> <idx>
	 * ARY_ELEM_WR <eIdx> <base object> <idx> <rhs object>
	 */
	public static boolean instrAryElemInst;
	/**
	 * Controls generation of following events:
	 * STAT_FLD_WR <rhs object>
	 */
	public static boolean instrStatFldInst;
	/**
	 * Controls generation of following events:
	 * ACQ_LOCK <lIdx> <lock object>
	 */
	public static boolean instrAcqLockInst;
	/**
	 * Controls generation of following events in crude trace:
	 * BEF_NEW <thread object> <hIdx>
	 * AFT_NEW <thread object> <hIdx> <alloc'ed object>
	 * NEW_ARRAY <hIdx> <alloc'ed object>
	 * Controls generation of following events in final trace:
	 * NEW <hIdx> <alloc'ed object>
	 * NEW_ARRAY <hIdx> <alloc'ed object>
	 */
	public static boolean instrNewAndNewArrayInst;
	public static boolean needsEmap() {
		return instrInstFldInst || InstrFormat.instrAryElemInst;
	}
	public static boolean needsFmap() {
		return instrInstFldInst;
	}
	public static boolean needsMmap() {
		return instrMethodEnterAndLeave || instrMethodAndLoopBound > 0;
	}
	public static boolean needsLmap() {
		return instrAcqLockInst;
	}
	public static boolean needsHmap() {
		return instrNewAndNewArrayInst;
	}
}

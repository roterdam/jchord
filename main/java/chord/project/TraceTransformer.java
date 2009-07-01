package chord.project;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import gnu.trove.TIntArrayList;

import chord.util.Assertions;
import chord.util.IntBuffer;
import chord.util.tuple.integer.IntTrio;

public class TraceTransformer {
	public static void main(String[] args) {
		new TraceTransformer().run();
	}
	private IntBuffer reader, writer;
	private boolean isInNew;
	private TIntArrayList tmp;
	private List<IntTrio> pending;
	private int count;

	public void run() {
		String rdFileName = System.getProperty("chord.crude.trace.file");
		Assertions.Assert(rdFileName != null);
		String wrFileName = System.getProperty("chord.final.trace.file");
		Assertions.Assert(wrFileName != null);
		run(rdFileName, wrFileName);
	}
	private void adjust() throws IOException {
		int limit;
		int pendingSize = pending.size();
		if (pendingSize == 0) {
			limit = count;
			isInNew = false;
		} else {
			IntTrio trio = pending.get(0);
			limit = trio.idx2;
			trio.idx2 = 0;
			for (int i = 1; i < pendingSize; i++) {
				trio = pending.get(i);
				trio.idx2 -= limit;
			}
		}
		int j = 0;
		for (; j < limit; j++) {
			int v = tmp.get(j);
			writer.put(v);
		}
		TIntArrayList tmp2 = new TIntArrayList();
		for (; j < count; j++) {
			int v = tmp.get(j);
			tmp2.add(v);
		}
		tmp.clear();
		tmp = tmp2;
		count -= limit;
	}
	public void run(String rdFileName, String wrFileName) {
		try {
			reader = new IntBuffer(1024, rdFileName, true);
			writer = new IntBuffer(1024, wrFileName, false);
			isInNew = false;
			pending = new ArrayList<IntTrio>();
 			tmp = new TIntArrayList();
			count = 0; // size of tmp
			while (!reader.isDone()) {
				Assertions.Assert(count == tmp.size());
				if (isInNew) {
					if (count > 5000000) {
						System.out.print("size: " + count + " PENDING:");
						for (int i = 0; i < pending.size(); i++)
							System.out.print(" " + pending.get(i).idx2);
						System.out.println();
						// remove 1st item in pending, it is oldest
						pending.remove(0);
						adjust();
					}
				} else
					Assertions.Assert(count == 0);
				int opcode = reader.get();
				switch (opcode) {
				case InstKind.BEF_NEW_INST:
				{
					isInNew = true;
					tmp.add(InstKind.NEW_INST);
					int tIdx = reader.get();
					int hIdx = reader.get();
					tmp.add(hIdx);
					pending.add(new IntTrio(tIdx, hIdx, tmp.size()));
					tmp.add(0); // dummy placeholder for obj
					count += 3;
					break;
				} 
				case InstKind.AFT_NEW_INST:
				{
					int tIdx = reader.get();
					int hIdx = reader.get();
					int oIdx = reader.get();
					int n = pending.size();
					for (int i = 0; i < n; i++) {
						IntTrio trio = pending.get(i);
						if (trio.idx0 == tIdx && trio.idx1 == hIdx) {
							tmp.set(trio.idx2, oIdx);
							pending.remove(i);
							if (i == 0)
								adjust();
							break;
						}
					}
					break;
				}
				default:
				{
					int offset = getOffset(opcode);
					if (isInNew) {
						tmp.add(opcode);
						for (int i = 0; i < offset; i++) {
							int v = reader.get();
							tmp.add(v);
						}
						count += offset + 1;
					} else {
						writer.put(opcode);
						for (int i = 0; i < offset; i++) {
							int v = reader.get();
							writer.put(v);
						}
					}
					break;
				}
				}
			}
			Assertions.Assert(!isInNew);
			Assertions.Assert(pending.size() == 0);
			Assertions.Assert(tmp.size() == 0);
			writer.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	private static int getOffset(int opcode) {
		switch (opcode) {
		case InstKind.ARY_ELEM_RD_INST:
		case InstKind.INST_FLD_RD_INST:
			return 3;
		case InstKind.ARY_ELEM_WR_INST:
		case InstKind.INST_FLD_WR_INST:
			return 4;
		case InstKind.STAT_FLD_WR_INST:
		case InstKind.NEW_ARRAY_INST:
		case InstKind.ACQ_LOCK_INST:
			return 2;
		case InstKind.FORK_HEAD_INST:
			return 1;
		default:
			throw new RuntimeException();
		}
	}
}

package chord.project;

import java.io.IOException;
import chord.util.IntBuffer;

public class TracePrinter {
	public static void main(String[] args) throws IOException {
		String fileName = System.getProperty("chord.trace.file");
		IntBuffer buffer = new IntBuffer(1024, fileName, true);
		while (!buffer.isDone()) {
			int opcode = buffer.get();
			switch (opcode) {
			case InstKind.BEF_NEW_INST:
			{
				int t = buffer.get();
				int h = buffer.get();
				System.out.println("BEF NEW " + t + " " + h);
				break;
			}
			case InstKind.AFT_NEW_INST:
			{
				int t = buffer.get();
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("AFT NEW " + t + " " + h + " " + o);
				break;
			}
			case InstKind.NEW_INST:
			{
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("NEW " + h + " " + o);
				break;
			}
			case InstKind.NEW_ARRAY_INST:
			{
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("NEW_ARRAY " + h + " " + o);
				break;
			}
			case InstKind.ACQ_LOCK_INST:
			{
				int l = buffer.get();
				int o = buffer.get();
				System.out.println("ACQ_LOCK " + l + " " + o);
				break;
			}
			case InstKind.INST_FLD_RD_INST:
			{
				int e = buffer.get();
				int b = buffer.get();
				int f = buffer.get();
				System.out.println("F_RD " + e + " " + b + " " + f);
				break;
			}
			case InstKind.INST_FLD_WR_INST:
			{
				int e = buffer.get();
				int b = buffer.get();
				int f = buffer.get();
				int r = buffer.get();
				System.out.println("F_WR " + e + " " + b + " " + f + " " + r);
				break;
			}
			case InstKind.ARY_ELEM_RD_INST:
			{
				int e = buffer.get();
				int b = buffer.get();
				int i = buffer.get();
				System.out.println("A_RD " + e + " " + b + " " + i);
				break;
			}
			case InstKind.ARY_ELEM_WR_INST:
			{
				int e = buffer.get();
				int b = buffer.get();
				int i = buffer.get();
				int r = buffer.get();
				System.out.println("A_WR " + e + " " + b + " " + i + " " + r);
				break;
			}
			case InstKind.STAT_FLD_WR_INST:
			{
				int f = buffer.get();
				int r = buffer.get();
				System.out.println("G_WR " + f + " " + r);
				break;
			}
			case InstKind.THREAD_START_INST:
			{
				int o = buffer.get();
				System.out.println("START " + o);
				break;
			}
			case InstKind.THREAD_SPAWN_INST:
			{
				int o = buffer.get();
				System.out.println("SPAWN " + o);
				break;
			}
			default:
				throw new RuntimeException("Opcode: " + opcode);
			}
		}
	}
}


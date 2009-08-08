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
			case EventKind.BEF_NEW:
			{
				int t = buffer.get();
				int h = buffer.get();
				System.out.println("BEF NEW " + t + " " + h);
				break;
			}
			case EventKind.AFT_NEW:
			{
				int t = buffer.get();
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("AFT NEW " + t + " " + h + " " + o);
				break;
			}
			case EventKind.NEW:
			{
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("NEW " + h + " " + o);
				break;
			}
			case EventKind.NEW_ARRAY:
			{
				int h = buffer.get();
				int o = buffer.get();
				System.out.println("NEW_ARRAY " + h + " " + o);
				break;
			}
			case EventKind.ACQ_LOCK:
			{
				int l = buffer.get();
				int o = buffer.get();
				System.out.println("ACQ_LOCK " + l + " " + o);
				break;
			}
			case EventKind.INST_FLD_RD:
			{
				int e = buffer.get();
				int b = buffer.get();
				int f = buffer.get();
				System.out.println("F_RD " + e + " " + b + " " + f);
				break;
			}
			case EventKind.INST_FLD_WR:
			{
				int e = buffer.get();
				int b = buffer.get();
				int f = buffer.get();
				int r = buffer.get();
				System.out.println("F_WR " + e + " " + b + " " + f + " " + r);
				break;
			}
			case EventKind.ARY_ELEM_RD:
			{
				int e = buffer.get();
				int b = buffer.get();
				int i = buffer.get();
				System.out.println("A_RD " + e + " " + b + " " + i);
				break;
			}
			case EventKind.ARY_ELEM_WR:
			{
				int e = buffer.get();
				int b = buffer.get();
				int i = buffer.get();
				int r = buffer.get();
				System.out.println("A_WR " + e + " " + b + " " + i + " " + r);
				break;
			}
			case EventKind.STAT_FLD_WR:
			{
				int r = buffer.get();
				System.out.println("G_WR " + r);
				break;
			}
			case EventKind.THREAD_START:
			{
				int o = buffer.get();
				System.out.println("START " + o);
				break;
			}
			case EventKind.THREAD_SPAWN:
			{
				int o = buffer.get();
				System.out.println("SPAWN " + o);
				break;
			}
			case EventKind.METHOD_ENTER:
			{
				int o = buffer.get();
				int m = buffer.get();
				System.out.println("METHOD_ENTER " + o + " " + m);
				break;
			}
			case EventKind.METHOD_LEAVE:
			{
				int o = buffer.get();
				int m = buffer.get();
				System.out.println("METHOD_LEAVE " + o + " " + m);
				break;
			}
			default:
				throw new RuntimeException("Opcode: " + opcode);
			}
		}
	}
}


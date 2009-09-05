/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.instr;

import java.io.IOException;

import chord.instr.InstrScheme.EventFormat;
import chord.util.ByteBufferedFile;
import chord.util.ReadException;
import chord.project.ChordRuntimeException;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class TracePrinter {
/*
	public static void main(String[] args) {
		(new TracePrinter()).run();
	}
	public static void run() {
		String traceFileName = System.getProperty("chord.trace.file");
		InstrScheme scheme = InstrScheme.load();
		run(traceFileName, scheme);
	}
*/
	public static void run(String traceFileName, InstrScheme scheme) {
		try {
		ByteBufferedFile buffer = new ByteBufferedFile(1024, traceFileName, true);
		while (!buffer.isDone()) {
			byte opcode = buffer.getByte();
			switch (opcode) {
			case EventKind.ENTER_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				int m = ef.hasMid() ? buffer.getInt() : -1;
				int t = ef.hasTid() ? buffer.getInt() : -1;
				System.out.println("ENTER_METHOD " + m + " " + t);
				break;
			}
			case EventKind.LEAVE_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				int m = ef.hasMid() ? buffer.getInt() : -1;
				int t = ef.hasTid() ? buffer.getInt() : -1;
				System.out.println("LEAVE_METHOD " + m + " " + t);
				break;
			}
			case EventKind.QUAD:
			{
				int p = buffer.getInt();
				int t = buffer.getInt();
				System.out.println("QUAD " + p + " " + t);
				break;
			}
			case EventKind.BASIC_BLOCK:
			{
				int b = buffer.getInt();
				int t = buffer.getInt();
				System.out.println("BASIC_BLOCK " + b + " " + t);
				break;
			}
			default:
				throw new ChordRuntimeException("Opcode: " + opcode);
			}
		}
		} catch (IOException ex) {
            throw new ChordRuntimeException(ex);
        } catch (ReadException ex) {
            throw new ChordRuntimeException(ex);
        }
	}
}


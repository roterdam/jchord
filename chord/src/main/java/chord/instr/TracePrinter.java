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
import chord.util.IndexMap;

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
	public static void run(String traceFileName, Instrumentor instrumentor) {
		InstrScheme scheme = instrumentor.getInstrScheme();
		IndexMap<String> Mmap = instrumentor.getMmap();
		try {
		ByteBufferedFile buffer = new ByteBufferedFile(1024, traceFileName, true);
		while (!buffer.isDone()) {
			byte opcode = buffer.getByte();
			switch (opcode) {
			case EventKind.ENTER_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				int m = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				// String mStr = (m == -1) ? "null" : Mmap.get(m);
				System.out.println("ENTER_METHOD " + m + " " + t);
				break;
			}
			case EventKind.LEAVE_METHOD:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
				int m = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				System.out.println("LEAVE_METHOD " + m + " " + t);
				break;
			}
			case EventKind.ENTER_LOOP:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
				int w = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				System.out.println("ENTER_LOOP " + w + " " + t);
				break;
			}
			case EventKind.LEAVE_LOOP:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ENTER_AND_LEAVE_LOOP);
				int w = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				System.out.println("LEAVE_LOOP " + w + " " + t);
				break;
			}
			case EventKind.BEF_NEW:
			{
				int h = buffer.getInt();
				int t = buffer.getInt();
				System.out.println("BEF_NEW " + h + " " + t);
				break;
			}
			case EventKind.AFT_NEW:
			{
				int h = buffer.getInt();
				int t = buffer.getInt();
				int o = buffer.getInt();
				System.out.println("AFT_NEW " + h + " " + t + " " + o);
				break;
			}
			case EventKind.NEW:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				int h = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("NEW " + h + " " + t + " " + o);
				break;
			}
			case EventKind.NEW_ARRAY:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
				int h = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("NEW_ARRAY " + h + " " + t + " " + o);
				break;
			}
			case EventKind.GETSTATIC_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				System.out.println("GETSTATIC_PRIMITIVE " + e + " " + t + " " + f);
				break;
			}
			case EventKind.GETSTATIC_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("GETSTATIC_REFERENCE " + e + " " + t + " " + f + " " + o);
				break;
			}
			case EventKind.PUTSTATIC_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				System.out.println("PUTSTATIC_PRIMITIVE " + e + " " + t + " " + f);
				break;
			}
			case EventKind.PUTSTATIC_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("PUTSTATIC_REFERENCE " + e + " " + t + " " + f + " " + o);
				break;
			}
			case EventKind.GETFIELD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				System.out.println("GETFIELD_PRIMITIVE " + e + " " + t + " " + b + " " + f);
				break;
			}
			case EventKind.GETFIELD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("GETFIELD_REFERENCE " + e + " " + t + " " + b + " " + f + " " + o);
				break;
			}
			case EventKind.PUTFIELD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				System.out.println("PUTFIELD_PRIMITIVE " + e + " " + t + " " + b + " " + f);
				break;
			}
			case EventKind.PUTFIELD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int f = ef.hasFld() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("PUTFIELD_REFERENCE " + e + " " + t + " " + b + " " + f + " " + o);
				break;
			}
			case EventKind.ALOAD_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				System.out.println("ALOAD_PRIMITIVE " + e + " " + t + " " + b + " " + i);
				break;
			}
			case EventKind.ALOAD_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("ALOAD_REFERENCE " + e + " " + t + " " + b + " " + i + " " + o);
				break;
			}
			case EventKind.ASTORE_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				System.out.println("ASTORE_PRIMITIVE " + e + " " + t + " " + b + " " + i);
				break;
			}
			case EventKind.ASTORE_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
				int e = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int b = ef.hasBaseObj() ? buffer.getInt() : -1;
				int i = ef.hasIdx() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("ASTORE_REFERENCE " + e + " " + t + " " + b + " " + i + " " + o);
				break;
			}
			case EventKind.THREAD_START:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("THREAD_START " + p + " " + t + " " + o);
				break;
			}
			case EventKind.THREAD_JOIN:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("THREAD_JOIN " + p + " " + t + " " + o);
				break;
			}
			case EventKind.ACQUIRE_LOCK:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("ACQUIRE_LOCK " + p + " " + t + " " + l);
				break;
			}
			case EventKind.RELEASE_LOCK:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("RELEASE_LOCK " + p + " " + t + " " + l);
				break;
			}
			case EventKind.WAIT:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("WAIT " + p + " " + t + " " + l);
				break;
			}
			case EventKind.NOTIFY:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int l = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("NOTIFY " + p + " " + t + " " + l);
				break;
			}
			case EventKind.METHOD_CALL:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
				int i = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				System.out.println("METHOD_CALL " + i + " " + t);
				break;
			}
			case EventKind.RETURN_PRIMITIVE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_PRIMITIVE);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				System.out.println("RETURN_PRIMITIVE " + p + " " + t);
				break;
			}
			case EventKind.RETURN_REFERENCE:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.RETURN_REFERENCE);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("RETURN_REFERENCE " + p + " " + t + " " + o);
				break;
			}
			case EventKind.EXPLICIT_THROW:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.EXPLICIT_THROW);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("EXPLICIT_THROW " + p + " " + t + " " + o);
				break;
			}
			case EventKind.IMPLICIT_THROW:
			{
				EventFormat ef = scheme.getEvent(InstrScheme.IMPLICIT_THROW);
				int p = ef.hasLoc() ? buffer.getInt() : -1;
				int t = ef.hasThr() ? buffer.getInt() : -1;
				int o = ef.hasObj() ? buffer.getInt() : -1;
				System.out.println("IMPLICIT_THROW " + p + " " + t + " " + o);
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


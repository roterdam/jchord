/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.instr;

import java.io.IOException;

import chord.project.Properties;
import chord.instr.InstrScheme.EventFormat;
import chord.util.ByteBufferedFile;
import chord.util.ReadException;
import chord.util.ChordRuntimeException;
import chord.util.IndexMap;
import chord.runtime.Runtime;

/**
 * Functionality for printing a trace of events generated during an
 * instrumented program's execution.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class TracePrinter {
	private final String traceFileName;
	private final Instrumentor instrumentor;
	/**
	 * Initializes a trace printer.
	 * 
	 * @param	traceFileName	The location of the file containing the
	 * trace to be printed.
	 * @param	instrumentor	The instrumentor used to instrument the
	 * program from which the trace was generated.
	 */
	public TracePrinter(String traceFileName, Instrumentor instrumentor) {
		this.traceFileName = traceFileName;
		this.instrumentor = instrumentor;
	}
	/**
	 * Runs the trace printer.
	 */
	public void run() {
		InstrScheme scheme = instrumentor.getInstrScheme();
		IndexMap<String> Mmap = instrumentor.getMmap();
		IndexMap<String> Hmap = instrumentor.getHmap();
		IndexMap<String> Emap = instrumentor.getEmap();
		IndexMap<String> Fmap = instrumentor.getFmap();
		IndexMap<String> Imap = instrumentor.getImap();
		IndexMap<String> Lmap = instrumentor.getLmap();
		IndexMap<String> Rmap = instrumentor.getRmap();
		IndexMap<String> Pmap = instrumentor.getPmap();
		IndexMap<String> Bmap = instrumentor.getBmap();
		IndexMap<String> Wmap = instrumentor.getWmap();
		boolean convert = false;
		try {
			ByteBufferedFile buffer = new ByteBufferedFile(
				Properties.traceBlockSize, traceFileName, true);
			while (!buffer.isDone()) {
				byte opcode = buffer.getByte();
				switch (opcode) {
				case EventKind.ENTER_METHOD:
				{
					int m = buffer.getInt();
					String mStr = convert ? Integer.toString(m) :
						((m < 0) ? "null" : Mmap.get(m));
					int t = buffer.getInt();
					System.out.println("ENTER_METHOD " + mStr + " " + t);
					break;
				}
				case EventKind.LEAVE_METHOD:
				{
					int m = buffer.getInt();
					String mStr = convert ? Integer.toString(m) :
						((m < 0) ? "null" : Mmap.get(m));
					int t = buffer.getInt();
					System.out.println("LEAVE_METHOD " + mStr + " " + t);
					break;
				}
				case EventKind.ENTER_LOOP:
				{
					int w = buffer.getInt();
					String wStr = convert ? Integer.toString(w) :
						((w < 0) ? "null" : Wmap.get(w));
					int t = buffer.getInt();
					System.out.println("ENTER_LOOP " + wStr + " " + t);
					break;
				}
				case EventKind.LEAVE_LOOP:
				{
					int w = buffer.getInt();
					String wStr = convert ? Integer.toString(w) :
						((w < 0) ? "null" : Wmap.get(w));
					int t = buffer.getInt();
					System.out.println("LEAVE_LOOP " + wStr + " " + t);
					break;
				}
				case EventKind.BEF_NEW:
				{
					int h = buffer.getInt();
					String hStr = convert ? Integer.toString(h) :
						((h < 0) ? "null" : Hmap.get(h));
					int t = buffer.getInt();
					System.out.println("BEF_NEW " + hStr + " " + t);
					break;
				}
				case EventKind.AFT_NEW:
				{
					int h = buffer.getInt();
					String hStr = convert ? Integer.toString(h) :
						((h < 0) ? "null" : Hmap.get(h));
					int t = buffer.getInt();
					int o = buffer.getInt();
					System.out.println("AFT_NEW " + hStr + " " + t + " " + o);
					break;
				}
				case EventKind.NEW:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
					int h = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String hStr = convert ? Integer.toString(h) : ((h < 0) ? "null" : Hmap.get(h));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("NEW " + hStr + " " + t + " " + o);
					break;
				}
				case EventKind.NEW_ARRAY:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
					int h = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String hStr = convert ? Integer.toString(h) : ((h < 0) ? "null" : Hmap.get(h));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("NEW_ARRAY " + hStr + " " + t + " " + o);
					break;
				}
				case EventKind.GETSTATIC_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					System.out.println("GETSTATIC_PRIMITIVE " + eStr + " " + t + " " + b + " " + fStr);
					break;
				}
				case EventKind.GETSTATIC_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("GETSTATIC_REFERENCE " + eStr + " " + t + " " + b + " " + fStr + " " + o);
					break;
				}
				case EventKind.PUTSTATIC_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					System.out.println("PUTSTATIC_PRIMITIVE " + eStr + " " + t + " " + b + " " + fStr);
					break;
				}
				case EventKind.PUTSTATIC_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("PUTSTATIC_REFERENCE " + eStr + " " + t + " " + b + " " + fStr + " " + o);
					break;
				}
				case EventKind.GETFIELD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					System.out.println("GETFIELD_PRIMITIVE " + eStr + " " + t + " " + b + " " + fStr);
					break;
				}
				case EventKind.GETFIELD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("GETFIELD_REFERENCE " + eStr + " " + t + " " + b + " " + fStr + " " + o);
					break;
				}
				case EventKind.PUTFIELD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					System.out.println("PUTFIELD_PRIMITIVE " + eStr + " " + t + " " + b + " " + fStr);
					break;
				}
				case EventKind.PUTFIELD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int f = ef.hasFld() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String fStr = convert ? Integer.toString(f) : ((f < 0) ? "null" : Fmap.get(f));
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("PUTFIELD_REFERENCE " + eStr + " " + t + " " + b + " " + fStr + " " + o);
					break;
				}
				case EventKind.ALOAD_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int i = ef.hasIdx() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("ALOAD_PRIMITIVE " + eStr + " " + t + " " + b + " " + i);
					break;
				}
				case EventKind.ALOAD_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ALOAD_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int i = ef.hasIdx() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("ALOAD_REFERENCE " + eStr + " " + t + " " + b + " " + i + " " + o);
					break;
				}
				case EventKind.ASTORE_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int i = ef.hasIdx() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("ASTORE_PRIMITIVE " + eStr + " " + t + " " + b + " " + i);
					break;
				}
				case EventKind.ASTORE_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ASTORE_REFERENCE);
					int e = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String eStr = convert ? Integer.toString(e) : ((e < 0) ? "null" : Emap.get(e));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int b = ef.hasBaseObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int i = ef.hasIdx() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("ASTORE_REFERENCE " + eStr + " " + t + " " + b + " " + i + " " + o);
					break;
				}
				case EventKind.THREAD_START:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.THREAD_START);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("THREAD_START " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.THREAD_JOIN:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.THREAD_JOIN);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("THREAD_JOIN " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.ACQUIRE_LOCK:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.ACQUIRE_LOCK);
					int l = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String lStr = convert ? Integer.toString(l) : ((l < 0) ? "null" : Lmap.get(l));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("ACQUIRE_LOCK " + lStr + " " + t + " " + o);
					break;
				}
				case EventKind.RELEASE_LOCK:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.RELEASE_LOCK);
					int r = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String rStr = convert ? Integer.toString(r) : ((r < 0) ? "null" : Rmap.get(r));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("RELEASE_LOCK " + rStr + " " + t + " " + o);
					break;
				}
				case EventKind.WAIT:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.WAIT);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("WAIT " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.NOTIFY:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("NOTIFY " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.NOTIFY_ALL:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.NOTIFY);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("NOTIFY_ALL " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.METHOD_CALL_BEF:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("METHOD_CALL_BEF " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.METHOD_CALL_AFT:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.METHOD_CALL);
					int i = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					String iStr = convert ? Integer.toString(i) : ((i < 0) ? "null" : Imap.get(i));
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("METHOD_CALL_AFT " + iStr + " " + t + " " + o);
					break;
				}
				case EventKind.RETURN_PRIMITIVE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.RETURN_PRIMITIVE);
					int p = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("RETURN_PRIMITIVE " + p + " " + t);
					break;
				}
				case EventKind.RETURN_REFERENCE:
				{
					EventFormat ef = scheme.getEvent(InstrScheme.RETURN_REFERENCE);
					int p = ef.hasLoc() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int t = ef.hasThr() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() : Runtime.MISSING_FIELD_VAL;
					System.out.println("RETURN_REFERENCE " + p + " " + t + " " + o);
					break;
				}
				case EventKind.EXPLICIT_THROW:
				{
					EventFormat ef = scheme.getEvent(
						InstrScheme.EXPLICIT_THROW);
					int p = ef.hasLoc() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					int t = ef.hasThr() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					System.out.println("EXPLICIT_THROW " +
						p + " " + t + " " + o);
					break;
				}
				case EventKind.IMPLICIT_THROW:
				{
					EventFormat ef = scheme.getEvent(
						InstrScheme.IMPLICIT_THROW);
					int p = ef.hasLoc() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					int t = ef.hasThr() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					int o = ef.hasObj() ? buffer.getInt() :
						Runtime.MISSING_FIELD_VAL;
					System.out.println("IMPLICIT_THROW " +
						p + " " + t + " " + o);
					break;
				}
				case EventKind.QUAD:
				{
					int p = buffer.getInt();
					String pStr = convert ? Integer.toString(p) :
						((p < 0) ? "null" : Pmap.get(p));
					int t = buffer.getInt();
					System.out.println("QUAD " + pStr + " " + t);
					break;
				}
				case EventKind.BASIC_BLOCK:
				{
					int b = buffer.getInt();
					String bStr = convert ? Integer.toString(b) :
						((b < 0) ? "null" : Bmap.get(b));
					int t = buffer.getInt();
					System.out.println("BASIC_BLOCK " + bStr + " " + t);
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


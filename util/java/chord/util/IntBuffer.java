/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;


/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class IntBuffer {
	private int size;
	private byte[] buffer;
	private FileInputStream iStream;
	private FileOutputStream oStream;
	private int curPos;
	private int maxPos;  // used only if isRead
	public IntBuffer(int size, String fileName, boolean isRead) throws IOException {
		assert (size %4 == 0 && size >= 4);
		this.size = size;
		buffer = new byte[size];
		if (isRead) {
			iStream = new FileInputStream(fileName);
			refill();
		} else {
			oStream = new FileOutputStream(fileName);
		}
	}

	// wr mode methods
	// note: calling flush multiple times or calling put after flush is undefined
	public void put(int v) throws IOException {
		buffer[curPos++] = (byte) (v >> 24);
		buffer[curPos++] = (byte) (v >> 16);
		buffer[curPos++] = (byte) (v >> 8);
		buffer[curPos++] = (byte) v;
		if (curPos == size) {
			oStream.write(buffer, 0, size);
			curPos = 0;
		}
	}
	public void flush() throws IOException {
		oStream.write(buffer, 0, curPos);
		oStream.close();
		oStream = null;
	}

	// rd mode methods
	// note: calling get after isDone returns true is undefined
	public int get() throws IOException {
		int v = (buffer[curPos++] << 24) |
			((buffer[curPos++] & 0xFF) << 16) |
			((buffer[curPos++] & 0xFF) << 8) |
			 (buffer[curPos++] & 0xFF);
		if (curPos == maxPos && iStream != null)
			refill();
		return v;
	}
	public boolean isDone() {
		return curPos == maxPos && iStream == null;
	}
	private void refill() throws IOException {
		curPos = 0;
		maxPos = iStream.read(buffer, 0, size);
		if (maxPos < size) {
			iStream.close();
			iStream = null;
		}
	}
}

/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.text.DateFormat;
import java.io.PrintWriter;

/**
 * Timer implementation.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Timer implements Iterable<Timer> {
	public final String name;
	private final List<Timer> subTimers;
	private Date initDate;
	private Date doneDate;
	public void saveToXMLFile(String fileName) {
		PrintWriter out = FileUtils.newPrintWriter(fileName);
		out.println("<profile>");
		printTimer(this, 0, out);
		out.println("</profile>");
		out.close();
	}
	private void printTimer(Timer timer, int numTabs, PrintWriter out) {
		String tabs = "";
		for (int i = 0; i < numTabs; i++)
			tabs += "\t";
		out.println(tabs + "<timer name=\"" + timer.name + "\"" +
			" initTime=\"" + timer.getInitTimeStr() + "\"" +
			" doneTime=\"" + timer.getDoneTimeStr() + "\"" +
			" execTime=\"" + timer.getExecTimeStr() + "\"" +
			">");
		for (Timer subTimer : timer.subTimers) {
			printTimer(subTimer, numTabs + 1, out);
		}
		out.println(tabs + "</timer>");
	}
	public Timer(String name) {
		this.name = name;
		subTimers = new ArrayList<Timer>();
		initDate = null;
		doneDate = null;
	}
	public Iterator<Timer> iterator() {
		return subTimers.iterator();
	}
	public void addSubTimer(Timer timer) {
		subTimers.add(timer);
	}
	public void init() {
		if (initDate != null)
			throw new RuntimeException("Timer '" + name + "' already started.");
		initDate = new Date();
	}
	public void done() {
		if (doneDate != null)
			throw new RuntimeException("Timer '" + name + "' already stopped."); 
		doneDate = new Date();
	}
	public String getInitTimeStr() {
		if (initDate == null)
			throw new RuntimeException("Timer '" + name + "' not started."); 
		return DateFormat.getDateTimeInstance().format(initDate);
    }
	public String getDoneTimeStr() {
		if (doneDate == null)
			throw new RuntimeException("Timer '" + name + "' not stopped."); 
		return DateFormat.getDateTimeInstance().format(doneDate);
	}
	public String getExecTimeStr() {
		if (initDate == null)
			throw new RuntimeException("Timer '" + name + "' not started."); 
		if (doneDate == null)
			throw new RuntimeException("Timer '" + name + "' not stopped."); 
		return getTimeStr(doneDate.getTime() - initDate.getTime());
	}
	public static String getTimeStr(long time) {
        time /= 1000;
        String ss = String.valueOf(time % 60);
        if (ss.length() == 1)
        	ss = "0" + ss;
        time /= 60;
        String mm = String.valueOf(time % 60);
        if (mm.length() == 1)
        	mm = "0" + mm;
        time /= 60;
        String hh = String.valueOf(time % 24);
        if (hh.length() == 1)
        	hh = "0" + hh;
        time /= 24;
        String timeStr;
        if (time > 0)
        	timeStr = "&gt; 1 day";
        else
        	timeStr = hh + ":" + mm + ":" + ss + " hh:mm:ss";
        return timeStr;
	}
}

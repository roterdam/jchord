/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;
import java.util.Timer;

/**
 * Utility to execute a system command specified as a string in a
 * separate process.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class ProcessExecutor {

	public static final int execute(String[] cmdarray) throws Throwable {
		return execute(cmdarray, -1);
	}
	/**
	 * Executes a given system command specified as a string in a
	 * separate process.
	 * <p>
	 * The invoking process waits till the invoked process finishes.
	 * 
	 * @param	cmdarray	A system command to be executed.
	 * 
	 * @return	The exit value of the invoked process.
	 * 			By convention, 0 indicates normal termination.
	 */
    public static final int execute(String[] cmdarray, int timeout) throws Throwable {
	   	Process proc = Runtime.getRuntime().exec(cmdarray);
		StreamGobbler err = new StreamGobbler(proc.getErrorStream(), "ERR");
		StreamGobbler out = new StreamGobbler(proc.getInputStream(), "OUT");
		err.start();
		out.start();

		TimerTask killOnDelay = null;

		if(timeout > 0) {
 			Timer t = new Timer();
			killOnDelay = new KillOnTimeout(proc);
			t.schedule(killOnDelay, timeout);
		}
		int exitValue = proc.waitFor();
		if(timeout > 0)
			killOnDelay.cancel();

		return exitValue;
	}
    private static class StreamGobbler extends Thread {
        private final InputStream s;
        private final String n;
        private StreamGobbler(InputStream s, String n) {
            this.s = s;
            this.n = n;
        }
        public void run() {
            try {
                BufferedReader r =
                	new BufferedReader(new InputStreamReader(s));
                String l;
                while ((l = r.readLine()) != null)
                    System.out.println(n + "> " + l);
            } catch (IOException e) {
            	throw new RuntimeException(e.getMessage());
            }
        }
    }
	private static class KillOnTimeout extends TimerTask {
		Process p;
		public KillOnTimeout(Process p) {
			this.p = p;
		}
		public void run() {
			p.destroy();
		}
	}
}


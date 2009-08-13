package javato.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Copyright (c) 2007-2008,
 * Koushik Sen    <ksen@cs.berkeley.edu>
 * Pallavi Joshi	<pallavi@cs.berkeley.edu>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class Parameters {
    public static final int DIVERGENCE_BREAKING_THRESHOLD = 25;
    public static final int N_RACE_TRACKED = 10;
    public static final long DEADLOCK_CHECKING_INTERVAL_MS = 5;
    public static final int N_VECTOR_CLOCKS_WINDOW = 5;
    public static final String RACE_LOG_FILE = System.getProperty("javato.race.logfile", "race.log");
    public static final String TRANSITION_LABELS_FILE = System.getProperty("pretex.transitionlabels", null);
    public static final String ATOMICITY_LOG_FILE = System.getProperty("javato.atomicity.logfile", "atomicity.log");
    public static final String EXCEPTIONS_LOG_FILE = System.getProperty("javato.exceptions.logfile", "exceptions.log");
    public static final String OBSERVER_CLASS_FILE = System.getProperty("javato.observer", "javato.pretex.PretexEventHandler");
    public static final boolean DEBUG = System.getProperty("javato.debug", "false").equals("true");
    public static final String RACE_ID = System.getProperty("javato.racenumber", "0");
    public static final boolean IS_LOGGING_TIME = System.getProperty("javato.timelog", "false").equals("true");
    public static final String TYPE_TO_TRACK = System.getProperty("printMethodCalls.type", null);

    public static final long RANDOM_SLEEP_TIME = 2;
    public static final int EXECUTE_BOTH_BIAS = 2;
    public static final String ATOMICITY_ID = System.getProperty("javato.atomicitynumber", "0");


    public static final String iidToLineMapFile = "iidToLine.map";
    public static final String usedIdFile = "javato.usedids";

    static public int readInteger(String filename, int defaultVal) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            int ret = Integer.parseInt(in.readLine());
            in.close();
            return ret;
        } catch (Exception e) {
        }
        return 0;
    }


}

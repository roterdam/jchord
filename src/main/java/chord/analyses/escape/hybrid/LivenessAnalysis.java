/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.escape.hybrid;

import chord.project.Project;
import chord.project.ProgramRel;
import chord.project.Properties;

import java.io.*;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class LivenessAnalysis {
	private static int[] def;
	private static int[][] defs;
	private static int[] use;
	private static int[][] uses;
	private static int[][] live;
	private static boolean changed;
	private static final int REDIRECTED = ThreadEscapePathAnalysis.REDIRECTED;
	private static final int NULL_U_VAL = ThreadEscapePathAnalysis.NULL_U_VAL;
	private static final int NULL_Q_VAL = ThreadEscapePathAnalysis.NULL_Q_VAL;
	private static final int AVG_LIVE_VARS_ESTIMATE = 4;
	public static void run(int[] def2, int[][] defs2,
			int[] use2, int[][] uses2,
			int[] succ, int[][] succs, int numQ) {
		System.out.println("ENTER LivenessAnalysis");
/*
		for (int i = 0; i < numQ; i++) {
			System.out.print(i + ": " + succ[i]);
			int[] S = succs[i];
			if (S != null) {
				int n = S[0];
				for (int j = 1; j <= n; j++)
					System.out.print(" " + S[j]);
			}
			System.out.println();
		}
*/
		def = def2;
		defs = defs2;
		use = use2;
		uses = uses2;
		live = new int[numQ][];
		for (int q = 0; q < numQ; q++)
			live[q] = new int[AVG_LIVE_VARS_ESTIMATE + 1];
		changed = true;
		for (int iter = 0; changed; iter++) {
			System.out.println("ITERATION: " + iter);
			changed = false;
			for (int q = numQ - 1; q >= 0; q--) {
				final int r = succ[q];
				if (r != NULL_Q_VAL) {
					if (r != REDIRECTED)
						process(q, r);
					else {
						final int[] S = succs[q];
						final int nS = S[0];
						for (int i = 1; i <= nS; i++)
							process(q, S[i]);
					}
				}
			}
		}
		System.out.println("LEAVE LivenessAnalysis");

        long numLive = 0;
        for (int q = 0; q < numQ; q++)
            numLive += live[q][0];
        System.out.println("NUM LIVE: " + numLive);

        ProgramRel liveQU_o = (ProgramRel) Project.getTrgt("liveQU_o");
		liveQU_o.zero();
		for (int q = 0; q < numQ; q++) {
			final int[] Q = live[q];
			final int nQ = Q[0];
			for (int i = 1; i <= nQ; i++)
				liveQU_o.add(q, Q[i]);
		}
		liveQU_o.save();
/*
		// presQU(q,v) :- live_o(q,v), !defQV(q,v).
		int[][] pres = new int[numQ][];
		for (int q = 0; q < numQ; q++)
			pres[q] = new int[AVG_LIVE_VARS_ESTIMATE + 1];
		for (int q = 0; q < numQ; q++) {
			int[] P = pres[q];
			final int[] origP = P;
			final int[] Q = live[q];
			final int nQ = Q[0];
			final int d = def[q];
			if (d == NULL_U_VAL) {
				for (int i = 1; i <= nQ; i++)
					P = addIfAbsent(Q[i], P);
			} else if (d != REDIRECTED) {
				for (int i = 1; i <= nQ; i++) {
					final int l = Q[i];
					if (d != l)
						P = addIfAbsent(l, P);
				}
			} else {
				final int[] D = defs[q];
				final int nD = D[0];
				for (int i = 1; i <= nQ; i++) {
					final int l = Q[i];
					boolean found = false;
					for (int j = 1; j <= nD; j++) {
						if (D[j] == l) {
							found = true;
							break;
						}
					}
					if (!found)
						P = addIfAbsent(l, P);
				}
			}
			if (P != origP)
				pres[q] = P;
		}

		long numPres = 0;
		for (int q = 0; q < numQ; q++) {
			numPres += pres[q][0];
		}
		System.out.println("NUM PRES: " + numPres);

        ProgramRel presQU = (ProgramRel) Project.getTrgt("presQU");
		presQU.zero();
		for (int q = 0; q < numQ; q++) {
			final int[] Q = pres[q];
			final int nQ = Q[0];
			for (int i = 1; i <= nQ; i++)
				presQU.add(q, Q[i]);
		}
		presQU.save();
*/
	}

	private static void process(int q, int r) {
		int[] Q = live[q];
		final int[] origQ = Q;

		// live[q] must contain all in use[r]
		final int u = use[r];
		if (u != NULL_U_VAL) {
			if (u != REDIRECTED)
				Q = addIfAbsent(u, Q);
			else {
				final int[] U = uses[r];
				final int nU = U[0];
				for (int i = 1; i <= nU; i++)
					Q = addIfAbsent(U[i], Q);
			}
		}

		// live[q] must contain all in (live[r] - def[r])
		final int[] R = live[r];
		final int nR = R[0];
		final int d = def[r];
		if (d == NULL_U_VAL) {
			for (int i = 1; i <= nR; i++)
				Q = addIfAbsent(R[i], Q);
		} else if (d != REDIRECTED) {
			for (int i = 1; i <= nR; i++) {
				final int l = R[i];
				if (d != l)
					Q = addIfAbsent(l, Q);
			}
		} else {
			final int[] D = defs[r];
			final int nD = D[0];
			for (int i = 1; i <= nR; i++) {
				final int l = R[i];
				boolean found = false;
				for (int j = 1; j <= nD; j++) {
					if (D[j] == l) {
						found = true;
						break;
					}
				}
				if (!found)
					Q = addIfAbsent(l, Q);
			}
		}

		if (Q != origQ)
			live[q] = Q;
	}

	private static int[] addIfAbsent(int v, int[] l) {
		final int n = l[0];
		for (int i = 1; i <= n; i++) {
			if (l[i] == v)
				return l;
		}
		int len = l.length;
		if (n == len - 1) {
			int[] l2 = new int[len * 2];
			System.arraycopy(l, 0, l2, 0, len);
			l = l2;
		}
		l[++l[0]] = v; 
		changed = true;
		return l;
	}
}

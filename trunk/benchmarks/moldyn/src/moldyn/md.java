/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - Version 2.0             *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                  Original version of this code by                       *
*                         Dieter Heermann                                 * 
*                       converted to Java by                              *
*                Lorna Smith  (l.smith@epcc.ed.ac.uk)                     *
*                   (see copyright notice below)                          *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/

package moldyn;

import jgfutil.*;

public class md {

  public static final int ITERS = 100;
  public static final double LENGTH = 50e-10;
  public static final double m = 4.0026;
  public static final double mu = 1.66056e-27;
  public static final double kb = 1.38066e-23;
  public static final double TSIM = 50;
  public static final double deltat = 5e-16;

  public static int PARTSIZE;

  public static double [] epot;
  public static double [] vir;
  public static double [] ek;

  int size,mm;
  int datasizes[] = {8,13};

  public static int interactions = 0;
  public static int [] interacts;
 
  public void initialise() {

  mm = datasizes[size];
  PARTSIZE = mm*mm*mm*4;

  }


  public void runiters(){

/* Create new arrays */

    epot = new double [JGFMolDynBench.nthreads];
    vir  = new double [JGFMolDynBench.nthreads];
    ek   = new double [JGFMolDynBench.nthreads];

    interacts = new int [JGFMolDynBench.nthreads];

    double sh_force [][] = new double[3][PARTSIZE];
    double sh_force2 [][][] = new double[3][JGFMolDynBench.nthreads][PARTSIZE];

/* spawn threads */

    Runnable thobjects[] = new Runnable [JGFMolDynBench.nthreads];
    Thread th[] = new Thread [JGFMolDynBench.nthreads];
    Barrier br= new TournamentBarrier(JGFMolDynBench.nthreads);

    for(int i=1;i<JGFMolDynBench.nthreads;i++) {
      thobjects[i] = new mdRunner(i,mm,sh_force,sh_force2,br);
      th[i] = new Thread(thobjects[i]);
      th[i].start();
    }

    thobjects[0] = new mdRunner(0,mm,sh_force,sh_force2,br);
    thobjects[0].run();

    for(int i=1;i<JGFMolDynBench.nthreads;i++) {
      try {
        th[i].join();
      }
      catch (InterruptedException e) {}
    }

  }


}


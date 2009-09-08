package net.javacoding.jspider.core.threading;

import junit.framework.TestCase;
import net.javacoding.jspider.core.SpiderContext;
import net.javacoding.jspider.core.task.DispatcherTask;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.mockobjects.*;
import net.javacoding.jspider.mockobjects.util.Counter;



/**
 * Unit tests for the WorkerThreadPool class.
 *
 * $Id: WorkerThreadPoolTest.java,v 1.11 2003/04/03 15:57:24 vanrogu Exp $
 *
 * @author  Günther Van Roey
 */
public class WorkerThreadPoolTest extends TestCase {

    /** How many threads are in the pools used during these tests. */
    public final int POOL_SIZE = 5;

    /**
     * Public constructor giving a name to the test.
     */
    public WorkerThreadPoolTest ( ) {
        super ( "workerThreadTest ");
        // make sure we're using the 'unittest' configuration
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
    }

    /**
     * JUnit's overridden setUp method
     * @throws Exception in case something fails during setup
     */
    protected void setUp() throws Exception {
    }

    /**
     * JUnit's overridden tearDown method
     * @throws Exception in case something fails during tearDown
     */
    protected void tearDown() throws Exception {
    }

    /**
     * Test the instantiation and the stopping of all worker threads that
     * are grouped in a pool.
     */
    public void testWorkerThreadPoolInstantiation ( ) {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        pool.stopAll ( );
    }

    /**
     * Tests whether the pool adheres to the size specifications passed.
     */
    public void testWorkerThreadPoolSize ( ) {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        assertEquals(pool.getSize(), POOL_SIZE);
        pool.stopAll();
    }

    /**
     * Tests whether the occupation of a non-assigned thread pool equals zero.
     */
    public void testWorkerThreadOccupation ( ) {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        assertEquals("Created fresh pool, occupation is not 0", 0, pool.getOccupation());
        pool.stopAll();
    }

    /**
     * Tests the dispatcher thread functionality.
     * Dispatch some tasks and see if all threads die smoothly in time and the
     * occupation of the pool is 0% afterwards.
     */
    public void testDispatcherThread ( ) throws Exception {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        SpiderContext context = new SimpleSpiderContext();
        DispatcherTask dispatcherTask = new WaitTaskDispatcherTask ( pool, context, 10, 100 );
          synchronized ( dispatcherTask ) {
              try {
                  pool.assignGroupTask(dispatcherTask);
                  // This SHOULDN'T take 10 seconds, even on the slowest machine!
                  // After 10 seconds, we can assume something is broke, and our
                  // threads hang.
                  dispatcherTask.wait(10000);
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
              }
          }
        pool.stopAll();
        assertEquals("Finished jobs, threadPool occupation is not 0%", 0, pool.getOccupation());
    }

    /**
     * Test method that tests the raw Thread Pool (without dispatcher).
     * Small set.
     */
    public void testThreadedCountingNonDispatchedSmall ( ) {
        doTestThreadedCountingNonDispatched ( 10 );
    }

    /**
     * Test method that tests the raw Thread Pool (without dispatcher).
     * large set.
     */
    public void testThreadedCountingNonDispatchedLarge ( ) {
        doTestThreadedCountingNonDispatched ( 50000 );
    }

    /**
     * Test method that tests the raw Thread Pool (without dispatcher).
     * @param number number of tasks to be carried out
     */
    public void doTestThreadedCountingNonDispatched ( int number ) {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        Counter counter = new Counter();
        for ( int i = 0; i < number; i++ ) {
            pool.assign(new CountTask(counter));
        }
        pool.stopAll();
        assertEquals("Counter jobs finished, counter value is not " + number, number, counter.getValue());
    }

    /**
     * Uses the ThreadPool (with dispatcher thread).
     * Small test set.
     */
    public void testThreadedCountingDispatchedSmall ( ) throws Exception {
        doTestThreadedCountingDispatched(10);
    }

    /**
     * Uses the ThreadPool (with dispatcher thread).
     * Large test set.
     */
    public void testThreadedCountingDispatchedLarge ( ) throws Exception {
        doTestThreadedCountingDispatched(50000);
    }

    /**
     * Test method that tests the Thread Pool (with dispatcher).
     * @param number number of tasks to be carried out
     */
    public void doTestThreadedCountingDispatched ( int number ) throws Exception {
        WorkerThreadPool pool = new WorkerThreadPool ( "testPool", "testThread", POOL_SIZE );
        SpiderContext context = new SimpleSpiderContext();
        Counter counter = new Counter();
        DispatcherTask dispatcherTask = new CountTaskDispatcherTask ( context, pool, counter, number );
         synchronized ( dispatcherTask ) {
              pool.assignGroupTask(dispatcherTask);
              try {
                  dispatcherTask.wait();
              } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
              }
          }
        pool.stopAll();
        assertEquals("Counter jobs finished, counter value is not " + number, number, counter.getValue());
    }

}
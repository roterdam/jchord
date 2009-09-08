package net.javacoding.jspider.core.threading;

import junit.framework.TestCase;
import net.javacoding.jspider.core.task.WorkerTask;
import net.javacoding.jspider.core.util.config.ConfigurationFactory;
import net.javacoding.jspider.mockobjects.*;

import java.net.URL;

/**
 * $Id: WorkerThreadTest.java,v 1.6 2003/04/09 17:08:14 vanrogu Exp $
 */
public class WorkerThreadTest extends TestCase {

    protected WorkerThread thread;
    protected WorkerThreadPool pool;

    public WorkerThreadTest ( ) {
        super ( "WorkerThreadTest" );
        // make sure we're using the 'unittest' configuration
        ConfigurationFactory.getConfiguration(ConfigurationFactory.CONFIG_UNITTEST);
    }

    protected void setUp() throws Exception {
        pool = new MockWorkerThreadPool ( );
        thread = new WorkerThread(pool, "testThread", 1);
    }

    public void testDoubleAssign ( ) {
        thread.start();
        sleep();
        try {
            thread.assign(new WaitTask(null, 1000));
        } catch (Exception e) {
            e.printStackTrace();
            fail("thread throw an exception during the first assign");
        }
        try {
            thread.assign(new WaitTask(null, 1000));
        } catch (Exception e) {
            // thread telling us he's already busy doing something else
            return;
        }
        fail("thread threw no exception during the first assign");
    }

    public void testAvailable ( ) {
        thread.start();
        sleep();
        assertTrue ( "idle thread claiming not to be available", thread.isAvailable() );
        assertFalse ( "idle thread claiming not to be occupied", thread.isOccupied() );
        try {
            thread.assign(new WaitTask(null, 1000));
            assertFalse ( "busy thread claiming to be available", thread.isAvailable() );
            assertTrue ( "busy thread claiming to be occupied", thread.isOccupied() );
        } catch (Exception e) {
            fail("thread throw an exception during the first assign");
        }
        try {
            thread.assign(new WaitTask(null, 1000));
        } catch (Exception e) {
            assertFalse ( "busy thread claiming to be available", thread.isAvailable() );
            assertTrue ( "busy thread claiming to be occupied", thread.isOccupied() );
            // thread telling us he's already busy doing something else
            return;
        }
        fail("thread threw no exception during the first assign");
    }

    public void testStopRunning ( ) {
        thread.start();
        sleep();
        thread.stopRunning();
        assertFalse ( "stopped thread claiming to be available", thread.isAvailable() );
        assertFalse ( "stopped thread claiming to be occupied", thread.isOccupied() );
    }

    public void testStopRunningNotStarted ( ) {
        try {
            thread.stopRunning();
        } catch (Exception e) {
            return;
        }
        fail ( "not started thread accepted to be stopped" );
    }

    public void testAssignNotStarted ( ) {
        try {
            thread.assign(new WaitTask(null, 1000));
        } catch (Exception e) {
            return;
        }
        fail("not started thread accepted an assigned task");
    }

    public void testAssignAfterStop ( ) {
        thread.start();
        sleep();
        thread.stopRunning();
        try {
            thread.assign(new WaitTask(null, 1000));
        } catch (Exception e) {
            return;
        }
        fail("stopped thread accepted an assigned task");
    }

    public void testInitialState ( ) {
        int state = thread.getState1();
        int expected = WorkerThread.WORKERTHREAD_IDLE;
        assertEquals ( "newly created thread reports another state than IDLE", expected, state);
    }

    public void testBlockedState ( ) throws Exception {
        URL url = new URL ("http://j-spider.sourceforge.net");
        WorkerTask task = new WaitTask ( new SimpleSpiderContext(url), 1000, 1000 );
        thread.start();
        sleep();
        thread.assign(task);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int state = thread.getState1();
        int expected = WorkerThread.WORKERTHREAD_BLOCKED;
        assertEquals ( "thread waiting in prepare() did report another state than BLOCKED", expected, state);
    }

    public void testBusyState ( ) throws Exception {
        URL url = new URL ("http://j-spider.sourceforge.net");
        WorkerTask task = new WaitTask ( new SimpleSpiderContext(url), 1000, 0 );
        thread.start();
        sleep();
        thread.assign(task);
        sleep();
        int state = thread.getState1();
        int expected = WorkerThread.WORKERTHREAD_BUSY;
        assertEquals ( "thread waiting in execute() did report another state than BUSY", expected, state);
    }

    public void testIdleStateAfterTask ( ) throws Exception {
        URL url = new URL ("http://j-spider.sourceforge.net");
        WorkerTask task = new WaitTask ( new SimpleSpiderContext(url), 10, 0 );
        thread.start();
        sleep();
        thread.assign(task);
        sleep();
        int state = thread.getState1();
        int expected = WorkerThread.WORKERTHREAD_IDLE;
        assertEquals ( "thread that should have completed it's job reported another state than IDLE", expected, state);
    }


    // give the thread the time to start the run() method.
    protected void sleep(){
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

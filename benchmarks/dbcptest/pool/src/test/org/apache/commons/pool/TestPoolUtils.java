/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.pool;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.TimerTask;
import java.util.Collection;
import java.util.Map;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Unit tests for {@link PoolUtils}.
 *
 * @author Sandy McArthur
 * @version $Revision: 384433 $ $Date: 2006-03-09 00:19:21 -0500 (Thu, 09 Mar 2006) $
 */
public class TestPoolUtils extends TestCase {

    /** Period between checks for minIdle tests. Increase this if you happen to get too many false failures. */
    private static final int CHECK_PERIOD = 300;

    /** Times to let the minIdle check run. */
    private static final int CHECK_COUNT = 4;

    /** Sleep time to let the minIdle tests run CHECK_COUNT times. */
    private static final int CHECK_SLEEP_PERIOD = CHECK_PERIOD * (CHECK_COUNT - 1) + CHECK_PERIOD / 2;

    public static Test suite() {
        return new TestSuite(TestPoolUtils.class);
    }

    public void testJavaBeanInstantiation() {
        new PoolUtils();
    }

    public void testAdaptKeyedPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.adapt((KeyedPoolableObjectFactory)null);
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedPoolableObjectFactoryKey() throws Exception {
        try {
            PoolUtils.adapt((KeyedPoolableObjectFactory)null, new Object());
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory, key) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            PoolUtils.adapt((KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, null), null);
            fail("PoolUtils.adapt(KeyedPoolableObjectFactory, key) must not allow null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedPoolableObjectFactory kpof =
                (KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, calledMethods);

        final PoolableObjectFactory pof = PoolUtils.adapt(kpof);
        final List expectedMethods = invokeEveryMethod(pof);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.adapt((PoolableObjectFactory)null);
            fail("PoolUtils.adapt(PoolableObjectFactory) must not allow null factory.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final PoolableObjectFactory pof =
                (PoolableObjectFactory)createProxy(PoolableObjectFactory.class, calledMethods);

        final KeyedPoolableObjectFactory kpof = PoolUtils.adapt(pof);
        final List expectedMethods = invokeEveryMethod(kpof);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptKeyedObjectPool() throws Exception {
        try {
            PoolUtils.adapt((KeyedObjectPool)null);
            fail("PoolUtils.adapt(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
    }

    public void testAdaptKeyedObjectPoolKey() throws Exception {
        try {
            PoolUtils.adapt((KeyedObjectPool)null, new Object());
            fail("PoolUtils.adapt(KeyedObjectPool, key) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            PoolUtils.adapt((KeyedObjectPool)createProxy(KeyedObjectPool.class, null), null);
            fail("PoolUtils.adapt(KeyedObjectPool, key) must not allow a null key.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedObjectPool kop = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        final ObjectPool op = PoolUtils.adapt(kop, new Object());
        final List expectedMethods = invokeEveryMethod(op);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testAdaptObjectPool() throws Exception {
        try {
            PoolUtils.adapt((ObjectPool)null);
            fail("PoolUtils.adapt(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final ObjectPool op = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        final KeyedObjectPool kop = PoolUtils.adapt(op);
        final List expectedMethods = invokeEveryMethod(kop);
        assertEquals(expectedMethods, calledMethods);
    }

    public void testCheckedPoolObjectPool() throws Exception {
        try {
            PoolUtils.checkedPool((ObjectPool)null, Object.class);
            fail("PoolUtils.checkedPool(ObjectPool, Class) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            PoolUtils.checkedPool((ObjectPool)createProxy(ObjectPool.class, null), null);
            fail("PoolUtils.checkedPool(ObjectPool, Class) must not allow a null type.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        ObjectPool op = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        ObjectPool cop = PoolUtils.checkedPool(op, Object.class);
        final List expectedMethods = invokeEveryMethod(cop);
        assertEquals(expectedMethods, calledMethods);

        op = new BaseObjectPool() {
            public Object borrowObject() throws Exception {
                return new Integer(0);
            }
            public void returnObject(Object obj) {}
            public void invalidateObject(Object obj) {}
        };
        cop = PoolUtils.checkedPool(op, String.class);

        try {
            cop.borrowObject();
            fail("borrowObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop.returnObject(new Integer(1));
            fail("returnObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop.invalidateObject(new Integer(2));
            fail("invalidateObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
    }

    public void testCheckedPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.checkedPool((KeyedObjectPool)null, Object.class);
            fail("PoolUtils.checkedPool(KeyedObjectPool, Class) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }
        try {
            PoolUtils.checkedPool((KeyedObjectPool)createProxy(KeyedObjectPool.class, null), null);
            fail("PoolUtils.checkedPool(KeyedObjectPool, Class) must not allow a null type.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        KeyedObjectPool op = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        KeyedObjectPool cop = PoolUtils.checkedPool(op, Object.class);
        final List expectedMethods = invokeEveryMethod(cop);
        assertEquals(expectedMethods, calledMethods);


        op = new BaseKeyedObjectPool() {
            public Object borrowObject(Object key) {
                return new Integer(0);
            }

            public void returnObject(Object key, Object obj) {}

            public void invalidateObject(Object key, Object obj) {}
        };
        cop = PoolUtils.checkedPool(op, String.class);

        try {
            cop.borrowObject(null);
            fail("borrowObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop.returnObject(null, new Integer(1));
            fail("returnObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
        try {
            cop.invalidateObject(null, new Integer(2));
            fail("invalidateObject should have failed as Integer !instanceof String.");
        } catch (ClassCastException cce) {
            // expected
        }
    }

    public void testCheckMinIdleObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, 1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            final ObjectPool pool = (ObjectPool)createProxy(ObjectPool.class, null);
            PoolUtils.checkMinIdle(pool, -1, 1);
            fail("PoolUtils.checkMinIdle(ObjectPool,,) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Because this isn't determinist and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                final List calledMethods = new ArrayList();
                final ObjectPool pool = (ObjectPool)createProxy(ObjectPool.class, calledMethods);
                final TimerTask task = PoolUtils.checkMinIdle(pool, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                task.cancel();
                task.toString();

                final List expectedMethods = new ArrayList();
                for (int i=0; i < CHECK_COUNT; i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                expectedMethods.add("toString");
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    public void testCheckMinIdleKeyedObjectPool() throws Exception {
        try {
            PoolUtils.checkMinIdle(null, new Object(), 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, null);
            PoolUtils.checkMinIdle(pool, (Object)null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, null);
            PoolUtils.checkMinIdle(pool, new Object(), -1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Object,int,long) must not accept negative min idle values.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Because this isn't determinist and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                final List calledMethods = new ArrayList();
                final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);
                final Object key = new Object();
                final TimerTask task = PoolUtils.checkMinIdle(pool, key, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                task.cancel();
                task.toString();

                final List expectedMethods = new ArrayList();
                for (int i=0; i < CHECK_COUNT; i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                expectedMethods.add("toString");
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    public void testCheckMinIdleKeyedObjectPoolKeys() throws Exception {
        try {
            final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, null);
            PoolUtils.checkMinIdle(pool, null, 1, 1);
            fail("PoolUtils.checkMinIdle(KeyedObjectPool,Collection,int,long) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        // Because this isn't determinist and you can get false failures, try more than once.
        AssertionFailedError afe = null;
        int triesLeft = 3;
        do {
            afe = null;
            try {
                final List calledMethods = new ArrayList();
                final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);
                final Collection keys = new ArrayList(2);
                keys.add("one");
                keys.add("two");
                final Map tasks = PoolUtils.checkMinIdle(pool, keys, 1, CHECK_PERIOD); // checks minIdle immediately

                Thread.sleep(CHECK_SLEEP_PERIOD); // will check CHECK_COUNT more times.
                final Iterator iter = tasks.values().iterator();
                while (iter.hasNext()) {
                    final TimerTask task = (TimerTask)iter.next();
                    task.cancel();
                }

                final List expectedMethods = new ArrayList();
                for (int i=0; i < CHECK_COUNT * keys.size(); i++) {
                    expectedMethods.add("getNumIdle");
                    expectedMethods.add("addObject");
                }
                assertEquals(expectedMethods, calledMethods); // may fail because of the thread scheduler
            } catch (AssertionFailedError e) {
                afe = e;
            }
        } while (--triesLeft > 0 && afe != null);
        if (afe != null) {
            throw afe;
        }
    }

    public void testPrefillObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, 1);
            fail("PoolUtils.prefill(ObjectPool,int) must not allow null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final ObjectPool pool = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        PoolUtils.prefill(pool, 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        PoolUtils.prefill(pool, 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testPrefillKeyedObjectPool() throws Exception {
        try {
            PoolUtils.prefill(null, new Object(), 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null pool.");
        } catch (IllegalArgumentException iae) {
            // expected
        }
        try {
            final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, null);
            PoolUtils.prefill(pool, (Object)null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Object,int) must not accept null key.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        PoolUtils.prefill(pool, new Object(), 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        PoolUtils.prefill(pool, new Object(), 3);
        for (int i=0; i < 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testPrefillKeyedObjectPoolCollection() throws Exception {
        try {
            final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, null);
            PoolUtils.prefill(pool, null, 1);
            fail("PoolUtils.prefill(KeyedObjectPool,Collection,int) must not accept null keys.");
        } catch (IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedObjectPool pool = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        final Set keys = new HashSet();
        PoolUtils.prefill(pool, keys, 0);
        final List expectedMethods = new ArrayList();
        assertEquals(expectedMethods, calledMethods);

        calledMethods.clear();
        keys.add(new Integer(1));
        keys.add("two");
        keys.add(new Double(3.1415926));
        PoolUtils.prefill(pool, keys, 3);
        for (int i=0; i < keys.size() * 3; i++) {
            expectedMethods.add("addObject");
        }
        assertEquals(expectedMethods, calledMethods);
    }

    public void testSynchronizedPoolObjectPool() throws Exception {
        try {
            PoolUtils.synchronizedPool((ObjectPool)null);
            fail("PoolUtils.synchronizedPool(ObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final ObjectPool op = (ObjectPool)createProxy(ObjectPool.class, calledMethods);

        final ObjectPool sop = PoolUtils.synchronizedPool(op);
        final List expectedMethods = invokeEveryMethod(sop);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolKeyedObjectPool() throws Exception {
        try {
            PoolUtils.synchronizedPool((KeyedObjectPool)null);
            fail("PoolUtils.synchronizedPool(KeyedObjectPool) must not allow a null pool.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedObjectPool kop = (KeyedObjectPool)createProxy(KeyedObjectPool.class, calledMethods);

        final KeyedObjectPool skop = PoolUtils.synchronizedPool(kop);
        final List expectedMethods = invokeEveryMethod(skop);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedPoolableFactory((PoolableObjectFactory)null);
            fail("PoolUtils.synchronizedPoolableFactory(PoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final PoolableObjectFactory pof =
                (PoolableObjectFactory)createProxy(PoolableObjectFactory.class, calledMethods);

        final PoolableObjectFactory spof = PoolUtils.synchronizedPoolableFactory(pof);
        final List expectedMethods = invokeEveryMethod(spof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    public void testSynchronizedPoolableFactoryKeyedPoolableObjectFactory() throws Exception {
        try {
            PoolUtils.synchronizedPoolableFactory((KeyedPoolableObjectFactory)null);
            fail("PoolUtils.synchronizedPoolableFactory(KeyedPoolableObjectFactory) must not allow a null factory.");
        } catch(IllegalArgumentException iae) {
            // expected
        }

        final List calledMethods = new ArrayList();
        final KeyedPoolableObjectFactory kpof =
                (KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, calledMethods);

        final KeyedPoolableObjectFactory skpof = PoolUtils.synchronizedPoolableFactory(kpof);
        final List expectedMethods = invokeEveryMethod(skpof);
        assertEquals(expectedMethods, calledMethods);

        // TODO: Anyone feel motivated to construct a test that verifies proper synchronization?
    }

    private static List invokeEveryMethod(ObjectPool op) throws Exception {
        op.addObject();
        op.borrowObject();
        op.clear();
        op.close();
        op.getNumActive();
        op.getNumIdle();
        op.invalidateObject(new Object());
        op.returnObject(new Object());
        op.setFactory((PoolableObjectFactory)createProxy(PoolableObjectFactory.class, null));
        op.toString();

        final List expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "close",
                "getNumActive", "getNumIdle", "invalidateObject",
                "returnObject", "setFactory", "toString"
        });
        return expectedMethods;
    }

    private static List invokeEveryMethod(KeyedObjectPool kop) throws Exception {
        kop.addObject(null);
        kop.borrowObject(null);
        kop.clear();
        kop.clear(null);
        kop.close();
        kop.getNumActive();
        kop.getNumActive(null);
        kop.getNumIdle();
        kop.getNumIdle(null);
        kop.invalidateObject(null, new Object());
        kop.returnObject(null, new Object());
        kop.setFactory((KeyedPoolableObjectFactory)createProxy(KeyedPoolableObjectFactory.class, null));
        kop.toString();

        final List expectedMethods = Arrays.asList(new String[] {
                "addObject", "borrowObject", "clear", "clear", "close",
                "getNumActive", "getNumActive", "getNumIdle", "getNumIdle", "invalidateObject",
                "returnObject", "setFactory", "toString"
        });
        return expectedMethods;
    }

    private static List invokeEveryMethod(PoolableObjectFactory pof) throws Exception {
        pof.activateObject(null);
        pof.destroyObject(null);
        pof.makeObject();
        pof.passivateObject(null);
        pof.validateObject(null);
        pof.toString();

        final List expectedMethods = Arrays.asList(new String[] {
                "activateObject", "destroyObject", "makeObject",
                "passivateObject", "validateObject", "toString",
        });
        return expectedMethods;
    }

    private static List invokeEveryMethod(KeyedPoolableObjectFactory kpof) throws Exception {
        kpof.activateObject(null, null);
        kpof.destroyObject(null, null);
        kpof.makeObject(null);
        kpof.passivateObject(null, null);
        kpof.validateObject(null, null);
        kpof.toString();

        final List expectedMethods = Arrays.asList(new String[] {
                "activateObject", "destroyObject", "makeObject",
                "passivateObject", "validateObject", "toString",
        });
        return expectedMethods;
    }

    private static Object createProxy(final Class clazz, final List logger) {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                new MethodCallLogger(logger));
    }

    private static class MethodCallLogger implements InvocationHandler {
        private final List calledMethods;

        MethodCallLogger(final List calledMethods) {
            this.calledMethods = calledMethods;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            calledMethods.add(method.getName());
            if (boolean.class.equals(method.getReturnType())) {
                return Boolean.FALSE;
            } else if (int.class.equals(method.getReturnType())) {
                return new Integer(0);
            } else if (long.class.equals(method.getReturnType())) {
                return new Long(0);
            } else if (Object.class.equals(method.getReturnType())) {
                return new Object();
            } else {
                return null;
            }
        }
    }
}

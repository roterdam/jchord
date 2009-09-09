/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.commons.pool.impl;

import java.util.BitSet;
import java.util.HashMap;
import java.util.NoSuchElementException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.TestKeyedObjectPool;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 383290 $ $Date: 2006-03-05 02:00:15 -0500 (Sun, 05 Mar 2006) $
 */
public class TestStackKeyedObjectPool extends TestKeyedObjectPool {
    public TestStackKeyedObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestStackKeyedObjectPool.class);
    }

    protected KeyedObjectPool makeEmptyPool(int mincapacity) {
        StackKeyedObjectPool pool = new StackKeyedObjectPool(new SimpleFactory(),mincapacity);
        return pool;
    }
    
    protected Object getNthObject(Object key, int n) {
        return String.valueOf(key) + String.valueOf(n);
    }
    
    protected Object makeKey(int n) {
        return String.valueOf(n);
    }

    private StackKeyedObjectPool pool = null;

    public void setUp() throws Exception {
        super.setUp();
        pool = new StackKeyedObjectPool(
            new KeyedPoolableObjectFactory()  {
                int counter = 0;
                public Object makeObject(Object key) { return String.valueOf(key) + String.valueOf(counter++); }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) { return true; }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) { }
            }
            );
    }

    
    public void tearDown() throws Exception {
        super.tearDown();
        pool = null;
    }

    public void testCloseBug() throws Exception {
        {
            Object obj0 = pool.borrowObject("");
            Object obj1 = pool.borrowObject("");
            assertEquals(2,pool.getNumActive(""));
            assertEquals(0,pool.getNumIdle(""));
            pool.returnObject("",obj1);
            pool.returnObject("",obj0);
            assertEquals(0,pool.getNumActive(""));
            assertEquals(2,pool.getNumIdle(""));
        }
        {
            Object obj0 = pool.borrowObject("2");
            Object obj1 = pool.borrowObject("2");
            assertEquals(2,pool.getNumActive("2"));
            assertEquals(0,pool.getNumIdle("2"));
            pool.returnObject("2",obj1);
            pool.returnObject("2",obj0);
            assertEquals(0,pool.getNumActive("2"));
            assertEquals(2,pool.getNumIdle("2"));
        }
        pool.close();
    }

    public void testIdleCap() throws Exception {
        Object[] active = new Object[100];
        for(int i=0;i<100;i++) {
            active[i] = pool.borrowObject("");
        }
        assertEquals(100,pool.getNumActive(""));
        assertEquals(0,pool.getNumIdle(""));
        for(int i=0;i<100;i++) {
            pool.returnObject("",active[i]);
            assertEquals(99 - i,pool.getNumActive(""));
            assertEquals((i < 8 ? i+1 : 8),pool.getNumIdle(""));
        }
    }
    
    public void testPoolWithNullFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool(10);
        for(int i=0;i<10;i++) {
            pool.returnObject("X",new Integer(i));
        }
        for(int j=0;j<3;j++) {
            Integer[] borrowed = new Integer[10];
            BitSet found = new BitSet();
            for(int i=0;i<10;i++) {
                borrowed[i] = (Integer)(pool.borrowObject("X"));
                assertNotNull(borrowed);
                assertTrue(!found.get(borrowed[i].intValue()));
                found.set(borrowed[i].intValue());
            }
            for(int i=0;i<10;i++) {
                pool.returnObject("X",borrowed[i]);
            }
        }
        pool.invalidateObject("X",pool.borrowObject("X"));
        pool.invalidateObject("X",pool.borrowObject("X"));
        pool.clear("X");        
        pool.clear();        
    }
    
    public void testVariousConstructors() throws Exception {
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool();
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(10);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(10,5);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null,10);
            assertNotNull(pool);
        }
        {
            StackKeyedObjectPool pool = new StackKeyedObjectPool(null,10,5);
            assertNotNull(pool);
        }
    }
    
    public void testToString() throws Exception {
        StackKeyedObjectPool pool = new StackKeyedObjectPool(new SimpleFactory());
        assertNotNull(pool.toString());
        Object obj = pool.borrowObject("key");
        assertNotNull(pool.toString());
        pool.returnObject("key",obj);
        assertNotNull(pool.toString());
    }
        
    public void testBorrowFromEmptyPoolWithNullFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
    
    public void testSetFactory() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        try {
            pool.borrowObject("x");
            fail("Expected NoSuchElementException");
        } catch(NoSuchElementException e) {
            // expected
        }
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject("x");
        assertNotNull(obj);
        pool.returnObject("x",obj);
    }

    public void testCantResetFactoryWithActiveObjects() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        pool.setFactory(new SimpleFactory());
        Object obj = pool.borrowObject("x");
        assertNotNull(obj);

        try {
            pool.setFactory(new SimpleFactory());
            fail("Expected IllegalStateException");
        } catch(IllegalStateException e) {
            // expected
        }        
    }
    
    public void testCanResetFactoryWithoutActiveObjects() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject("x");        
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
        {
            pool.setFactory(new SimpleFactory());
            Object obj = pool.borrowObject("x");        
            assertNotNull(obj);
            pool.returnObject("x",obj);
        }
    }

    public void testBorrowReturnWithSometimesInvalidObjects() throws Exception {
        KeyedObjectPool pool = new StackKeyedObjectPool();
        pool.setFactory(
            new KeyedPoolableObjectFactory() {
                int counter = 0;
                public Object makeObject(Object key) { return new Integer(counter++); }
                public void destroyObject(Object key, Object obj) { }
                public boolean validateObject(Object key, Object obj) {
                    if(obj instanceof Integer) {
                        return ((((Integer)obj).intValue() % 2) == 1);
                    } else {
                        return false;
                    }
                }
                public void activateObject(Object key, Object obj) { }
                public void passivateObject(Object key, Object obj) { 
                    if(obj instanceof Integer) {
                        if((((Integer)obj).intValue() % 3) == 0) {
                            throw new RuntimeException("Couldn't passivate");
                        }
                    } else {
                        throw new RuntimeException("Couldn't passivate");
                    }
                }
            }
        );

        Object[] obj = new Object[10];
        for(int i=0;i<10;i++) {
            obj[i] = pool.borrowObject("key");
        }
        for(int i=0;i<10;i++) {
            pool.returnObject("key",obj[i]);
        }
        assertEquals(3,pool.getNumIdle("key"));
    }
 
    class SimpleFactory implements KeyedPoolableObjectFactory {
        HashMap map = new HashMap();
        public Object makeObject(Object key) { 
            int counter = 0;
            Integer Counter = (Integer)(map.get(key));
            if(null != Counter) {
                counter = Counter.intValue();
            }
            map.put(key,new Integer(counter + 1));                       
            return String.valueOf(key) + String.valueOf(counter); 
        }
        public void destroyObject(Object key, Object obj) { }
        public boolean validateObject(Object key, Object obj) { return true; }
        public void activateObject(Object key, Object obj) { }
        public void passivateObject(Object key, Object obj) { }
    }

    protected boolean isLifo() {
        return true;
    }

    protected boolean isFifo() {
        return false;
    }
}

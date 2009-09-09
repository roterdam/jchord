  //Modified the original test case taken from http://commons.apache.org/dbcp/xref-test/org/apache/commons/dbcp/TestAbandonedObjectPool.html 

   /*
    * Licensed to the Apache Software Foundation (ASF) under one or more
    * contributor license agreements.  See the NOTICE file distributed with
    * this work for additional information regarding copyright ownership.
    * The ASF licenses this file to You under the Apache License, Version 2.0
    * (the "License"); you may not use this file except in compliance with
    * the License.  You may obtain a copy of the License at
    * 
    *      http://www.apache.org/licenses/LICENSE-2.0
    * 
    * Unless required by applicable law or agreed to in writing, software
    * distributed under the License is distributed on an "AS IS" BASIS,
    * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    * See the License for the specific language governing permissions and
    * limitations under the License.
    */
  import java.util.Vector;
  
  import org.apache.commons.pool.PoolableObjectFactory;
  import org.apache.commons.pool.impl.GenericObjectPool;
  
  public class DBCPTest {
      private static SimpleFactory fac = new SimpleFactory(); 
      private static GenericObjectPool pool = new GenericObjectPool(fac);
      private static final int POOL_SIZE = 30;
      private static final int CONCURRENT_BORROWS = 5;
      private static final int EXTRA_BORROWS = 3;
  
      public static void main(String[] args) throws Exception {
          pool.setMaxActive(POOL_SIZE/2);
          pool.setMaxIdle(POOL_SIZE);
          pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
	  pool.setMaxWait(-1);

	  for(int i = 0; i < 2*POOL_SIZE/3; i++){
		pool.addObject();
	  }

	  Vector _borrowed = new Vector();

	  for(int i = 0; i < 2*POOL_SIZE/3; i++){
		_borrowed.add(pool.borrowObject());
	  }
  
          Thread[] threads = new Thread[CONCURRENT_BORROWS];
          
          // Exhaust the connection pool so that some threads block
	  for (int i = 0; i < CONCURRENT_BORROWS; i++) {
              threads[i] = new ConcurrentBorrower(POOL_SIZE/CONCURRENT_BORROWS + EXTRA_BORROWS, pool, POOL_SIZE, CONCURRENT_BORROWS);
              threads[i].start();
          }

	  for(int i = 0; i < 2*POOL_SIZE/3; i++){
		pool.returnObject(_borrowed.get(i));		
	  }
  
          // Wait for all the threads to finish
          for (int i = 0; i < CONCURRENT_BORROWS; i++) {
              threads[i].join();
          }
     }
  }
     
  class ConcurrentBorrower extends Thread {
         private Vector _borrowed = new Vector();
	 private int numObjectsToBorrow;
 	 private GenericObjectPool pool;
	 private int POOL_SIZE;
	 private int CONCURRENT_BORROWS;

         public ConcurrentBorrower(int nBorrow, GenericObjectPool pool, int poolSize, int concBorrows) {
	     numObjectsToBorrow = nBorrow;
	     this.pool = pool;
	     this.POOL_SIZE = poolSize;
	     this.CONCURRENT_BORROWS = concBorrows;
         }	
         
         public void run() {
	   try{
	     	for(int i = 0; i < numObjectsToBorrow; i++){
				_borrowed.add(pool.borrowObject());
	     	}
           
	     	int nObjectsHandled = 0;

	     	for(int i = 0; i < numObjectsToBorrow/2; i++){
				Object obj = _borrowed.get(i);
				pool.returnObject(obj);
				nObjectsHandled++;
	     	}

	     	pool.setMaxActive(2*POOL_SIZE/3);
	     
	     	for(int i = nObjectsHandled; i < numObjectsToBorrow; i++){
				Object obj = _borrowed.get(i);
				pool.invalidateObject(obj);
				nObjectsHandled++;
	     	}

	     	for(int i = 0; i < POOL_SIZE/(3*CONCURRENT_BORROWS); i++){
				pool.addObject();
	     	}
	   }
	   catch(Exception e){
		System.err.println("Error in a borrower thread");
		e.printStackTrace();
	   }
         }
   }
     
   class SimpleFactory implements PoolableObjectFactory {
 
         public Object makeObject() {
             return new PooledTestObject();
         }
         
         public boolean validateObject(Object obj) { return true; }
         
         public void activateObject(Object obj) {
             ((PooledTestObject)obj).setActive(true);
         }
         
         public void passivateObject(Object obj) {
             ((PooledTestObject)obj).setActive(false);
         }
 
         public void destroyObject(Object obj) {
             ((PooledTestObject)obj).setActive(false);
             // while destroying connections, yield control to other threads
             // helps simulate threading errors
             Thread.yield();
         }
   }
 
 
 class PooledTestObject {
     private boolean active = false;
     private int _hash = 0;
     private boolean _abandoned = false;
 
     private static int hash = 1;
     
     public PooledTestObject() {
         _hash = hash++;
     }
     
     public synchronized void setActive(boolean b) {
         active = b;
     }
 
     public synchronized boolean isActive() {
         return active;
     }
     
     public int hashCode() {
         return _hash;
     }
     
     public void setAbandoned(boolean b) {
         _abandoned = b;
     }
     
     protected long getLastUsed() {
         if (_abandoned) {
             return 1;
         } else {
             return 0;
         }
     }
     
     public boolean equals(Object obj) {
         if (!(obj instanceof PooledTestObject)) return false;
         return obj.hashCode() == hashCode();
     }
 }
 


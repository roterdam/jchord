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

import java.sql.*;

// Here are the dbcp-specific classes.
// Note that they are only used in the setupDriver
// method. In normal use, your classes interact
// only with the standard JDBC API

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;

public class DBCPHarness {
	public static void main(String[] args) {
		MainThread tt = new MainThread();
		tt.start();
	}
}

class MainThread extends Thread{
	private Connection conn;
	public MainThread() {
		super("Main");
	}
	public void run() {
		System.out.println("Setting up driver.");
		try {
			setupDriver();
		} catch (Exception e) {
			e.printStackTrace();
       		}
       		System.out.println("Done.");

       		try {
          		System.out.println("Creating connection.");
			conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:example");
			Thread t1 = new ConnThread("Conn 1", conn);
			t1.start();
			try{
				Thread.sleep(2);
			}
			catch(Exception e){

			}
			Thread t2 = new ConnThread("Conn 2", conn);
			t2.start();

		} catch(Exception e) {
           		e.printStackTrace();
		}

	}
   	public static void setupDriver() throws Exception {
    		GenericObjectPool pool = new GenericObjectPool(null);
		/*
		CHANGE 100 TO SMALL NUMBER LIKE 10 TO SEE DEADLOCK 1
		*/
		pool.setTimeBetweenEvictionRunsMillis(100);
		pool.setMinIdle(10);
		pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
		pool.setMaxWait(-1);
        	ConnectionFactory connectionFactory =
			new DriverManagerConnectionFactory("jdbc:hsqldb:database",null);
		KeyedObjectPoolFactory keyedFactory =
			new GenericKeyedObjectPoolFactory(null);
    		PoolableConnectionFactory factory =
			new PoolableConnectionFactory(connectionFactory,pool,keyedFactory,null,false,true);
        	PoolingDriver driver = new PoolingDriver();
        	driver.registerPool("example",pool);
    }
}

class ConnThread extends Thread {
	private Connection conn;
	public ConnThread(String threadName, Connection conn) {
		super(threadName);
		this.conn = conn;
	}
	public void run() {
		try {
			/* REMOVE COMMENT TO SEE DEADLOCK 2 
			while (true) {
			*/
				PreparedStatement stmt = conn.prepareStatement("");
				stmt.close();
			/* REMOVE COMMENT TO SEE DEADLOCK 2
			}
			*/
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
}

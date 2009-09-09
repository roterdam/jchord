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

package org.apache.commons.pool;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Rodney Waldhoff
 * @version $Revision: 384433 $ $Date: 2006-03-09 00:19:21 -0500 (Thu, 09 Mar 2006) $
 */
public class TestAll extends TestCase {
    public TestAll(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(org.apache.commons.pool.TestBaseObjectPool.suite());
        suite.addTest(org.apache.commons.pool.TestBaseKeyedObjectPool.suite());
        suite.addTest(org.apache.commons.pool.TestBasePoolableObjectFactory.suite());
        suite.addTest(org.apache.commons.pool.TestBaseKeyedPoolableObjectFactory.suite());
        suite.addTest(org.apache.commons.pool.TestPoolUtils.suite());
        suite.addTest(org.apache.commons.pool.impl.TestAll.suite());
        return suite;
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestAll.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
}

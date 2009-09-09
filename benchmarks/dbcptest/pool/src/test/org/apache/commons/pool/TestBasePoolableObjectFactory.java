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
 * @version $Revision: 155430 $ $Date: 2005-02-26 08:13:28 -0500 (Sat, 26 Feb 2005) $ 
 */
public class TestBasePoolableObjectFactory extends TestCase {
    public TestBasePoolableObjectFactory(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBasePoolableObjectFactory.class);
    }
    
    // tests
    public void testDefaultMethods() throws Exception {
        PoolableObjectFactory factory = new BasePoolableObjectFactory() { 
            public Object makeObject() throws Exception {
                return null;
            }
        };   
        
        factory.activateObject(null); // a no-op
        factory.passivateObject(null); // a no-op
        factory.destroyObject(null); // a no-op
        assertTrue(factory.validateObject(null)); // constant true
    }
}

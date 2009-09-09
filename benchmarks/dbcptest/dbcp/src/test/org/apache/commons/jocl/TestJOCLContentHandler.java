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

package org.apache.commons.jocl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.helpers.AttributesImpl;

/**
 * @version $Revision: 1.8 $ $Date: 2004/02/28 12:18:18 $
 */
public class TestJOCLContentHandler extends TestCase {
    public TestJOCLContentHandler(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestJOCLContentHandler.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestJOCLContentHandler.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private JOCLContentHandler jocl = null;

    public void setUp() {
        jocl = new JOCLContentHandler();
    }

    public void testPrimatives() throws Exception {
        jocl.startDocument();
        jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl",new AttributesImpl());
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","true");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","boolean","boolean",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","boolean","boolean");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","1");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","byte","byte",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","byte","byte");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","c");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","char","char",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","char","char");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","2.0");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","double","double",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","double","double");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","3.0");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","float","float",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","float","float");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","5");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","7");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","11");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","short","short",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","short","short");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","All your base are belong to us.");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","string","string",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","string","string");
        }
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl");
        jocl.endDocument();

        assertEquals(Boolean.TYPE,jocl.getType(0));
        assertEquals(Byte.TYPE,jocl.getType(1));
        assertEquals(Character.TYPE,jocl.getType(2));
        assertEquals(Double.TYPE,jocl.getType(3));
        assertEquals(Float.TYPE,jocl.getType(4));
        assertEquals(Integer.TYPE,jocl.getType(5));
        assertEquals(Long.TYPE,jocl.getType(6));
        assertEquals(Short.TYPE,jocl.getType(7));
        assertEquals(String.class,jocl.getType(8));

        assertEquals(Boolean.TRUE,jocl.getValue(0));
        assertEquals(new Byte("1"),jocl.getValue(1));
        assertEquals(new Character('c'),jocl.getValue(2));
        assertEquals(new Double("2.0"),jocl.getValue(3));
        assertEquals(new Float("3.0"),jocl.getValue(4));
        assertEquals(new Integer("5"),jocl.getValue(5));
        assertEquals(new Long("7"),jocl.getValue(6));
        assertEquals(new Short("11"),jocl.getValue(7));
        assertEquals("All your base are belong to us.",jocl.getValue(8));
    }

    public void testObject() throws Exception {
        jocl.startDocument();
        jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl",new AttributesImpl());
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","null","null","CDATA","true");
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.lang.String");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.util.Date");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.util.Date");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","345");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long");
        }
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl");
        jocl.endDocument();

        assertEquals(String.class,jocl.getType(0));
        assertEquals(java.util.Date.class,jocl.getType(1));
        assertEquals(java.util.Date.class,jocl.getType(2));

        assertTrue(null == jocl.getValue(0));
        assertTrue(null != jocl.getValue(1));
        assertEquals(new java.util.Date(345L),jocl.getValue(2));
    }
}

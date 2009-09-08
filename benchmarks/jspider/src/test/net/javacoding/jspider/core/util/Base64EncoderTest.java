package net.javacoding.jspider.core.util;

import junit.framework.TestCase;

/**
 * $Id: Base64EncoderTest.java,v 1.2 2003/02/11 17:27:04 vanrogu Exp $
 */
public class Base64EncoderTest extends TestCase {

    public Base64EncoderTest ( ) {
        super ( "Base64EncoderTest" );
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSimpleString ( ) {
        String encoded = Base64Encoder.base64Encode("teststring");
        String expected="dGVzdHN0cmluZw==";
        assertEquals("The base64 encoding of a simple teststring didn't turn out as expected", expected, encoded);
    }

    public void testLongString ( ) {
        String encoded = Base64Encoder.base64Encode("abcdefghijklmnopqrstuvwxyz0123456789й'(§и!за)-'ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        String expected="YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg56Scop+gh5+ApLSdBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWg==";
        assertEquals("The base64 encoding of a long teststring didn't turn out as expected", expected, encoded);
    }

    public void testByteArray ( ) {
        byte[] encodedBytes = Base64Encoder.base64Encode("teststring".getBytes());
        String encoded = new String(encodedBytes);
        String expected="dGVzdHN0cmluZw==";
        assertEquals("The base64 encoding of a byte array didn't turn out as expected", expected, encoded);
    }

    public void testLongByteArray ( ) {
        byte[] encodedBytes = Base64Encoder.base64Encode("abcdefghijklmnopqrstuvwxyz0123456789й'(§и!за)-'ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes());
        String encoded = new String(encodedBytes);
        String expected="YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg56Scop+gh5+ApLSdBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWg==";
        assertEquals("The base64 encoding of a long teststring didn't turn out as expected", expected, encoded);
    }

    public void testStringNull ( ) {
        String source = null;
        String encoded = Base64Encoder.base64Encode(source);
        String expected = null;
        assertEquals("The base64 encoding of a null String didn't turn out null", expected, encoded);
    }

    public void testByteArrayNull ( ) {
        byte[] source = null;
        byte[] encodedBytes = Base64Encoder.base64Encode(source);
        assertNull("The base64 encoding of a null byte array didn't turn out null", encodedBytes);
    }

}

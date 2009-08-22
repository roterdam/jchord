package org.apache.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.lucene.store.OutputStream;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store._TestHelper;


/**
 * @author dmitrys@earthlink.net
 * @version $Id: TestCompoundFile.java,v 1.5 2004/03/29 22:48:06 cutting Exp $
 */
public class TestCompoundFile extends TestCase
{
    /** Main for running test case by itself. */
    public static void main(String args[]) {
        TestRunner.run (new TestSuite(TestCompoundFile.class));
//        TestRunner.run (new TestCompoundFile("testSingleFile"));
//        TestRunner.run (new TestCompoundFile("testTwoFiles"));
//        TestRunner.run (new TestCompoundFile("testRandomFiles"));
//        TestRunner.run (new TestCompoundFile("testClonedStreamsClosing"));
//        TestRunner.run (new TestCompoundFile("testReadAfterClose"));
//        TestRunner.run (new TestCompoundFile("testRandomAccess"));
//        TestRunner.run (new TestCompoundFile("testRandomAccessClones"));
//        TestRunner.run (new TestCompoundFile("testFileNotFound"));
//        TestRunner.run (new TestCompoundFile("testReadPastEOF"));

//        TestRunner.run (new TestCompoundFile("testIWCreate"));

    }


    private Directory dir;


    public void setUp() throws IOException {
        //dir = new RAMDirectory();
        dir = FSDirectory.getDirectory(new File(System.getProperty("tempDir"), "testIndex"), true);
    }


    /** Creates a file of the specified size with random data. */
    private void createRandomFile(Directory dir, String name, int size)
    throws IOException
    {
        OutputStream os = dir.createFile(name);
        for (int i=0; i<size; i++) {
            byte b = (byte) (Math.random() * 256);
            os.writeByte(b);
        }
        os.close();
    }

    /** Creates a file of the specified size with sequential data. The first
     *  byte is written as the start byte provided. All subsequent bytes are
     *  computed as start + offset where offset is the number of the byte.
     */
    private void createSequenceFile(Directory dir,
                                    String name,
                                    byte start,
                                    int size)
    throws IOException
    {
        OutputStream os = dir.createFile(name);
        for (int i=0; i < size; i++) {
            os.writeByte(start);
            start ++;
        }
        os.close();
    }


    private void assertSameStreams(String msg,
                                   InputStream expected,
                                   InputStream test)
    throws IOException
    {
        assertNotNull(msg + " null expected", expected);
        assertNotNull(msg + " null test", test);
        assertEquals(msg + " length", expected.length(), test.length());
        assertEquals(msg + " position", expected.getFilePointer(),
                                        test.getFilePointer());

        byte expectedBuffer[] = new byte[512];
        byte testBuffer[] = new byte[expectedBuffer.length];

        long remainder = expected.length() - expected.getFilePointer();
        while(remainder > 0) {
            int readLen = (int) Math.min(remainder, expectedBuffer.length);
            expected.readBytes(expectedBuffer, 0, readLen);
            test.readBytes(testBuffer, 0, readLen);
            assertEqualArrays(msg + ", remainder " + remainder, expectedBuffer,
                testBuffer, 0, readLen);
            remainder -= readLen;
        }
    }


    private void assertSameStreams(String msg,
                                   InputStream expected,
                                   InputStream actual,
                                   long seekTo)
    throws IOException
    {
        if(seekTo >= 0 && seekTo < expected.length())
        {
            expected.seek(seekTo);
            actual.seek(seekTo);
            assertSameStreams(msg + ", seek(mid)", expected, actual);
        }
    }



    private void assertSameSeekBehavior(String msg,
                                        InputStream expected,
                                        InputStream actual)
    throws IOException
    {
        // seek to 0
        long point = 0;
        assertSameStreams(msg + ", seek(0)", expected, actual, point);

        // seek to middle
        point = expected.length() / 2l;
        assertSameStreams(msg + ", seek(mid)", expected, actual, point);

        // seek to end - 2
        point = expected.length() - 2;
        assertSameStreams(msg + ", seek(end-2)", expected, actual, point);

        // seek to end - 1
        point = expected.length() - 1;
        assertSameStreams(msg + ", seek(end-1)", expected, actual, point);

        // seek to the end
        point = expected.length();
        assertSameStreams(msg + ", seek(end)", expected, actual, point);

        // seek past end
        point = expected.length() + 1;
        assertSameStreams(msg + ", seek(end+1)", expected, actual, point);
    }


    private void assertEqualArrays(String msg,
                                   byte[] expected,
                                   byte[] test,
                                   int start,
                                   int len)
    {
        assertNotNull(msg + " null expected", expected);
        assertNotNull(msg + " null test", test);

        for (int i=start; i<len; i++) {
            assertEquals(msg + " " + i, expected[i], test[i]);
        }
    }


    // ===========================================================
    //  Tests of the basic CompoundFile functionality
    // ===========================================================


    /** This test creates compound file based on a single file.
     *  Files of different sizes are tested: 0, 1, 10, 100 bytes.
     */
    public void testSingleFile() throws IOException {
        int data[] = new int[] { 0, 1, 10, 100 };
        for (int i=0; i<data.length; i++) {
            String name = "t" + data[i];
            createSequenceFile(dir, name, (byte) 0, data[i]);
            CompoundFileWriter csw = new CompoundFileWriter(dir, name + ".cfs");
            csw.addFile(name);
            csw.close();

            CompoundFileReader csr = new CompoundFileReader(dir, name + ".cfs");
            InputStream expected = dir.openFile(name);
            InputStream actual = csr.openFile(name);
            assertSameStreams(name, expected, actual);
            assertSameSeekBehavior(name, expected, actual);
            expected.close();
            actual.close();
            csr.close();
        }
    }


    /** This test creates compound file based on two files.
     *
     */
    public void testTwoFiles() throws IOException {
        createSequenceFile(dir, "d1", (byte) 0, 15);
        createSequenceFile(dir, "d2", (byte) 0, 114);

        CompoundFileWriter csw = new CompoundFileWriter(dir, "d.csf");
        csw.addFile("d1");
        csw.addFile("d2");
        csw.close();

        CompoundFileReader csr = new CompoundFileReader(dir, "d.csf");
        InputStream expected = dir.openFile("d1");
        InputStream actual = csr.openFile("d1");
        assertSameStreams("d1", expected, actual);
        assertSameSeekBehavior("d1", expected, actual);
        expected.close();
        actual.close();

        expected = dir.openFile("d2");
        actual = csr.openFile("d2");
        assertSameStreams("d2", expected, actual);
        assertSameSeekBehavior("d2", expected, actual);
        expected.close();
        actual.close();
        csr.close();
    }

    /** This test creates a compound file based on a large number of files of
     *  various length. The file content is generated randomly. The sizes range
     *  from 0 to 1Mb. Some of the sizes are selected to test the buffering
     *  logic in the file reading code. For this the chunk variable is set to
     *  the length of the buffer used internally by the compound file logic.
     */
    public void testRandomFiles() throws IOException {
        // Setup the test segment
        String segment = "test";
        int chunk = 1024; // internal buffer size used by the stream
        createRandomFile(dir, segment + ".zero", 0);
        createRandomFile(dir, segment + ".one", 1);
        createRandomFile(dir, segment + ".ten", 10);
        createRandomFile(dir, segment + ".hundred", 100);
        createRandomFile(dir, segment + ".big1", chunk);
        createRandomFile(dir, segment + ".big2", chunk - 1);
        createRandomFile(dir, segment + ".big3", chunk + 1);
        createRandomFile(dir, segment + ".big4", 3 * chunk);
        createRandomFile(dir, segment + ".big5", 3 * chunk - 1);
        createRandomFile(dir, segment + ".big6", 3 * chunk + 1);
        createRandomFile(dir, segment + ".big7", 1000 * chunk);

        // Setup extraneous files
        createRandomFile(dir, "onetwothree", 100);
        createRandomFile(dir, segment + ".notIn", 50);
        createRandomFile(dir, segment + ".notIn2", 51);

        // Now test
        CompoundFileWriter csw = new CompoundFileWriter(dir, "test.cfs");
        final String data[] = new String[] {
            ".zero", ".one", ".ten", ".hundred", ".big1", ".big2", ".big3",
            ".big4", ".big5", ".big6", ".big7"
        };
        for (int i=0; i<data.length; i++) {
            csw.addFile(segment + data[i]);
        }
        csw.close();

        CompoundFileReader csr = new CompoundFileReader(dir, "test.cfs");
        for (int i=0; i<data.length; i++) {
            InputStream check = dir.openFile(segment + data[i]);
            InputStream test = csr.openFile(segment + data[i]);
            assertSameStreams(data[i], check, test);
            assertSameSeekBehavior(data[i], check, test);
            test.close();
            check.close();
        }
        csr.close();
    }


    /** Setup a larger compound file with a number of components, each of
     *  which is a sequential file (so that we can easily tell that we are
     *  reading in the right byte). The methods sets up 20 files - f0 to f19,
     *  the size of each file is 1000 bytes.
     */
    private void setUp_2() throws IOException {
        CompoundFileWriter cw = new CompoundFileWriter(dir, "f.comp");
        for (int i=0; i<20; i++) {
            createSequenceFile(dir, "f" + i, (byte) 0, 2000);
            cw.addFile("f" + i);
        }
        cw.close();
    }


    public void testReadAfterClose() throws IOException {
        demo_FSInputStreamBug((FSDirectory) dir, "test");
    }

    private void demo_FSInputStreamBug(FSDirectory fsdir, String file)
    throws IOException
    {
        // Setup the test file - we need more than 1024 bytes
        OutputStream os = fsdir.createFile(file);
        for(int i=0; i<2000; i++) {
            os.writeByte((byte) i);
        }
        os.close();

        InputStream in = fsdir.openFile(file);

        // This read primes the buffer in InputStream
        byte b = in.readByte();

        // Close the file
        in.close();

        // ERROR: this call should fail, but succeeds because the buffer
        // is still filled
        b = in.readByte();

        // ERROR: this call should fail, but succeeds for some reason as well
        in.seek(1099);

        try {
            // OK: this call correctly fails. We are now past the 1024 internal
            // buffer, so an actual IO is attempted, which fails
            b = in.readByte();
        } catch (IOException e) {
        }
    }


    static boolean isCSInputStream(InputStream is) {
        return is instanceof CompoundFileReader.CSInputStream;
    }

    static boolean isCSInputStreamOpen(InputStream is) throws IOException {
        if (isCSInputStream(is)) {
            CompoundFileReader.CSInputStream cis =
            (CompoundFileReader.CSInputStream) is;

            return _TestHelper.isFSInputStreamOpen(cis.base);
        } else {
            return false;
        }
    }


    public void testClonedStreamsClosing() throws IOException {
        setUp_2();
        CompoundFileReader cr = new CompoundFileReader(dir, "f.comp");

        // basic clone
        InputStream expected = dir.openFile("f11");
        assertTrue(_TestHelper.isFSInputStreamOpen(expected));

        InputStream one = cr.openFile("f11");
        assertTrue(isCSInputStreamOpen(one));

        InputStream two = (InputStream) one.clone();
        assertTrue(isCSInputStreamOpen(two));

        assertSameStreams("basic clone one", expected, one);
        expected.seek(0);
        assertSameStreams("basic clone two", expected, two);

        // Now close the first stream
        one.close();
        assertTrue("Only close when cr is closed", isCSInputStreamOpen(one));

        // The following should really fail since we couldn't expect to
        // access a file once close has been called on it (regardless of
        // buffering and/or clone magic)
        expected.seek(0);
        two.seek(0);
        assertSameStreams("basic clone two/2", expected, two);


        // Now close the compound reader
        cr.close();
        assertFalse("Now closed one", isCSInputStreamOpen(one));
        assertFalse("Now closed two", isCSInputStreamOpen(two));

        // The following may also fail since the compound stream is closed
        expected.seek(0);
        two.seek(0);
        //assertSameStreams("basic clone two/3", expected, two);


        // Now close the second clone
        two.close();
        expected.seek(0);
        two.seek(0);
        //assertSameStreams("basic clone two/4", expected, two);

        expected.close();
    }


    /** This test opens two files from a compound stream and verifies that
     *  their file positions are independent of each other.
     */
    public void testRandomAccess() throws IOException {
        setUp_2();
        CompoundFileReader cr = new CompoundFileReader(dir, "f.comp");

        // Open two files
        InputStream e1 = dir.openFile("f11");
        InputStream e2 = dir.openFile("f3");

        InputStream a1 = cr.openFile("f11");
        InputStream a2 = dir.openFile("f3");

        // Seek the first pair
        e1.seek(100);
        a1.seek(100);
        assertEquals(100, e1.getFilePointer());
        assertEquals(100, a1.getFilePointer());
        byte be1 = e1.readByte();
        byte ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now seek the second pair
        e2.seek(1027);
        a2.seek(1027);
        assertEquals(1027, e2.getFilePointer());
        assertEquals(1027, a2.getFilePointer());
        byte be2 = e2.readByte();
        byte ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Now make sure the first one didn't move
        assertEquals(101, e1.getFilePointer());
        assertEquals(101, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now more the first one again, past the buffer length
        e1.seek(1910);
        a1.seek(1910);
        assertEquals(1910, e1.getFilePointer());
        assertEquals(1910, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now make sure the second set didn't move
        assertEquals(1028, e2.getFilePointer());
        assertEquals(1028, a2.getFilePointer());
        be2 = e2.readByte();
        ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Move the second set back, again cross the buffer size
        e2.seek(17);
        a2.seek(17);
        assertEquals(17, e2.getFilePointer());
        assertEquals(17, a2.getFilePointer());
        be2 = e2.readByte();
        ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Finally, make sure the first set didn't move
        // Now make sure the first one didn't move
        assertEquals(1911, e1.getFilePointer());
        assertEquals(1911, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        e1.close();
        e2.close();
        a1.close();
        a2.close();
        cr.close();
    }

    /** This test opens two files from a compound stream and verifies that
     *  their file positions are independent of each other.
     */
    public void testRandomAccessClones() throws IOException {
        setUp_2();
        CompoundFileReader cr = new CompoundFileReader(dir, "f.comp");

        // Open two files
        InputStream e1 = cr.openFile("f11");
        InputStream e2 = cr.openFile("f3");

        InputStream a1 = (InputStream) e1.clone();
        InputStream a2 = (InputStream) e2.clone();

        // Seek the first pair
        e1.seek(100);
        a1.seek(100);
        assertEquals(100, e1.getFilePointer());
        assertEquals(100, a1.getFilePointer());
        byte be1 = e1.readByte();
        byte ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now seek the second pair
        e2.seek(1027);
        a2.seek(1027);
        assertEquals(1027, e2.getFilePointer());
        assertEquals(1027, a2.getFilePointer());
        byte be2 = e2.readByte();
        byte ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Now make sure the first one didn't move
        assertEquals(101, e1.getFilePointer());
        assertEquals(101, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now more the first one again, past the buffer length
        e1.seek(1910);
        a1.seek(1910);
        assertEquals(1910, e1.getFilePointer());
        assertEquals(1910, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        // Now make sure the second set didn't move
        assertEquals(1028, e2.getFilePointer());
        assertEquals(1028, a2.getFilePointer());
        be2 = e2.readByte();
        ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Move the second set back, again cross the buffer size
        e2.seek(17);
        a2.seek(17);
        assertEquals(17, e2.getFilePointer());
        assertEquals(17, a2.getFilePointer());
        be2 = e2.readByte();
        ba2 = a2.readByte();
        assertEquals(be2, ba2);

        // Finally, make sure the first set didn't move
        // Now make sure the first one didn't move
        assertEquals(1911, e1.getFilePointer());
        assertEquals(1911, a1.getFilePointer());
        be1 = e1.readByte();
        ba1 = a1.readByte();
        assertEquals(be1, ba1);

        e1.close();
        e2.close();
        a1.close();
        a2.close();
        cr.close();
    }


    public void testFileNotFound() throws IOException {
        setUp_2();
        CompoundFileReader cr = new CompoundFileReader(dir, "f.comp");

        // Open two files
        try {
            InputStream e1 = cr.openFile("bogus");
            fail("File not found");

        } catch (IOException e) {
            /* success */
            //System.out.println("SUCCESS: File Not Found: " + e);
        }

        cr.close();
    }


    public void testReadPastEOF() throws IOException {
        setUp_2();
        CompoundFileReader cr = new CompoundFileReader(dir, "f.comp");
        InputStream is = cr.openFile("f2");
        is.seek(is.length() - 10);
        byte b[] = new byte[100];
        is.readBytes(b, 0, 10);

        try {
            byte test = is.readByte();
            fail("Single byte read past end of file");
        } catch (IOException e) {
            /* success */
            //System.out.println("SUCCESS: single byte read past end of file: " + e);
        }

        is.seek(is.length() - 10);
        try {
            is.readBytes(b, 0, 50);
            fail("Block read past end of file");
        } catch (IOException e) {
            /* success */
            //System.out.println("SUCCESS: block read past end of file: " + e);
        }

        is.close();
        cr.close();
    }
}

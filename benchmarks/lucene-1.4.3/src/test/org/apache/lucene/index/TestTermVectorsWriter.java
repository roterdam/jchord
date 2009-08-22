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

import junit.framework.TestCase;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

public class TestTermVectorsWriter extends TestCase {

  private String[] testTerms = {"this", "is", "a", "test"};
  private String [] testFields = {"f1", "f2", "f3"};
  private int[][] positions = new int[testTerms.length][];
  private RAMDirectory dir = new RAMDirectory();
  private String seg = "testSegment";
  private FieldInfos fieldInfos = new FieldInfos();

  public TestTermVectorsWriter(String s) {
    super(s);
  }

  protected void setUp() {

    for (int i = 0; i < testFields.length; i++) {
      fieldInfos.add(testFields[i], true, true);
    }
    

    for (int i = 0; i < testTerms.length; i++) {
      positions[i] = new int[5];
      for (int j = 0; j < positions[i].length; j++) {
        positions[i][j] = i * 100;
      }
    }
  }

  protected void tearDown() {
  }

  public void test() {
    assertTrue(dir != null);
    assertTrue(positions != null);
  }
  
  /*public void testWriteNoPositions() {
    try {
      TermVectorsWriter writer = new TermVectorsWriter(dir, seg, 50);
      writer.openDocument();
      assertTrue(writer.isDocumentOpen() == true);
      writer.openField(0);
      assertTrue(writer.isFieldOpen() == true);
      for (int i = 0; i < testTerms.length; i++) {
        writer.addTerm(testTerms[i], i);
      }
      writer.closeField();
      
      writer.closeDocument();
      writer.close();
      assertTrue(writer.isDocumentOpen() == false);
      //Check to see the files were created
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVD_EXTENSION));
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVX_EXTENSION));
      //Now read it back in
      TermVectorsReader reader = new TermVectorsReader(dir, seg);
      assertTrue(reader != null);
      checkTermVector(reader, 0, 0);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }  */  

  public void testWriter() {
    try {
      TermVectorsWriter writer = new TermVectorsWriter(dir, seg, fieldInfos);
      writer.openDocument();
      assertTrue(writer.isDocumentOpen() == true);
      writeField(writer, testFields[0]);
      writer.closeDocument();
      writer.close();
      assertTrue(writer.isDocumentOpen() == false);
      //Check to see the files were created
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVD_EXTENSION));
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVX_EXTENSION));
      //Now read it back in
      TermVectorsReader reader = new TermVectorsReader(dir, seg, fieldInfos);
      assertTrue(reader != null);
      checkTermVector(reader, 0, testFields[0]);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }
  private void checkTermVector(TermVectorsReader reader, int docNum, String field) throws IOException {
    TermFreqVector vector = reader.get(docNum, field);
    assertTrue(vector != null);
    String[] terms = vector.getTerms();
    assertTrue(terms != null);
    assertTrue(terms.length == testTerms.length);
    for (int i = 0; i < terms.length; i++) {
      String term = terms[i];
      assertTrue(term.equals(testTerms[i]));
    }
  }

  /**
   * Test one document, multiple fields
   */
  public void testMultipleFields() {
    try {
      TermVectorsWriter writer = new TermVectorsWriter(dir, seg, fieldInfos);
      writeDocument(writer, testFields.length);

      writer.close();

      assertTrue(writer.isDocumentOpen() == false);
      //Check to see the files were created
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVD_EXTENSION));
      assertTrue(dir.fileExists(seg + TermVectorsWriter.TVX_EXTENSION));
      //Now read it back in
      TermVectorsReader reader = new TermVectorsReader(dir, seg, fieldInfos);
      assertTrue(reader != null);

      for (int j = 0; j < testFields.length; j++) {
        checkTermVector(reader, 0, testFields[j]);
      }
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

  private void writeDocument(TermVectorsWriter writer, int numFields) throws IOException {
    writer.openDocument();
    assertTrue(writer.isDocumentOpen() == true);

    for (int j = 0; j < numFields; j++) {
      writeField(writer, testFields[j]);
    }
    writer.closeDocument();
    assertTrue(writer.isDocumentOpen() == false);
  }

  /**
   * 
   * @param writer The writer to write to
   * @param j The field number
   * @throws IOException
   */
  private void writeField(TermVectorsWriter writer, String f) throws IOException {
    writer.openField(f);
    assertTrue(writer.isFieldOpen() == true);
    for (int i = 0; i < testTerms.length; i++) {
      writer.addTerm(testTerms[i], i);
    }
    writer.closeField();
  }


  public void testMultipleDocuments() {

    try {
      TermVectorsWriter writer = new TermVectorsWriter(dir, seg, fieldInfos);
      assertTrue(writer != null);
      for (int i = 0; i < 10; i++) {
        writeDocument(writer, testFields.length);
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }      
    //Do some arbitrary tests
    try {
      TermVectorsReader reader = new TermVectorsReader(dir, seg, fieldInfos);
      for (int i = 0; i < 10; i++) {        
        assertTrue(reader != null);
        checkTermVector(reader, 5, testFields[0]);
        checkTermVector(reader, 2, testFields[2]);
      }
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

}

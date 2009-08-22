package org.apache.lucene.index;

/**
 * Created by IntelliJ IDEA.
 * User: Grant Ingersoll
 * Date: Feb 2, 2004
 * Time: 6:16:12 PM
 * $Id: DocHelper.java,v 1.1 2004/02/20 20:14:55 cutting Exp $
 * Copyright 2004.  Center For Natural Language Processing
 */

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

/**
 *
 *
 **/
class DocHelper {
  public static final String FIELD_1_TEXT = "field one text";
  public static final String TEXT_FIELD_1_KEY = "textField1";
  public static Field textField1 = Field.Text(TEXT_FIELD_1_KEY, FIELD_1_TEXT, false);
  
  public static final String FIELD_2_TEXT = "field field field two text";
  //Fields will be lexicographically sorted.  So, the order is: field, text, two
  public static final int [] FIELD_2_FREQS = {3, 1, 1}; 
  public static final String TEXT_FIELD_2_KEY = "textField2";
  public static Field textField2 = Field.Text(TEXT_FIELD_2_KEY, FIELD_2_TEXT, true);
  
  public static final String KEYWORD_TEXT = "Keyword";
  public static final String KEYWORD_FIELD_KEY = "keyField";
  public static Field keyField = Field.Keyword(KEYWORD_FIELD_KEY, KEYWORD_TEXT);
  
  public static final String UNINDEXED_FIELD_TEXT = "unindexed field text";
  public static final String UNINDEXED_FIELD_KEY = "unIndField";
  public static Field unIndField = Field.UnIndexed(UNINDEXED_FIELD_KEY, UNINDEXED_FIELD_TEXT);
  
  public static final String UNSTORED_1_FIELD_TEXT = "unstored field text";
  public static final String UNSTORED_FIELD_1_KEY = "unStoredField1";
  public static Field unStoredField1 = Field.UnStored(UNSTORED_FIELD_1_KEY, UNSTORED_1_FIELD_TEXT, false);

  public static final String UNSTORED_2_FIELD_TEXT = "unstored field text";
  public static final String UNSTORED_FIELD_2_KEY = "unStoredField2";
  public static Field unStoredField2 = Field.UnStored(UNSTORED_FIELD_2_KEY, UNSTORED_2_FIELD_TEXT, true);

//  public static Set fieldNamesSet = null;
//  public static Set fieldValuesSet = null;
  public static Map nameValues = null;
  
  static
  {
    
    nameValues = new HashMap();
    nameValues.put(TEXT_FIELD_1_KEY, FIELD_1_TEXT);
    nameValues.put(TEXT_FIELD_2_KEY, FIELD_2_TEXT);
    nameValues.put(KEYWORD_FIELD_KEY, KEYWORD_TEXT);
    nameValues.put(UNINDEXED_FIELD_KEY, UNINDEXED_FIELD_TEXT);
    nameValues.put(UNSTORED_FIELD_1_KEY, UNSTORED_1_FIELD_TEXT);
    nameValues.put(UNSTORED_FIELD_2_KEY, UNSTORED_2_FIELD_TEXT);
  }
  
  /**
   * Adds the fields above to a document 
   * @param doc The document to write
   */ 
  public static void setupDoc(Document doc) {
    doc.add(textField1);
    doc.add(textField2);
    doc.add(keyField);
    doc.add(unIndField);
    doc.add(unStoredField1);
    doc.add(unStoredField2);
  }                         
  /**
   * Writes the document to the directory using a segment named "test"
   * @param dir
   * @param doc
   */ 
  public static void writeDoc(Directory dir, Document doc)
  {
    
    writeDoc(dir, "test", doc);
  }
  /**
   * Writes the document to the directory in the given segment
   * @param dir
   * @param segment
   * @param doc
   */ 
  public static void writeDoc(Directory dir, String segment, Document doc)
  {
    Analyzer analyzer = new WhitespaceAnalyzer();
    Similarity similarity = Similarity.getDefault();
    writeDoc(dir, analyzer, similarity, segment, doc);
  }
  /**
   * Writes the document to the directory segment named "test" using the specified analyzer and similarity
   * @param dir
   * @param analyzer
   * @param similarity
   * @param doc
   */ 
  public static void writeDoc(Directory dir, Analyzer analyzer, Similarity similarity, Document doc)
  {
    writeDoc(dir, analyzer, similarity, "test", doc);
  }
  /**
   * Writes the document to the directory segment using the analyzer and the similarity score
   * @param dir
   * @param analyzer
   * @param similarity
   * @param segment
   * @param doc
   */ 
  public static void writeDoc(Directory dir, Analyzer analyzer, Similarity similarity, String segment, Document doc)
  {
    DocumentWriter writer = new DocumentWriter(dir, analyzer, similarity, 50);
    try {
      writer.addDocument(segment, doc);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static int numFields(Document doc) {
    Enumeration fields = doc.fields();
    int result = 0;
    while (fields.hasMoreElements()) {
      fields.nextElement();
      result++;
    }
    return result;
  }
}
/*
    fieldNamesSet = new HashSet();
    fieldNamesSet.add(TEXT_FIELD_1_KEY);
    fieldNamesSet.add(TEXT_FIELD_2_KEY);
    fieldNamesSet.add(KEYWORD_FIELD_KEY);
    fieldNamesSet.add(UNINDEXED_FIELD_KEY);
    fieldNamesSet.add(UNSTORED_FIELD_1_KEY);
    fieldNamesSet.add(UNSTORED_FIELD_2_KEY);
    fieldValuesSet = new HashSet();
    fieldValuesSet.add(FIELD_1_TEXT);
    fieldValuesSet.add(FIELD_2_TEXT);
    fieldValuesSet.add(KEYWORD_TEXT);
    fieldValuesSet.add(UNINDEXED_FIELD_TEXT);
    fieldValuesSet.add(UNSTORED_1_FIELD_TEXT);
    fieldValuesSet.add(UNSTORED_2_FIELD_TEXT);
*/

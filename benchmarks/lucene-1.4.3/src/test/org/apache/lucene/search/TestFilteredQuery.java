package org.apache.lucene.search;

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
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;
import java.util.BitSet;
import java.io.IOException;


/**
 * FilteredQuery JUnit tests.
 *
 * <p>Created: Apr 21, 2004 1:21:46 PM
 *
 * @author  Tim Jones
 * @version $Id: TestFilteredQuery.java,v 1.5 2004/07/10 06:19:01 otis Exp $
 * @since   1.4
 */
public class TestFilteredQuery
extends TestCase {

  private IndexSearcher searcher;
  private RAMDirectory directory;
  private Query query;
  private Filter filter;

  public void setUp()
  throws Exception {
    directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter (directory, new WhitespaceAnalyzer(), true);

    Document doc = new Document();
    doc.add (Field.Text ("field", "one two three four five"));
    doc.add (Field.Text ("sorter", "b"));
    writer.addDocument (doc);

    doc = new Document();
    doc.add (Field.Text ("field", "one two three four"));
    doc.add (Field.Text ("sorter", "d"));
    writer.addDocument (doc);

    doc = new Document();
    doc.add (Field.Text ("field", "one two three y"));
    doc.add (Field.Text ("sorter", "a"));
    writer.addDocument (doc);

    doc = new Document();
    doc.add (Field.Text ("field", "one two x"));
    doc.add (Field.Text ("sorter", "c"));
    writer.addDocument (doc);

    writer.optimize ();
    writer.close ();

    searcher = new IndexSearcher (directory);
    query = new TermQuery (new Term ("field", "three"));
    filter = new Filter() {
      public BitSet bits (IndexReader reader) throws IOException {
        BitSet bitset = new BitSet(5);
        bitset.set (1);
        bitset.set (3);
        return bitset;
      }
    };
  }

  public void tearDown()
  throws Exception {
    searcher.close();
    directory.close();
  }

  public void testFilteredQuery()
  throws Exception {
    Query filteredquery = new FilteredQuery (query, filter);
    Hits hits = searcher.search (filteredquery);
    assertEquals (1, hits.length());
    assertEquals (1, hits.id(0));

    hits = searcher.search (filteredquery, new Sort("sorter"));
    assertEquals (1, hits.length());
    assertEquals (1, hits.id(0));

    filteredquery = new FilteredQuery (new TermQuery (new Term ("field", "one")), filter);
    hits = searcher.search (filteredquery);
    assertEquals (2, hits.length());

    filteredquery = new FilteredQuery (new TermQuery (new Term ("field", "x")), filter);
    hits = searcher.search (filteredquery);
    assertEquals (1, hits.length());
    assertEquals (3, hits.id(0));

    filteredquery = new FilteredQuery (new TermQuery (new Term ("field", "y")), filter);
    hits = searcher.search (filteredquery);
    assertEquals (0, hits.length());
  }

  /**
   * This tests FilteredQuery's rewrite correctness
   */
  public void testRangeQuery() throws Exception {
    RangeQuery rq = new RangeQuery(
        new Term("sorter", "b"), new Term("sorter", "d"), true);

    Query filteredquery = new FilteredQuery(rq, filter);
    Hits hits = searcher.search(filteredquery);
    assertEquals(2, hits.length());
  }

}


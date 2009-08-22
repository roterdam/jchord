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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * Tests {@link MultiSearcher} class.
 *
 * @version $Id: TestMultiSearcher.java,v 1.6 2004/03/02 13:09:57 otis Exp $
 */
public class TestMultiSearcher extends TestCase
{
    public TestMultiSearcher(String name)
    {
        super(name);
    }

	/**
	 * ReturnS a new instance of the concrete MultiSearcher class
	 * used in this test.
	 */
	protected MultiSearcher getMultiSearcherInstance(Searcher[] searchers) throws IOException {
		return new MultiSearcher(searchers);
	}

    public void testEmptyIndex()
        throws Exception
    {
        // creating two directories for indices
        Directory indexStoreA = new RAMDirectory();
        Directory indexStoreB = new RAMDirectory();

        // creating a document to store
        Document lDoc = new Document();
        lDoc.add(Field.Text("fulltext", "Once upon a time....."));
        lDoc.add(Field.Keyword("id", "doc1"));
        lDoc.add(Field.Keyword("handle", "1"));

        // creating a document to store
        Document lDoc2 = new Document();
        lDoc2.add(Field.Text("fulltext", "in a galaxy far far away....."));
        lDoc2.add(Field.Keyword("id", "doc2"));
        lDoc2.add(Field.Keyword("handle", "1"));

        // creating a document to store
        Document lDoc3 = new Document();
        lDoc3.add(Field.Text("fulltext", "a bizarre bug manifested itself...."));
        lDoc3.add(Field.Keyword("id", "doc3"));
        lDoc3.add(Field.Keyword("handle", "1"));

        // creating an index writer for the first index
        IndexWriter writerA = new IndexWriter(indexStoreA, new StandardAnalyzer(), true);
        // creating an index writer for the second index, but writing nothing
        IndexWriter writerB = new IndexWriter(indexStoreB, new StandardAnalyzer(), true);

        //--------------------------------------------------------------------
        // scenario 1
        //--------------------------------------------------------------------

        // writing the documents to the first index
        writerA.addDocument(lDoc);
        writerA.addDocument(lDoc2);
        writerA.addDocument(lDoc3);
        writerA.optimize();
        writerA.close();

        // closing the second index
        writerB.close();

        // creating the query
        Query query = QueryParser.parse("handle:1", "fulltext", new StandardAnalyzer());

        // building the searchables
        Searcher[] searchers = new Searcher[2];
        // VITAL STEP:adding the searcher for the empty index first, before the searcher for the populated index
        searchers[0] = new IndexSearcher(indexStoreB);
        searchers[1] = new IndexSearcher(indexStoreA);
        // creating the multiSearcher
        Searcher mSearcher = getMultiSearcherInstance(searchers);
        // performing the search
        Hits hits = mSearcher.search(query);

        assertEquals(3, hits.length());

        try {
            // iterating over the hit documents
            for (int i = 0; i < hits.length(); i++) {
                Document d = hits.doc(i);
            }
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            fail("ArrayIndexOutOfBoundsException thrown: " + e.getMessage());
            e.printStackTrace();
        } finally{
            mSearcher.close();
        }


        //--------------------------------------------------------------------
        // scenario 2
        //--------------------------------------------------------------------

        // adding one document to the empty index
        writerB = new IndexWriter(indexStoreB, new StandardAnalyzer(), false);
        writerB.addDocument(lDoc);
        writerB.optimize();
        writerB.close();

        // building the searchables
        Searcher[] searchers2 = new Searcher[2];
        // VITAL STEP:adding the searcher for the empty index first, before the searcher for the populated index
        searchers2[0] = new IndexSearcher(indexStoreB);
        searchers2[1] = new IndexSearcher(indexStoreA);
        // creating the mulitSearcher
        Searcher mSearcher2 = getMultiSearcherInstance(searchers2);
        // performing the same search
        Hits hits2 = mSearcher2.search(query);

        assertEquals(4, hits2.length());

        try {
            // iterating over the hit documents
            for (int i = 0; i < hits2.length(); i++) {
                // no exception should happen at this point
                Document d = hits2.doc(i);
            }
        }
        catch (Exception e)
        {
            fail("Exception thrown: " + e.getMessage());
            e.printStackTrace();
        } finally{
            mSearcher2.close();
        }

        //--------------------------------------------------------------------
        // scenario 3
        //--------------------------------------------------------------------

        // deleting the document just added, this will cause a different exception to take place
        Term term = new Term("id", "doc1");
        IndexReader readerB = IndexReader.open(indexStoreB);
        readerB.delete(term);
        readerB.close();

        // optimizing the index with the writer
        writerB = new IndexWriter(indexStoreB, new StandardAnalyzer(), false);
        writerB.optimize();
        writerB.close();

        // building the searchables
        Searcher[] searchers3 = new Searcher[2];

        searchers3[0] = new IndexSearcher(indexStoreB);
        searchers3[1] = new IndexSearcher(indexStoreA);
        // creating the mulitSearcher
        Searcher mSearcher3 = getMultiSearcherInstance(searchers3);
        // performing the same search
        Hits hits3 = mSearcher3.search(query);

        assertEquals(3, hits3.length());

        try {
            // iterating over the hit documents
            for (int i = 0; i < hits3.length(); i++) {
                Document d = hits3.doc(i);
            }
        }
        catch (IOException e)
        {
            fail("IOException thrown: " + e.getMessage());
            e.printStackTrace();
        } finally{
            mSearcher3.close();
        }
    }
}

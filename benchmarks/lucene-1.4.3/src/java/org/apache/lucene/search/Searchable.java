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

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;       // for javadoc

/** The interface for search implementations.
 *
 * <p>Implementations provide search over a single index, over multiple
 * indices, and over indices on remote servers.
 */
public interface Searchable extends java.rmi.Remote {
  /** Lower-level search API.
   *
   * <p>{@link HitCollector#collect(int,float)} is called for every non-zero
   * scoring document.
   *
   * <p>Applications should only use this if they need <i>all</i> of the
   * matching documents.  The high-level search API ({@link
   * Searcher#search(Query)}) is usually more efficient, as it skips
   * non-high-scoring hits.
   *
   * @param query to match documents
   * @param filter if non-null, a bitset used to eliminate some documents
   * @param results to receive hits
   */
  void search(Query query, Filter filter, HitCollector results)
    throws IOException;

  /** Frees resources associated with this Searcher.
   * Be careful not to call this method while you are still using objects
   * like {@link Hits}.
   */
  void close() throws IOException;

  /** Expert: Returns the number of documents containing <code>term</code>.
   * Called by search code to compute term weights.
   * @see IndexReader#docFreq(Term).
   */
  int docFreq(Term term) throws IOException;

  /** Expert: Returns one greater than the largest possible document number.
   * Called by search code to compute term weights.
   * @see IndexReader#maxDoc().
   */
  int maxDoc() throws IOException;

  /** Expert: Low-level search implementation.  Finds the top <code>n</code>
   * hits for <code>query</code>, applying <code>filter</code> if non-null.
   *
   * <p>Called by {@link Hits}.
   *
   * <p>Applications should usually call {@link Searcher#search(Query)} or
   * {@link Searcher#search(Query,Filter)} instead.
   */
  TopDocs search(Query query, Filter filter, int n) throws IOException;

  /** Expert: Returns the stored fields of document <code>i</code>.
   * Called by {@link HitCollector} implementations.
   * @see IndexReader#document(int).
   */
  Document doc(int i) throws IOException;

  /** Expert: called to re-write queries into primitive queries. */
  Query rewrite(Query query) throws IOException;

  /** Returns an Explanation that describes how <code>doc</code> scored against
   * <code>query</code>.
   *
   * <p>This is intended to be used in developing Similarity implementations,
   * and, for good performance, should not be displayed with every hit.
   * Computing an explanation is as expensive as executing the query over the
   * entire index.
   */
  Explanation explain(Query query, int doc) throws IOException;

  /** Expert: Low-level search implementation with arbitrary sorting.  Finds
   * the top <code>n</code> hits for <code>query</code>, applying
   * <code>filter</code> if non-null, and sorting the hits by the criteria in
   * <code>sort</code>.
   *
   * <p>Applications should usually call {@link
   * Searcher#search(Query,Filter,Sort)} instead.
   */
  TopFieldDocs search(Query query, Filter filter, int n, Sort sort)
    throws IOException;
}

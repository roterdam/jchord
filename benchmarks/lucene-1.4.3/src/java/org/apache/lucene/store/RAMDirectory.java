package org.apache.lucene.store;

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
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.InputStream;
import org.apache.lucene.store.OutputStream;

/**
 * A memory-resident {@link Directory} implementation.
 *
 * @version $Id: RAMDirectory.java,v 1.15 2004/05/09 12:41:47 ehatcher Exp $
 */
public final class RAMDirectory extends Directory {
  Hashtable files = new Hashtable();

  /** Constructs an empty {@link Directory}. */
  public RAMDirectory() {
  }

  /**
   * Creates a new <code>RAMDirectory</code> instance from a different
   * <code>Directory</code> implementation.  This can be used to load
   * a disk-based index into memory.
   * <P>
   * This should be used only with indices that can fit into memory.
   *
   * @param dir a <code>Directory</code> value
   * @exception IOException if an error occurs
   */
  public RAMDirectory(Directory dir) throws IOException {
    this(dir, false);
  }
  
  private RAMDirectory(Directory dir, boolean closeDir) throws IOException {
    final String[] files = dir.list();
    for (int i = 0; i < files.length; i++) {
      // make place on ram disk
      OutputStream os = createFile(files[i]);
      // read current file
      InputStream is = dir.openFile(files[i]);
      // and copy to ram disk
      int len = (int) is.length();
      byte[] buf = new byte[len];
      is.readBytes(buf, 0, len);
      os.writeBytes(buf, len);
      // graceful cleanup
      is.close();
      os.close();
    }
    if(closeDir)
      dir.close();
  }

  /**
   * Creates a new <code>RAMDirectory</code> instance from the {@link FSDirectory}.
   *
   * @param dir a <code>File</code> specifying the index directory
   */
  public RAMDirectory(File dir) throws IOException {
    this(FSDirectory.getDirectory(dir, false), true);
  }

  /**
   * Creates a new <code>RAMDirectory</code> instance from the {@link FSDirectory}.
   *
   * @param dir a <code>String</code> specifying the full index directory path
   */
  public RAMDirectory(String dir) throws IOException {
    this(FSDirectory.getDirectory(dir, false), true);
  }

  /** Returns an array of strings, one for each file in the directory. */
  public final String[] list() {
    String[] result = new String[files.size()];
    int i = 0;
    Enumeration names = files.keys();
    while (names.hasMoreElements())
      result[i++] = (String)names.nextElement();
    return result;
  }

  /** Returns true iff the named file exists in this directory. */
  public final boolean fileExists(String name) {
    RAMFile file = (RAMFile)files.get(name);
    return file != null;
  }

  /** Returns the time the named file was last modified. */
  public final long fileModified(String name) throws IOException {
    RAMFile file = (RAMFile)files.get(name);
    return file.lastModified;
  }

  /** Set the modified time of an existing file to now. */
  public void touchFile(String name) throws IOException {
//     final boolean MONITOR = false;
    
    RAMFile file = (RAMFile)files.get(name);
    long ts2, ts1 = System.currentTimeMillis();
    do {
      try {
        Thread.sleep(0, 1);
      } catch (InterruptedException e) {}
      ts2 = System.currentTimeMillis();
//       if (MONITOR) {
//         count++;
//       }
    } while(ts1 == ts2);

    file.lastModified = ts2;

//     if (MONITOR)
//         System.out.println("SLEEP COUNT: " + count);
  }

  /** Returns the length in bytes of a file in the directory. */
  public final long fileLength(String name) {
    RAMFile file = (RAMFile)files.get(name);
    return file.length;
  }

  /** Removes an existing file in the directory. */
  public final void deleteFile(String name) {
    files.remove(name);
  }

  /** Removes an existing file in the directory. */
  public final void renameFile(String from, String to) {
    RAMFile file = (RAMFile)files.get(from);
    files.remove(from);
    files.put(to, file);
  }

  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
  public final OutputStream createFile(String name) {
    RAMFile file = new RAMFile();
    files.put(name, file);
    return new RAMOutputStream(file);
  }

  /** Returns a stream reading an existing file. */
  public final InputStream openFile(String name) {
    RAMFile file = (RAMFile)files.get(name);
    return new RAMInputStream(file);
  }

  /** Construct a {@link Lock}.
   * @param name the name of the lock file
   */
  public final Lock makeLock(final String name) {
    return new Lock() {
      public boolean obtain() throws IOException {
        synchronized (files) {
          if (!fileExists(name)) {
            createFile(name).close();
            return true;
          }
          return false;
        }
      }
      public void release() {
        deleteFile(name);
      }
      public boolean isLocked() {
        return fileExists(name);
      }
    };
  }

  /** Closes the store to future operations. */
  public final void close() {
  }
}

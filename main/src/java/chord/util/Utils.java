/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chord.project.Config;

/**
 * General utilities.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public class Utils {
  public static <S, T> void add(Map<S, List<T>> map, S key1, T key2) {
    List<T> s = map.get(key1);
    if(s == null) map.put(key1, s = new ArrayList<T>());
    s.add(key2);
  }

  public static PrintWriter openOut(String path) {
    try {
      return new PrintWriter(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static PrintWriter openOutAppend(String path) {
    try {
      return new PrintWriter(new FileOutputStream(path, true));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> String join(List<T> objs, String delim) {
    if(objs == null) return "";
    return join(objs, delim, 0, objs.size());
  }
  public static <T> String join(List<T> objs, String delim, int start, int end) {
    if(objs == null) return "";
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for(int i = start; i < end; i++) {
      if(!first) sb.append(delim);
      sb.append(objs.get(i));
      first = false;
    }
    return sb.toString();
  }
}

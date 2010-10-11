package edu.berkeley.confspell;

import java.io.FileInputStream;
import java.io.File;
import java.util.*;

/**
 * Model for a set of options with associated auxiliary data.
 * In particular, stores a set of key value pairs as well
 * as a list of options that are used indirectly by substitution.
 *
 */
public class OptionSet { //extends TreeMap<String,String>

  private static final long serialVersionUID = 1L;
  HashSet<String> usedBySubst = new HashSet<String>();
  Map<String,String> conf = new TreeMap<String,String>();
  
  public OptionSet() {}
  
  public OptionSet(Properties p) {
    addAll(p, "");
  }
  
  public OptionSet(Properties p, String prefix) {
    addAll(p, prefix);
  }


  public void addAll(Properties p) {  
    addAll(p, "");
  }

  /**
   * Add entries from properties bundle starting with prefix 
   * @param p
   */
  public void addAll(Properties p, String prefix) {  
    for(Map.Entry<Object , Object> e: p.entrySet()) {
      String key = e.getKey().toString();
      if(key.startsWith(prefix))
        conf.put(key, e.getValue().toString());
    }
  }

  /**
   * True if k is a value used indirectly, by substitution, in this properties bundle
   * @param k
   * @return
   */
  public boolean usedBySubst(String k) {
    return usedBySubst.contains(k);
  }
  

  public Set<Map.Entry<String, String>> entrySet() {
    return conf.entrySet();
  }

  public void put(String key, String string) {
    conf.put(key, string);
  }
  
  public void addSubstUse(String s) {
    usedBySubst.add(s);
  }

  public boolean contains(String s) {
    return conf.containsKey(s);
  }

  public static OptionSet fromPropsFile(String propsfilename) throws java.io.IOException {
     return fromPropsFile(new File(propsfilename));
  }
  
  public static OptionSet fromPropsFile(File propsfilename) throws java.io.IOException {

    OptionSet result = new OptionSet();
    Properties p = new Properties();
    FileInputStream fis = new FileInputStream(propsfilename);
    p.load(fis);
    fis.close();
    
    for(Map.Entry e:  p.entrySet()) {
      String v = e.getValue().toString();
//      if(v.contains("#"))
      // v = v.substring(0, v.indexOf("#"))
      result.put(e.getKey().toString(), v);
    }
    return result;
  }
  
  

}

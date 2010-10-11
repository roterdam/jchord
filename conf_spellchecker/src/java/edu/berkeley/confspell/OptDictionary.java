package edu.berkeley.confspell;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

public class OptDictionary {
  
  public OptDictionary() {}
  
  public OptDictionary(File f) throws IOException {
    read(f);
  }
  
  //use a tree map to get alphabetization for free
  TreeMap<String,String> dict = new TreeMap<String,String>();
  ArrayList<Pattern> regexOpts = new ArrayList<Pattern>();
  HashMap<String,String> annotations = new HashMap<String,String>();
  
  public void update(String opt, String ty) {
    String oldT = dict.get(opt);
    if(oldT == null)
      dict.put(opt, ty);
    else {
      if(ty == null || oldT.contains(ty))
        return;
      else
        dict.put(opt, oldT + " or " + ty);
    }
  }
  
  public void annotate(String optName, String annotation) {
    if(!annotation.contains("\n")) //line breaks here can break file format
      annotations.put(optName, annotation);
    
  }

  public void dump(PrintWriter writer) {
    for(Map.Entry<String, String> e: dict.entrySet()) {
      String k = e.getKey();
      String v = e.getValue();
      if(v == null)
         v= "";
      String annot = annotations.get(k);
      if(annot != null) 
        writer.println(k + "\t" + v + "\t" + annot);
      else
        writer.println(k + "\t" + v);
    }
  }
  

  public void read(File dictionary) throws IOException {
    
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dictionary)));
    String s = null;
    while( (s = br.readLine()) != null ) {
      String[] parts = s.split("\t");
      String opt = pruneName(parts[0]);

      if(opt.contains(".*")) {
        regexOpts.add(Pattern.compile(opt));
      }

      if(parts.length == 1 || parts[1].length() < 1)
        dict.put(opt, null);
      else 
        dict.put(opt, parts[1]);

      if(parts.length > 2) {
        annotations.put(opt, parts[2]);       
      } 
    }
    br.close();
  }
  
  
  private String pruneName(String s) {
    if(s.startsWith("CONF-") || s.startsWith("PROP-"))
      return s.substring(5);
    if(s.startsWith("$"))
      return s.substring(1);
    else if(s.startsWith("CXCONF-"))
      return s.substring(7);
    else return s;
  }

  public void show() {
    for(Map.Entry<String, String> ent: dict.entrySet()) {
      String type = ent.getValue();
      String opt = ent.getKey();
      if(type != null)
        System.out.println("option " + opt + " has type " + type);
      else
        System.out.println("option " + opt + " has unknown type");
    }
  }
  
  public boolean contains(String s) {
    if(dict.containsKey(s))
      return true;
    else {
      for(Pattern regex: regexOpts) {
        if(regex.matcher(s).matches())
          return true;
      }
      return false;
    }
  }

  private String lookupPat(String k) {
    if(dict.containsKey(k))
      return k;
    
    for(Pattern regex: regexOpts) {
      if(regex.matcher(k).matches()) {
        return regex.pattern();
      }
    }
    return null;
  }

  public String get(String k) {
    String s = dict.get(k);
    if(s == null) {
      String pat = lookupPat(k);
      if(pat == null) {
        System.err.println("DICT: lookup for absent entry " + k);
        return null;
      }
      return dict.get(pat);
    }
    return s;
  }

  public String getFullname(String k) {
    String pat = lookupPat(k);
    if(pat == null)
      return null;
    else {
      String annot = annotations.get(pat);
      if(annot == null)
        return dict.get(pat);
      else 
        return dict.get(pat) + " " + annot;
    }
  }

  public Set<String> names() {
    return dict.keySet();
  }

}

package edu.berkeley.confspell;

import java.io.*;
import java.util.*;
import edu.berkeley.confspell.SpellcheckConf.Slurper;

class PSlurper implements Slurper {
  public void slurp(File f, OptionSet res) throws IOException {
    Properties p = new Properties();
    FileInputStream fis = new FileInputStream(f);
    p.load(fis);
    fis.close();
    
    for(Map.Entry e:  p.entrySet()) {
      String v = e.getValue().toString();
//      if(v.contains("#"))
      // v = v.substring(0, v.indexOf("#"))
      res.put(e.getKey().toString(), v);
    }
    //"PROP-" + 
  }
  
}
package edu.berkeley.confspell;

import org.apache.hadoop.conf.*;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import edu.berkeley.confspell.SpellcheckConf.Slurper;

public class HSlurper implements Slurper {

  //stolen from hadoop source
  private static Pattern varPat = Pattern.compile("\\$\\{[^\\}\\$\u0020]+\\}");

  
  public void slurp(File f, OptionSet res) {
    Configuration c = new Configuration();
    c.addResource(f.getAbsolutePath());
    fromHConf(res, c);
  }


  public static void fromHConf(OptionSet res, Configuration c) {
    for(Map.Entry<String, String> e: c) {
      
      String rawV = e.getValue();
      String cookedV =  c.get(e.getKey());
      
      res.put(e.getKey(), cookedV); //to force substitution
      
      Matcher m = varPat.matcher(rawV);
      if(m.find()) {
        String var = m.group();
        var = var.substring(2, var.length()-1); // remove ${ .. }
        res.addSubstUse(var);
      }
    }
  }
  
  public static OptionSet fromHConf(Configuration c) {
    OptionSet res = new OptionSet();
    fromHConf(res, c);
    return res;
  }
 
}

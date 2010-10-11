package edu.berkeley.confspell;

import java.io.*;
import java.util.*;

public class SpellcheckConf {
  
  interface Slurper {
    void slurp(File f, OptionSet res) throws IOException ;
  }
  
  static HSlurper ReadHadoop = new HSlurper();
  static PSlurper ReadProp = new PSlurper();

  
  /**
   * @param args
   */
  public static void main(String[] args) {
    if(args.length < 1)
      usageAndExit("need args");
    
    File confDict = new File(args[0]);
    if(!confDict.exists())
      usageAndExit("no conf dict " + args[0] );
    try { 
      
      OptDictionary dictionary = new OptDictionary();
      dictionary.read(confDict);
      
      if(args.length == 1)
        dictionary.show();
      else {
        OptionSet conf = new OptionSet();
        
        Slurper s =  ReadProp;
        for(int i = 1; i < args.length; ++i) {
          if(args[i].equals("-prop"))
            s =  ReadProp;
          else if(args[i].equals("-hadoop"))
            s =  ReadHadoop;
          else {
            File optsFile = new File(args[i]);
            s.slurp(optsFile, conf);
          }
        }

        Checker.checkConf(dictionary, conf);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void usageAndExit(String error) {
    System.out.println("usage: SpellcheckConf dictionary [conf files list]");
    System.out.println("files list should be a list of files, interspersed with optional " +
    		"type tags; valid ones include -hadoop and -prop.  Prop is default");
    System.out.println("err: " + error);
    System.exit(0);
  }

}

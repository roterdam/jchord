/*
 * Copyright Ari Rabkin (asrabkin@gmail.com)
 * See the COPYING file for copyright license information. 
 */
package edu.berkeley.confspell;

import java.io.*;

/**
 * This is the user-invoked main class for using the configuration spellchecker
 * in standalone mode.Its main job is to read files and parse command line options.
 *
 */
public class SpellcheckConf {
  
  /**
   * A Slurper is responsible for reading configuration options out of a file.
   * Subclasses have parsers for various file formats.
   * 
   */
  interface Slurper {
    /**
     * Read a file, load contents into an existing option set
     * @param f the file to read
     * @param res the set to hold the read values
     * @throws IOException
     */
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

package edu.berkeley.confspell;

import java.io.File;
import java.net.*;
import java.util.*;

public class Checker {
  
  public Checker(OptDictionary dictionary) {
    dict = dictionary;
  }
  

  public static class Res {
    String msg;
    public String msg() {
      return msg;
    }
    
    public Res(String s) {
      msg = s;
    } 
  }
  
  /**
   * A TCheck is a rule for checking values of a particular type.
   *
   */
  interface TCheck {
    /**
     * Check that value val is an appropriate value of this type.
     * Annot is the annotation entry for the appropriate dictionary entry, e.g., the 
     * list of values for an Enum or the parent class type for a ClassName option.
     * 
     * This method should return Checker.OK on success.
     * An exception or other return value indicates failure.
     * 
     */
    Res check(String val, String annot) throws Exception;
  }

 //determines if a file is writeable
  public static final long MIN_FREE_SPACE = 1000 * 1000 * 1000;
  public static final int SOCK_TIMEOUT = 1000; //ms
  public static final int TRIAL_PORT = 80;
  
  static Res OK = new Res("OK");
//  static Res NoTypeKnown = new Res("No type known for");
  static Res NoCheckerFor = new Res("No checker for type");
  
  public boolean PRINT_DIST = false;
  public boolean PRINT_OKS = true;

  OptDictionary dict;
  
  class Guess implements Comparable<Guess>{
    double simMetric;
    String val;
    public Guess(double d, String s) {
      simMetric = d; 
      val = s;
    }
    @Override
    public int compareTo(Guess o) {
      if( simMetric < o.simMetric)
        return -1;
      else if (simMetric == o.simMetric)
        return 0;
      else 
        return 1;
    }
  }
  
  /**
   * Returns candidate approximate matches.
   * @param optName the option to match approximately
   * @param conf the set of already-set options
   * @param count the number of matches to return
   * @return A sorted list of candidates
   */
  List<String> nearestMatches(String optName, OptionSet conf, int count) {
    ArrayList<String> l = new ArrayList<String>();
    
    if(dict.contains(optName)) {
      l.add(optName);
      return l;
    }
    PriorityQueue<Guess> guesses = new PriorityQueue<Guess>();
    
    for(String s: dict.names()) {
      if(!conf.contains(s)) {
        double m = CEditDistance.getLevenshteinDistance(s, optName);
        guesses.add(new Guess(m, s));
      }
    }
    
    for(int i =0; i < count; ++i) {
      Guess g = guesses.poll();
      if(g == null)
        break;
      l.add(g.val + (PRINT_DIST? " (" + g.simMetric+")":""));
    }
    
    return l;
  }
  
  
  
  HashMap<String, TCheck> checkers = new HashMap<String, TCheck>();  
  {
    checkers.put("Address", new CheckAddr());

    checkers.put("Boolean", new TCheck() {
      public Res check(String v, String annot) {
        if(v.equals("true") || v.equals("false"))
          return OK;
        else
          return new Res("expected true or false");
      }
    });
    
    checkers.put("ClassName", new CheckClass());
    checkers.put("File", new CheckFile());
    checkers.put("Fraction", new CheckFract());
    checkers.put("Integral", new CheckLong());
    checkers.put("NetworkInterface", new CheckIface());
    checkers.put("Portno", new CheckPortno());
    checkers.put("Special", new CheckSpecial());
    checkers.put("Time", new CheckLong());
    checkers.put("URI", new CheckURI());
    checkers.put("URL", new CheckURI());
  }

  /**
   * Add an additional checker rule, to cope with application-specific rules
   * @param category
   * @param checker
   */
  public void addRule(String category, TCheck checker) {
    checkers.put(category, checker);
  }

  public Res check(String k, String v) {
    String ty = dict.get(k);
    
    boolean list = false;
    if(ty != null && ty.endsWith(" list")) {
      ty = ty.substring(0, ty.length() - 5);
      list = true;
    }
    
    TCheck checker = checkers.get(ty);
    if(checker == null)
      return NoCheckerFor; //can't check
    else 
      try {

        String [] parts;
        if(list) 
          parts = v.split(",");
        else
          parts = new String[] {v};
        
        for(String s: parts) {
          Res r=  checker.check(s, dict.annotations.get(k));
          if(r != OK)
            return r;
        }
        return OK;
      } catch(Exception e) {
        return new Res(e.getMessage());
      }
  }

  static class CheckIface implements TCheck {
    public Res check(String v, String a) throws Exception {
      if(NetworkInterface.getByName(v) != null)
        return OK;
      else
        return new Res("no network interface");
    }
  }

  static class CheckLong implements TCheck {
    public Res check(String v, String a) throws Exception {
      Long.parseLong(v);
      return OK;
    }
  }
  
  static class CheckFract implements TCheck {
    public Res check(String v, String a)  throws Exception {
      Double.parseDouble(v);
      return OK;
    }
  }
  
  static class CheckClass implements TCheck {
    public Res check(String val, String annot) throws Exception {
      Class<?> cl = Class.forName(val);
      if(cl != null) {
        if(annot != null) {
          String[] parents = annot.split(" ");
          for(String parent: parents) {
            Class<?> cl2 = Class.forName(parent);
            if(cl2.isAssignableFrom(cl))
              return OK;
          }
         // else
          return new Res("can't cast " + cl + " to any of " + annot);
        }
        return OK;
      } else
        return new Res("null return from forName");
    }
  }
  
  static class CheckSpecial implements TCheck {
    public Res check(String v, String annot) {
      if(annot == null || annot.length() < 1)
        return new Res("no set");

      annot = annot.substring(1, annot.length() -1);
      String[] possibleVals = annot.split(",");
      for(String p: possibleVals)
        if(p.equals(v))
          return OK;
      return new Res("not in set {" + annot + "}");
    }
  }

  
  static class CheckPortno implements TCheck {
    public Res check(String val, String annot) {
      int portno = Integer.parseInt(val);
      if(portno < 1<<16) //2^16 = 65536, the highest legal portno
        return OK;
      else
        return new Res("portno should be between 0 and " + (1 << 16));
    }
  }

  static class CheckAddr implements TCheck {
    public Res check(String hostname, String a) throws Exception{
      InetAddress address = InetAddress.getByName(hostname);
      if(address.isReachable(SOCK_TIMEOUT))
        return OK;
      else {
        Socket testS = new Socket();
        testS.connect(new InetSocketAddress(address, TRIAL_PORT), SOCK_TIMEOUT);
        testS.close();
        return OK;
//        return new Res("not a reachable host");
      }
    }
  }
  
  static class CheckURI implements TCheck {
    public Res check(String v, String a) throws Exception {
      URI u = new URI(v);
      return OK;
    }
  }
  
  
  
  static class CheckFile implements TCheck {
    
    private boolean isReadableFile(String s) {
      File f = new File(s);
      return f.exists();
    }
    
    private boolean isWriteableFile(String s) {
      File f = new File(s);
      File parentDir = f.getParentFile();
      if(parentDir == null)
        return false;
      else
        return parentDir.canWrite() && parentDir.getFreeSpace() > MIN_FREE_SPACE;
    }
    
    public Res check(String v, String a) throws Exception {
      if(isWriteableFile(v) || isReadableFile(v)) 
        return OK;
      else return new Res("file neither readable nor writable");
    }  
  }
  

  public static void checkConf(OptDictionary dictionary, OptionSet conf) {
    Checker checker = new Checker(dictionary);
    checker.checkConf(conf);
  }

  public void checkConf(OptionSet conf) {
    
    ArrayList<String> noCheckerNames = new ArrayList<String>();
    TreeSet<String> noCheckerTypes = new TreeSet<String>();
    //= new TreeMap<String,String>();

    for(Map.Entry<String, String> opt: conf.entrySet()) {
      String k = opt.getKey();
      String v = opt.getValue();
      if(!dict.contains(k)) {
        if(conf.usedBySubst(k)) {
          System.out.println("OK " + k + " used by substitution");
          continue;
        } else {
          System.out.println("WARN: option " + k + " may not exist; val = " + v);
          printGuesses(this, conf, k);
        }
      } else {
        Checker.Res res = check(k, v);
        if(res == Checker.OK) {
          if(PRINT_OKS)
            System.out.println(res.msg() +" " + k+ " ["+ dict.getFullname(k) +"] = " + v);
        } else if(res == Checker.NoCheckerFor) {
          noCheckerNames.add(k);
          String t = dict.get(k);
          if(t != null)
            noCheckerTypes.add(t);
        }
        else 
          System.out.println("WARN " + k + " = " + v + " -- " + res.msg());
      }
    }    
    System.out.print("No checker rules for:\t");
    for(String opt: noCheckerNames) {
      System.out.print(opt + " ");
    }
    if(noCheckerTypes.size() > 0) {
      System.out.println("\nUn-checkable types:\t");
      for(String t: noCheckerTypes) {
        System.out.print(t + " ");
      }
    }
    System.out.println();
  }


  private static void printGuesses(Checker checker, OptionSet conf, String k) {
    List<String> guesses = checker.nearestMatches(k, conf, 3);
    System.out.println("Guesses: ");
    for(String s: guesses) {
      System.out.println("\t" + s + " " + checker.dict.getFullname(s));
    }
    
  }


  
}

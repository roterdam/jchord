/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.snapshot;

import java.io.File;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chord.project.Properties;

/**
 * Allow for organized execution of experiments.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public class Execution {
  public String path(String name) { return name == null ? basePath : basePath+"/"+name; }

  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    }
    catch(UnknownHostException e) {
      return "(unknown)";
    }
  }

  public Execution(String name) {
    this.name = name;
    basePath = Properties.outDirName;
    System.out.println("Execution directory: "+basePath);
    logOut = new PrintWriter(System.out);
    output.put("hostname", getHostName());
    output.put("exec.status", "running");

    addSaveFiles("log.txt", "options.map", "output.map", "addToView");
    
    String view = System.getProperty("chord."+name+".addToView", null);
    if (view != null) {
      PrintWriter out = Utils.openOut(path("addToView"));
      out.println(view);
      out.close();
    }
    watch.start();
  }

  public void logs(String format, Object... args) {
    logOut.println(String.format(format, args));
    logOut.flush();
  }

  public void errors(String format, Object... args) {
    numErrors++;
    logOut.print("ERROR: ");
    logOut.println(String.format(format, args));
    logOut.flush();
  }

  public void writeMap(String name, HashMap<Object,Object> map) {
    PrintWriter out = Utils.openOut(path(name));
    for (Object key : map.keySet()) {
      out.println(key+"\t"+map.get(key));
    }
    out.close();
  }

  public static boolean system(String[] cmd) {
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      p.getOutputStream().close();
      p.getInputStream().close();
      p.getErrorStream().close();
      return p.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }

  Random random = new Random();
  public String symlinkPath = null; // Link from symlinkPath to the final exec path
  public void finish(Throwable t) {
    if (t == null)
      output.put("exec.status", "done");
    else {
      output.put("exec.status", "failed");
      errors("%s", t);
      for (StackTraceElement e : t.getStackTrace())
        logs("  %s", e);
    }

    watch.stop();
    output.put("exec.time", watch);
    output.put("exec.errors", numErrors);
    writeMap("output.map", output);

    String finalPoolPath = System.getProperty("chord."+name+".finalPoolPath");
    if (finalPoolPath != null) {
      String path;
      for (int i = random.nextInt(1000); new File(path = finalPoolPath+"/"+i+".exec").exists(); i++);
      if (!new File(path).mkdir()) throw new RuntimeException("Tried to created directory "+path+" but it already exists");
      for (String file : saveFiles)
        system(new String[] { "cp", basePath+"/"+file, path });

      if (symlinkPath != null) system(new String[] { "ln", "-s", path, symlinkPath });
    }
    else {
      if (symlinkPath != null) system(new String[] { "ln", "-s", basePath, symlinkPath });
    }

    if (t != null) System.exit(1); // Die violently
  }

  public String getStringArg(String key, String defaultValue) {
    return System.getProperty("chord."+name+"."+key, defaultValue);
  }
  public boolean getBooleanArg(String key, boolean defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : s.equals("true");
  }
  public int getIntArg(String key, int defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Integer.parseInt(s);
  }
  public double getDoubleArg(String key, double defaultValue) {
    String s = getStringArg(key, null);
    return s == null ? defaultValue : Double.parseDouble(s);
  }

  public void addSaveFiles(String... files) {
    for (String file : files) saveFiles.add(file);
  }

  public HashMap<Object,Object> output = new LinkedHashMap<Object,Object>(); // For statistics, which get dumped
  private int numErrors = 0;
  private StopWatch watch = new StopWatch();
  private List<String> saveFiles = new ArrayList<String>();
  private final String name;
  private String basePath;
	private PrintWriter logOut;

  public static Execution v(String name) { return singleton != null ? singleton : (singleton = new Execution(name)); }
  private static Execution singleton;
}

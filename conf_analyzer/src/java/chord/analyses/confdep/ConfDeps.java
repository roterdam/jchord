/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.confdep;

import java.io.PrintWriter;
import java.util.*;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.project.Chord;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.integer.*;
import chord.util.tuple.object.*;
import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.primtrack.DomUV;
import chord.analyses.string.DomStrConst;
import chord.bddbddb.Rel.*;
import chord.analyses.invk.DomI;
import chord.analyses.var.DomV;
import chord.analyses.field.DomF;
import chord.analyses.alloc.DomH;

@Chord(
	name = "ConfDeps"
)
public class ConfDeps extends JavaAnalysis {
 
  public boolean STATIC = false;
  public boolean DYNTRACK = false;
  public boolean SUPERCONTEXT = false;
  public static final String CONFDEP_SCANLOGS_OPT = "confdep.scanlogs";
  public static final String CONFDEP_DYNAMIC_OPT = "confdep.dynamic"; //should be either static, dynamic-load or dynamic-track
  DomH domH;
  DomI domI;
  DomV domV;
  DomUV domUV;
  DomF domF;
  DomStrConst domConst;
  public boolean lookAtLogs = false;
  
	public void run() {
	  ClassicProject Project = ClassicProject.g();
	  
	  lookAtLogs = Config.buildBoolProperty(CONFDEP_SCANLOGS_OPT, true);
	  String dynamism = System.getProperty(CONFDEP_DYNAMIC_OPT, "static");
	  if(dynamism.equals("static")) {
	    STATIC = true;
	    DYNTRACK = false;
	    SUPERCONTEXT = System.getProperty(RelFailurePath.FAILTRACE_OPT, "").length() > 0;
	  } else if (dynamism.equals("dynamic-track")) {
	    STATIC = false;
	    DYNTRACK = true;
	  } else if(dynamism.equals("dynamic-load")) {
      STATIC = false;
      DYNTRACK = false;
	  } else {
	    System.err.println("ERR: " + CONFDEP_DYNAMIC_OPT + " must be 'static', 'dynamic-track', or dynamic-load");
	    System.exit(-1);
	  }
	  boolean miniStrings = Config.buildBoolProperty("useMiniStrings", false);
	  boolean dumpIntermediates = Config.buildBoolProperty("dumpArgTaints", false);
	  
    slurpDoms();

    if(STATIC) {
      Project.runTask("cipa-0cfa-arr-dlog");
      
//      Project.runTask("mini-findconf-dlog");
      Project.runTask("findconf-dlog");

      if(miniStrings)
        Project.runTask("mini-str-dlog");
      else
        Project.runTask("strcomponents-dlog");
      
      Project.runTask("CnfNodeSucc");

      Project.runTask("Opt");
    } else {
      Project.runTask("dynamic-cdep-java");	  
      Project.runTask("cipa-0cfa-arr-dlog");
    }
    
	  
	  if(DYNTRACK) {
	    Project.runTask("dyn-datadep");
	  } else
	    Project.runTask("datadep-func-dlog");
	  
	  
	  if(SUPERCONTEXT) {
      Project.runTask("scs-datadep-dlog");
      Project.runTask("scs-confdep-dlog");
	  } else 
	    Project.runTask("confdep-dlog");
	
//    dumpOptsRead(domH, domConst, domUV);
	  DomOpts domOpt  = (DomOpts) Project.getTrgt("Opt");

    dumpOptUses(domOpt);
    dumpFieldTaints(domOpt);
    if(dumpIntermediates)  {
      dumpOptRegexes("conf_regex.txt", DomOpts.optSites());
      Project.runTask("datadep-debug-dlog");
      dumpArgDTaints();
    }
	}
	
  private void dumpArgDTaints() {
    PrintWriter writer =
      OutDirUtils.newPrintWriter("meth_arg_conf_taints.txt");
    
    ProgramRel confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("argCdep");//outputs m, z, String
    confArgRel.load();  
    for(Trio<jq_Method, Integer, String> methArg:  confArgRel.<jq_Method, Integer, String>getAry3ValTuples()) {
      String optName = methArg.val2;
      int z = methArg.val1;
      jq_Method meth = methArg.val0;
//      jq_Type ty = meth.getParamTypes()[meth.isStatic() ? z : z-1];
      jq_Type ty = meth.getParamTypes()[z];
      writer.println(meth.getDeclaringClass() + " " + meth.getNameAndDesc().toString() +
          " arg " + z +  " of type " + ty + " " +  optName);
    }
    
    confArgRel.close();
    writer.close();
    
    writer =
      OutDirUtils.newPrintWriter("inv_arg_conf_taints.txt");
    
    confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("IargCdep");//outputs i, z, String
    confArgRel.load();  
    for(Trio<Quad, Integer, String> methArg:  confArgRel.<Quad, Integer, String>getAry3ValTuples()) {
      String optName = methArg.val2;
      int z = methArg.val1;
      jq_Method calledMeth = Invoke.getMethod(methArg.val0).getMethod();
      jq_Method callerM = methArg.val0.getMethod();
//      jq_Type ty = meth.getParamTypes()[meth.isStatic() ? z : z-1];
      jq_Type ty = calledMeth.getParamTypes()[z];
      String caller = callerM.getDeclaringClass() + " " + callerM.getName() + ":" + methArg.val0.getLineNumber();
      writer.println(caller + " calling " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
          " arg " + z +  " of type " + ty + " " +  optName);
    }
    
    confArgRel.close();
    writer.close();
    
    writer =
      OutDirUtils.newPrintWriter("inv_ret_conf_taints.txt");
    
    confArgRel =  (ProgramRel) ClassicProject.g().getTrgt("IretDep");//outputs i, i
    confArgRel.load();  
    for(Pair<Quad, String> invkRet:  confArgRel.<Quad, String>getAry2ValTuples()) {
      String optName = invkRet.val1;
      jq_Method calledMeth = Invoke.getMethod(invkRet.val0).getMethod();
      jq_Method callerM = invkRet.val0.getMethod();
//      jq_Type ty = meth.getParamTypes()[meth.isStatic() ? z : z-1];
      jq_Type ty = calledMeth.getReturnType();
      String caller = callerM.getDeclaringClass() + " " + callerM.getName() + ":" + invkRet.val0.getLineNumber();
      writer.println(caller + " calling " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
          " returns type " + ty + " taint: " +  optName);
    }
    
    confArgRel.close();
    writer.close();
  }

  private void dumpFieldTaints(DomOpts opts) {

    HashSet<String> printedLines = new HashSet<String>();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_fields.txt");
    
    { //a new block to mask 'tuples'
      ProgramRel relConfFields =
        (ProgramRel) ClassicProject.g().getTrgt("instHF");//ouputs H0,F0,Opt
      relConfFields.load();
      IntTrioIterable tuples = relConfFields.getAry3IntTuples();
      for (IntTrio p : tuples) {

        jq_Field f = domF.get(p.idx1);
        String optName = opts.get(p.idx2);
  
        if(f == null ) {
          if(p.idx1 != 0) //null f when p.idx1 == 0 is uninteresting
            System.out.println("ERR: no F entry for " + p.idx1+ " (Option was " + optName+")");
          continue;
        }
        jq_Class cl = f.getDeclaringClass();
        String clname;
        if(cl == null)
          clname = "UNKNOWN";
        else
          clname = cl.getName();
        String optAndLine = optName + clname+ f.getName();
        if(!printedLines.contains(optAndLine)) {
          printedLines.add(optAndLine);
          writer.println(clname+ " "+ ": " + optName  + " affects field " + 
              f.getName()+ " of type " + f.getType().getName() + ".");
        }
      }
      relConfFields.close();
    }
    ProgramRel relStatFields =
      (ProgramRel) ClassicProject.g().getTrgt("statHF");//ouputs F0,I
    relStatFields.load();
    IntPairIterable statTuples = relStatFields.getAry2IntTuples();
    for (IntPair p : statTuples) {
      jq_Field f = domF.get(p.idx0);
      String optName = opts.get(p.idx1);

      if(f == null&& p.idx1 != 0) {
        System.out.println("ERR: no F entry for " + p.idx0+ " (Option was " + optName+")");
        continue;
      }
      jq_Class cl = f.getDeclaringClass();
      String clname;
      if(cl == null)
        clname = "UNKNOWN";
      else
        clname = cl.getName();
      String optAndLine = optName + clname+ f.getName();
      if(!printedLines.contains(optAndLine)) {
        printedLines.add(optAndLine);
        writer.println(clname+ " "+ ": " + optName + " affects static field " +
            f.getName() + " of type " + f.getType().getName()+ ".");
      }
    }
    
    writer.close();
  }


  private void dumpOptUses(DomOpts opts) {

    HashSet<String> printedLines = new HashSet<String>();
    HashSet<Integer> quadsPrinted = new HashSet<Integer>();

    HashSet<Integer> quadsSeen = new HashSet<Integer>();
    int confUseQuad = 0;
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_uses.txt");
    
    ProgramRel relConfUses =
      (ProgramRel) ClassicProject.g().getTrgt("cOnLine");//ouputs I0,Opt
    relConfUses.load();
    IntPairIterable tuples = relConfUses.getAry2IntTuples();
    for (IntPair p : tuples) {
      Quad q1 = (Quad) domI.get(p.idx0);
      
      quadsSeen.add(p.idx0);
      confUseQuad++;

      jq_Method m = q1.getMethod();
      int lineno = q1.getLineNumber();

      String optName = opts.get(p.idx1);
      
      String filename = m.getDeclaringClass().getSourceFileName();
      String optAndLine = optName+lineno + filename;
      if(!printedLines.contains(optAndLine)) {
        printedLines.add(optAndLine);
        quadsPrinted.add(p.idx0);
        jq_Method calledMethod = Invoke.getMethod(q1).getMethod();
        String calltarg = calledMethod.getDeclaringClass().getName() + " "+ calledMethod.getName();
//        if(p.idx2 == 0)
//          writer.println(filename+ " "+ lineno + ": " + optName +"-"+ p.idx1 + " in use  (" + m.getName() + ").");
//        else
          writer.println(filename+ " "+ lineno + ": " + optName + " in use  (" + m.getName() + "). Called method was " + calltarg);
//          writer.println("\tCall to " + calltarg + " tainted by " +  optName);
      }
    }
    writer.close();
    relConfUses.close();
    int quadCount = quadsPrinted.size();
    int confUses = printedLines.size();
    
    
    ProgramRel reachableI =  (ProgramRel) ClassicProject.g().getTrgt("reachableI");
    reachableI.load();
    int reachableIsize = reachableI.size();
    reachableI.close();

    
    PrintWriter stats = OutDirUtils.newPrintWriter("confdep_stats.txt");
    stats.println("saw " + quadCount + " lines with a conf option and " + confUses + " conf uses. (LRatio = " + confUses *1.0 / quadCount+ ")");
    quadCount = quadsSeen.size();
    stats.println("saw " + quadCount + " quads with a conf option and " + confUseQuad + " conf uses. (QRatio = " + confUseQuad *1.0 / quadCount+ ")");
    stats.println("saw " + reachableIsize + " invokes, and " + confUses + " uses over those. IRatio =" + confUseQuad *1.0 / reachableIsize);
    stats.println(opts.size() + " total options");
    stats.close();
  }

  
  public static void dumpOptRegexes(String filename, Map<Quad,String> names) {
    PrintWriter writer =
      OutDirUtils.newPrintWriter(filename);

    for(Map.Entry<Quad, String> s: names.entrySet()) {
      Quad quad = s.getKey();
      String regexStr = s.getValue();
      jq_Method m = quad.getMethod();
      jq_Class cl = m.getDeclaringClass();
      int lineno = quad.getLineNumber();
      String optType = ConfDefines.optionPrefix(quad);
      String readingMeth = Invoke.getMethod(quad).getMethod().getName().toString();

      writer.println(optType + " " + regexStr + " read by " +
          m.getDeclaringClass().getName()+ " " + m.getName() + ":" + lineno + " " + readingMeth);
    }
    writer.close();
  }

  public void slurpDoms() {
    
    ClassicProject project = ClassicProject.g();

    domConst = (DomStrConst) project.getTrgt("StrConst");
    domI = (DomI) project.getTrgt("I");
    domH = (DomH) project.getTrgt("H");
    domV = (DomV) project.getTrgt("V");
    domUV = (DomUV) project.getTrgt("UV");
    domF = (DomF) project.getTrgt("F");
  }
}

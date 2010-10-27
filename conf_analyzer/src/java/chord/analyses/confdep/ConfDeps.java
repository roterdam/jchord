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
import joeq.Compiler.Quad.RegisterFactory.Register;
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
import chord.doms.*;


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
	  boolean dumpIntermediates = Config.buildBoolProperty("dumpArgTaints", true);
	  
	  Project.runTask("cipa-0cfa-arr-dlog");
    
//	  Project.runTask("findconf-dlog");
	  Project.runTask("mini-findconf-dlog");
	  
    if(miniStrings)
      Project.runTask("mini-str-dlog");
    else
      Project.runTask("strcomponents-dlog");
    
    Project.runTask("CnfNodeSucc");

    slurpDoms();
    Map<Quad, String> names = dumpOptRegexes();

	  /*
	  if(STATIC)
	    Project.runTask("ImarkConf");
	  else
	    Project.runTask("dynamic-cdep-java");
	  
	  if(DYNTRACK) {
	    Project.runTask("dyn-datadep");
	  } else*/
	    Project.runTask("datadep-dlog");
	  
	  
	  if(SUPERCONTEXT) {
      Project.runTask("scs-datadep-dlog");
      Project.runTask("scs-confdep-dlog");
	  } else 
	    Project.runTask("confdep-dlog");
	
//    dumpOptsRead(domH, domConst, domUV);
	  DomOpts domOpt  = (DomOpts) Project.getTrgt("Opt");

    dumpOptUses(domOpt);
    dumpFieldTaints(domOpt);
    if(dumpIntermediates) 
      dumpArgDTaints();
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
      writer.println(caller + " calling  " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
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
      writer.println(caller + " calling  " + calledMeth.getDeclaringClass() + " " + calledMeth.getNameAndDesc().toString() +
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
 //   HashSet<String> quadConfPair = new HashSet<String>();
    int confUseCount = 0;
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_uses.txt");
    
    ProgramRel relConfUses =
      (ProgramRel) ClassicProject.g().getTrgt("cOnLine");//ouputs I0,Opt
    relConfUses.load();
    IntPairIterable tuples = relConfUses.getAry2IntTuples();
    for (IntPair p : tuples) {
      Quad q1 = (Quad) domI.get(p.idx0);
      
      quadsSeen.add(p.idx0);
      confUseCount++;

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
    System.out.println("saw " + quadCount + " lines with a conf option and " + confUses + " conf uses. (LRatio = " + confUses *1.0 / quadCount+ ")");
    
    quadCount = quadsSeen.size();
    confUses = confUseCount;
    System.out.println("saw " + quadCount + " quads with a conf option and " + confUses + " conf uses. (QRatio = " + confUses *1.0 / quadCount+ ")");
    System.out.println("saw " + domI.size() + " invokes, and " + confUses + " uses over those. IRatio =" + confUses *1.0 / domI.size());
  }


  //reconstruct the string at program point quad, using relation logStrings, of form I,Cst,Z
  public static String reconcatenate(Quad quad, ProgramRel logStrings, boolean makeRegex, int maxFilled) {
    RelView v = logStrings.getView();
    v.selectAndDelete(0, quad);
    String[] wordsByPos = new String[DomZZ.MAXZ];

    if(v.size() == 0)
      return "X";
    
    for(Pair<String,Integer> t: v.<String,Integer>getAry2ValTuples()) {
      int i = t.val1;
      if(wordsByPos[i] == null)
        wordsByPos[i] = t.val0;
      else 
        wordsByPos[i] = wordsByPos[i]+"|"+t.val0;
      maxFilled = Math.max(maxFilled, i);
    }
     
    StringBuilder sb = new StringBuilder();
    for(int i =0; i < maxFilled+1 ; ++ i) {
      if(wordsByPos[i] != null)
        sb.append(wordsByPos[i]);
      else
        if(makeRegex)
          sb.append(".*");
        else
        sb.append(" X ");
    }
    v.free();
    return sb.toString();
  }
  
  public Map<Quad,String> dumpOptRegexes() {
    return dumpRegexes("conf_regexes.txt", "confOpts", "confOptLen", "confOptName", domH);
  }
  
  public static Map<Quad,String> dumpRegexes(String filename, String ptRel, String lenRel, String nameRel, DomH domH) {
    
    ClassicProject Project = ClassicProject.g();

    Map<Quad, String> returnedMap = new LinkedHashMap<Quad, String>();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter(filename);
    ProgramRel relConfOptStrs =
      (ProgramRel) Project.getTrgt(nameRel);//outputs I, Str, ZZ
    relConfOptStrs.load();
    ProgramRel relConfOptLens =  (ProgramRel) Project.getTrgt(lenRel);
    relConfOptLens.load();
    ProgramRel opts = (ProgramRel) Project.getTrgt(ptRel);
    opts.load();
    
    ProgramRel vh = (ProgramRel) Project.getTrgt("VH");
    vh.load();
    ProgramRel strs = (ProgramRel) Project.getTrgt("VConstFlow"); //v:V0, cst:StrConst
    strs.load();
    ProgramRel cnfNodeSucc = (ProgramRel) Project.getTrgt("CnfNodeSucc");
    cnfNodeSucc.load();
    
    for(Object q: opts.getAry1ValTuples()) {
      Quad quad = (Quad) q;
      jq_Method m = quad.getMethod();
      jq_Class cl = m.getDeclaringClass();
      int lineno = quad.getLineNumber();
      
      RelView lenV = relConfOptLens.getView();
      lenV.selectAndDelete(0, quad);
      int lenMax = -1;
      for(Integer aLen: lenV.<Integer>getAry1ValTuples()) {
        if(aLen > lenMax)
          lenMax = aLen;
      }
      String optType = ConfDefines.optionPrefix(quad);
      String readingMeth = Invoke.getMethod(quad).getMethod().getName().toString();
      String regexStr = reconcatenate(quad, relConfOptStrs, true, lenMax);
      
      String s  = supplementName(quad, vh, strs, cnfNodeSucc, domH);
      if(s.length() > 1 ) {
        if(s.length() > regexStr.length()) {
          System.out.println("RENAME: changing '"+regexStr + "' to " + s);
          regexStr = s;
        } else if(!s.equals(regexStr)) {
          System.err.println("WARN: rename wants to turn " + regexStr + " into " + s);
        }
      }
      
      returnedMap.put(quad, regexStr);
      writer.println(optType + " " + regexStr + " read by " +
          m.getDeclaringClass().getName()+ " " + m.getName() + ":" + lineno + " " + readingMeth);
    }
    opts.close();
    relConfOptStrs.close();
    relConfOptLens.close();
    writer.close();
    
    vh.close();
    strs.close();
    cnfNodeSucc.close();
    
    return returnedMap;
  }
  
  static String supplementName(Quad q, ProgramRel vh, ProgramRel strs, ProgramRel cnfNodeSucc, DomH domH) {
    
    Quad prevHop = q;
    ArrayList<String> pathParts = new ArrayList<String>();
    int hIdx = domH.indexOf(prevHop);
    while(prevHop != null && hIdx > 0 && cnfNodeSucc.contains(prevHop)) {
//      System.err.println("Renamer: supplementing quad " + q + " in " +
//          q.getMethod().getDeclaringClass().getName() + " " + q.getMethod().getName() + ":" + q.getLineNumber());
      Pair<Register,String> prevAndComponent = getCompAt(prevHop, strs,cnfNodeSucc);
//      System.err.println("Renamer: backtracking on " + prevAndComponent.val0 + " and appending " + prevAndComponent.val1);
      pathParts.add(prevAndComponent.val1);
      prevHop = getPrevHop(vh, prevAndComponent.val0);
      hIdx = domH.indexOf(q);
    }
    
    StringBuilder res = new StringBuilder();
    int i = pathParts.size() -1;
//    if( i >= 0)
//      System.err.println("Renamer: found  " + i + " supplemental path parts");
    for(; i >= 0; --i) {
      res.append(pathParts.get(i));
    }
    
    return res.toString();
  }

  static private Quad getPrevHop(ProgramRel vh, Register val0) {
    if(val0 == null)
      return null;
    
    RelView view = vh.getView();
    view.selectAndDelete(0, val0);
    Quad res = null;
//    System.err.println("Renamer: in getPrevHop, have " + view.size() + " candidate prevs for "
//        + val0.toString() + " of type " + val0.getType());
    for(Object q: view.<Object>getAry1ValTuples()) {
      if(q instanceof Quad &&  ((Quad) q).getOperator() instanceof Invoke) {
        res = (Quad) q;
        break;
      }
    }
//    if(res == null)
//      System.err.println("prev didn't pan out");
//    else
//      System.err.println("found prev; it was " + res);
    view.free();
    return res;
  }

  private static Pair<Register, String> getCompAt(Quad prevHop, ProgramRel strs,
      ProgramRel cnfNodeSucc) {
    RelView view = cnfNodeSucc.getView();
    view.selectAndDelete(0, prevHop);
    jq_Method methForComponent = Invoke.getMethod(prevHop).getMethod();

    
    Register pathPartV = null;
    Register basePart = null;

    if(view.size() < 1 || view.size() > 1) {
      System.err.println("Renamer:  didn't expect cnfNodeSucc to have " + view.size() + " elems at point");
    }
    for(Pair<Register,Register> parms: view.<Register,Register>getAry2ValTuples()) {
      basePart = parms.val0;
      pathPartV = parms.val1;
      break;
    }
    view.free();

    String str="/.*";
    if(pathPartV != null) {
      RelView strsAt = strs.getView();
      strsAt.selectAndDelete(0, pathPartV);
//      System.err.println("Renamer: in getCompAt, have " + strsAt.size() + " candidate strings");
      StringBuffer sb;
      if(methForComponent.getName().toString().contains("Attribute"))
        sb = new StringBuffer("@");
      else sb= new StringBuffer("/");
      
      for(String s: strsAt.<String>getAry1ValTuples()) {
        sb.append(s);
        sb.append("|");
      }
      if(sb.length() > 1) {
        sb.deleteCharAt(sb.length() -1);
        str = sb.toString();
      }
      
      strsAt.free();
    }
    
    return new Pair<Register,String>(basePart, str);
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

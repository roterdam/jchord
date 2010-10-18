/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.analyses.confdep;

import java.io.Console;
import java.io.PrintWriter;
import java.util.*;
import joeq.Class.jq_Class;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
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
	  
    Project.runTask("cipa-0cfa-arr-dlog");
	  
	  Project.runTask("strcomponents-dlog");
	  if(STATIC)
	    Project.runTask("ImarkConf");
	  else
	    Project.runTask("dynamic-cdep-java");
	  
	  if(DYNTRACK) {
	    Project.runTask("dyn-datadep");
	  } else
	    Project.runTask("datadep-dlog");
	  
	  if(SUPERCONTEXT) {
      Project.runTask("scs-datadep-dlog");
      Project.runTask("scs-confdep-dlog");
	  } else
	    Project.runTask("confdep-dlog");
    if(lookAtLogs)
      Project.runTask("logconfdep-dlog");
		
    Project.runTask("CnfNodeSucc");

    slurpDoms();
    dumpOptsRead(domH, domConst, domUV);
    dumpOptUses(domI, domH, domConst);
    dumpFieldTaints(domF, domH, domConst);
    dumpOptRegexes();

    
	}
	
  private void dumpFieldTaints(DomF domF, DomH domH, DomStrConst domConst) {

    HashSet<String> printedLines = new HashSet<String>();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_fields.txt");
    
    { //a new block to mask 'tuples'
      ProgramRel relConfFields =
        (ProgramRel) ClassicProject.g().getTrgt("HFC");//ouputs H0,F0,H1,StrConst0
      relConfFields.load();
      IntQuadIterable tuples = relConfFields.getAry4IntTuples();
      for (IntQuad p : tuples) {
  //      Quad q1 = (Quad) domI.get(p.idx0);
  
  //      jq_Method m = Program.v().getMethod((Inst) q1);
  //      int lineno = Program.getLineNumber(q1, m);
        jq_Field f = domF.get(p.idx1);
        String optName = getOptName(p.idx0, p.idx3, domH, domConst);
  
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
      (ProgramRel) ClassicProject.g().getTrgt("cdepf");//ouputs F0,H0,StrConst0
    relStatFields.load();
    IntTrioIterable statTuples = relStatFields.getAry3IntTuples();
    for (IntTrio p : statTuples) {
      jq_Field f = domF.get(p.idx0);
      String optName = getOptName(p.idx1, p.idx2, domH, domConst);

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

  public static String getOptName(int hIdx, int cIdx, DomH domH, DomStrConst domConst) {
    if(hIdx == 0)
      return "Main arg";
    else {
      if(cIdx == 0)
        return ConfDefines.optionPrefix(domH.get(hIdx)) + domConst.get(cIdx) + "-"+hIdx;
      else
        return ConfDefines.optionPrefix(domH.get(hIdx)) + domConst.get(cIdx);
    }
  }

  private void dumpOptUses(DomI domI,DomH domH, DomStrConst domConst) {

    HashSet<String> printedLines = new HashSet<String>();
    HashSet<Integer> quadsPrinted = new HashSet<Integer>();

    HashSet<Integer> quadsSeen = new HashSet<Integer>();
 //   HashSet<String> quadConfPair = new HashSet<String>();
    int confUseCount = 0;
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("conf_uses.txt");
    
    ProgramRel relConfUses =
      (ProgramRel) ClassicProject.g().getTrgt("cOnLine");//ouputs V0,H0,StrConst0
    relConfUses.load();
    IntTrioIterable tuples = relConfUses.getAry3IntTuples();
    for (IntTrio p : tuples) {
      Quad q1 = (Quad) domI.get(p.idx0);
      
      quadsSeen.add(p.idx0);
      confUseCount++;

      jq_Method m = q1.getMethod();
      int lineno = q1.getLineNumber();

      String optName = getOptName(p.idx1, p.idx2, domH, domConst);
      
      String filename = m.getDeclaringClass().getSourceFileName();
      String optAndLine = optName+lineno + filename;
      if(!printedLines.contains(optAndLine)) {
        printedLines.add(optAndLine);
        quadsPrinted.add(p.idx0);
//        if(p.idx2 == 0)
//          writer.println(filename+ " "+ lineno + ": " + optName +"-"+ p.idx1 + " in use  (" + m.getName() + ").");
//        else
          writer.println(filename+ " "+ lineno + ": " + optName + " in use  (" + m.getName() + ").");
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

  private void dumpOptsRead(DomH domH, DomStrConst domConst, DomUV domUV) {
    
    ClassicProject Project = ClassicProject.g();

    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("confoptions.txt");
    ProgramRel relConfOpts =
      (ProgramRel) Project.getTrgt("ImarkConf");//ouputs H0,V0,StrConst0
    relConfOpts.load();
    
    List<String> inconsistent_uses = new ArrayList<String>();
    Map<String, String> option_types = new HashMap<String, String>();
    
    IntTrioIterable tuples = relConfOpts.getAry3IntTuples();
    for (IntTrio p : tuples) {
      if(p.idx0 == 0) { //special H value for main's arg
        //writer.println("main argument");
        continue;
      }
      Quad q1 = (Quad) domH.get(p.idx0);
      jq_Method m = q1.getMethod();
      int lineno = q1.getLineNumber();

      String optName = getOptName(p.idx0, p.idx2, domH, domConst);
      if(p.idx2 != 0) {
        String optType = ConfDefines.optionPrefix((Quad) domH.get(p.idx0));
        String optN = domConst.get(p.idx2);
        String oldOT = option_types.get(optN);
        if(oldOT == null) {
          option_types.put(optN, optType);
        } else {
          if(!optType.equals(oldOT)) {
            inconsistent_uses.add(optN + " is sometimes " + oldOT + " and sometimes " + optType);
          }
        }
      }
      String filename = m.getDeclaringClass().getSourceFileName();
      RegisterFactory.Register v = domUV.get(p.idx1);
      
      writer.println(optName+ " ("+ v.getType() +") read on line " + lineno + " of " + filename + " (" + m.getName() + "). "
          + "Internal var name was " + v + " and alloc site was " + q1.toString_short());
    }
    writer.close();
    relConfOpts.close();
    if(inconsistent_uses.size() > 0) {
      writer = OutDirUtils.newPrintWriter("inconsistent_uses.txt");
      for(String s: inconsistent_uses) {
        writer.println(s);
      }
      writer.close();
    }
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
    return dumpRegexes("conf_regexes.txt", "confOpts", "confOptLen", "confOptName");
  }
  
  public Map<Quad,String> dumpRegexes(String filename, String ptRel, String lenRel, String nameRel) {
    
    ClassicProject Project = ClassicProject.g();

    HashMap<Quad, String> returnedMap = new HashMap<Quad, String>();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter(filename);
    ProgramRel relConfOptStrs =
      (ProgramRel) Project.getTrgt(nameRel);//outputs I, Str, Z
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

//    Rel.RelView opts = relConfOptStrs.getView();
//    opts.delete(2);
//    opts.delete(1);
    
    
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
      
      String s  = supplementName(quad, vh, strs, cnfNodeSucc);
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
  
  String supplementName(Quad q, ProgramRel vh, ProgramRel strs, ProgramRel cnfNodeSucc) {
    
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

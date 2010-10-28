package chord.analyses.confdep.docu;

import java.io.PrintWriter;
import java.util.Map;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.confdep.ConfDeps;
import chord.analyses.confdep.optnames.DomOpts;
import chord.bddbddb.Rel.RelView;
import chord.doms.*;
import chord.project.*;
import chord.project.analyses.*;
import chord.util.tuple.object.Pair;

@Chord(
    name = "ExplainMyLogs"
  )

public class ExplainMyLogs extends JavaAnalysis{
  
  boolean miniStrings;
  
  @Override
  public void run() {
    
    ClassicProject project = ClassicProject.g();

    
    miniStrings = Config.buildBoolProperty("useMiniStrings", false);
    
    project.runTask("cipa-0cfa-arr-dlog");
    
    project.runTask("findconf-dlog");
    
    if(miniStrings)
      project.runTask("mini-str-dlog");
    else
      project.runTask("strcomponents-dlog");
    
    project.runTask("CnfNodeSucc"); //used for name-finding
    project.runTask("logconfdep-dlog");
    
    ConfDeps c = new ConfDeps();
    c.slurpDoms();
    Map<Quad, String> returnedMap = DomOpts.optSites();
    dumpLogDependencies(returnedMap);
    
//    dumpLogToProgPtMap();
  }
  



  static public void dumpLogDependencies(Map<Quad, String> optNames) {
    ClassicProject project = ClassicProject.g();

    PrintWriter writer =
      OutDirUtils.newPrintWriter("log_dependency.txt");
    ProgramRel logConfDeps =
      (ProgramRel) project.getTrgt("logConfDep");//ouputs I0,Opt (stmt, src)
    logConfDeps.load();
    ProgramRel allLogs = (ProgramRel) project.getTrgt("logStmt"); //just quads
    allLogs.load();

    ProgramRel logStrings = (ProgramRel) project.getTrgt("logString"); //i,cst,z
    logStrings.load();
    
    ProgramRel dataDep = (ProgramRel) project.getTrgt("logFieldDataDep");// i, z, Opt
    dataDep.load();
    
    for(Object q: allLogs.getAry1ValTuples()) {
      Quad logCall = (Quad) q;
      jq_Method m = logCall.getMethod();
      jq_Class cl = m.getDeclaringClass();
      int lineno = logCall.getLineNumber();
      
      String msg = renderLogMsg(logCall, logStrings, dataDep);
      for(String thisLine: msg.split("\n")) {
        writer.println(cl.toString()+":" +lineno  + " (" + m.getName() +") " +
            Invoke.getMethod(logCall).getMethod().getName()+ "("  + thisLine+")");
      }
      
      RelView ctrlDepView = logConfDeps.getView();
      ctrlDepView.selectAndDelete(0, logCall);
      for(Quad ctrlDep: ctrlDepView.<Quad>getAry1ValTuples()) {
        String optName =  optNames.get(ctrlDep);
        if(optName == null)
          continue;
//          optName = "Unknown Conf";
        writer.println("\tcontrol-depends on "+optName);
      }
      
    }
    
    writer.println("total of " + allLogs.size() + " log statements; " + logConfDeps.size() + " dependencies");
    dataDep.close();
    allLogs.close();
    logConfDeps.close();
    writer.close();
  }
  
//reconstruct the string at program point quad, using relation logStrings, of form I,Cst,Z
  public static String renderLogMsg(Quad quad, ProgramRel logStrings, ProgramRel dataDep) {
    RelView constStrs = logStrings.getView();
    constStrs.selectAndDelete(0, quad);
    String[] wordsByPos = new String[DomZZ.MAXZ];

    int maxFilled = -1;
    if(constStrs.size() == 0)
      return "X";
    
    for(Pair<String,Integer> t: constStrs.<String,Integer>getAry2ValTuples()) {
      int i = t.val1;
      if(wordsByPos[i] == null)
        wordsByPos[i] = t.val0;
      else 
        wordsByPos[i] = wordsByPos[i]+"|"+t.val0;
      maxFilled = Math.max(maxFilled, i);
    }
    RelView dataDepV = dataDep.getView();
    dataDepV.selectAndDelete(0, quad);
    
    for(Pair<Integer,String> t: dataDepV.<Integer,String>getAry2ValTuples()) {
      int i = t.val0;
      String optStr =  "[" + t.val1 + "]";
      
      if(wordsByPos[i] == null)
        wordsByPos[i] = optStr;
      else 
        wordsByPos[i] = wordsByPos[i]+"|"+optStr;
      maxFilled = Math.max(maxFilled, i);
    }
     
    StringBuilder sb = new StringBuilder();
    for(int i =0; i < maxFilled+1 ; ++ i) {
      if(wordsByPos[i] != null)
        sb.append(wordsByPos[i]);
      else
        sb.append(" X ");
    }
    constStrs.free();
    dataDepV.free();
    return sb.toString();
  }

}

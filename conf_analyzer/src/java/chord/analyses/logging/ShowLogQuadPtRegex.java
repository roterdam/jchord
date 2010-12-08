package chord.analyses.logging;

import java.io.PrintWriter;
import joeq.Compiler.Quad.Quad;
import chord.analyses.confdep.ConfDeps;
import chord.analyses.confdep.optnames.DomOpts;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;


@Chord(
    name = "DumpLogQuadPoints",
    consumes = { "I","logString", "logStmt"}

  )
public class ShowLogQuadPtRegex extends JavaAnalysis {
  
  @Override
  public void run() {
    ClassicProject project = ClassicProject.g();
    PrintWriter out =
      OutDirUtils.newPrintWriter("log_stmt_locations.txt");

    
    ProgramRel logStrings = (ProgramRel) project.getTrgt("logString"); //i,cst,z
    logStrings.load();
    ProgramRel logQuads = (ProgramRel) project.getTrgt("logStmt"); //just quads
    logQuads.load();
    
    ProgramRel logVHV = (ProgramRel) project.getTrgt("logVHolds"); //i,cst,z
    logVHV.load();
    ProgramRel logVHU = (ProgramRel) project.getTrgt("logVHoldsU"); //just quads
    logVHU.load();
    
    DomI domI = (DomI) project.getTrgt("I"); 
    
    for(Object q: logQuads.getAry1ValTuples()) {
      Quad logCall = (Quad) q;
      int quadID = domI.indexOf(logCall);
//      if(quadID > 0) {
      String logRegex = logMsgText(logCall, logStrings, logVHV, logVHU);
        out.println(quadID + " " +  logRegex);
        out.println( logCall.getMethod().getNameAndDesc() +":" + logCall.getLineNumber());
//      }
    }
    
    logVHV.close();
    logVHU.close();
    logQuads.close();
    logStrings.close();
    out.close();
  }
  
  public static String logMsgText(Quad logCall, ProgramRel logStrings, ProgramRel logVHV, ProgramRel logVHU) {
    return DomOpts.reconcatenate(logCall, logStrings, true, logStmtLen(logCall, logVHV, logVHU));
  }
  
  private static int logStmtLen(Quad q, ProgramRel logVHoldU, ProgramRel logVHoldV) {
    
    return Math.max(stmtLen(q, logVHoldU), stmtLen(q, logVHoldV));
  }

  private static int stmtLen(Quad q, ProgramRel logVHold) {
    int maxL = -1;
    RelView componentsAtPt = logVHold.getView();
    componentsAtPt.selectAndDelete(0, q);
    
    for(Pair<Object, Integer> t: componentsAtPt.<Object,Integer>getAry2ValTuples()) {
      if(t.val1> maxL)
        maxL = t.val1;
    }
    
    componentsAtPt.free();
    return maxL;
  }

}

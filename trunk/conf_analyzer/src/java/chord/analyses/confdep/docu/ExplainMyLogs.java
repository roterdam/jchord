package chord.analyses.confdep.docu;

import java.io.PrintWriter;
import java.util.*;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.confdep.ConfDeps;
import chord.analyses.confdep.optnames.DomOpts;
import chord.bddbddb.Rel.RelView;
import chord.doms.*;
import chord.project.*;
import chord.project.analyses.*;
import chord.util.Utils;
import chord.util.tuple.object.Pair;

@Chord(
    name = "ExplainMyLogs"
  )

public class ExplainMyLogs extends JavaAnalysis{
  
  boolean miniStrings;
  
  int MAX_CTRLDEPS_TO_DUMP = 30;
  
  String[] inScopePrefixes;
  @Override
  public void run() {
    
    inScopePrefixes = Config.toArray(System.getProperty("dictionary.scope", ""));
    if(inScopePrefixes.length == 0)
      inScopePrefixes = new String[] {""};
  	
    ClassicProject project = ClassicProject.g();
    
    miniStrings = Config.buildBoolProperty("useMiniStrings", false);
    
    project.runTask("cipa-0cfa-arr-dlog");
    
    project.runTask("findconf-dlog");
    
    if(miniStrings)
      project.runTask("mini-str-dlog");
    else
      project.runTask("strcomponents-dlog");
    
    project.runTask("Opt"); //used for name-finding
    project.runTask("logconfdep-dlog");
    
    ConfDeps c = new ConfDeps();
    c.slurpDoms();
    
    Map<Quad, Pair<String,Integer>> renderedMessages = renderAllLogMessages();
    Map<Quad,Integer> depsPerLine = dumpLogDependencies(renderedMessages);
    dumpIndexedDependencies(renderedMessages, depsPerLine);
//    dumpLogToProgPtMap();
  }
  
  
  private Map<Quad, Pair<String,Integer>> renderAllLogMessages() {
    ClassicProject project = ClassicProject.g();

  	Map<Quad, Pair<String,Integer>> rendered = new LinkedHashMap<Quad, Pair<String,Integer>>();
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
      
      boolean isInScope = Utils.prefixMatch(cl.getName(), inScopePrefixes);
      if(!isInScope)
      	continue;
    	String msg = renderLogMsg(logCall, logStrings, dataDep);
    	
    	RelView depCount = dataDep.getView();
    	depCount.selectAndDelete(0, logCall);
    	int depsForLine = depCount.size();
    	rendered.put(logCall, new Pair<String,Integer>(msg, depsForLine));      

    }

    dataDep.close();
    allLogs.close();
    return rendered;
  }
  

/**
 * Dumps a listing of log messages, annotated with the source option.
 * Returns map of the number of dependences per log message
 */
  private Map<Quad, Integer> dumpLogDependencies(Map<Quad, Pair<String,Integer>> renderedMessages) {
    ClassicProject project = ClassicProject.g();
   
    HashMap<Quad, Integer> depsPerLine = new HashMap<Quad, Integer>();
    
    PrintWriter writer =
      OutDirUtils.newPrintWriter("log_dependency.txt");
    ProgramRel logConfDeps =
      (ProgramRel) project.getTrgt("logConfDep");//ouputs I0,Opt (stmt, src)
    logConfDeps.load();

    
    for(Map.Entry<Quad, Pair<String,Integer>> msgPair: renderedMessages.entrySet()) {
    	Quad logCall = msgPair.getKey();
      jq_Method m = logCall.getMethod();
      jq_Class cl = m.getDeclaringClass();
      int lineno = logCall.getLineNumber();
      
    	String msg = msgPair.getValue().val0;

      for(String thisLine: msg.split("\n")) {
        String formatted =  cl.toString()+":" +lineno  + " (" + m.getName() +") " +
            Invoke.getMethod(logCall).getMethod().getName()+ "("  + thisLine+")";
        writer.println(formatted);
      }
      int dataDeps = msgPair.getValue().val1;
    	
      RelView ctrlDepView = logConfDeps.getView();
      ctrlDepView.selectAndDelete(0, logCall);
      for(String ctrlDep: ctrlDepView.<String>getAry1ValTuples()) {
        if(ctrlDep == null)
          continue;
//          optName = "Unknown Conf";
        writer.println("\tcontrol-depends on "+ctrlDep);
      }
      depsPerLine.put(logCall, ctrlDepView.size() + dataDeps);
      
    }
    
    writer.println("total of " + renderedMessages.size() + " log statements; " + logConfDeps.size() + " dependencies");
    writer.close();

    logConfDeps.close();
    return depsPerLine;
  }
  
  private void dumpIndexedDependencies(Map<Quad, Pair<String,Integer>> renderedMessages,
  		  Map<Quad,Integer> depsPerLine) {
//  HashMap<String, Set<String>> messagesByDataOpt = new HashMap<String, Set<String>>();
//HashMap<String, Set<String>> messagesByCtrlOpt = new HashMap<String, Set<String>>();
    ClassicProject project = ClassicProject.g();

  	PrintWriter writer = OutDirUtils.newPrintWriter("messages_by_option.txt");
    ProgramRel logConfDeps =
      (ProgramRel) project.getTrgt("logConfDep");//outputs I0,Opt (stmt, src)
    logConfDeps.load();
    ProgramRel dataDep = (ProgramRel) project.getTrgt("logFieldDataDep");// i, z, Opt
    dataDep.load();
//    RelView dataDep = dataDepWide.getView();
 //   dataDep.delete(1);
    
    DomOpts opts = (DomOpts) project.getTrgt("Opt");
    
    for(String option: opts) {
    	TreeSet<String> messagesForThisOpt = new TreeSet<String>();//for sorting
    	
    	//start with data dependences
    	RelView myDataDeps = dataDep.getView();
    	myDataDeps.selectAndDelete(2, option);
    	myDataDeps.delete(1);
    	for(Quad stmt: myDataDeps.<Quad>getAry1ValTuples()) {
    		Pair<String,Integer> p = renderedMessages.get(stmt);
    			
//    		if( p == null) {
 //   			System.err.println("WARN: shouldn't get " + )
 //   		}
    		if(p == null) {
    			System.out.println("WARN: no rendered version of message at " + stmt.getMethod().getDeclaringClass() + "."+stmt.getMethod());
    		} else
    		if(p != null && p.val1 < 10)
    			tagAndAddMsg(renderedMessages, messagesForThisOpt, stmt);
    	}
    	int dataDeps = messagesForThisOpt.size();

    	//compute control dependencies
    	RelView deptsOfThisOpt = logConfDeps.getView();
    	deptsOfThisOpt.selectAndDelete(1, option);
    	int ctrlDepCnt = deptsOfThisOpt.size();
    	if(dataDeps + ctrlDepCnt > 0)
    		writer.println(option + " has " + (dataDeps + ctrlDepCnt) + " dependences. "+
    				ctrlDepCnt + " control, " + dataDeps + " data");
    	//don't dump excess control deps
    	if(ctrlDepCnt <= MAX_CTRLDEPS_TO_DUMP) {
	    	for(Quad logStmt: deptsOfThisOpt.<Quad>getAry1ValTuples()) {
	    		tagAndAddMsg(renderedMessages, messagesForThisOpt, logStmt);
	    	}
      }
    	


    	//should add datadeps here
    	if(messagesForThisOpt.size() <= MAX_CTRLDEPS_TO_DUMP) {
	    	for(String s: messagesForThisOpt)
	    		writer.println(s);
	    	writer.println();
    	}
    }
    
    dataDep.close();
    logConfDeps.close();
    writer.close();

}


	private void tagAndAddMsg(Map<Quad, Pair<String,Integer>> renderedMessages,
			TreeSet<String> messagesForThisOpt, Quad logStmt) {
		String rendered = renderedMessages.get(logStmt).val0;
		if(rendered == null)
			return;
		
		jq_Method m = logStmt.getMethod();
		jq_Class cl = m.getDeclaringClass();
		int lineno = logStmt.getLineNumber();
		for(String thisLine: rendered.split("\n")) {
		  String formatted = "\t" + cl.toString()+":" +lineno  + " (" + m.getName() +") " +
		      Invoke.getMethod(logStmt).getMethod().getName()+ "("  + thisLine+")";
			messagesForThisOpt.add(formatted);
		}
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

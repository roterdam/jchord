package chord.analyses.confdep;

import chord.analyses.confdep.optnames.DomOpts;
import chord.doms.DomI;
import chord.doms.DomV;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.*;
import chord.runtime.CoreEventHandler;
import chord.util.tuple.object.Pair;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operator.Invoke;

@Chord(
  name = "dynamic-cdep-java",
  consumes = { "H","V", "Z", "I"},
  produces = {"dynCUse","nullI", "Opt", "nullConf" },
    signs = {"I0,Z0,Opt0",  "I0", "V0,Opt0:V0_Opt0"},
    namesOfSigns = {"dynCUse" , "nullI", "nullConf"}
  //nullconf = "H0,UV0,Opt0",
  
  
//    namesOfTypes = { "H" ,"UV","StrConst", "Z","I"},
//    types = { DomH.class, DomUV.class, DomStrConst.class, DomZ.class, DomI.class }
)
public class DynConfDep extends CoreDynamicAnalysis {
  //Config.workDirName, 
  static File results = new File("dyn_cdep.temp");
  static final String SCHEME_FILE= "dynconfdep.instr";
  
  static final Pattern readPat = Pattern.compile("([0-9]*) calling .* returns option (.*)-([0-9]+) value=(.+)");
  static final Pattern usePat = Pattern.compile("([0-9]*) invoking .* ([0-9]+)=(.*)");
  static final Pattern nullVPat = Pattern.compile("([0-9]*) returns null");
  
  @Override
  public Pair<Class, Map<String, String>> getInstrumentor() {
    InstrScheme instrScheme = new InstrScheme();
    
    instrScheme.setAloadReferenceEvent(false, false, true, false, true);
    instrScheme.setAstoreReferenceEvent(false, false, true, false, true);
    instrScheme.setMethodCallEvent(true, false, true, false, true);
    instrScheme.save(SCHEME_FILE);
    
    Map<String,String> args = new HashMap<String,String>();
    args.put(InstrScheme.INSTR_SCHEME_FILE_KEY, SCHEME_FILE);
    args.put(CoreEventHandler.EVENT_HANDLER_CLASS_KEY, DynConfDepRuntime.class.getCanonicalName());
    return new Pair<Class, Map<String, String>>(ArgMonInstr.class, args);
  }
 
  boolean retrace = false;
  @Override
  public void initAllPasses() {
  	retrace = Config.buildBoolProperty("retrace_conf", false);

  	if(!retrace)
  		results.delete();
    System.out.println("starting execution; clearing buffer file.");
  }

  @Override
	public void run() {
  	if(retrace && results.exists())
  		return;
  	else
  		super.run();
  }

  @Override
  public void doneAllPasses() {
    ClassicProject project = ClassicProject.g();
    System.out.println("done all passes; slurping results");

//    DomH domH = (DomH) project.getTrgt("H");
//    project.runTask(domH);

    DomI domI = (DomI) project.getTrgt("I");
    project.runTask(domI);
    

//    DomUV domUV = (DomUV) project.getTrgt("UV");
//    project.runTask(domUV);
    DomV domV = (DomV) project.getTrgt("V");
    project.runTask(domV);


    try {
      
      DomOpts domOpts = buildDomOpts();
      //need to save so adding tuples will work
//      DomOpts domOpts =  (DomOpts)  project.getTrgt("Opt");
 //     domOpts.clear(); //necessary?
 //     domOpts.getOrAdd(DomOpts.NONE);
      
      ProgramRel relConf = (ProgramRel) project.getTrgt("OptNames"); //opt, i
      ProgramRel relUse = (ProgramRel) project.getTrgt("dynCUse");
      ProgramRel relNullC = (ProgramRel) project.getTrgt("nullConf");
      ProgramRel relNullI = (ProgramRel) project.getTrgt("nullI");
      relConf.zero();
      relUse.zero();
      relNullC.zero();
      relNullI.zero();
    
      BufferedReader br = new BufferedReader(new FileReader(results));
      
      String s = null;
      while( (s = br.readLine()) != null) {
        Matcher m = readPat.matcher(s);
        if(m.matches()) {
          int iId = Integer.parseInt(m.group(1));
          Quad q = (Quad) domI.get(iId);
          
          String cst = m.group(2);
          int cstID = domOpts.getOrAdd(cst);
          if(cstID == -1) {
            cstID = 0;
            System.err.println("UNKNOWN OPTION " + cst);
          } else
            System.out.println("Found option " + cst + " at idx " + cstID);
          
          RegisterFactory.Register targ = Invoke.getDest(q).getRegister();
          int vID = domV.indexOf(targ);
          
          String value = m.group(4);
          if(vID >-1 && iId > -1 ) {
            if("null".equals(value))
              relNullC.add(vID,cstID);

            relConf.add(cstID, iId);
          }
        } else {
          m = usePat.matcher(s);
          if(m.matches()) {
            int iId = Integer.parseInt(m.group(1));
            int zId = Integer.parseInt(m.group(2));
            String cstList = m.group(3);
            String[] confDeps = cstList.split("\\|");
            for(String dep: confDeps) {
              int i = dep.lastIndexOf('-');
              String optName = dep.substring(0, i); //ConfDefines.pruneName(dep.substring(0, i));
              
              int cstID = domOpts.indexOf(optName);
              if(cstID == -1) {
                System.out.println("WARN: found use of option " + optName + " without ever having seen a read");
                cstID = 0;
              }

              relUse.add(iId, zId, cstID);
            }
          } else {
            m = nullVPat.matcher(s);
            if(m.matches()) {
              int i = Integer.parseInt(m.group(1));
              Quad u = (Quad) domI.get(i);
              if(u != null && (u.getOperator() instanceof Invoke) && Invoke.getDest(u) != null) {
                relNullI.add(i);
              }
            } else
              if(!s.contains("without taint"))
                System.err.println("NO MATCH FOR LINE: " + s);
          }
        }
      }
      
      br.close();
//      domOpts.save();
      relConf.save();
      relUse.save();
      relNullC.save();
      relNullI.save();
    } catch(IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    
  }

  private DomOpts buildDomOpts() throws IOException {
    ClassicProject project = ClassicProject.g();
    
    DomOpts domOpts =  (DomOpts)  project.getTrgt("Opt");
    domOpts.getOrAdd(DomOpts.NONE);
//    System.out.println("in buildDomOpts, filename is " + results);
    BufferedReader br = new BufferedReader(new FileReader(results));
    String s;
    while( (s = br.readLine()) != null) {
      Matcher m = readPat.matcher(s);
      if(m.matches()) {
        int iId = Integer.parseInt(m.group(1));
        
        String cst = m.group(2);
//        String prefix = ConfDefines.optionPrefix(domI.get(iId))
        int cstID = domOpts.getOrAdd(cst);
        if(cstID == -1) {
          cstID = 0;
          System.err.println("UNKNOWN OPTION " + cst);
        } else
          System.out.println("Found and added option " + cst + " at idx " + cstID);
       }
    }
    br.close();
    domOpts.save();
    return domOpts;
  }


}

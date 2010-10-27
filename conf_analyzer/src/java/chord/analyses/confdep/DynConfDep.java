package chord.analyses.confdep;

import chord.analyses.primtrack.DomUV;
import chord.analyses.string.DomStrConst;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.doms.DomZ;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Project;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operator.Invoke;

@Chord(
  name = "dynamic-cdep-java",
  consumes = { "H","UV", "StrConst", "Z", "I"},
  produces = { "ImarkConf", "dynCUse", "nullConf", "nullI" },
    signs = {"H0,UV0,StrConst0:H0_UV0_StrConst0", "I0,Z0,StrConst0,H0:I0_Z0_StrConst0_H0",
      "H0,UV0,StrConst0:H0_UV0_StrConst0", "I0:I0"},
    namesOfSigns = { "ImarkConf","dynCUse", "nullConf" , "nullI"},
    namesOfTypes = { "H" ,"UV","StrConst", "Z","I"},
    types = { DomH.class, DomUV.class, DomStrConst.class, DomZ.class, DomI.class }
)
public class DynConfDep extends DynamicAnalysis {
  
  InstrScheme instrScheme;
  static File results = new File("dyn_cdep.temp");
  
  
  public InstrScheme getInstrScheme() {
    if (instrScheme != null) return instrScheme;
    instrScheme = new InstrScheme();
    
    
    instrScheme.setAloadReferenceEvent(false, false, true, false, true);
    instrScheme.setAstoreReferenceEvent(false, false, true, false, true);
    instrScheme.setMethodCallEvent(true, false, false, false, false, true); //Super
    //commented out pending re-merging of my dyn instr tree with trunk
    
//    instrScheme.setMethodCallEvent(true, false, false, false, true, false); ///vanilla

    return instrScheme;
  }
  
  @Override
  public void initAllPasses() {
    results.delete();
    System.out.println("starting execution; clearing buffer file.");
  }


  @Override
  public void doneAllPasses() {
    ClassicProject project = ClassicProject.g();
    Pattern readPat = Pattern.compile("([0-9]*) calling .* returns option (.*)-([0-9]+) value=(.+)");
    Pattern usePat = Pattern.compile("([0-9]*) invoking .* ([0-9]+)=(.*)");
    Pattern nullVPat = Pattern.compile("([0-9]*) returns null");
    System.out.println("done all passes; slurping results");

    DomH domH = (DomH) project.getTrgt("H");
    project.runTask(domH);

    DomI domI = (DomI) project.getTrgt("I");
    project.runTask(domI);
    

    DomUV domUV = (DomUV) project.getTrgt("UV");
    project.runTask(domUV);

    DomStrConst domConst = (DomStrConst) project.getTrgt("StrConst");
    project.runTask(domConst);
    
    ProgramRel relConf = (ProgramRel) project.getTrgt("ImarkConf");
    ProgramRel relUse = (ProgramRel) project.getTrgt("dynCUse");
    ProgramRel relNullC = (ProgramRel) project.getTrgt("nullConf");
    ProgramRel relNullI = (ProgramRel) project.getTrgt("nullI");
    relConf.zero();
    relUse.zero();
    relNullC.zero();
    relNullI.zero();
    
    try {
      BufferedReader br = new BufferedReader(new FileReader(results));
      
      String s = null;
      while( (s = br.readLine()) != null) {
        Matcher m = readPat.matcher(s);
        if(m.matches()) {
          int iId = Integer.parseInt(m.group(1));
          Quad q = (Quad) domI.get(iId);
          int hId = domH.indexOf(q);
          
          String cst = m.group(2);
          int cstID = domConst.indexOf(cst);
          if(cstID == -1) {
            cstID = 0;
            System.err.println("UNKNOWN OPTION " + cst);
          }
          
          RegisterFactory.Register targ = Invoke.getDest(q).getRegister();
          int uvID = domUV.indexOf(targ);
          
          String value = m.group(4);
          if(uvID >-1 && hId > -1 ) {
            if("null".equals(value))
              relNullC.add(hId, uvID,cstID);
            //always add it to IMarkConf, so that prim propagation works
            relConf.add(hId, uvID, cstID);
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
              String optName = ConfDefines.pruneName(dep.substring(0, i));
              int iConfID = Integer.parseInt( dep.substring(i+1) );
              Quad q = (Quad) domI.get(iConfID);
              int hId = domH.indexOf(q);
              int cstID = domConst.indexOf(optName);
              if(cstID == -1)
                cstID = 0;

              relUse.add(iId, zId, cstID,hId);
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
    } catch(IOException e) {
      e.printStackTrace();
    }
    relConf.save();
    relUse.save();
    relNullC.save();
    relNullI.save();
  }


}

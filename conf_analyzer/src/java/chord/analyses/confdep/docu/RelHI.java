package chord.analyses.confdep.docu;


import joeq.Compiler.Quad.Quad;
import chord.doms.DomH;
import chord.doms.DomI;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;


@Chord(
    name = "HI",
    sign = "H0,I0:H0_I0"
  )
public class RelHI extends ProgramRel {
  public void fill() {
    DomH domH = (DomH) doms[0];
    DomI domI = (DomI) doms[1];
    int numA = domH.getLastRealIdx() + 1;
//    int numH = domH.size();
    for (int hIdx = 1; hIdx < numA; hIdx++) {
      Object h = domH.get(hIdx);
      if(h instanceof Quad) {
        int iIdx = domI.indexOf(h);
        if(iIdx > -1)
          add(hIdx,iIdx);
      }
    }
  }

}

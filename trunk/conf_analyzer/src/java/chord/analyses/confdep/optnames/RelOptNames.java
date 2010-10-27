package chord.analyses.confdep.optnames;

import java.util.Map;
import joeq.Compiler.Quad.Quad;
import chord.analyses.confdep.ConfDefines;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(name="OptNames",
  sign = "Opt0,I0"
 )
public class RelOptNames extends ProgramRel {
  
  @Override
  public void fill() {
    
    for( Map.Entry<Quad, String> e: DomOpts.optSites.entrySet()) {
      String prefix = ConfDefines.optionPrefix(e.getKey());

      super.add(prefix+ e.getValue(),e.getKey());
    }
  }

}

package chord.analyses.confdep.optnames;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.confdep.ConfDefines;
import chord.bddbddb.Rel.RelView;
import chord.doms.DomH;
import chord.doms.DomZZ;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.OutDirUtils;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

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

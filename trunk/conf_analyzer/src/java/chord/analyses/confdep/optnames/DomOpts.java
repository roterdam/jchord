package chord.analyses.confdep.optnames;

import java.util.Map;
import joeq.Compiler.Quad.Quad;
import chord.analyses.confdep.ConfDefines;
import chord.analyses.confdep.ConfDeps;
import chord.doms.DomH;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

@Chord(name = "Opt",
  consumes = { "VH", "VConstFlow", "CnfNodeSucc", "confOpts", "confOptLen", "confOptName"}
)
public class DomOpts extends ProgramDom<String> {
  
  static Map<Quad,String> optSites= null;
  public static final String NONE = "UNKNOWN";
  
  public void fill() {
    System.out.println("constructing opts domain");
//    super.init();
    getOrAdd(NONE);
    ClassicProject project = ClassicProject.g();
    DomH domH = (DomH) project.getTrgt("H");

    if(optSites == null)
      optSites = ConfDeps.dumpRegexes("conf_regexes.txt", "confOpts", "confOptLen", "confOptName", domH);
    for( Map.Entry<Quad, String> e: optSites.entrySet()) {
      String prefix = ConfDefines.optionPrefix(e.getKey());
      getOrAdd(prefix + e.getValue());
    }
  }

}

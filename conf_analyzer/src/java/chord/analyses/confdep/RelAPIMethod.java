package chord.analyses.confdep;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.doms.DomI;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "APIMethod",
    sign = "I0:I0"
  )
public class RelAPIMethod extends ProgramRel implements IInvokeInstVisitor {
  DomI domI;
//  DomV domV;
  jq_Method method;
  public void init() {
    domI = (DomI) doms[0];
 //   domV = (DomV) doms[1];
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }
  
  public static final boolean isAPI(String classname, String methname) {
    return classname.startsWith("java") && (!classname.startsWith("java.io.") || classname.equals("java.io.File"));
  }

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
    if(isAPI(classname, methname)) {
//    if(classname.startsWith("java.lang.") || classname.startsWith("java.net.") //|| classname.startsWith("java.util.") 
//         || classname.equals("java.io.File")) { //any string op 
      int iIdx = domI.indexOf(q);
      super.add(iIdx);
    }
  }
}

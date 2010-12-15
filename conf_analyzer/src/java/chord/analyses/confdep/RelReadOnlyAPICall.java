package chord.analyses.confdep;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import chord.analyses.invk.DomI;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "APIReadOnly",
    sign = "I0:I0"
  )
public class RelReadOnlyAPICall extends ProgramRel implements IInvokeInstVisitor {
  DomI domI;
//DomV domV;
jq_Method method;
public void init() {
  domI = (DomI) doms[0];
//   domV = (DomV) doms[1];
}

public void visit(jq_Class c) { }
public void visit(jq_Method m) {
  method = m;
}

@Override
public void visitInvokeInst(Quad q) {
  jq_Method meth = Invoke.getMethod(q).getMethod();
  String classname = meth.getDeclaringClass().getName();
  String methname = meth.getName().toString();
  if(methname.equals("equals") || methname.equals("compareTo") ||
  		classname.startsWith("joeq") || classname.startsWith("net.sf.bddb")) {
    int iIdx = domI.indexOf(q);
    super.add(iIdx);
  }
  

}

}

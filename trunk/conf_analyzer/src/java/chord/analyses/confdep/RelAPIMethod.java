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
  
  /*
   * Some notes:
   *   With subtype rule, but not collection-sensitivity
   *    URI is bad. But the rest of the API is fine, including java.lang.reflect
   *
   */
  public static final boolean isAPI(String classname, String methname) {
    if(ConfDefines.isConf(classname, methname))
      return false;
    
    //compareTo and equals should taint return value, only
    //that's handled in primRefDep for control dependencies
//    if(methname.equals("compareTo") || methname.equals("equals") || methname.equals("hashCode"))
//      return false;

    if(classname.equals("java.lang.Thread"))
    	return false;
    if(classname.startsWith("java.lang") && methname.equals("newInstance"))
    	return false;
    
    	/*    if(classname.equals("java.net.URI"))
        return (methname.equals("<init>") || methname.startsWith("get")
            || methname.startsWith("to")
            || methname.equals("resolve")
            || methname.equals("normalize")
            || methname.equals("create"));*/
      //apparently resolve is the dangerous method?
    
    if(classname.equals("org.apache.hadoop.fs.Path"))
      return true;
    if(classname.startsWith("joeq") || classname.startsWith("net.sf.bddb")) //for analyzing jchord itself
    	return true;
    
    return classname.startsWith("java") 
    && (!classname.startsWith("java.io") || classname.equals("java.io.File"));//io is mostly bad, File is ok

  }

  @Override
  public void visitInvokeInst(Quad q) {
    jq_Method meth = Invoke.getMethod(q).getMethod();
    String classname = meth.getDeclaringClass().getName();
    String methname = meth.getName().toString();
    if(isAPI(classname, methname)) {
      int iIdx = domI.indexOf(q);
      super.add(iIdx);
    }
  }
}

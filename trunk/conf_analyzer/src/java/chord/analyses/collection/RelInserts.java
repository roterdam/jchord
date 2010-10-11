package chord.analyses.collection;

import joeq.Class.jq_Class;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.doms.DomI;
import chord.doms.DomV;
import chord.program.visitors.IInvokeInstVisitor;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;

@Chord(
    name = "IInsert",
    sign = "I0,V0,V1:I0_V0_V1"
  )
public class RelInserts extends ProgramRel implements IInvokeInstVisitor {
  
  DomI domI;
  DomV domV;
  jq_Method method;
  jq_Type OBJ_T;
  public void init() {
    domI = (DomI) doms[0];
    domV = (DomV) doms[1];
    OBJ_T = jq_Type.parseType("java.lang.Object");
    RelINewColl.tInit();
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }

  @Override
  public void visitInvokeInst(Quad q) {
    ParamListOperand argList = Invoke.getParamList(q);
    int args = argList.length();
    if (args > 1) {

      Register thisObj = Invoke.getParam(q, 0).getRegister();
      int thisObjID = domV.indexOf(thisObj);
      jq_Method meth = Invoke.getMethod(q).getMethod();
      jq_Class cl = meth.getDeclaringClass();
      String mname = meth.getName().toString();
      
      if(!meth.isStatic() && RelINewColl.isCollectionType(cl)) {
        if(mname.equals("add") || mname.equals("offer") || mname.equals("put") || mname.equals("set") ) {
          for(int i =1; i< args; ++i) {
            RegisterOperand op = argList.get(i);
            if(op.getType().isReferenceType()) {
              int iID = domI.indexOf(q);
              int idx1 = domV.indexOf(op.getRegister());
              super.add(iID,thisObjID, idx1);
            }
          }
        }
      }
    }
  }
  

}

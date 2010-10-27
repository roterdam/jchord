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

/**
 * contains (i,v2,cv) where v2 is a variable pointing to a new collection
 * based on the collection pointed to by cv
 * 
 * @author asrabkin
 *
 */
@Chord(
    name = "INewColl",
    sign = "I0,V0,V1:I0_V0_V1"
  )
public class RelINewColl extends ProgramRel implements IInvokeInstVisitor {
  
  DomI domI;
  DomV domV;
  jq_Method method;
//  static jq_Type COLLECTION, MAP, ITERATOR;
  static final String[] colTypeNames = {"java.util.Collection","java.util.Map","java.util.Iterator",
    "java.util.concurrent.atomic.AtomicReference"};
  static jq_Type[] collTypes;
  public void init() {
    domI = (DomI) doms[0];
    domV = (DomV) doms[1];
    tInit();

  }
  
  static void tInit() {
    if(collTypes != null)
      return;
    
    collTypes = new jq_Type[colTypeNames.length];
    for(int i =0; i < colTypeNames.length; ++i) {
      collTypes[i] = jq_Type.parseType(colTypeNames[i]);
      collTypes[i].prepare();
    }
  }
  
  static boolean isCollectionType(jq_Type cl) {

    if(cl.getName().contains("Propert") || cl.getName().contains("Config"))
      return false;
    for(jq_Type t: collTypes)
      if(cl.isSubtypeOf(t)) {
        return true;
      }
    return false;
  }

  public void visit(jq_Class c) { }
  public void visit(jq_Method m) {
    method = m;
  }

  @Override
  public void visitInvokeInst(Quad q) {
    ParamListOperand argList = Invoke.getParamList(q);
    RegisterOperand vo = Invoke.getDest(q);
    int args = argList.length();

    //was args > 1
    
    if (vo != null && args > 0) {
      Register v = vo.getRegister();
      jq_Method meth = Invoke.getMethod(q).getMethod();
      jq_Class cl = meth.getDeclaringClass();

      if (v.getType().isReferenceType()) {
//        String mname = meth.getName().toString();

//        System.out.println("INewColl inspecting call to " + mname);
        
        if( isCollectionType(cl) && !meth.isStatic() ) {
          jq_Type destType = meth.getReturnType();
//          System.out.println("INewColl saw call to " + meth + " returning " + destType);
          if(isCollectionType(destType)) {
            Register thisObj = Invoke.getParam(q, 0).getRegister();
            int iID = domI.indexOf(q);
            int vID = domV.indexOf(v);
            int thisID = domV.indexOf(thisObj);
            super.add(iID, vID, thisID);
          }
        }
      }
      
    }
  }
}

package chord.analyses.confdep;

import java.util.Map;
import javassist.*;
import javassist.expr.*;
import chord.instr.CoreInstrumentor;
import chord.instr.Instrumentor;
import chord.runtime.EventHandler;
import chord.util.ChordRuntimeException;

/**
 * Code to insert calls to the DynConfDepRuntime
 *
 */
public class ArgMonInstr extends Instrumentor {
  
  protected static final String methodCallAftEventCallSuper = "chord.analyses.confdep.DynConfDepRuntime." + "methodCallAftEventSuper(";


  public ArgMonInstr(Map<String, String> argsMap) {
    super(argsMap);
//    EventFormat ef = new EventFormat();
//    instrScheme.setAloadReferenceEvent(false, false, true, false, true);
//    instrScheme.setAstoreReferenceEvent(false, false, true, false, true);
  }
  
  public void edit(ConstructorCall c)  {
    try {
      String aftInstr = "";
      int iId =  set(Imap, c) ;
      String o =  "$0" ;

      aftInstr += methodCallAftEventCallSuper +iId + ",\""+c.getClassName()+"\",\"<init>\",($w)$_,null,$args);";        
      c.replace("{ $_ = $proceed($$); " +  aftInstr + " }");
    } catch (CannotCompileException ex) {
      throw new ChordRuntimeException(ex);
    } 
  }
  
  public void edit(MethodCall e) {
    try {
  
      String befInstr = "";
      String aftInstr = "";
      // Part 1: add METHOD_CALL event if present
  
      int iId =  set(Imap, e) ;
      String o = "$0";
      befInstr += methodCallBefEventCall + iId + "," + o + ");";
  
      if(Modifier.isStatic(e.getMethod().getModifiers())) { 
        aftInstr += methodCallAftEventCallSuper +iId + ",\""+e.getClassName()+"\",\""+ 
        e.getMethodName() + "\",($w)$_,null,$args);";
      } else {
        aftInstr += methodCallAftEventCallSuper + iId+",\""+e.getClassName()+"\",\""+ 
        e.getMethodName() + "\",($w)$_,$0,$args);"; 
      }
      e.replace("{ $_ = $proceed($$); " +  aftInstr + " }");
      return;
    } catch (CannotCompileException ex) {
      throw new ChordRuntimeException(ex);
    } catch (NotFoundException ex) {
      throw new ChordRuntimeException(ex);
    }
  }
}

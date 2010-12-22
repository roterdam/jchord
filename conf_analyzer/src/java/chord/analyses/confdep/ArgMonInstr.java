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
  
  protected static final String methodCallAftEventCallSuper = DynConfDepRuntime.class.getCanonicalName() + ".methodCallAftEventSuper(";


  public ArgMonInstr(Map<String, String> argsMap) {
    super(argsMap);
   // assert aloadReferenceEvent.present();
   // assert astoreReferenceEvent.present();
    if(!aloadReferenceEvent.present() || !astoreReferenceEvent.present()) {
      System.err.println("ArgMonInstr expected load and store events");
      System.exit(-1);
    }
  }
  
  /*
   * I think this is actually just for nested ctor calls, and therefore almost useless
   */
  @Override
  public void edit(ConstructorCall c)  {
    System.out.println("editing a constructor call, method is " + c.getClassName());
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
  
  @Override
  public void edit(NewExpr e) throws CannotCompileException {
/*
    if (newAndNewArrayEvent.present()) {
      String instr1, instr2;
      if (newAndNewArrayEvent.hasObj()) {
        // instrument hId regardless of whether the client wants it
        int hId = set(Hmap, e);
        instr1 = befNewEventCall + hId + ");";
        instr2 = aftNewEventCall + hId + ",$_);";
      } else {
        int hId = newAndNewArrayEvent.hasLoc() ?
          set(Hmap, e) : EventHandler.MISSING_FIELD_VAL;
        instr1 = newEventCall + hId + ");";
        instr2 = "";
      }
      e.replace("{ " + instr1 + " $_ = $proceed($$); " + instr2 + " }");
    } else if(methodCallEvent.isSuperAft()) { */
      int iId = set(Imap, e.indexOfOriginalBytecode() );
      e.replace("{ $_ = $proceed($$); " + methodCallAftEventCallSuper + // "-2,\""+
           iId +",\""+ 
        e.getClassName()+"\",\"<init>\",($w)$_,null,$args); }");
//    }
  }
  
  public void edit(MethodCall e) {
    try {
  
      String befInstr = "";
      String aftInstr = "";
      // Part 1: add METHOD_CALL event if present
  
      int iId =  set(Imap, e) ;
      String o = "$0";
      befInstr += befMethodCallEventCall + iId + "," + o + ");";
  
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

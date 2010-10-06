package chord.program.reflect;

import java.util.Map;
import chord.instr.CoreInstrumentor;
import javassist.expr.MethodCall;
import javassist.CannotCompileException;
import javassist.CtConstructor;
import chord.program.MethodElem;

public class ReflectInstrumentor extends CoreInstrumentor {
	private static final String eventStr =
		ReflectEventHandler.class.getName() + ".reflectEvent(";
	public ReflectInstrumentor(Map<String, String> argsMap) {
		super(argsMap);
	}
	@Override
	public void edit(MethodCall e) {
		String cName = e.getClassName();
		if (!cName.equals("java.lang.Class")) 
			return;
		String mName = e.getMethodName();
		String mSign = e.getSignature();
		String cName2;
		byte eventKind;
		if (mName.equals("forName") &&
				mSign.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
			eventKind = ReflectEventKind.CLS_FOR_NAME_CALL;
			cName2 = "$1";
		} else if (mName.equals("newInstance") &&
				mSign.equals("()Ljava/lang/Object;")) {
			eventKind = ReflectEventKind.OBJ_NEW_INST_CALL;
			cName2 = "$0.getName()";
/*
		} else if (...) {
			eventKind = ReflectEventKind.CON_NEW_INST_CALL;
		} else if (...) {
			eventKind = ReflectEventKind.ARY_NEW_INST_CALL;
*/
		} else 
			return;
		String mElem = getMethodElem(e);
		String instr = "{ " + eventStr + eventKind + "," +
			mElem + "," + cName2 + "); $_ = $proceed($$); }";
		try {
			e.replace(instr);
		} catch (CannotCompileException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	private String getMethodElem(MethodCall e) {
		int bci = e.indexOfOriginalBytecode();
		String mName;
        if (currentMethod instanceof CtConstructor) {
            mName = ((CtConstructor) currentMethod).isClassInitializer() ?
				"<clinit>" : "<init>";
        } else {
            mName = currentMethod.getName();
		}
		String mSign = currentMethod.getSignature();
		String cName = currentClass.getName();
		MethodElem me = new MethodElem(bci, mName, mSign, cName);
		return "\"" + me.toString() + "\"";
	}
}


package chord.analyses.mantis;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import chord.project.analyses.JavaAnalysis;
import chord.instr.OfflineTransformer;
import chord.project.Chord;
import chord.project.Config;
import chord.project.Messages;
import chord.instr.JavassistPool;

import javassist.NotFoundException;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

@Chord(
	name="mantis-java"
)
public class MantisAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		MantisInstrumentor instrumentor = new MantisInstrumentor();
		OfflineTransformer transformer = new OfflineTransformer(instrumentor);
		transformer.run();

		String mainClassName = Config.mainClassName;
		JavassistPool pool = instrumentor.getPool();
		CtClass mainClass = null;
		try {
			mainClass = pool.get(mainClassName);
		} catch (NotFoundException ex) {
			Messages.fatal(ex);
		}
		CtMethod mainMethod = null;
		try {
			mainMethod = mainClass.getMethod("main", "([Ljava/lang/String;)V");
		} catch (NotFoundException ex) {
			Messages.fatal(ex);
		}
		if (mainClass.isFrozen())
			mainClass.defrost();

		Map<String, Set<FldInfo>> clsNameToFldInfosMap =
			instrumentor.getClsNameToFldInfosMap();
		for (String cName : clsNameToFldInfosMap.keySet()) {
			Set<FldInfo> fInfos = clsNameToFldInfosMap.get(cName);
			String instr = "";
			for (FldInfo fInfo : fInfos) {
				String fName = cName + "." + fInfo.fldName;
				String javaPos = fInfo.javaPos;
				instr += "System.out.println(\"" + fName + " " + javaPos +
					" = \" + " + fName + ");";
			}
			String mName = "print_" + cName.replace('.', '_');
			try {
				CtMethod m = CtNewMethod.make(
					"private static void " + mName + "() { " + instr + " }",
					mainClass);
				mainClass.addMethod(m);
				mainMethod.insertAfter(mName + "();");
			} catch (CannotCompileException ex) {
				Messages.fatal(ex);
			}
		}
		String outDir = transformer.getOutDir(mainClassName);
		try {
			mainClass.writeFile(outDir);
		} catch (IOException ex) {
			Messages.fatal(ex);
		} catch (NotFoundException ex) {
			Messages.fatal(ex);
		} catch (CannotCompileException ex) {
			Messages.fatal(ex);
		}
	}
}


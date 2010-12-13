package chord.analyses.mantis;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
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

		String featureListFileName = Config.outRel2AbsPath("chord.feature.list.file", "feature_list.txt");
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(featureListFileName));
		} catch (IOException ex) {
			Messages.fatal(ex);
		}
		String mainMethodInstr = "";
		Map<String, Set<FldInfo>> clsNameToFldInfosMap = instrumentor.getClsNameToFldInfosMap();
		for (String cName : clsNameToFldInfosMap.keySet()) {
			Set<FldInfo> fldInfos = clsNameToFldInfosMap.get(cName);
			String instr = "";
			for (FldInfo fldInfo : fldInfos) {
				String fldBaseName = cName + "." + fldInfo.fldBaseName;
				String javaPos = fldInfo.javaPos;
				switch (fldInfo.kind) {
				case CTRL:
				{
					String fldName1 = fldBaseName + "_bef";
					String fldName2 = fldBaseName + "_aft";
					String fldName3 = fldBaseName + "_bef_sub_aft";
					instr += "out.println(\"" + fldName1 + " " + javaPos + " = \" + " + fldName1 + ");";
					instr += "out.println(\"" + fldName2 + " " + javaPos + " = \" + " + fldName2 + ");";
					instr += "out.println(\"" + fldName3 + " " + javaPos + " = \" + (" + fldName1 + " - " + fldName2 + "));";
					writer.println(fldName1);
					writer.println(fldName2);
					writer.println(fldName3);
					break;
				}
				case DATA_BOOL:
				{
					break;
				}
				case DATA_LONG:
				{
					String fldName1 = fldBaseName + "_sum";
					String fldName2 = fldBaseName + "_freq";
					String fldName3 = fldBaseName + "_avg";
					instr += "out.println(\"" + fldName1 + " " + javaPos + " = \" + " + fldName1 + ");";
					instr += "out.println(\"" + fldName2 + " " + javaPos + " = \" + " + fldName2 + ");";
					instr += "out.println(\"" + fldName3 + " " + javaPos + " = \" + (" + fldName2 + " == 0 ? 0 : " + fldName1 + "/" + fldName2 + "));";
					writer.println(fldName1);
					writer.println(fldName2);
					writer.println(fldName3);
					break;
				}
				case DATA_DOUBLE:
				{
					String fldName1 = fldBaseName + "_sum";
					String fldName2 = fldBaseName + "_freq";
					String fldName3 = fldBaseName + "_avg";
					instr += "out.println(\"" + fldName1 + " " + javaPos + " = \" + " + fldName1 + ");";
					instr += "out.println(\"" + fldName2 + " " + javaPos + " = \" + " + fldName2 + ");";
					instr += "out.println(\"" + fldName3 + " " + javaPos + " = \" + (" + fldName2 + " == 0 ? 0 : " + fldName1 + "/" + fldName2 + "));";
					writer.println(fldName1);
					writer.println(fldName2);
					writer.println(fldName3);
					break;
				}
				}
			}
			String mName = "print_" + cName.replace('.', '_');
			try {
				CtMethod m = CtNewMethod.make("private static void " + mName +
					"(java.io.PrintWriter out) { " + instr + " }", mainClass);
				mainClass.addMethod(m);
				mainMethodInstr += mName + "(out);";
			} catch (CannotCompileException ex) {
				Messages.fatal(ex);
			}
		}
		writer.close();

		String featureValsFileName = Config.outRel2AbsPath("chord.feature.vals.file", "feature_vals.txt");
		try {
			mainMethod.insertAfter(
				"try { " +
					"java.io.PrintWriter out = new java.io.PrintWriter(" +
						"new java.io.FileWriter(\"" + featureValsFileName + "\")); " +
					 mainMethodInstr +
					" out.close(); " +
				"} catch (java.io.IOException ex) { " +
					"ex.printStackTrace(); " +
					"System.exit(1); " +
				"}");
		} catch (CannotCompileException ex) {
			Messages.fatal(ex);
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


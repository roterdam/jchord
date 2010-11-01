package chord.analyses.mantis;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import chord.instr.CoreInstrumentor;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.doms.DomM;
import chord.project.Messages;
import chord.project.Config;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.expr.MethodCall;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import javassist.CtConstructor;
import javassist.CtBehavior;
import javassist.Modifier;

import gnu.trove.TIntObjectHashMap;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Util.Templates.ListIterator;

public class MantisInstrumentor extends CoreInstrumentor {
    private static final int maxFldsPerMantisClass = 8000;
    private static final int maxFldsPerMantisPrintMethod = 1000;
	private final DomM domM;
	// index in domain M of currently instrumented method
	private int mIdx;
	private final List<FldInfo> fldInfosList = new ArrayList<FldInfo>();
	private final TIntObjectHashMap<String> bciToInstrMap = new TIntObjectHashMap<String>();
	private String currClsNameStr;
	private CtClass[] mantisClasses;
    private int currMantisClassId;
    private int numFldsInCurrMantisClass;

    private CtClass createClass(String name) {
        return getPool().getPool().makeClass(name);
    }

	public MantisInstrumentor() {
		super(null);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
	}

    private void ensure(int nf) {
        if (mantisClasses == null)
            mantisClasses = new CtClass[2];
        else if (numFldsInCurrMantisClass + nf > maxFldsPerMantisClass) {
            int nc = mantisClasses.length;
            if (currMantisClassId == nc - 1) {
                CtClass[] newMantisClasses = new CtClass[nc * 2];
                System.arraycopy(mantisClasses, 0, newMantisClasses, 0, nc);
                mantisClasses = newMantisClasses;
            }
            ++currMantisClassId;
            numFldsInCurrMantisClass = 0;
        } else
            return;
        CtClass mantisClass = createClass("Mantis" + currMantisClassId);
        mantisClasses[currMantisClassId] = mantisClass;
    }

	@Override
	public boolean isExplicitlyExcluded(String cName) {
		if (cName.startsWith("java.") ||
				cName.startsWith("javax.") ||
				cName.startsWith("com.sun.") ||
				cName.startsWith("com.ibm.") ||
				cName.startsWith("sun.") ||
				cName.startsWith("org.apache.harmony."))
			return true;
		if (cName.startsWith("Mantis"))
			Messages.fatal("Instrumenting Mantis class itself.");
		return super.isExplicitlyExcluded(cName);
	}

	@Override
	public CtClass edit(CtClass clazz) throws CannotCompileException {
		currClsNameStr = clazz.getName().replace('.', '_').replace('$', '_');
		return super.edit(clazz);
	}

	@Override
	public void edit(CtBehavior method) throws CannotCompileException {
        int mods = method.getModifiers();
        if (Modifier.isNative(mods) || Modifier.isAbstract(mods))
            return;
        String mName;
        if (method instanceof CtConstructor)
            mName = ((CtConstructor) method).isClassInitializer() ? "<clinit>" : "<init>";
        else
            mName = method.getName();
        String mDesc = method.getSignature();
        String cName = currentClass.getName();
        String mSign = mName + ":" + mDesc + "@" + cName;
        jq_Method meth = Program.g().getMethod(mSign);
        if (meth == null) 
            return;
		mIdx = domM.indexOf(meth);
		assert (mIdx >= 0);
		ControlFlowGraph cfg = meth.getCFG();
		bciToInstrMap.clear();
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator(); it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			int n = bb.size();
			for (int i = 0; i < n; i++) {
				Quad q = bb.getQuad(i);
				Operator op = q.getOperator();
				if (op instanceof IntIfCmp)
					generateIntIfCmpInstr(q);
           }
		}
		super.edit(method);
	}
		
	private void generateIntIfCmpInstr(Quad q) throws CannotCompileException {
		int bci = q.getBCI();
		assert (bci >= 0);
		String fldBaseName = getBaseName(bci);
		String befFldName = fldBaseName + "_bef";
		String aftFldName = fldBaseName + "_aft";
		String javaPos = "(" + q.toJavaLocStr() + ")";
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.CTRL, mantisClassName + fldBaseName, javaPos));
		CtField befFld = CtField.make("public static int " + befFldName + ";", mantisClass);
		CtField aftFld = CtField.make("public static int " + aftFldName + ";", mantisClass);
		mantisClass.addField(befFld);
		mantisClass.addField(aftFld);
        numFldsInCurrMantisClass += 2;
		String befInstr = mantisClassName + befFldName + "++;";
		String aftInstr = mantisClassName + aftFldName + "++;";
		putInstrBefBCI(bci, befInstr);
		putInstrBefBCI(bci + 3, aftInstr);
	}

	private void putInstrBefBCI(int bci, String instr) {
        String s = bciToInstrMap.get(bci);
        bciToInstrMap.put(bci, (s == null) ? instr : instr + s);
	}

	private String getBaseName(MethodCall e) {
		int bci = e.indexOfOriginalBytecode();
		return getBaseName(bci);
	}

	private String getBaseName(int bci) {
		return currClsNameStr + "_b" + bci + "m" + mIdx;
	}

	private static String getJavaPos(MethodCall e) {
		return "(" + e.getFileName() + ":" + e.getLineNumber() + ")";
	}

	private String getBoolInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e);
		String truFldName = fldBaseName + "_true";
		String flsFldName = fldBaseName + "_false";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_BOOL, mantisClassName + fldBaseName, javaPos));
		CtField truFld = CtField.make("public static int " + truFldName + ";", mantisClass);
		CtField flsFld = CtField.make("public static int " + flsFldName + ";", mantisClass);
		mantisClass.addField(truFld);
		mantisClass.addField(flsFld);
        numFldsInCurrMantisClass += 2;
		String instr = "if ($_) " + mantisClassName + truFldName + "++; else " +
            mantisClassName + flsFldName + "++;";
		return instr;
	}

	private String getLongInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e) + "_long";
		String sumFldName = fldBaseName + "_sum";
		String frqFldName = fldBaseName + "_freq";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_LONG, mantisClassName + fldBaseName, javaPos));
		CtField sumFld = CtField.make("public static long " + sumFldName + ";", mantisClass);
		CtField frqFld = CtField.make("public static int  " + frqFldName + ";", mantisClass);
		mantisClass.addField(sumFld);
		mantisClass.addField(frqFld);
        numFldsInCurrMantisClass += 2;
		String instr = mantisClassName + sumFldName + " += $_; " +
            mantisClassName + frqFldName + "++;";
		return instr;
	}

	private String getDoubleInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e) + "_double";
		String sumFldName = fldBaseName + "_sum";
		String frqFldName = fldBaseName + "_freq";
		String javaPos = getJavaPos(e);
        ensure(2);
        CtClass mantisClass = mantisClasses[currMantisClassId];
        String mantisClassName = "Mantis" + currMantisClassId + ".";
		fldInfosList.add(new FldInfo(FldKind.DATA_DOUBLE, mantisClassName + fldBaseName, javaPos));
		CtField sumFld = CtField.make("public static double " + sumFldName + ";", mantisClass);
		CtField frqFld = CtField.make("public static int " + frqFldName + ";", mantisClass);
		mantisClass.addField(sumFld);
		mantisClass.addField(frqFld);
        numFldsInCurrMantisClass += 2;
		String instr = mantisClassName + sumFldName + " += $_; " +
            mantisClassName + frqFldName + "++;";
		return instr;
	}

	@Override
	public String insertBefore(int pos) {
		return bciToInstrMap.get(pos);
	}

	@Override
	public void edit(MethodCall e) throws CannotCompileException {
		CtClass retType = null;
		try {
			retType = e.getMethod().getReturnType();
		} catch (NotFoundException ex) {
			throw new CannotCompileException(ex);
		}
		if (!retType.isPrimitive())
			return;
		String instr = null;
		if (retType == CtClass.booleanType)
			instr = getBoolInvkInstr(e);
		else if (retType == CtClass.floatType || retType == CtClass.doubleType)
			instr = getDoubleInvkInstr(e);
		else if (retType != CtClass.voidType)
			instr = getLongInvkInstr(e);
		if (instr != null)
			e.replace("{ $_ = $proceed($$); " + instr + " }"); 
	}

	public void done() {
        CtClass mantisPrinterClass = createClass("MantisPrinter");
        try {
            CtField ctrlOutFld = CtField.make("public static java.io.PrintWriter ctrlOut;", mantisPrinterClass);
            CtField boolOutFld = CtField.make("public static java.io.PrintWriter boolOut;", mantisPrinterClass);
            CtField longOutFld = CtField.make("public static java.io.PrintWriter longOut;", mantisPrinterClass);
            CtField realOutFld = CtField.make("public static java.io.PrintWriter realOut;", mantisPrinterClass);
            mantisPrinterClass.addField(ctrlOutFld);
            mantisPrinterClass.addField(boolOutFld);
            mantisPrinterClass.addField(longOutFld);
            mantisPrinterClass.addField(realOutFld);
        } catch (CannotCompileException ex) {
            Messages.fatal(ex);
        }

        String ctrlFeatureNameFileName = "ctrl_feature_name.txt";
        String boolFeatureNameFileName = "bool_feature_name.txt";
        String longFeatureNameFileName = "long_feature_name.txt";
        String realFeatureNameFileName = "real_feature_name.txt";
        PrintWriter ctrlWriter = null;
        PrintWriter boolWriter = null;
        PrintWriter longWriter = null;
        PrintWriter realWriter = null;
        try {
            ctrlWriter = new PrintWriter(new FileWriter(ctrlFeatureNameFileName));
            boolWriter = new PrintWriter(new FileWriter(boolFeatureNameFileName));
            longWriter = new PrintWriter(new FileWriter(longFeatureNameFileName));
            realWriter = new PrintWriter(new FileWriter(realFeatureNameFileName));
        } catch (IOException ex) {
            Messages.fatal(ex);
        }

		String globalInstr = "", localInstr = "";
        int numFldInfos = fldInfosList.size();
		for (int i = 0, currMantisPrintMethodId = 0; i < numFldInfos; i++) {
            FldInfo fldInfo = fldInfosList.get(i);
            String fldBaseName = fldInfo.fldBaseName;
            String javaPos = fldInfo.javaPos;
            switch (fldInfo.kind) {
                case CTRL:
                {
                    String fldName1 = fldBaseName + "_bef";
                    String fldName2 = fldBaseName + "_aft";
                    localInstr += "ctrlOut.println(" + fldName1 + ");";
                    localInstr += "ctrlOut.println(" + fldName2 + ");";
                    ctrlWriter.println(fldName1 + " " + javaPos);
                    ctrlWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_BOOL:
                {
                    String fldName1 = fldBaseName + "_true";
                    String fldName2 = fldBaseName + "_false";
                    localInstr += "boolOut.println(" + fldName1 + ");";
                    localInstr += "boolOut.println(" + fldName2 + ");";
                    boolWriter.println(fldName1 + " " + javaPos);
                    boolWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_LONG:
                {
                    String fldName1 = fldBaseName + "_sum";
                    String fldName2 = fldBaseName + "_freq";
                    localInstr += "longOut.println(" + fldName1 + ");";
                    localInstr += "longOut.println(" + fldName2 + ");";
                    longWriter.println(fldName1 + " " + javaPos);
                    longWriter.println(fldName2 + " " + javaPos);
                    break;
                }
                case DATA_DOUBLE:
                {
                    String fldName1 = fldBaseName + "_sum";
                    String fldName2 = fldBaseName + "_freq";
                    localInstr += "realOut.println(" + fldName1 + ");";
                    localInstr += "realOut.println(" + fldName2 + ");";
                    realWriter.println(fldName1 + " " + javaPos);
                    realWriter.println(fldName2 + " " + javaPos);
                    break;
                }
            }
            if (i % maxFldsPerMantisPrintMethod == 0 || i == numFldInfos - 1) {
                String mName = "print" + currMantisPrintMethodId;
                try {
                    CtMethod m = CtNewMethod.make("private static void " + mName +
                           "() { " + localInstr + " }", mantisPrinterClass);
                    mantisPrinterClass.addMethod(m);
                } catch (CannotCompileException ex) {
                    Messages.fatal(ex);
                }
                localInstr = "";
                globalInstr += mName + "();";
                currMantisPrintMethodId++;
            }
        }
        ctrlWriter.close();
        boolWriter.close();
        longWriter.close();
        realWriter.close();

        String ctrlFeatureDataFileName = "ctrl_feature_data.txt";
        String boolFeatureDataFileName = "bool_feature_data.txt";
        String longFeatureDataFileName = "long_feature_data.txt";
        String realFeatureDataFileName = "real_feature_data.txt";
        String outDir = Config.userClassesDirName;
        try {
            for (int i = 0; i < mantisClasses.length; i++) {
                CtClass c = mantisClasses[i];
                if (c == null)
                    break;
                c.writeFile(outDir);
            }
			CtMethod doneMethod = CtNewMethod.make("public static void done() { " +
                "try { " +
                    "ctrlOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + ctrlFeatureDataFileName + "\")); " +
                    "boolOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + boolFeatureDataFileName + "\")); " +
                    "longOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + longFeatureDataFileName + "\")); " +
                    "realOut = new java.io.PrintWriter(" +
                        "new java.io.FileWriter(\"" + realFeatureDataFileName + "\")); " +
                     globalInstr +
                    " ctrlOut.close(); " +
                    " boolOut.close(); " +
                    " longOut.close(); " +
                    " realOut.close(); " +
                "} catch (java.io.IOException ex) { " + 
                    "ex.printStackTrace(); " +
                    "System.exit(1); " +
                "}" +
			"}", mantisPrinterClass);
			mantisPrinterClass.addMethod(doneMethod);
			CtClass mainClass = getPool().get(Config.mainClassName);
			if (mainClass.isFrozen())
				mainClass.defrost();
            CtMethod mainMethod = mainClass.getMethod("main", "([Ljava/lang/String;)V");
            mainMethod.insertAfter("MantisPrinter.done();");
            mainClass.writeFile(outDir); 
			mantisPrinterClass.writeFile(outDir);
		} catch (NotFoundException ex) {
            Messages.fatal(ex);
        } catch (CannotCompileException ex) {
            Messages.fatal(ex);
        } catch (IOException ex) {
            Messages.fatal(ex);
		}
	}
}


package chord.analyses.mantis;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import chord.instr.CoreInstrumentor;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.doms.DomM;

import javassist.expr.MethodCall;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.CtField;
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
	// map from each class to set of static fields created in it
	private final Map<String, Set<FldInfo>> clsNameToFldInfosMap =
		new HashMap<String, Set<FldInfo>>();
	private final DomM domM;
	private final TIntObjectHashMap<String> bciToInstrMap =
		new TIntObjectHashMap<String>();
	// index in domain M of currently instrumented method
	private int mIdx;
	// static fields created in currently instrumented class
	private Set<FldInfo> fldInfos;

	public MantisInstrumentor() {
		super(null);
		domM = (DomM) ClassicProject.g().getTrgt("M");
		ClassicProject.g().runTask(domM);
	}

	public Map<String, Set<FldInfo>> getClsNameToFldInfosMap() {
		return clsNameToFldInfosMap;
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
		return super.isExplicitlyExcluded(cName);
	}

	@Override
	public CtClass edit(CtClass clazz) throws CannotCompileException {
		fldInfos = new HashSet<FldInfo>();
		clsNameToFldInfosMap.put(clazz.getName(), fldInfos);
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
				if (op instanceof IntIfCmp) {
/*
					BasicBlock target = IntIfCmp.getTarget(q).getTarget();
					List.BasicBlock succs = bb.getSuccessors();
					int n = succs.size();
					assert (n == 2) : " Basic block " + bb + " in method " + meth +
						" has " + n + " successors; expected 2";
*/
					generateIntIfCmpInstr(q);
				}
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
		fldInfos.add(new FldInfo(befFldName, javaPos));
		fldInfos.add(new FldInfo(aftFldName, javaPos));
		CtField befFld = CtField.make("public static int " + befFldName + ";", currentClass);
		CtField aftFld = CtField.make("public static int " + aftFldName + ";", currentClass);
		currentClass.addField(befFld);
		currentClass.addField(aftFld);
		String befInstr = befFldName + "++;";
		String aftInstr = aftFldName + "++;";
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
		return "b" + bci + "m" + mIdx;
	}

	private static String getJavaPos(MethodCall e) {
		return "(" + e.getFileName() + ":" + e.getLineNumber() + ")";
	}

	private String getBoolInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e);
		String truFldName = fldBaseName + "_true";
		String flsFldName = fldBaseName + "_false";
		String javaPos = getJavaPos(e);
		fldInfos.add(new FldInfo(truFldName, javaPos));
		fldInfos.add(new FldInfo(flsFldName, javaPos));
		CtField truFld = CtField.make("public static int " + truFldName + ";", currentClass);
		CtField flsFld = CtField.make("public static int " + flsFldName + ";", currentClass);
		currentClass.addField(truFld);
		currentClass.addField(flsFld);
		String instr = "if ($_) " + truFldName + "++; else " + flsFldName + "++;";
		return instr;
	}

	private String getLongInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e);
		String sumFldName = fldBaseName + "_long_sum";
		String frqFldName = fldBaseName + "_long_freq";
		String javaPos = getJavaPos(e);
		fldInfos.add(new FldInfo(sumFldName, javaPos));
		fldInfos.add(new FldInfo(frqFldName, javaPos));
		CtField sumFld = CtField.make("public static long " + sumFldName + ";", currentClass);
		CtField frqFld = CtField.make("public static int  " + frqFldName + ";", currentClass);
		currentClass.addField(sumFld);
		currentClass.addField(frqFld);
		String instr = sumFldName + " += $_; " + frqFldName + "++;";
		return instr;
	}

	private String getDoubleInvkInstr(MethodCall e) throws CannotCompileException {
		String fldBaseName = getBaseName(e);
		String sumFldName = fldBaseName + "_double_sum";
		String frqFldName = fldBaseName + "_double_freq";
		String javaPos = getJavaPos(e);
		fldInfos.add(new FldInfo(sumFldName, javaPos));
		fldInfos.add(new FldInfo(frqFldName, javaPos));
		CtField sumFld = CtField.make("public static double " + sumFldName + ";", currentClass);
		CtField frqFld = CtField.make("public static int " + frqFldName + ";", currentClass);
		currentClass.addField(sumFld);
		currentClass.addField(frqFld);
		String instr = sumFldName + " += $_; " + frqFldName + "++;";
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
}


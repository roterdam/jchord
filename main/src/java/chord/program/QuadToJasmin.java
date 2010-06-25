package chord.program;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import chord.program.Program;
import chord.project.Properties;
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_Field;
import joeq.Class.jq_Initializer;
import joeq.Class.jq_InstanceField;
import joeq.Class.jq_Member;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_StaticField;
import joeq.Class.jq_Type;
import joeq.Class.jq_Reference.jq_NullType;
import joeq.Compiler.BytecodeAnalysis.BytecodeVisitor;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.BasicBlockTableOperand;
import joeq.Compiler.Quad.Operand.Const4Operand;
import joeq.Compiler.Quad.Operand.Const8Operand;
import joeq.Compiler.Quad.Operand.ConstOperand;
import joeq.Compiler.Quad.Operand.IntValueTableOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.TargetOperand;
import joeq.Compiler.Quad.Operand.TypeOperand;
import joeq.Compiler.Quad.Operator.ALength;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Binary;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Goto;
import joeq.Compiler.Quad.Operator.InstanceOf;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Jsr;
import joeq.Compiler.Quad.Operator.LookupSwitch;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.TableSwitch;
import joeq.Compiler.Quad.Operator.Unary;
import joeq.Compiler.Quad.Operator.ALoad.ALOAD_P;
import joeq.Compiler.Quad.Operator.AStore.ASTORE_P;
import joeq.Compiler.Quad.Operator.IntIfCmp.IFCMP_A;
import joeq.Compiler.Quad.Operator.IntIfCmp.IFCMP_I;
import joeq.Compiler.Quad.Operator.Return.RETURN_A;
import joeq.Compiler.Quad.Operator.Return.RETURN_D;
import joeq.Compiler.Quad.Operator.Return.RETURN_F;
import joeq.Compiler.Quad.Operator.Return.RETURN_I;
import joeq.Compiler.Quad.Operator.Return.RETURN_L;
import joeq.Compiler.Quad.Operator.Return.RETURN_V;
import joeq.Compiler.Quad.Operator.Return.THROW_A;
import joeq.Compiler.Quad.Operator.Special.GET_EXCEPTION;
import joeq.Compiler.Quad.QuadVisitor.EmptyVisitor;
import joeq.Compiler.Quad.RegisterFactory.Register;

import chord.util.IndexSet;

public class QuadToJasmin {
	private static final JasminQuadVisitor visitor = new JasminQuadVisitor();
	private static PrintStream out;

	static void put(String jasminCode){
		out.println(jasminCode);
	}

	static List<String> methodBody = new LinkedList<String>(); 

	static void putInst(String jasminInstCode){
		methodBody.add(jasminInstCode);
	}

	private static boolean filterOut(String className){
		for(String s : Properties.checkExcludeAry){
			if(className.startsWith(s)) return true;
		}

		return false;
	}

	private static void prepareOutputStream(jq_Class c){

		File outFile = new File(Properties.outDirName + "/" +c.getName().replace(".", "/")+".j");
		outFile.getParentFile().mkdirs();

		try {
			if(out != null) out.close();
			out = new PrintStream(outFile);
		} catch (FileNotFoundException e) {
			System.out.println("## path : " + outFile.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public static void main(String[] args) {

		Program program = Program.getProgram();
		IndexSet<jq_Reference> classes = program.getClasses();		
		for (jq_Reference r : classes) {

			if (r instanceof jq_Array)
				continue;

			System.out.println("\n#### class : " + r);

			jq_Class c = (jq_Class) r;
			String cname = c.getName();
			if(filterOut(cname))
				continue;

			prepareOutputStream(c);

			String fileName = Program.getSourceFileName(c);
			if (fileName != null)
				put(".source " + fileName);			

			if(c.isPublic()) cname = "public " + cname;
			if(c.isFinal()) cname = "final " + cname;		
			if(c.isAbstract()) cname = "abstract " + cname;

			String type = null;
			if(c.isClassType()){
				type = ".class";
				if (c.isInterface()){
					type = ".interface";
				}
			}
			put(type + " " + cname.replace(".", "/"));

			jq_Class super_c = c.getSuperclass();
			if(super_c != null){
				put(".super " + super_c.getName().replace(".", "/"));												
			}			

			for(jq_Class jqif : c.getDeclaredInterfaces()){
				put(".implements " + jqif.getName().replace(".", "/"));
			}
			put("");

			jq_StaticField[] arrSF = c.getDeclaredStaticFields();

			for(jq_StaticField jqsf : arrSF){
				String name = jqsf.getName().toString();
				String attrName = ".field";
				String access = getAccessString(jqsf);				
				String typeDesc = jqsf.getType().getDesc().toString();				
				String attr = attrName + access + " "+ name + " " + typeDesc;
				Object o = jqsf.getConstantValue();
				if(o != null){
					attr += " = " + o;
				}
				put(attr);
			}

			for(jq_InstanceField jqif : c.getDeclaredInstanceFields()){
				String name = jqif.getName().toString();
				String attrName = ".field";
				String access = getAccessString(jqif);				
				String typeDesc = jqif.getType().getDesc().toString();				
				String attr = attrName + access + " "+ name + " " + typeDesc;

				put(attr);
			}

			put("");			


			// TODO: read joeq.Class.jq_Class to get things like
			// access modifier, superclass, implemented interfaces, fields, etc.
			// superclass will be null iff c == javalangobject
			for (jq_Method m : c.getDeclaredStaticMethods()) {
				processMethod(m);
			}
			for (jq_Method m : c.getDeclaredInstanceMethods()) {
				processMethod(m);
			}
		}

		out.flush();
		out.close();
	}

	private static String getAccessString(jq_Member member){
		String ret = "";
		if(member.isPublic()) ret += " public";
		if(member.isPrivate()) ret += " private";
		if(member.isProtected()) ret += " protected";
		if(member.isFinal()) ret += " final";
		if(member.isStatic()) ret += " static";		
		return ret;		
	}

	private static void processMethod(jq_Method m) {

		jq_NameAndDesc nd = m.getNameAndDesc();
		String access = getAccessString(m);
		if(m.isAbstract()) access += " abstract";

		put(".method" + access + " "  + nd.getName() + nd.getDesc());

		if (m.isAbstract()){
			put(".end method");
			return;
		}

		current_method = m;
		methodBody.clear();
		ControlFlowGraph cfg = m.getCFG();
//		joeq.Util.Templates.List.BasicBlock basicBlockList = cfg.reversePostOrder(cfg.entry());
		joeq.Util.Templates.List.BasicBlock basicBlockList = cfg.postOrderOnReverseGraph(cfg.exit());
//		joeq.Util.Templates.List.BasicBlock basicBlockList = cfg.reversePostOrderOnReverseGraph(cfg.exit());
		// PreProcess PHI info
		PhiElementMap.clear();
		/*
		for (ListIterator.BasicBlock it = basicBlockList.basicBlockIterator();
		it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			System.out.println(bb);
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				
				Quad q = it2.nextQuad();
				System.out.println(q);
			}
		}
		
		BasicBlock prevBB = null;
		Quad prevQuad = null;
		for (ListIterator.BasicBlock it = basicBlockList.basicBlockIterator();
		it.hasNext();) {
			
			BasicBlock bb = it.nextBasicBlock();
//			
//			if(bb.getFallthroughPredecessor() != null){				
//				assert prevBB.getID() == bb.getFallthroughPredecessor().getID() : "pred : " + bb.getFallthroughPredecessor() + ", prevBB : " + prevBB;
//			}
//			prevBB = bb;
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {
				
				Quad q = it2.nextQuad();
				
				if(prevQuad != null && m.getBCI(q) >0 && m.getBCI(prevQuad) > 0){
					assert m.getBCI(q) >= m.getBCI(prevQuad) : "currQuad : " + q + " at " + m.getBCI(q)+", prevQuad : " + prevQuad + " at " + m.getBCI(prevQuad); 
				}
				prevQuad = q;				
			}			
		}*/
		
		for (ListIterator.BasicBlock it = basicBlockList.basicBlockIterator();
		it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {        		
				Quad q = it2.nextQuad();
				if(q.getOperator() instanceof Phi){
					RegisterOperand dst = Phi.getDest(q);
					BasicBlockTableOperand bbto = Phi.getPreds(q);
					ParamListOperand plo = Phi.getSrcs(q);			
					for(int i=0; i < plo.length(); i++){
						addPhiElements(bbto.get(i), plo.get(i), dst);
					}
				}  		
			}        	
		}

		// see src/java/chord/analyses/sandbox/LocalVarAnalysis.java if you want
		// to distinguish local vars from stack vars 
		// see src/java/chord/rels/RelMmethArg.java to see how to distinguish
		// formal arguments from temporary variables
		visitor.init();
		
		for (ListIterator.BasicBlock it = basicBlockList.basicBlockIterator();
		it.hasNext();) {
			BasicBlock bb = it.nextBasicBlock();
			current_BB = bb;
			System.out.println(bb);
			putInst(bb.toString().split("\\s+")[0]+":");
			for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {            	
				Quad q = it2.nextQuad();
				System.out.println(q);
				q.accept(visitor);
			}
		}

		put(".limit stack " + visitor.maxStackSize);
		put(".limit locals " + visitor.registers.size()*2);

		for(RegisterOperand r : visitor.registers){
			put(".var " + r.getRegister().getNumber() +
					" is " + r.getRegister() + " " + r.getType().getDesc() + " from BB0 to BB1");
		}

		put("");
		for(String s : methodBody){
			put(s);
		}

		put("");
		// thrown exceptions
		jq_Class[] thrownExceptionTable = m.getThrownExceptionsTable();
		if(thrownExceptionTable != null){
			for(jq_Class exception : thrownExceptionTable){
				put(".throws " + exception.getName().replace(".", "/"));
			}
		}
		
		// Exception Handlers
		ListIterator.ExceptionHandler iter = cfg.getExceptionHandlers().exceptionHandlerIterator();
		while(iter.hasNext()){
			joeq.Compiler.Quad.ExceptionHandler handler = iter.nextExceptionHandler();
			joeq.Util.Templates.List.BasicBlock handledBBList = handler.getHandledBasicBlocks();
			BasicBlock startBB = handledBBList.getBasicBlock(0);			
			BasicBlock endBBInclusive = handledBBList.getBasicBlock(handledBBList.size()-1);															
			BasicBlock endBBExclusive = basicBlockList.getBasicBlock(basicBlockList.indexOf(endBBInclusive)+1);
			put(".catch " + handler.getExceptionType().getName().replace(".", "/")
					+ " from " + startBB + " to " + endBBExclusive + " using " 
					+ handler.getEntry());
		}
		
		put(".end method\n");
	}

	static jq_Method current_method;
	static BasicBlock current_BB;

	// map from PhiElementKey to dst register
	private static HashMap<PhiElementKey, RegisterOperand> PhiElementMap = new HashMap<PhiElementKey, RegisterOperand>();

	static class PhiElementKey {

		BasicBlock bb;
		RegisterOperand src;

		private PhiElementKey(BasicBlock bb, RegisterOperand src){
			this.bb = bb;
			this.src = src;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;			
			result = prime * result + ((bb == null) ? 0 : bb.getID());
			result = prime * result + ((src == null) ? 0 : src.getRegister().getNumber());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PhiElementKey other = (PhiElementKey) obj;
			if (bb == null) {
				if (other.bb != null)
					return false;
			} else if (bb.getID() != other.bb.getID())
				return false;
			if (src == null) {
				if (other.src != null)
					return false;
			} else if (src.getRegister().getNumber() != other.src.getRegister().getNumber())
				return false;
			return true;
		}

	}

	static void addPhiElements(BasicBlock bb, RegisterOperand src, RegisterOperand dst){		
		PhiElementMap.put(new PhiElementKey(bb, src), dst);		
	}

	static class JasminQuadVisitor extends EmptyVisitor {

		int curStackSize, maxStackSize;

		Set<RegisterOperand> registers = new TreeSet<RegisterOperand>(
				new Comparator<RegisterOperand>(){
					public int compare(RegisterOperand arg0, RegisterOperand arg1) {
						return 
						new Integer(arg0.getRegister().getNumber()).compareTo(
								arg1.getRegister().getNumber());
					}});

		void init(){
			curStackSize = maxStackSize = 10;
			registers.clear();
		}

		/**
		 * Collects used registers
		 */
		public void visitQuad(Quad q){
			joeq.Util.Templates.List.Operand opList = q.getAllOperands();
			ListIterator.Operand iter = opList.operandIterator();
			while(iter.hasNext()){
				Operand op = iter.nextOperand();
				if(op instanceof RegisterOperand){
					RegisterOperand regOp = (RegisterOperand)op;					
					if(regOp.getRegister().getNumber() >= 0){
						registers.add(regOp);
					}
				}								
			}
		}

		private void checkPhiElement(RegisterOperand src){
			PhiElementKey key = new PhiElementKey(current_BB, src);
			if(PhiElementMap.containsKey(key)){				
				RegisterOperand dst = PhiElementMap.get(key);
				QuadToJasmin.putInst(";; PHI RESOLUTION ");
				QuadToJasmin.putInst(getOperandLoadingInst(src));
				QuadToJasmin.putInst("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());				
			}
		}

		public void visitNew(Quad d){
			QuadToJasmin.putInst("\tnew " + New.getType(d).getType().toString().replace(".", "/"));

			RegisterOperand dst = New.getDest(d);
			QuadToJasmin.putInst("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}
		
		public void visitNewArray(Quad d){
			Operand sizeOperand = NewArray.getSize(d);
			QuadToJasmin.putInst(getOperandLoadingInst(sizeOperand));
			TypeOperand typeOperand = NewArray.getType(d);
			jq_Type type = ((jq_Array)typeOperand.getType()).getElementType();
			
			if(type.isPrimitiveType()){
				QuadToJasmin.putInst("\tnewarray " + type.getName());
			} else if (type.isReferenceType()){
				QuadToJasmin.putInst("\tanewarray " + type.getName().replace(".", "/"));
			} else {
				assert false : "HANDLE this case: " + d;
			}
			RegisterOperand dst = NewArray.getDest(d);
			QuadToJasmin.putInst("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());
			
			checkPhiElement(dst);			
		}

		public void visitMove(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(Move.getSrc(d))); // load
			RegisterOperand dst = Move.getDest(d);
			QuadToJasmin.putInst("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);

		}

		public void visitReturn(Quad q){
			Operator operator = q.getOperator();

			if(operator instanceof RETURN_V){
				QuadToJasmin.putInst("\treturn");
				return;				
			}

			QuadToJasmin.putInst(getOperandLoadingInst(Return.getSrc(q)));
			
			if (operator instanceof THROW_A){
				QuadToJasmin.putInst("\tathrow");
			} else if (operator instanceof RETURN_I){
				QuadToJasmin.putInst("\tireturn");
			} else if (operator instanceof RETURN_A){
				QuadToJasmin.putInst("\tareturn");
			} else if (operator instanceof RETURN_F){
				QuadToJasmin.putInst("\tfreturn");
			} else if (operator instanceof RETURN_D){
				QuadToJasmin.putInst("\tdreturn");
			} else if (operator instanceof RETURN_L){
				QuadToJasmin.putInst("\tlreturn");
			} else assert false : "Unknown return type: " + operator;

		}

		private static String getStoreInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(type.isIntLike()){
				return "istore";
			} else if (typeDesc.startsWith("J")){
				return "lstore";
			} else if (typeDesc.startsWith("F")){
				return "fstore";
			} else if (typeDesc.startsWith("D")){
				return "dstore";
			} else if (typeDesc.startsWith("L") || typeDesc.startsWith("[") ){
				return "astore";
			}
			
			assert false : "Unhandled type " + type + " : desc = " + type.getDesc();
			return "?????";
		}

		private static String getLoadInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(type.isIntLike()){
				return "iload";
			} else if (typeDesc.startsWith("J")){
				return "lload";
			} else if (typeDesc.startsWith("F")){
				return "fload";
			} else if (typeDesc.startsWith("D")){
				return "dload";
			} else if (typeDesc.startsWith("L") || typeDesc.startsWith("[") ){
				return "aload";
			}
			
			assert false : "Unhandled type " + type + " : desc = " + type.getDesc();
			return "?????";
		}

		public void visitGetfield(Quad d){			

			RegisterOperand base = (RegisterOperand) Getfield.getBase(d);
			jq_Field field = Getfield.getField(d).getField();
			QuadToJasmin.putInst("\taload " + base.getRegister().getNumber()); // load base
			QuadToJasmin.putInst("\tgetfield " + field.getDeclaringClass().getName().replace(".", "/") +"/"+ field.getName() + " " + field.getDesc());
			RegisterOperand dst = Getfield.getDest(d);
			QuadToJasmin.putInst("\t" + getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}

		public void visitPutfield(Quad d){
			RegisterOperand base = (RegisterOperand) Putfield.getBase(d);
			QuadToJasmin.putInst("\taload " + base.getRegister().getNumber()); // load base
			Operand src = Putfield.getSrc(d);
			QuadToJasmin.putInst(getOperandLoadingInst(src)); // load src
			jq_Field field = Putfield.getField(d).getField();
			QuadToJasmin.putInst("\tputfield " + field.getDeclaringClass().getName().replace(".", "/")+"/"+ field.getName() + " " + field.getDesc());			

		}

		public void visitGetstatic(Quad d){
			jq_Field field = Getstatic.getField(d).getField();
			QuadToJasmin.putInst("\tgetstatic " + field.getDeclaringClass().getName().replace(".", "/") +"/"+ field.getName() + " " + field.getDesc());
			RegisterOperand dst = Getstatic.getDest(d);
			QuadToJasmin.putInst("\t" + getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}

		public void visitPutstatic(Quad d){
			Operand src = Putstatic.getSrc(d);
			QuadToJasmin.putInst(getOperandLoadingInst(src)); // load src
			jq_Field field = Putstatic.getField(d).getField();
			QuadToJasmin.putInst("\tputstatic " + field.getDeclaringClass().getName().replace(".", "/")+"/"+ field.getName() + " " + field.getDesc());
		}

		private String getBinaryOpName(Binary binOp){
			String[] strs = binOp.toString().split("_");
			assert strs.length == 2;
			return strs[1].toLowerCase() + strs[0].toLowerCase();
		}

		private String getOperandLoadingInst(Operand operand){
			String inst = "";
			if(operand instanceof RegisterOperand){
				Register reg = ((RegisterOperand)operand).getRegister();
				String loadInstName = getLoadInst(reg.getType());
				inst = "\t" + loadInstName + " " + reg.getNumber();
			} else if (operand instanceof ConstOperand){
				if(operand instanceof AConstOperand &&
						((AConstOperand) operand).getValue() == null){
						inst = "\taconst_null";
				} else if (operand instanceof AConstOperand &&
						((AConstOperand) operand).getValue() instanceof String) {
						inst = "\tldc \""+ ((AConstOperand) operand).getValue() + "\"";
				} else if(operand instanceof Const4Operand){
					inst = "\tldc " + ((ConstOperand)operand).getWrapped();
				} else if (operand instanceof Const8Operand){
					inst = "\tldc_w " + ((ConstOperand)operand).getWrapped();
				} else assert false : "Unknown Const Type: " + operand;
			} else assert false : "Unknown Operand Type: " + operand;

			return inst;
		}

		public void visitBinary(Quad d){
			Binary operator = (Binary) d.getOperator();

			QuadToJasmin.putInst(getOperandLoadingInst(Binary.getSrc1(d)));
			QuadToJasmin.putInst(getOperandLoadingInst(Binary.getSrc2(d)));

			String opname = getBinaryOpName(operator);									
			QuadToJasmin.putInst("\t" + opname);

			RegisterOperand dst = (RegisterOperand)Binary.getDest(d);			
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}

		public void visitInvoke(Quad d){
			MethodOperand mOp = Invoke.getMethod(d);
			Invoke operator = (Invoke) d.getOperator();
			jq_Method m = mOp.getMethod();

			String instName = "";
			String strInvoke = operator.toString();
			
			if( m instanceof jq_Initializer){
				instName = "invokespecial";
			} else if(strInvoke.startsWith("INVOKESPECIAL")){
				instName = "invokespecial";
			} else if (strInvoke.startsWith("INVOKESTATIC")){
				instName = "invokestatic";
			} else if (strInvoke.startsWith("INVOKEVIRTUAL")){
				instName = "invokevirtual";
			} else if (strInvoke.startsWith("INVOKEINTERFACE")){
				instName = "invokeinterface";
			} else assert false : "Unexpected operator: " + operator; 

			ParamListOperand paramlist = Invoke.getParamList(d);
			for(int i=0; i < paramlist.length(); i++){
				RegisterOperand regOpr = paramlist.get(i);
				QuadToJasmin.putInst(getOperandLoadingInst(regOpr));
			}

			QuadToJasmin.putInst("\t" + instName + " " + m.getDeclaringClass().getName().replace(".", "/") + "/" + m.getName() + m.getDesc());

			RegisterOperand dst = Invoke.getDest(d);
			if(dst != null){
				String storeInstName = getStoreInst(dst.getType());
				QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

				checkPhiElement(dst);
			}

		}

		public void visitGoto(Quad d){
			TargetOperand targetOp = Goto.getTarget(d);
			QuadToJasmin.putInst("\tgoto " + targetOp.toString());
		}		

		public void visitIntIfCmp(Quad d){

			IntIfCmp iic = (IntIfCmp) d.getOperator();
			if(iic instanceof IFCMP_A){
				// To handle null comparison case
				Operand src1 = IFCMP_A.getSrc1(d);
				Operand src2 = IFCMP_A.getSrc2(d);
				if(src1 instanceof AConstOperand && 
						((AConstOperand)src1).getType() instanceof jq_NullType){
					QuadToJasmin.putInst(getOperandLoadingInst(src2));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.putInst("\tifnull " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.putInst("\tifnonnull " + IntIfCmp.getTarget(d).getTarget());
					} else assert false : d;
				} else if (src2 instanceof AConstOperand && 
						((AConstOperand)src2).getType() instanceof jq_NullType){
					QuadToJasmin.putInst(getOperandLoadingInst(src1));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.putInst("\tifnull " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.putInst("\tifnonnull " + IntIfCmp.getTarget(d).getTarget());
					} else assert false : d;
				} else {
					QuadToJasmin.putInst(getOperandLoadingInst(src1));
					QuadToJasmin.putInst(getOperandLoadingInst(src2));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.putInst("\tif_acmpeq " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.putInst("\tif_acmpne " + IntIfCmp.getTarget(d).getTarget());
					} else assert false : d;					
				}

			} else if (iic instanceof IFCMP_I){
				Operand src1 = IFCMP_I.getSrc1(d);
				Operand src2 = IFCMP_I.getSrc2(d);
				QuadToJasmin.putInst(getOperandLoadingInst(src1));
				QuadToJasmin.putInst(getOperandLoadingInst(src2));
				byte condition = IntIfCmp.getCond(d).getCondition();
				String instSuffix = "";
				switch(condition){
				case BytecodeVisitor.CMP_EQ:
					instSuffix="eq"; break;
				case BytecodeVisitor.CMP_NE:
					instSuffix="ne"; break;
				case BytecodeVisitor.CMP_LT:
					instSuffix="lt"; break;
				case BytecodeVisitor.CMP_LE:
					instSuffix="le"; break;
				case BytecodeVisitor.CMP_GT:
					instSuffix="gt"; break;
				case BytecodeVisitor.CMP_GE:
					instSuffix="ge"; break;
				default:
					assert false : "Unexpected condition " + d;

				}
				QuadToJasmin.putInst("\tif_icmp"+instSuffix+" "+ IntIfCmp.getTarget(d).getTarget());

			} else assert false : "HANDLE THIS CASE " + d;

		}
		
		public void visitSpecial(Quad d){
			Operator operator = d.getOperator();
			if(operator instanceof GET_EXCEPTION){
				RegisterOperand dst = (RegisterOperand) GET_EXCEPTION.getOp1(d);
				QuadToJasmin.putInst("\t" + getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());
			} else assert false : d;				
			
		}

		public void visitInstanceOf(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(InstanceOf.getSrc(d)));
			QuadToJasmin.putInst("\tinstanceof "+InstanceOf.getType(d).getType().getName().replace(".", "/"));
			
			RegisterOperand dst = InstanceOf.getDest(d);				
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
			
		}
				
		public void visitALoad(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(ALoad.getBase(d)));
			QuadToJasmin.putInst(getOperandLoadingInst(ALoad.getIndex(d)));
			ALoad operator = (ALoad)d.getOperator();
			assert !(operator instanceof ALOAD_P) : d;
			String jasminInstOperator = operator.toString().split("_")[1].toLowerCase()+"aload";
			QuadToJasmin.putInst("\t"+jasminInstOperator);
			RegisterOperand dst = ALoad.getDest(d);
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);			
		}
		
		public void visitAStore(Quad d){			
			QuadToJasmin.putInst(getOperandLoadingInst(AStore.getBase(d)));
			QuadToJasmin.putInst(getOperandLoadingInst(AStore.getIndex(d)));
			QuadToJasmin.putInst(getOperandLoadingInst(AStore.getValue(d)));
			AStore operator = (AStore)d.getOperator();
			assert !(operator instanceof ASTORE_P) : d;
			String jasminInstOperator = operator.toString().split("_")[1].toLowerCase()+"astore";
			QuadToJasmin.putInst("\t"+jasminInstOperator);			
		}
		
		public void visitCheckCast(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(CheckCast.getSrc(d)));
			QuadToJasmin.putInst("\tcheckcast " 
					+ CheckCast.getType(d).getType().getName().replace(".", "/"));
			
			RegisterOperand dst = CheckCast.getDest(d);				
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);			
		}
		
		public void visitMonitor(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(Monitor.getSrc(d)));
			Monitor monitorOperator = (Monitor)d.getOperator();
			if(monitorOperator instanceof Monitor.MONITORENTER){
				QuadToJasmin.putInst("\tmonitorenter");
			} else if (monitorOperator instanceof Monitor.MONITOREXIT) {
				QuadToJasmin.putInst("\tmonitorexit");
			} else assert false : d;			
		}
		
		public void visitUnary(Quad d){
			Operand src = Unary.getSrc(d);
			QuadToJasmin.putInst(getOperandLoadingInst(src));
			Unary unaryOperator = (Unary) d.getOperator();
			String strUnary = unaryOperator.toString();
			String jasminInstOperator = null;						
			
			if(strUnary.startsWith("NEG")){
				jasminInstOperator = strUnary.split("_")[1].toLowerCase() + "neg";
			} else if (strUnary.startsWith("INT_2")){
				if(unaryOperator instanceof Unary.INT_2BYTE
						|| unaryOperator instanceof Unary.INT_2CHAR
						|| unaryOperator instanceof Unary.INT_2SHORT){
					jasminInstOperator = "int2" + strUnary.split("_2")[1].toLowerCase();
				} else {
					jasminInstOperator = "i2" + strUnary.charAt((strUnary.indexOf('2')+1));
					jasminInstOperator = jasminInstOperator.toLowerCase();					
				}				
			} else if (strUnary.startsWith("FLOAT_2")){
				assert !(unaryOperator instanceof Unary.FLOAT_2INTBITS) : d;
				jasminInstOperator = "f2" + strUnary.charAt((strUnary.indexOf('2')+1));
				jasminInstOperator = jasminInstOperator.toLowerCase();					
			} else if (strUnary.startsWith("DOUBLE_2")) {
				assert !(unaryOperator instanceof Unary.DOUBLE_2LONGBITS) : d;
				jasminInstOperator = "d2" + strUnary.charAt((strUnary.indexOf('2')+1));
				jasminInstOperator = jasminInstOperator.toLowerCase();					
			} else if (strUnary.startsWith("LONG_2")) { 				
				jasminInstOperator = "l2" + strUnary.charAt((strUnary.indexOf('2')+1));
				jasminInstOperator = jasminInstOperator.toLowerCase();
			} else assert false : d;
			
			QuadToJasmin.putInst("\t"+jasminInstOperator);
			
			RegisterOperand dst = Unary.getDest(d);
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}
		
		public void visitAlength(Quad d){
			QuadToJasmin.putInst(getOperandLoadingInst(ALength.getSrc(d)));
			
			QuadToJasmin.putInst("\tarraylength");
			
			RegisterOperand dst = ALength.getDest(d);
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getRegister().getNumber());

			checkPhiElement(dst);
		}
		
		public void visitRet(Quad d){
			
			assert false : d;
		}

		public void visitMemLoad(Quad d){
			assert false : d;
		}
		
		public void visitMemStore(Quad d){
			assert false : d;
		}		
		
		public void visitJsr(Quad d){
			TargetOperand target = Jsr.getTarget(d);
			QuadToJasmin.putInst("\tjsr " + target.getTarget());
			
			
			
		}

		public void visitTableSwitch(Quad d){
			Operand src = TableSwitch.getSrc(d);
			QuadToJasmin.putInst(getOperandLoadingInst(src));
			BasicBlockTableOperand targetTable = TableSwitch.getTargetTable(d);
			int low = TableSwitch.getLow(d).getValue();
			int size = targetTable.size();
			QuadToJasmin.putInst("\ttableswitch " + low + " " + (low+size-1));
			for(int i=0; i < size; i++){
				QuadToJasmin.putInst("\t\t"+targetTable.get(i));				
			}
			QuadToJasmin.putInst("\t\tdefault: "+TableSwitch.getDefault(d).getTarget());						
		}

		public void visitLookupSwitch(Quad d){
			Operand src = LookupSwitch.getSrc(d);
			QuadToJasmin.putInst(getOperandLoadingInst(src));
			QuadToJasmin.putInst("\tlookupswitch");
			IntValueTableOperand valTable = LookupSwitch.getValueTable(d);
			BasicBlockTableOperand targetTable = LookupSwitch.getTargetTable(d);
			int size = valTable.size();
			for(int i=0; i < size; i++){
				QuadToJasmin.putInst("\t\t"+valTable.get(i)+" : "+targetTable.get(i));				
			}
			QuadToJasmin.putInst("\t\tdefault: "+LookupSwitch.getDefault(d).getTarget());
		}
		// TODO: add all other visit* methods from EmptyVisitor
	}

}


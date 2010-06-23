package chord.program;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.sun.org.apache.bcel.internal.generic.RETURN;

import chord.program.Program;
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_ClassInitializer;
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
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.UTF.Utf8;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.BasicBlockTableOperand;
import joeq.Compiler.Quad.Operand.Const4Operand;
import joeq.Compiler.Quad.Operand.Const8Operand;
import joeq.Compiler.Quad.Operand.ConstOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.MethodOperand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.TargetOperand;
import joeq.Compiler.Quad.Operator.Binary;
import joeq.Compiler.Quad.Operator.Branch;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Goto;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.IntIfCmp.IFCMP_A;
import joeq.Compiler.Quad.Operator.Return.RETURN_A;
import joeq.Compiler.Quad.Operator.Return.RETURN_I;
import joeq.Compiler.Quad.Operator.Return.RETURN_V;
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
		if(className.startsWith("java")){
			return true;
		}

		return false;
	}

	private static void prepareOutputStream(jq_Class c){
		File outFile = new File("chord_output/"+c.getName().replace(".", "/")+".j");
		outFile.getParentFile().mkdirs();
		try {
			if(out != null) out.close();
			out = new PrintStream(outFile);
		} catch (FileNotFoundException e) {
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

			System.out.println("\n####");

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

		// PreProcess PHI info
		PhiElementMap.clear();		
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
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
		for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;			
			result = prime * result + ((bb == null) ? 0 : bb.getID());
			result = prime * result + ((src == null) ? 0 : src.getRegister().getNumber());
			return result;
		}

		@Override
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
					@Override
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
			Iterator<Operand> iter = opList.iterator();
			while(iter.hasNext()){
				Operand op = iter.next();
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

			if (operator instanceof RETURN_I){
				QuadToJasmin.putInst("\tireturn");
			} else if (operator instanceof RETURN_A){
				QuadToJasmin.putInst("\tareturn");
			} else assert false : "Unknown return type: " + operator;

		}

		private static String getStoreInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(typeDesc.startsWith("I")){
				return "istore";
			} else if (typeDesc.startsWith("J")){
				return "lstore";
			} else if (typeDesc.startsWith("F")){
				return "fstore";
			} else if (typeDesc.startsWith("D")){
				return "dstore";
			} else if (typeDesc.startsWith("L")){
				return "astore";
			}
			assert false : "Unhandled type " + type + " : desc = " + type.getDesc();

			return "?????";
		}

		private static String getLoadInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(typeDesc.startsWith("I")){
				return "iload";
			} else if (typeDesc.startsWith("J")){
				return "lload";
			} else if (typeDesc.startsWith("F")){
				return "fload";
			} else if (typeDesc.startsWith("D")){
				return "dload";
			} else if (typeDesc.startsWith("L")){
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
						((AConstOperand)operand).getType() instanceof jq_NullType){
					inst = "\taconst_null";
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

			Register dst = ((RegisterOperand)Binary.getDest(d)).getRegister();			
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.putInst("\t" + storeInstName + " " + dst.getNumber());

			checkPhiElement((RegisterOperand)Binary.getDest(d));

		}

		public void visitInvoke(Quad d){
			MethodOperand mOp = Invoke.getMethod(d);
			jq_Method m = mOp.getMethod();
			if( m instanceof jq_ClassInitializer){
				// no explicit invoking class initializer (only by class loader)
				assert false : "This should not happen!";
			} else if ( m instanceof jq_Initializer){
				ParamListOperand paramlist = Invoke.getParamList(d);
				for(int i=0; i < paramlist.length(); i++){
					RegisterOperand regOpr = paramlist.get(i);
					QuadToJasmin.putInst(getOperandLoadingInst(regOpr));
				}
				QuadToJasmin.putInst("\tinvokespecial " + m.getDeclaringClass().getName().replace(".", "/") + "/" + m.getName() + m.getDesc());
			} else {				
				assert false : "HANDLE THIS : " + d;
			}

		}

		public void visitGoto(Quad d){
			TargetOperand targetOp = Goto.getTarget(d);
			QuadToJasmin.putInst("\tgoto " + targetOp.toString());
		}		

		public void visitCondBranch(Quad d){

			IntIfCmp iic = (IntIfCmp) d.getOperator();
			if(iic instanceof IFCMP_A){

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

			} else assert false : "HANDLE THIS CASE " + d;

		}

		public void visitRet(Quad d){
			assert false : d;
		}

		public void visitJsr(Quad d){
			assert false : d;
		}

		public void visitTableSwitch(Quad d){
			assert false : d;
		}

		public void visitLookupSwitch(Quad d){
			assert false : d;
		}
		// TODO: add all other visit* methods from EmptyVisitor
	}

}


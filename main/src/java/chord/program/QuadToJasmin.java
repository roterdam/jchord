package chord.program;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

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
	
	static void put(String jasminCode){
		
		System.out.println(jasminCode);
	}
	
	private static boolean filterOut(String className){
		if(className.startsWith("java")){
			return true;
		}
		
		return false;
	}
	
	public static void main(String[] args) {
					
		Program program = Program.getProgram();
		IndexSet<jq_Reference> classes = program.getClasses();		
		for (jq_Reference r : classes) {
			
			if (r instanceof jq_Array)
				continue;
			System.out.println("\n####");
			jq_Class c = (jq_Class) r;
			String fileName = Program.getSourceFileName(c);
			if (fileName != null)
				put(".source " + fileName);			

			String cname = c.getName();
			if(filterOut(cname))
				continue;
			
			if(c.isPublic()) cname = "public " + cname;
			if(c.isFinal()) cname = "final " + cname;		
			if(c.isAbstract()) cname = "abstaract " + cname;
			
			String type = null;
			
			if(c.isClassType()){
				type = ".class";
				if (c.isInterface()){
					type = ".interface";
				}
			}			
			put(type + " " + cname);
			
			jq_Class super_c = c.getSuperclass();
			if(super_c != null){
				put(".super " + super_c.getName());												
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
			
			// XXX GOTTA handle this!
			c.getDeclaredInterfaces();
			
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
		ControlFlowGraph cfg = m.getCFG();
		
		RegisterFactory rf = cfg.getRegisterFactory();
		localVarRegs = rf.getLocalNumberingMap().values();
		stackVarRegs = rf.getStackNumberingMap().values();
		rf.getLocalNumberingMap();
		// see src/java/chord/analyses/sandbox/LocalVarAnalysis.java if you want
		// to distinguish local vars from stack vars 
		// see src/java/chord/rels/RelMmethArg.java to see how to distinguish
		// formal arguments from temporary variables
        for (ListIterator.BasicBlock it = cfg.reversePostOrderIterator();
                it.hasNext();) {
        	
            BasicBlock bb = it.nextBasicBlock();
            //System.out.println(bb);
            put(bb+":");
			// TODO: generate label of bb
            for (ListIterator.Quad it2 = bb.iterator(); it2.hasNext();) {            	
                Quad q = it2.nextQuad();
                System.out.println(q);
				q.accept(visitor);
			}
		}
        
        put(".end method");
	}
	
	static jq_Method current_method;
	static Collection<Register> localVarRegs;
	static Collection<Register> stackVarRegs;
	
	static boolean isParameter(Operand operand){
		
		int numParams = current_method.getParamTypes().length;
		if(operand instanceof RegisterOperand){
			RegisterOperand regOp = (RegisterOperand)operand;
			Register reg = regOp.getRegister();
			if ( reg.getNumber() < numParams ) {
				return true;
			}
		}
		
		return false;
		
	}
	
	static class PhiCollector extends EmptyVisitor {
		
		public void visitPhi(Quad d){
			
		}
	}
	
	static class JasminQuadVisitor extends EmptyVisitor {
		final static boolean track_stack = false; 
		Quad predecessor;
		Operand pred_dst;
		
		int num_stack_elem;
		// see src/joeq/Compiler/Quad/QuadVisitor.java
		public void visitALoad(Quad q) {
			// TODO: generate jasmin code for quad q
		}
		
		public void visitNew(Quad d){
			QuadToJasmin.put("\tnew " + New.getType(d).getType());
		
			RegisterOperand dst = New.getDest(d);
			QuadToJasmin.put("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());
		}
		
		public void visitMove(Quad d){
			QuadToJasmin.put(getOperandLoadingInst(Move.getSrc(d))); // load
			RegisterOperand dst = Move.getDest(d);
			QuadToJasmin.put("\t"+getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());			
		}
		
		public void visitReturn(Quad q){
			Operator operator = q.getOperator();
						
			if(operator instanceof RETURN_V){
				QuadToJasmin.put("\treturn");
				return;				
			}
			
			QuadToJasmin.put(getOperandLoadingInst(Return.getSrc(q)));
			
			if (operator instanceof RETURN_I){
				if(track_stack){
					assert num_stack_elem >= 1;
				}
				QuadToJasmin.put("\treturn");
			} else if (operator instanceof RETURN_A){
				QuadToJasmin.put("\tareturn");
			} else assert false : "Unknown return type: " + operator;
			
			predecessor = q;
			pred_dst = null;
			
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
			QuadToJasmin.put("\taload " + base.getRegister().getNumber()); // load base
			QuadToJasmin.put("\tgetfield " + field.getDeclaringClass().getName() +"/"+ field.getName() + " " + field.getDesc());
			RegisterOperand dst = Getfield.getDest(d);
			QuadToJasmin.put("\t" + getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());			
			
		}
		
		public void visitPutfield(Quad d){
			RegisterOperand base = (RegisterOperand) Putfield.getBase(d);
			QuadToJasmin.put("\taload " + base.getRegister().getNumber()); // load base
			Operand src = Putfield.getSrc(d);
			QuadToJasmin.put(getOperandLoadingInst(src)); // load src
			jq_Field field = Putfield.getField(d).getField();
			QuadToJasmin.put("\tputfield " + field.getDeclaringClass().getName()+"/"+ field.getName() + " " + field.getDesc());			
			
		}
		
		public void visitGetstatic(Quad d){
			jq_Field field = Getstatic.getField(d).getField();
			QuadToJasmin.put("\tgetstatic " + field.getDeclaringClass().getName() +"/"+ field.getName() + " " + field.getDesc());
			RegisterOperand dst = Getstatic.getDest(d);
			QuadToJasmin.put("\t" + getStoreInst(dst.getType()) + " " + dst.getRegister().getNumber());
		}
		
		public void visitPutstatic(Quad d){
			Operand src = Putstatic.getSrc(d);
			QuadToJasmin.put(getOperandLoadingInst(src)); // load src
			jq_Field field = Putstatic.getField(d).getField();
			QuadToJasmin.put("\tputstatic " + field.getDeclaringClass().getName()+"/"+ field.getName() + " " + field.getDesc());
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
			
			QuadToJasmin.put(getOperandLoadingInst(Binary.getSrc1(d)));
			QuadToJasmin.put(getOperandLoadingInst(Binary.getSrc2(d)));
						
			String opname = getBinaryOpName(operator);									
			QuadToJasmin.put("\t" + opname);
			
			Register dst = ((RegisterOperand)Binary.getDest(d)).getRegister();			
			String storeInstName = getStoreInst(dst.getType());
			QuadToJasmin.put("\t" + storeInstName + " " + dst.getNumber());
			
		}
		
		public void visitInvoke(Quad d){
			MethodOperand mOp = Invoke.getMethod(d);
			jq_Method m = mOp.getMethod();
			if( m instanceof jq_ClassInitializer){
				//System.out.println("NAME Class Init: " + m.getName());
			} else if ( m instanceof jq_Initializer){
				//System.out.println("NAME init: " + m.getName());
				ParamListOperand paramlist = Invoke.getParamList(d);
				for(int i=0; i < paramlist.length(); i++){
					RegisterOperand regOpr = paramlist.get(i);
					QuadToJasmin.put(getOperandLoadingInst(regOpr));
				}
				QuadToJasmin.put("\tinvokespecial " + m.getDeclaringClass().getName() + "/" + m.getName() + m.getDesc());
			} else {
				System.out.println("NAME : " + m.getName());
				assert false : "HANDLE THIS";
			}
						
		}
		
		public void visitGoto(Quad d){
			TargetOperand targetOp = Goto.getTarget(d);
			QuadToJasmin.put("\tgoto " + targetOp.toString());
		}		
		
		public void visitCondBranch(Quad d){
			
			IntIfCmp iic = (IntIfCmp) d.getOperator();
			if(iic instanceof IFCMP_A){
				
				Operand src1 = IFCMP_A.getSrc1(d);
				Operand src2 = IFCMP_A.getSrc2(d);
				if(src1 instanceof AConstOperand && 
						((AConstOperand)src1).getType() instanceof jq_NullType){
					QuadToJasmin.put(getOperandLoadingInst(src2));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.put("\tifnull " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.put("\tifnonnull " + IntIfCmp.getTarget(d).getTarget());
					} else assert false : d;
				} else if (src2 instanceof AConstOperand && 
						((AConstOperand)src2).getType() instanceof jq_NullType){
					QuadToJasmin.put(getOperandLoadingInst(src1));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.put("\tifnull " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.put("\tifnonnull " + IntIfCmp.getTarget(d).getTarget());
					} else assert false : d;
				} else {
					QuadToJasmin.put(getOperandLoadingInst(src1));
					QuadToJasmin.put(getOperandLoadingInst(src2));
					if(IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_EQ){
						QuadToJasmin.put("\tif_acmpeq " + IntIfCmp.getTarget(d).getTarget());
					} else if (IntIfCmp.getCond(d).getCondition() == BytecodeVisitor.CMP_NE){
						QuadToJasmin.put("\tif_acmpne " + IntIfCmp.getTarget(d).getTarget());
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


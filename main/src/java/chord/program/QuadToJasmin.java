package chord.program;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.sun.org.apache.bcel.internal.generic.RETURN;

import chord.program.Program;
import joeq.Class.jq_Class;
import joeq.Class.jq_Array;
import joeq.Class.jq_Field;
import joeq.Class.jq_InstanceField;
import joeq.Class.jq_Member;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Reference;
import joeq.Class.jq_StaticField;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.UTF.Utf8;
import joeq.Util.Templates.ListIterator;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Binary;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Return;
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
	
	static class JasminQuadVisitor extends EmptyVisitor {
		final static boolean track_stack = false; 
		Quad predecessor;
		Operand pred_dst;
		
		int num_stack_elem;
		// see src/joeq/Compiler/Quad/QuadVisitor.java
		public void visitALoad(Quad q) {
			// TODO: generate jasmin code for quad q
		}
		
		
		public void visitReturn(Quad q){
			Operator operator = q.getOperator();
			
			
			
			if(operator instanceof RETURN_V){
				QuadToJasmin.put("\treturn");
				return;				
			}
//			Register src = ((RegisterOperand)Return.getSrc(q)).getRegister();
//			QuadToJasmin.put("\t" + getLoadInst(src.getType()) + " " + src.getNumber());
			if (operator instanceof RETURN_I){
				if(track_stack){
					assert num_stack_elem >= 1;
				}
				QuadToJasmin.put("\treturn");
			}		
			
			predecessor = q;
			pred_dst = null;
			
		}
		
		private static String getStoreInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(typeDesc.startsWith("I")){
				return "istore";
			} else if (typeDesc.startsWith("L")){
				return "lstore";
			} else if (typeDesc.startsWith("F")){
				return "fstore";
			} else if (typeDesc.startsWith("D")){
				return "dstore";
			} else if (typeDesc.startsWith("A")){
				return "astore";
			}
			assert false : "Unhandled type " + type + " : desc = " + type.getDesc();
			
			return "?????";
		}
		private static String getLoadInst(jq_Type type){

			String typeDesc = type.getDesc()+"";
			if(typeDesc.startsWith("I")){
				return "iload";
			} else if (typeDesc.startsWith("L")){
				return "lload";
			} else if (typeDesc.startsWith("F")){
				return "fload";
			} else if (typeDesc.startsWith("D")){
				return "dload";
			} else if (typeDesc.startsWith("A")){
				return "aload";
			}
			assert false : "Unhandled type " + type + " : desc = " + type.getDesc();
			
			return "?????";
		}
		public void visitGetfield(Quad d){
			Getfield operator = (Getfield) d.getOperator();
			
			RegisterOperand dst = Getfield.getDest(d);
			RegisterOperand base = (RegisterOperand) Getfield.getBase(d);
			jq_Field field = Getfield.getField(d).getField();
			QuadToJasmin.put("\taload " + base.getRegister().getNumber());
			QuadToJasmin.put("\tgetfield " + field.getDeclaringClass().getName() +"/"+ field.getName() + " " + field.getDesc());
			QuadToJasmin.put("\t" + getStoreInst(field.getType()) + " " + dst.getRegister().getNumber());			
			
		}
		
		private String getBinaryOpName(Binary binOp){
			String[] strs = binOp.toString().split("_");
			assert strs.length == 2;
			return strs[1].toLowerCase() + strs[0].toLowerCase();
		}
		
		public void visitBinary(Quad d){
			Binary operator = (Binary) d.getOperator();
			Register s1 = ((RegisterOperand)Binary.getSrc1(d)).getRegister();
			Register s2 = ((RegisterOperand)Binary.getSrc2(d)).getRegister();
			Register dst = ((RegisterOperand)Binary.getDest(d)).getRegister();
			String loadInstName = getLoadInst(s1.getType());
			String storeInstName = getStoreInst(dst.getType());
			String opname = getBinaryOpName(operator);
			
			QuadToJasmin.put("\t" + loadInstName + " " + s1.getNumber());
			QuadToJasmin.put("\t" + loadInstName + " " + s2.getNumber());
			QuadToJasmin.put("\t" + opname);
			QuadToJasmin.put("\t" + storeInstName + " " + dst.getNumber());
			
		}
		
		
		
		// TODO: add all other visit* methods from EmptyVisitor
	}

}


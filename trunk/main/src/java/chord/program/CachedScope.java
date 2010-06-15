/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import com.java2html.Java2HTML;

import chord.util.FileUtils;
import chord.project.OutDirUtils;
import chord.project.Messages;
import chord.project.Properties;
import chord.util.IndexSet;
import chord.util.ChordRuntimeException;
import chord.util.tuple.object.Pair;
 
import joeq.UTF.Utf8;
import joeq.Class.jq_Type;
import joeq.Class.jq_Class;
import joeq.Class.jq_Reference.jq_NullType;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.PrimordialClassLoader;
import joeq.Compiler.Quad.BytecodeToQuad.jq_ReturnAddressType;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Main.Helper;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CachedScope implements IScope {
	private boolean isBuilt = false;
	private IndexSet<jq_Class> classes;
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Class> getClasses() {
		return classes;
	}
	public IndexSet<jq_Method> getMethods() {
		return methods;
	}
	public Set<Pair<Quad, jq_Method>> getRfCasts() {
		return null;
	}
	public void build() {
		if (isBuilt)
			return;
		String classesFileName = Properties.classesFileName;
		List<String> classNames = FileUtils.readFileToList(classesFileName);
		classes = Program.loadClasses(classNames);
		Map<String, jq_Class> nameToClassMap = new HashMap<String, jq_Class>();
		for (jq_Class c : classes)
			nameToClassMap.put(c.getName(), c);
		methods = new IndexSet<jq_Method>();
		String methodsFileName = Properties.methodsFileName;
		List<String> methodSigns = FileUtils.readFileToList(methodsFileName);
		for (String s : methodSigns) {
			MethodSign sign = MethodSign.parse(s);
			String cName = sign.cName;
			jq_Class c = nameToClassMap.get(cName);
			if (c == null)
				Messages.log("SCOPE.EXCLUDING_METHOD", s);
			else {
				String mName = sign.mName;
				String mDesc = sign.mDesc;
				jq_Method m = (jq_Method) c.getDeclaredMember(mName, mDesc);
				assert (m != null);
				methods.add(m);
			}
		}
		write(this);
		isBuilt = true;
	}
	public static void write(IScope scope) {
        try {
            PrintWriter out;
            out = new PrintWriter(Properties.classesFileName);
            IndexSet<jq_Class> classes = scope.getClasses();
            for (jq_Class c : classes)
                out.println(c);
            out.close();
            out = new PrintWriter(Properties.methodsFileName);
            IndexSet<jq_Method> methods = scope.getMethods();
            for (jq_Method m : methods)
                out.println(m);
            out.close();
            Set<Pair<Quad, jq_Method>> rfCasts = scope.getRfCasts();
            out = new PrintWriter(Properties.rfcastsFileName);
			if (rfCasts != null) {
				for (Pair<Quad, jq_Method> p : rfCasts)
					out.println(Program.toBytePosStr(p.val0, p.val1));
			}
			out.close();
        } catch (IOException ex) {
            throw new ChordRuntimeException(ex);
        }
    }
}

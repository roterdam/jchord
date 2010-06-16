/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.program;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chord.util.FileUtils;
import chord.project.Messages;
import chord.project.Properties;
import chord.util.IndexSet;
import chord.util.ChordRuntimeException;
 
import joeq.Class.jq_Class;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Method;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class CachedScope implements IScope {
	private boolean isBuilt = false;
	private IndexSet<jq_Reference> classes;
	private IndexSet<jq_Method> methods;
	public IndexSet<jq_Reference> getClasses() {
		return classes;
	}
	public IndexSet<jq_Reference> getNewInstancedClasses() {
		return null;
	}
	public IndexSet<jq_Method> getMethods() {
		return methods;
	}
	public void build() {
		if (isBuilt)
			return;
		String classesFileName = Properties.classesFileName;
		List<String> classNames = FileUtils.readFileToList(classesFileName);
		classes = Program.loadClasses(classNames);
		Map<String, jq_Reference> nameToClassMap = new HashMap<String, jq_Reference>();
		for (jq_Reference r : classes)
			nameToClassMap.put(r.getName(), r);
		methods = new IndexSet<jq_Method>();
		String methodsFileName = Properties.methodsFileName;
		List<String> methodSigns = FileUtils.readFileToList(methodsFileName);
		for (String s : methodSigns) {
			MethodSign sign = MethodSign.parse(s);
			String cName = sign.cName;
			jq_Reference r = nameToClassMap.get(cName);
			if (r == null)
				Messages.log("SCOPE.EXCLUDING_METHOD", s);
			else {
				assert (r instanceof jq_Class);
				jq_Class c = (jq_Class) r;
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
            IndexSet<jq_Reference> classes = scope.getClasses();
            for (jq_Reference r : classes)
                out.println(r);
            out.close();
            out = new PrintWriter(Properties.methodsFileName);
            IndexSet<jq_Method> methods = scope.getMethods();
            for (jq_Method m : methods)
                out.println(m);
            out.close();
            IndexSet<jq_Reference> newInstancedClasses = scope.getNewInstancedClasses();
            out = new PrintWriter(Properties.newInstancedClassesFileName);
			if (newInstancedClasses != null) {
				for (jq_Reference r : newInstancedClasses)
					out.println(r);
			}
			out.close();
        } catch (IOException ex) {
            throw new ChordRuntimeException(ex);
        }
    }
}

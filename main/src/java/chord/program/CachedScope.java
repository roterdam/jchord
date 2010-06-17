/*
 * Copyright (c) 2008-2010, Intel Corporation.
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
public class CachedScope extends Scope {
	private IndexSet<jq_Method> methods;
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
		IndexSet<jq_Reference> cList = Program.loadClasses(classNames);
		Map<String, jq_Reference> nameToClassMap = new HashMap<String, jq_Reference>();
		for (jq_Reference r : cList)
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
		isBuilt = true;
	}
}

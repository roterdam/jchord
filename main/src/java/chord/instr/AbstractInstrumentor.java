/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.instr;

import java.util.Map;
import java.util.HashMap;

import javassist.NotFoundException;
import javassist.CannotCompileException;
import javassist.CtClass;

import chord.project.Messages;
import chord.project.ChordProperties;

/**
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class AbstractInstrumentor {
	private static final String EXPLICITLY_EXCLUDING_CLASS =
		"WARN: Instrumentor: Not instrumenting class %s as it is excluded by chord.scope.exclude.";
	private static final String IMPLICITLY_EXCLUDING_CLASS =
		"WARN: Instrumentor: Not instrumenting class %s.";
	protected boolean verbose;
	protected final JavassistPool pool;
	protected String[] scopeExcludeAry;
	protected Map<String, String> argsMap;

	public AbstractInstrumentor(Map<String, String> _argsMap) {
		scopeExcludeAry = ChordProperties.scopeExcludeAry;
 		verbose = ChordProperties.verbose;
		String mainClassPathName = ChordProperties.mainClassPathName;
		String userClassPathName = ChordProperties.classPathName;
		pool = new JavassistPool(mainClassPathName, userClassPathName);
		argsMap = _argsMap;
    }

    public JavassistPool getPool() {
        return pool;
    }

	public boolean isExplicitlyExcluded(String cName) {
		for (String s : scopeExcludeAry) {
			if (cName.startsWith(s))
				return true;
		}
		return false;
	}
	
	public boolean isImplicitlyExcluded(String cName) {
		return cName.equals("java.lang.J9VMInternals") ||
			cName.startsWith("sun.reflect.Generated") ||
			cName.startsWith("java.lang.ref.");
	}

	public boolean isExcluded(String cName) {
		if (isImplicitlyExcluded(cName)) {
			if (verbose) Messages.log(IMPLICITLY_EXCLUDING_CLASS, cName);
			return true;
		}
		if (isExplicitlyExcluded(cName)) {
			if (verbose) Messages.log(EXPLICITLY_EXCLUDING_CLASS, cName);
			return true;
		}
		return false;
	}

	public CtClass instrument(String cName)
			throws NotFoundException, CannotCompileException {
		if (isExcluded(cName))
			return null;
		CtClass clazz = pool.get(cName);
		return instrument(clazz);
	}

    /**
     * Runs the instrumentor which reads each .class file of the
     * given program and writes a corresponding .class file with
     * instrumentation for generating the specified kind and format
     * of events during the execution of the instrumented program.
     */
	public CtClass instrument(CtClass clazz)
		throws NotFoundException, CannotCompileException {
		return clazz;
	}
}


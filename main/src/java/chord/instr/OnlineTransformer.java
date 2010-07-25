/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 */
package chord.instr;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import chord.util.StringUtils;
import chord.project.Config;

import javassist.CtClass;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import java.util.Properties;

import chord.project.Messages;

/**
 * Online (load-time) class-file transformer.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public final class OnlineTransformer implements ClassFileTransformer {
	private static final String RETRANSFORM_NOT_SUPPORTED =
		"ERROR: JVM does not support retransforming classes.";
	private static final String CANNOT_RETRANSFORM_LOADED_CLASSES =
		"ERROR: Failed to retransform alreaded loaded classes; reason follows.";
	private static final String CANNOT_INSTRUMENT_CLASS =
		"ERROR: Skipping instrumenting class %s; reason follows.";
	private static final String CANNOT_MODIFY_CLASS =
		"WARN: Cannot modify class %s.";

    private final BasicInstrumentor instrumentor;

	public OnlineTransformer(BasicInstrumentor instr) {
		instrumentor = instr;
	}

	public static List<String> getCmd(String classPathName, String mainClassName,
			String agentArgs, String args) {
		List<String> cmd = new ArrayList<String>();
		cmd.add("java");
		cmd.addAll(StringUtils.tokenize(Config.runtimeJvmargs));
		Properties props = System.getProperties();
		for (Map.Entry e : props.entrySet()) {
			String key = (String) e.getKey();
			if (key.startsWith("chord."))
				cmd.add("-D" + key + "=" + e.getValue());
		}
		cmd.add("-javaagent:" + Config.jInstrAgentFileName + agentArgs);
		cmd.add("-cp");
		cmd.add(classPathName);
		cmd.add(mainClassName);
		cmd.addAll(StringUtils.tokenize(args));
		return cmd;
	}

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        boolean isSupported = instrumentation.isRetransformClassesSupported();
        if (!isSupported) {
            Messages.fatal(RETRANSFORM_NOT_SUPPORTED);
        }
		Map<String, String> argsMap = new HashMap<String, String>();
		String[] args = agentArgs.split("=");
		int n = args.length / 2;
		for (int i = 0; i < n; i++)
			argsMap.put(args[i*2], args[i*2+1]);
		String instrClassName = argsMap.get("instr_class_name");
		Class instrClass = null;
		if (instrClassName != null) {
			try {
				instrClass = Class.forName(instrClassName);
			} catch (ClassNotFoundException ex) {
				Messages.fatal(ex);
			}
		} else
			instrClass = BasicInstrumentor.class;
		BasicInstrumentor instr = null;
		Exception ex = null;
		try {
			Constructor c = instrClass.getConstructor(new Class[] { Map.class });
			Object o = c.newInstance(new Object[] { argsMap });
			instr = (BasicInstrumentor) o;
		} catch (InstantiationException e) {
			ex = e;
		} catch (NoSuchMethodException e) {
			ex = e;
		} catch (InvocationTargetException e) {
			ex = e;
		} catch (IllegalAccessException e) {
			ex = e;
		}
		if (ex != null)
			Messages.fatal(ex);
        OnlineTransformer transformer = new OnlineTransformer(instr);
        instrumentation.addTransformer(transformer, true);
        Class[] classes = instrumentation.getAllLoadedClasses();
        List<Class> retransformClasses = new ArrayList<Class>();
        for (Class c : classes) {
            if (c.getName().startsWith("[")) 
				continue;
			if (!instrumentation.isModifiableClass(c)) {
				Messages.log(CANNOT_MODIFY_CLASS, c.getName());
				continue;
			}
			retransformClasses.add(c);
        }
		Class[] retransformClassesAry =
			retransformClasses.toArray(new Class[retransformClasses.size()]);
        try {
            instrumentation.retransformClasses(retransformClassesAry);
        } catch (UnmodifiableClassException e) {
			Messages.log(CANNOT_RETRANSFORM_LOADED_CLASSES);
			e.printStackTrace();
        }
    }

    public byte[] transform(ClassLoader loader, String cName, Class<?> cls,
			ProtectionDomain pd, byte[] classfile) throws IllegalClassFormatException {
		// cName is of the form "java/lang/Object"
		cName = cName.replace('/', '.');
		Exception ex = null;
		try {
			CtClass clazz = instrumentor.edit(cName);
			if (clazz != null)
				return clazz.toBytecode();
		} catch (IOException e) {
			ex = e;
		} catch (NotFoundException e) {
			ex = e; 
		} catch (CannotCompileException e) {
			ex = e; 
		}
		if (ex != null) {
			Messages.log(CANNOT_INSTRUMENT_CLASS, cName);
			ex.printStackTrace();
		}
		return null;
    }
}


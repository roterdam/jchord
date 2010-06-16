package chord.program;

import chord.util.IndexSet;
import chord.project.Properties;

import java.io.PrintWriter;
import java.io.IOException;

import joeq.Class.PrimordialClassLoader;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Class.jq_Reference;
import joeq.Class.jq_Reference.jq_NullType;
import joeq.Compiler.Quad.BytecodeToQuad.jq_ReturnAddressType;

import java.util.Comparator;
import java.util.Arrays;

import chord.util.ChordRuntimeException;

/**
 * Generic interface for algorithms computing analysis scope
 * (i.e., reachable classes and methods).
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public abstract class Scope {
    public static final boolean DEBUG = false;
	protected boolean isBuilt = false;
	private IndexSet<jq_Type> types;
	private IndexSet<jq_Reference> classes;
	public final IndexSet<jq_Type> getTypes() {
		assert (isBuilt);
		if (types == null)
			computeClassesAndTypes();
		return types;
	}
	public final IndexSet<jq_Reference> getClasses() {
		assert (isBuilt);
		if (types == null)
			computeClassesAndTypes();
		return classes;
	}
	private void computeClassesAndTypes() {
        assert (classes == null);
        assert (types == null);
        PrimordialClassLoader loader = PrimordialClassLoader.loader;
        jq_Type[] typesAry = loader.getAllTypes();
        int numTypes = loader.getNumTypes();
        Arrays.sort(typesAry, 0, numTypes, comparator);
        types = new IndexSet<jq_Type>(numTypes + 2);
        classes = new IndexSet<jq_Reference>();
        types.add(jq_NullType.NULL_TYPE);
        types.add(jq_ReturnAddressType.INSTANCE);
        for (int i = 0; i < numTypes; i++) {
            jq_Type t = typesAry[i];
            assert (t != null);
            types.add(t);
            if (t instanceof jq_Reference && t.isPrepared()) {
				jq_Reference r = (jq_Reference) t;
                classes.add(r);
			}
        }
    }
	public abstract void build();
	public abstract IndexSet<jq_Reference> getNewInstancedClasses();
	public abstract IndexSet<jq_Method> getMethods();
    public void write() {
		assert (isBuilt);
        try {
            PrintWriter out;
            out = new PrintWriter(Properties.classesFileName);
			IndexSet<jq_Reference> cList = getClasses();
            for (jq_Reference r : cList)
                out.println(r);
            out.close();
            IndexSet<jq_Reference> newInstancedClasses =
				getNewInstancedClasses();
            out = new PrintWriter(Properties.newInstancedClassesFileName);
            if (newInstancedClasses != null) {
                for (jq_Reference r : newInstancedClasses)
                    out.println(r);
            }
            out.close();
            out = new PrintWriter(Properties.methodsFileName);
			IndexSet<jq_Method> mList = getMethods();
            for (jq_Method m : mList)
                out.println(m);
            out.close();
        } catch (IOException ex) {
            throw new ChordRuntimeException(ex);
        }
    }
    private static Comparator comparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            jq_Type t1 = (jq_Type) o1;
            jq_Type t2 = (jq_Type) o2;
            String s1 = t1.getName();
            String s2 = t2.getName();
            return s1.compareTo(s2);
        }
    };
}

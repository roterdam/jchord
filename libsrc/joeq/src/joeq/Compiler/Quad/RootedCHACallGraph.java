// RootedCHACallGraph.java, created Sat Mar 29  0:56:01 2003 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Compiler.Quad;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Method;
import joeq.Main.HostedVM;
import jwutil.graphs.CountPaths;
import jwutil.graphs.Navigator;
import jwutil.graphs.SCCTopSortedGraph;
import jwutil.graphs.SCComponent;
import java.util.ArrayList;

/**
 * @author John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: RootedCHACallGraph.java,v 1.12 2005/08/18 01:56:59 livshits Exp $
 */
public class RootedCHACallGraph extends CHACallGraph {
    
    private static final boolean DUMP_CALLGRAPH = false;
    Collection roots;
    
    public RootedCHACallGraph() { }
    public RootedCHACallGraph(Set classes) {
        super(classes);
    }
    
    /* (non-Javadoc)
     * @see joeq.Compiler.Quad.CallGraph#getRoots()
     */
    public Collection getRoots() {
        return roots;
    }

    /* (non-Javadoc)
     * @see Compiler.Quad.CallGraph#setRoots(java.util.Collection)
     */
    public void setRoots(Collection roots) {
        this.roots = roots;
    }

    public static void main(String[] args) {
		CallGraph cg = build(args[0]);
/*
		for (jq_Type t : jq_Type.set) {
		Set<jq_Class> classes = new HashSet<jq_Class>();
		for (Object o : cg.getAllMethods()) {
			jq_Method m = (jq_Method) o;
			jq_Class c = m.getDeclaringClass();
			if (classes.add(c)) {
				System.out.println("ZZZ " + c);
			}
		}
*/
	}

	public static CallGraph build(String mainClassName) {
        long time;
        
        HostedVM.initialize();
        
        jq_Class c = (jq_Class) jq_Type.parseType(mainClassName);
        c.prepare();
        
        System.out.print("Building call graph...");
        time = System.currentTimeMillis();
        CallGraph cg = new RootedCHACallGraph();
        cg = new CachedCallGraph(cg);
        jq_NameAndDesc sign = new jq_NameAndDesc("main", "([Ljava/lang/String;)V");
        jq_Method mainMethod = c.getDeclaredStaticMethod(sign);
        if (mainMethod == null)
			throw new RuntimeException("Class " + c + " lacks a main method");
        Collection roots = new ArrayList(1);
		roots.add(mainMethod);
        cg.setRoots(roots);
        time = System.currentTimeMillis() - time;
        System.out.println("done. ("+(time/1000.)+" seconds)");
        
        if(DUMP_CALLGRAPH) {
            String callgraphFileName = "callgraph";
            BufferedWriter dos;
            try {
                dos = new BufferedWriter(new FileWriter(callgraphFileName ));
                LoadedCallGraph.write(cg, dos);            
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    	//   test(cg);
		return cg;
    }
    
    public static void test(CallGraph cg) {
        long time;
        if (true) {
            System.out.print("Building navigator...");
            time = System.currentTimeMillis();
            Navigator n = cg.getNavigator();
            time = System.currentTimeMillis() - time;
            System.out.println("done. ("+(time/1000.)+" seconds)");
            
            System.out.print("Building strongly-connected components...");
            time = System.currentTimeMillis();
            Set s = SCComponent.buildSCC(cg.getRoots(), n);
            time = System.currentTimeMillis() - time;
            System.out.println("done. ("+(time/1000.)+" seconds)");
            
            System.out.print("Topologically sorting strongly-connected components...");
            time = System.currentTimeMillis();
            SCCTopSortedGraph g = SCCTopSortedGraph.topSort(s);
            time = System.currentTimeMillis() - time;
            System.out.println("done. ("+(time/1000.)+" seconds)");
            
            /*
            for (Iterator j=g.getFirst().listTopSort().iterator(); j.hasNext(); ) {
                SCComponent d = (SCComponent) j.next();
                System.out.println(d);
            }
            */
            
            /*
            long paths = 0L;
            for (int i=1; ; ++i) {
                long paths2 = joeq.Util.Graphs.CountPaths.countPaths(g.getNavigator(), s, i);
                if (paths2 == paths) break;
                paths = paths2;
                System.out.println("Number of paths (k="+i+") = "+paths);
            }
            */
            System.out.println("Number of paths (k=infinity) = "+CountPaths.countPaths(g.getNavigator(), s));
        }
        
        if (false) {
            Collection[] depths = INSTANCE.findDepths();
            for (int i=0; i<depths.length; ++i) {
                System.out.println(">>>>> Depth "+i);
                System.out.println(depths[i]);
            }
        }
    }

}

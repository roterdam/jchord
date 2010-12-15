package chord.analyses;

import java.io.PrintWriter;
import java.util.TreeSet;

import joeq.Class.jq_Reference;

import chord.program.ClassHierarchy;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Config;
import chord.project.OutDirUtils;
import chord.project.analyses.JavaAnalysis;
import chord.util.Utils;

/**
 * Dumps a subset classes that CHA found that weren't in-scope.
 * Subset is defined by option deadClasses.relevantPrefixes 
 * @author asrabkin
 * 
 * Output is in dead_classes.txt
 *
 */
@Chord(
		name="DeadClasses",
		consumes={"T"})
public class DeadClasses extends JavaAnalysis {
	
	String[] relevantPrefixes;
	
  @Override
  public void run() {
    relevantPrefixes = Config.toArray(System.getProperty("deadClasses.relevantPrefixes", ""));
    if(relevantPrefixes.length == 0) {
    	System.err.println("You must specify property deadClasses.relevantPrefixes to use the DeadClasses analysis");
    	System.exit(-1);
//    	relevantPrefixes = new String[] {""};
    }
    Program program = Program.g();
//    ClassicProject project = ClassicProject.g();

    ClassHierarchy ch = program.getClassHierarchy();

//    DomT domT = (DomT) project.getTrgt("T");

    TreeSet<String> sortedDead = new TreeSet<String>();
    for(String s: ch.allClassNamesInPath()) {
    	if(Utils.prefixMatch(s,relevantPrefixes)) {
	    	jq_Reference r = program.getClass(s);
	    	if(r == null)
	    		sortedDead.add(s);
    	}
    }
    
    PrintWriter writer = OutDirUtils.newPrintWriter("dead_classes.txt");
    
    for(String s: sortedDead)
  		writer.println(s);
    
    writer.close();  	
  }
}

package chord.analyses.provenance.typestate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alloc.DomH;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.bddbddb.Rel.IntAryNIterable;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.ITask;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.provenance.MaxSatGenerator;
import chord.project.analyses.provenance.Tuple;
import chord.project.analyses.provenance.Model;
import chord.util.tuple.object.Pair;

/**
 * A general class to run experiments based on k-cfa analysis.
 * -Dchord.provenance.queryOption=<all/separate/single>: specify the way to solve queries
 * -Dchord.provenance.heap=<true/false>: specify whether to turn on heap-cloning
 * -Dchord.provenance.mono=<true/false>: specify whether to monotonically grow the k values
 * -Dchord.provenance.queryWeight=<Integer>: specify the weight we want to use for queries; if -1, treat them as hard constraints.
 * If 0, use the sum(input weight) + 1
 * -Dchord.provenance.onlyTrackedTypes=<true/false>:specify if we only want queries where the types of alloc sites are those from the SAFE paper.
 * -Dchord.provenance.typestateQueries: number of queries to track
 * 
 * @author xin
 * 
 */
@Chord(name = "typestate-refiner")
public class TypeStateRefiner extends JavaAnalysis {
	DomV domV;
	DomP domP;
	DomH domH;
	Map<Tuple, Set<Register>> absMap;
	Set<Tuple> unresolvedQs = new HashSet<Tuple>();
	Set<Tuple> impossiQs = new HashSet<Tuple>();
	MaxSatGenerator gen;
	String[] configFiles;

	ProgramRel allowRel;
	ProgramRel denyRel;
	ProgramRel queryRel;
	List<ITask> tasks;
	PrintWriter debugPW;
	PrintWriter statPW;

	String queryRelName;
	

	boolean ifMono;
	boolean ifUpdateTS;

	static int iterLimit = 100;//max number of refinement iterations allowed
	static int max = 20; //max number of k value for both IK and HK
	
	int queryWeight;

	int numQueries;

	private MaxSatGenerator createMaxSatGenerator(int queryWeight) {
		PTHandler ptHandler = new PTHandler(ifMono);
		Model model = new TypeStateModel(ptHandler);
		MaxSatGenerator g = new MaxSatGenerator(configFiles, queryRelName, ptHandler, model, queryWeight);
		return g;
	}
	
	@Override
	public void run() {
		try {
			debugPW = new PrintWriter(new File(Config.outDirName + File.separator + "debug.txt"));
			statPW = new PrintWriter(new File(Config.outDirName+File.separator+"stat.txt"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		this.queryRelName = "unprovenQuery";

		domV = (DomV)ClassicProject.g().getTrgt("V");
		ClassicProject.g().runTask(domV);
		
		domH = (DomH)ClassicProject.g().getTrgt("H");
		ClassicProject.g().runTask(domH);
		
		domP = (DomP)ClassicProject.g().getTrgt("P");
		ClassicProject.g().runTask(domP);
		
		
		//TODO fill in the necessary analyses here
		//The analyses we need to run
		tasks = new ArrayList<ITask>();
		tasks.add(ClassicProject.g().getTask("cipa-0cfa-dlog"));
		tasks.add(ClassicProject.g().getTask("mustSet-java"));
		tasks.add(ClassicProject.g().getTask("typestate-dlog_XZ89_"));

		String chordIncu = System.getenv("CHORD_INCUBATOR");
		String typestateConfig = chordIncu + File.separator + "src/chord/analyses/provenance/typestate/typestate-dlog_XZ89_.config";
		configFiles = new String[]{ typestateConfig };

		allowRel = (ProgramRel) ClassicProject.g().getTrgt("allow");
		denyRel = (ProgramRel) ClassicProject.g().getTrgt("deny");
		queryRel = (ProgramRel) ClassicProject.g().getTrgt(queryRelName);

		absMap = new HashMap<Tuple, Set<Register>>();

		String opt = System.getProperty("chord.provenance.queryOption", "all");
		ifMono = Boolean.getBoolean("chord.provenance.mono");
		queryWeight = Integer.getInteger("chord.provenance.queryWeight", MaxSatGenerator.QUERY_HARD);
		numQueries = Integer.getInteger("chord.provenance.typestateQueries", 500);
		ifUpdateTS = Boolean.getBoolean("chord.provenance.typestateUpdateTrackedH");
		
		System.out.println("chord.provenance.queryOption = "+opt);
		System.out.println("chord.provenance.mono = "+ifMono);
		System.out.println("chord.provenance.queryWeight = "+queryWeight);
		System.out.println("chord.provenance.typestateQueries = "+numQueries);
		
		//Initialize the queries
		unresolvedQs =getQueries();
		for (Tuple t : unresolvedQs) {// put empty abstraction map for each query
			absMap.put(t, new HashSet<Register>());
		}
		if (opt.equals("all")) {
			runAll();
		}
		if (opt.equals("separate")) {
			runSeparate();
		}
		if (opt.equals("single")) {
			String queryString = System.getProperty("chord.provenance.query");
			Tuple t = new Tuple(queryString);
			runSingle(t);
		}
		if (opt.equals("group")) {
			runGroup();
		}
		debugPW.flush();
		debugPW.close();
		statPW.flush();
		statPW.close();
	}

	private void runAll() {
		//Set up MaxSatGenerator
		gen = createMaxSatGenerator(queryWeight);
		gen.DEBUG = false;
		int numIter = 0;
		int kcfaImp = 0;
		int totalQs = unresolvedQs.size();
		while (unresolvedQs.size() != 0) {
			if(ifMono)
				gen = createMaxSatGenerator(queryWeight);
			printlnInfo("===============================================");
			printlnInfo("===============================================");
			printlnInfo("Iteration: " + numIter + " unresolved queries size: " + unresolvedQs.size());
			for (Tuple t : unresolvedQs) {
				printlnInfo(t.toVerboseString());
			}
			int unresolNum = unresolvedQs.size();
			int impossiNum = impossiQs.size();
			int provenNum = totalQs-unresolNum - impossiNum;
			statPW.println(numIter+" "+unresolNum+" "+provenNum+" "+impossiNum+" "+kcfaImp);
			statPW.flush();
			printlnInfo("++++++++++++++++++++++++++++++++++++++++++++++");
			printlnInfo("Abstraction used: ");
			Set<Register> abs = absMap.get(unresolvedQs.iterator().next());
			printlnInfo("Num of var tracked: "+abs.size());
			printlnInfo(abs.toString());
			runAnalysis(abs);
			Set<Tuple> hardQueries = updateAllQs(numIter);
			if(hardQueries.size() != 0){
//				MaxSatGenerator current = gen;
				printlnInfo("********************************");
				printlnInfo("Some queries might be impossible (indeed impossible using complete max sat solver:");
				printlnInfo(hardQueries.toString());
				impossiQs.addAll(hardQueries);
				printlnInfo("********************************");
				printlnInfo("Coming back to the normal loop");
//				gen = current; // some recover, not entirely necessary
			}
			numIter++;
		}
		int impossiNum = impossiQs.size();
		int provenNum = 0;
		printlnInfo("Impossible Qs: " + impossiQs);
		for (Map.Entry<Tuple, Set<Register>> entry : absMap.entrySet()) {
			Tuple t = entry.getKey();
			if (!impossiQs.contains(t)) {
				provenNum++;
				printlnInfo("Query: " + t + ", " + entry.getValue());
			}
		}
		System.out.println("Impossible Num: "+impossiNum);
		System.out.println("Proven Num: "+provenNum);
	}

	private Set<Tuple> updateAllQs(int numIter) {
		queryRel.load();
		Set<Tuple> nrqs = new HashSet<Tuple>();
		IntAryNIterable iter = queryRel.getAryNIntTuples();
		for (int[] indices : iter) {
			nrqs.add(new Tuple(queryRel, indices));
		}
		unresolvedQs.retainAll(nrqs);
		gen.update(unresolvedQs);
		Set<Tuple> tupleToEli = gen.solve(unresolvedQs, numIter+"");
		if (tupleToEli == null) {
			impossiQs.addAll(unresolvedQs);
			unresolvedQs.clear();
		}
		Set<Tuple> ret = new HashSet<Tuple>();
		for(Tuple t : tupleToEli){
			if(t.getRelName().equals(queryRelName))
				ret.add(t);
		}
		unresolvedQs.removeAll(ret);// remove the queries from the group, we'll deal with them individually
		updateAbsMap(unresolvedQs, tupleToEli);
		return ret;
	}

	private void updateAbsMap(Set<Tuple> qts, Set<Tuple> tupleToEli) {
		for (Tuple t : qts) {
			Set<Register> abs = absMap.get(t);
			if (abs == null) {
				abs = new HashSet<Register>();
				absMap.put(t, abs);
			}
			if(!ifMono)
				abs.clear();
			for (Tuple t1 : tupleToEli) {
				if (t1.getRelName().equals("deny")) {
					Register v = (Register) t1.getValue(0);
					abs.add(v);
				}  else
					if(!t1.getRelName().equals(this.queryRelName))
						throw new RuntimeException("Unexpected param tuple: " + t1);
			}
		}
	}
	
	/**
	 * Run the analysis with the abstraction map specified in the parameter
	 * @param abs
	 */
	private void runAnalysis(Set<Register> abs) {
		if(ifUpdateTS){
	    	ProgramRel relCurrentQueries = (ProgramRel) ClassicProject.g().getTrgt("currentQueries");
	    	relCurrentQueries.zero();
	    	for(Tuple q : unresolvedQs){
	    		relCurrentQueries.add(q.getValue(0),q.getValue(1));
	    	}
	    	relCurrentQueries.save();
		}
		allowRel.zero();
		denyRel.zero();
		for(Register r: domV)
			if(abs.contains(r))
				allowRel.add(r);
			else
				denyRel.add(r);
		allowRel.save();
		denyRel.save();
		for (ITask t : tasks) {
			ClassicProject.g().resetTaskDone(t);
			ClassicProject.g().runTask(t);
		}
	}

	private void runSeparate() {
		for (Tuple q : unresolvedQs) {
			runSingle(q);
		}
	}


	private boolean updateQuery(Tuple t, int numIter) {
		queryRel.load();
		int colNum = t.getIndices().length;
		boolean containment = false;
		if (colNum == 1)
			containment = queryRel.contains(t.getValue(0));
		if (colNum == 2)
			containment = queryRel.contains(t.getValue(0), t.getValue(1));
		if (containment) {
			Set<Tuple> tmp = new HashSet<Tuple>();
			tmp.add(t);
			gen.update(tmp);
			Set<Tuple> tupleToEli = gen.solve(tmp,t.toString()+numIter);
			if (tupleToEli == null) {
				impossiQs.add(t);
				return true;
			}
			updateAbsMap(tmp, tupleToEli);
			return false;
		}
		return true;
	}

	private Set<Tuple> getQueries() {
		List<Pair<Quad,Quad>> allQueries = new ArrayList<Pair<Quad,Quad>>();
    	ProgramRel relAllQueries= (ProgramRel) ClassicProject.g().getTrgt("allTypestateQueries");
    	ClassicProject.g().runTask(relAllQueries);
    	relAllQueries.load();
    	Iterable<Pair<Quad,Quad>> tuples = relAllQueries.getAry2ValTuples();
    	for(Pair<Quad,Quad> p : tuples){
    		allQueries.add(p);
    	}
    	relAllQueries.close();
    	
    	Collections.shuffle(allQueries);
		Set<Tuple> ret = new HashSet<Tuple>();
    	ProgramRel relCurrentQueries = (ProgramRel) ClassicProject.g().getTrgt("currentQueries");
    	relCurrentQueries.zero();
    	for(int i = 0; i < numQueries && i < allQueries.size(); i++){
    		Pair<Quad,Quad> chosenQuery = allQueries.get(i);
    		relCurrentQueries.add(chosenQuery.val0,chosenQuery.val1);
    		int idices[] = new int[2];
    		idices[0] = domP.indexOf(chosenQuery.val0);
    		idices[1] = domH.indexOf(chosenQuery.val1);
			ret.add(new Tuple(queryRel, idices));
    	}
    	relCurrentQueries.save();

		return ret;
	}

	private void runSingle(Tuple q) {
		printlnInfo("Processing query: " + q);
		int numIter = 0;
		gen = createMaxSatGenerator(MaxSatGenerator.QUERY_HARD);
		gen.DEBUG = false;
		while (true) {
			if(ifMono)
				gen = createMaxSatGenerator(MaxSatGenerator.QUERY_HARD);
			printlnInfo("===============================================");
			printlnInfo("===============================================");
			printlnInfo("Iteration: " + numIter);
			Set<Register> abs = absMap.get(q);
			printlnInfo("Abstraction used: ");
			printlnInfo(abs.toString());
			runAnalysis(abs);
			if (updateQuery(q,numIter)) {
				if (impossiQs.contains(q)) {
					printlnInfo("Impossible");
				} else {
					printlnInfo("Proven");
				}
				break;
			}
			numIter++;
			if (numIter > iterLimit) {
				printlnInfo("Too many iteration for " + q);
				break;
			}
		}
	}

	private void runGroup() {

	}

	private void printlnInfo(String s) {
		System.out.println(s);
		debugPW.println(s);
	}

	private void printInfo(String s) {
		System.out.print(s);
		debugPW.print(s);
	}

}

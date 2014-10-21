package chord.analyses.provenance.kobj;

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
import chord.analyses.alloc.DomH;
import chord.analyses.argret.DomK;
import chord.analyses.invk.DomI;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.ITask;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.analyses.provenance.DefaultModel;
import chord.project.analyses.provenance.MaxSatGenerator;
import chord.project.analyses.provenance.Model;
import chord.project.analyses.provenance.Tuple;

/**
 * A general class to run experiments based on k-obj analysis.
 * -Dchord.provenance.client=<polysite/downcast/datarace>: specify the client to use
 * -Dchord.provenance.obj2=<true/false>: specify whether to run only with the queries solvable using 2-OBJ
 * -Dchord.provenance.queryOption=<all/separate/single>: specify the way to solve queries
 * -Dchord.provenance.heap=<true/false>: specify whether to turn on heap-cloning
 * -Dchord.provenance.mono=<true/false>: specify whether to monotonically grow the k values
 * -Dchord.provenance.queryWeight=<Integer>: specify the weight we want to use for queries; if -1, treat them as hard constraints.
 * -Dchord.provenance.model=<default>: specify what model to use to bias the refinement. Default: default(no bias)
 * If 0, use the sum(input weight) + 1
 * -Dchord.provenance.boolDomain=<true/false>: specify whether we want to use boolean domain based kcfa
 * -Dchord.provenance.invkK=<2>: if we use boolean domain, what is the k value we want for invoke sites
 * -Dchord.provenance.allocK=<2>: if we use boolean domain, what is the k value we want for alloc sites 
 * -Dchord.provenance.numQueries: randomly track given number of queries, default(-1) to track all queries
 * @author xin
 * 
 */
@Chord(name = "kobj-refiner")
public class KOBJRefiner extends JavaAnalysis {
	DomI domI;
	DomH domH;
	DomK domK;
	Map<Tuple, Map<Quad, Integer>> absMap;
	Set<Tuple> unresolvedQs = new HashSet<Tuple>();
	Set<Tuple> impossiQs = new HashSet<Tuple>();
	MaxSatGenerator gen;
	String[] configFiles;

	ProgramRel IKRel;
	ProgramRel HKRel;
	ProgramRel OKRel;
	ProgramRel allowHRel;
	ProgramRel denyHRel;
	ProgramRel allowORel;
	ProgramRel denyORel;
	ProgramRel queryRel;
	List<ITask> tasks;
	PrintWriter debugPW;
	PrintWriter statPW;

	int client; // 0 polysite, 1 downcast, 2 datarace, 3. pts

	String clientFile;
	String clientConfigPath;
	String queryRelName;
	String modelStr;
	
	boolean ifCfa2;
	boolean ifHeap;
	boolean ifMono;
	boolean ifBool;
	int invkK;
	int allocK;

	static int iterLimit = 100;//max number of refinement iterations allowed
	static int max = 20; //max number of k value for both OK and HK
	
	int queryWeight;
	int numQueries;

	@Override
	public void run() {
		try {
			debugPW = new PrintWriter(new File(Config.outDirName + File.separator + "debug.txt"));
			statPW = new PrintWriter(new File(Config.outDirName+File.separator+"stat.txt"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		String client = System.getProperty("chord.provenance.client");
		if (client.equals("polysite")) {
			this.client = 0;
			clientFile = "polysite-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/monosite/polysite-dlog_XZ89_.config";
			queryRelName = "polySite";
		} else if (client.equals("downcast")) {
			this.client = 1;
			clientFile = "pro-downcast-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/downcast/pro-downcast-dlog_XZ89_.config";
			queryRelName = "unsafeDowncast";
		} else if (client.equals("datarace")){
			this.client = 2;
			clientFile = "pro-datarace-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/race/pro-datarace-dlog_XZ89_.config";
			queryRelName = "racePairs";
		}else if (client.equals("pts")){
			this.client = 3;
			clientFile = "pro-pts-dlog_XZ89_";
			clientConfigPath = "src/chord/analyses/provenance/pts/pro-pts-dlog_XZ89_.config";
			queryRelName = "ptsVH";
		}else
			throw new RuntimeException("Unknown client: " + this.client);

		//The analyses we need to run
		tasks = new ArrayList<ITask>();
		tasks.add(ClassicProject.g().getTask("cipa-0cfa-dlog"));
		tasks.add(ClassicProject.g().getTask("simple-pro-ctxts-java"));
		tasks.add(ClassicProject.g().getTask("pro-argCopy-dlog"));
		tasks.add(ClassicProject.g().getTask("kobj-bit-init-dlog_XZ89_"));
		tasks.add(ClassicProject.g().getTask("pro-cspa-kobj-dlog_XZ89_"));
		tasks.add(ClassicProject.g().getTask(clientFile));

		System.setProperty("chord.ctxt.kind", "co");
		System.setProperty("chord.kobj.khighest", "" + max);
		System.setProperty("chord.kcfa.khighest", "" + max);
		String chordMain = System.getenv("CHORD_MAIN");
		String kinitConfig = chordMain + File.separator + "src/chord/analyses/provenance/kobj/kobj-bit-init-dlog_XZ89_.config";
		String kobjConfig = chordMain + File.separator + "src/chord/analyses/provenance/kobj/pro-cspa-kobj-dlog_XZ89_.config";
		String clientConfig = chordMain + File.separator + clientConfigPath;
		configFiles = new String[]{ kinitConfig, kobjConfig, clientConfig };

		IKRel = (ProgramRel) ClassicProject.g().getTrgt("IK");
	
		HKRel = (ProgramRel) ClassicProject.g().getTrgt("HK");
		OKRel = (ProgramRel) ClassicProject.g().getTrgt("OK");
		allowHRel = (ProgramRel) ClassicProject.g().getTrgt("AllowH");
		denyHRel = (ProgramRel) ClassicProject.g().getTrgt("DenyH");
		allowORel = (ProgramRel) ClassicProject.g().getTrgt("AllowO");
		denyORel = (ProgramRel) ClassicProject.g().getTrgt("DenyO");
		domI = (DomI) ClassicProject.g().getTrgt("I");
		domK = (DomK) ClassicProject.g().getTrgt("K");
		domH = (DomH) ClassicProject.g().getTrgt("H");
		queryRel = (ProgramRel) ClassicProject.g().getTrgt(queryRelName);
		ClassicProject.g().runTask(domI);
		ClassicProject.g().runTask(domK);
		ClassicProject.g().runTask(domH);
		absMap = new HashMap<Tuple, Map<Quad, Integer>>();

		IKRel.zero();
		for (int i = 0; i < domI.size(); i++) {
			Quad I = (Quad) domI.get(i);
			IKRel.add(I,0);
		}
		IKRel.save();	
		
		String opt = System.getProperty("chord.provenance.queryOption", "all");
		ifCfa2 = Boolean.getBoolean("chord.provenance.obj2");
		ifHeap = Boolean.getBoolean("chord.provenance.heap");
		ifMono = Boolean.getBoolean("chord.provenance.mono");
		queryWeight = Integer.getInteger("chord.provenance.queryWeight", MaxSatGenerator.QUERY_HARD);
		ifBool = Boolean.getBoolean("chord.provenance.boolDomain");
		invkK = Integer.getInteger("chord.provenance.invkK",2);
		allocK = Integer.getInteger("chord.provenance.allocK",2);
		numQueries = Integer.getInteger("chord.provenance.numQueries", -1);
		
		modelStr = System.getProperty("chord.provenance.model", "default");
		
		System.out.println("chord.provenance.queryOption = "+opt);
		System.out.println("chord.provenance.obj2 = "+ifCfa2);
		System.out.println("chord.provenance.mono = "+ifMono);
		System.out.println("chord.provenance.queryWeight = "+queryWeight);
		System.out.println("chord.provenance.boolDomain = "+ifBool);
		System.out.println("chord.provenance.invkK = "+invkK);
		System.out.println("chord.provenance.allocK = "+allocK);
		System.out.println("chord.provenance.numQueries = "+numQueries);
		System.out.println("chord.provenance.model = "+modelStr);
		
		//Initialize the queries
		unresolvedQs = this.runClientWithK(0);
		if(ifCfa2){//if we want to solve 2-CFA queries only
			Set<Tuple> cfa2qs = runClientWithK(2);
			unresolvedQs.removeAll(cfa2qs);
		}
		
		if(numQueries != -1){
			List<Tuple> allQs = new ArrayList<Tuple>(unresolvedQs);
			Collections.shuffle(allQs);
			Set<Tuple> ret = new HashSet<Tuple>();
	    	for(int i = 0; i < numQueries && i < allQs.size(); i++){
	    		Tuple chosenQuery = allQs.get(i);
	    		ret.add(chosenQuery);
	    	}
	    	unresolvedQs.retainAll(ret);
		}
		
		for (Tuple t : unresolvedQs) {// put empty abstraction map for each query
			absMap.put(t, new HashMap<Quad, Integer>());
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
			// DBG Tuple t = new Tuple("polySite(64)");
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
	
	private MaxSatGenerator createMaxSatGenerator(PTHandler ptHandler, int queryWeight) {
		Model model;
		if(modelStr.endsWith("default"))
			model = new DefaultModel();
		else
			throw new RuntimeException("A model must be specified to bias the refinement!");
		MaxSatGenerator g = new MaxSatGenerator(configFiles, queryRelName, ptHandler, model, queryWeight);
		return g;
	}

	private void runAll() {
		//Set up MaxSatGenerator
		gen = createMaxSatGenerator(new PTHandler(ifMono, ifBool), queryWeight);
		gen.DEBUG = false;
		PTHandler.max = max;
		int numIter = 0;
		int kcfaImp = 0;
		int totalQs = unresolvedQs.size();
		while (unresolvedQs.size() != 0) {
			if(ifMono)
				gen = createMaxSatGenerator(new PTHandler(ifMono, ifBool), queryWeight);
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
			Map<Quad, Integer> abs = absMap.get(unresolvedQs.iterator().next());
			int totalKs = 0;
			for(Map.Entry<Quad, Integer> entry : abs.entrySet())
				totalKs += entry.getValue();
			printlnInfo("Total k values: "+totalKs);
			printlnInfo(abs.toString());
			runAnalysis(abs);
			Set<Tuple> hardQueries = updateAllQs(numIter);
			if(hardQueries.size() != 0){
//				MaxSatGenerator current = gen;
				printlnInfo("********************************");
				printlnInfo("Some queries might be impossible (indeed impossible using complete max sat solver:");
				printlnInfo(hardQueries.toString());
//				if(!ifMono)
//					updateAbsMap(hardQueries,new HashSet<Tuple>());
//				for(Tuple t : hardQueries)
//					runSingle(t);
				if(ifBool){
					gen = null;//save some memory
					System.out.println("Let's check if they're impossible because of the boolean domain limitaion");
					//Attempt to solve in one run
					MaxSatGenerator tempGen = createMaxSatGenerator(new PTHandler(ifMono,false), MaxSatGenerator.QUERY_MAX);
					tempGen.update(hardQueries);
					Set<Tuple> tupleToEli = tempGen.solve(hardQueries, -1+"");
					tupleToEli.retainAll(hardQueries);
					System.out.println("Queries unsovable using KCFA: "+tupleToEli);
					kcfaImp += tupleToEli.size();
				}
				impossiQs.addAll(hardQueries);
				printlnInfo("********************************");
				printlnInfo("Coming back to the normal loop");
//				gen = current; // some recover, not entirely necessary
			}
			numIter++;
		}
		int impossiNum = impossiQs.size();
		int provNum = 0;
		printlnInfo("Impossible Qs: " + impossiQs);
		System.out.println("Proven Qs: ");
		for (Map.Entry<Tuple, Map<Quad, Integer>> entry : absMap.entrySet()) {
			Tuple t = entry.getKey();
			if (!impossiQs.contains(t)) {
				printlnInfo("Query: " + t + ", " + entry.getValue());
				provNum++;
			}
		}
		System.out.println("Proven num: "+provNum);
		System.out.println("Impossi num: "+impossiNum);
		System.out.println("KOBJ impossi num: "+kcfaImp);
		statPW.println(numIter+" 0 "+provNum+" "+impossiNum+" "+kcfaImp);
		statPW.flush();
	}

	private Set<Tuple> updateAllQs(int numIter) {
		Set<Tuple> nrqs = tuples(queryRel);
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
			Map<Quad, Integer> abs = absMap.get(t);
			if (abs == null) {
				abs = new HashMap<Quad, Integer>();
				absMap.put(t, abs);
			}
			if(!ifMono)
				abs.clear();
			for (Tuple t1 : tupleToEli) {
				if (t1.getRelName().equals("DenyO")) {
					Quad H = (Quad) t1.getValue(0);
					Integer K = (Integer) t1.getValue(1);
					if(ifBool)
						K = invkK;
					Integer ek = abs.get(H);
					if(ek == null)
						abs.put(H, K);
					else
						if(K > ek)
							abs.put(H, K);
				} else if (t1.getRelName().equals("DenyH")) {
					Quad H = (Quad) t1.getValue(0);
					Integer K = (Integer) t1.getValue(1);
					if(ifBool)
						K = allocK;
					Integer ek = abs.get(H);
					if(ek == null)
						abs.put(H, K);
					else
						if(K > ek)
							abs.put(H, K);
				} else
					if(!t1.getRelName().equals(this.queryRelName))
						throw new RuntimeException("Unexpected param tuple: " + t1);
			}
		}
	}


	private void setHK(Quad q, int k){
		HKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowHRel.add(q,i);
		}
		for(int i = k+1; i <= max; i++){
			denyHRel.add(q,i);
		}
	}
	
	private void setOK(Quad q, int k){
		OKRel.add(q,k);
		for(int i = 0; i <= k; i++){
			allowORel.add(q,i);
		}
		for(int i = k+1; i <= max; i++){
			denyORel.add(q,i);
		}	
	}
	
	/**
	 * Run the analysis with the abstraction map specified in the parameter
	 * @param abs
	 */
	private void runAnalysis(Map<Quad, Integer> abs) {
		HKRel.zero();
		allowHRel.zero();
		denyHRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Quad H = (Quad) domH.get(i);
			Integer k = abs.get(H);
			if (k == null||k==0)
				k = 1;
			setHK(H,k);
		}
		HKRel.save();
		allowHRel.save();
		denyHRel.save();

		OKRel.zero();
		allowORel.zero();
		denyORel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Quad H = (Quad) domH.get(i);
			Integer k = abs.get(H);
			if (k == null)
				k = 0;
			setOK(H,k);
		}
		OKRel.save();
		allowORel.save();
		denyORel.save();
		
		runAllTasks();
	}

	private void runSeparate() {
		for (Tuple q : unresolvedQs) {
			runSingle(q);
		}
	}


	private boolean updateQuery(Tuple t, int numIter) {
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

	private Set<Tuple> run0CFA(){
		ClassicProject.g().runTask("cipa-0cfa-dlog");
		ClassicProject.g().runTask("ci-polysite-dlog");
		queryRel.load();
		return tuples(queryRel);
	}
	
	private Set<Tuple> runClientWithK(int k) {
		int hk;
		if(k == 0)
			hk = 1;
		else
			hk = k;
		System.out.println("k: "+k);
		System.out.println("hk: "+hk);
		HKRel.zero();
		allowHRel.zero();
		denyHRel.zero();
		for (int i = 0; i < domH.size(); i++) {
			Quad H = (Quad) domH.get(i);
			setHK(H,hk);
		}
		HKRel.save();
		allowHRel.save();
		denyHRel.save();
		
		OKRel.zero();
		allowORel.zero();
		denyORel.zero();
		for(int i = 0; i < domH.size(); i++){
			Quad H = (Quad) domH.get(i);
			setOK(H,k);
		}
		OKRel.save();
		allowORel.save();
		denyORel.save();
	
		runAllTasks();
		return tuples(queryRel);
	}

	private void runSingle(Tuple q) {
		printlnInfo("Processing query: " + q);
		int numIter = 0;
		gen = createMaxSatGenerator(new PTHandler(ifMono,ifBool), MaxSatGenerator.QUERY_HARD);
		gen.DEBUG = false;
		while (true) {
			if(ifMono)
				gen = createMaxSatGenerator(new PTHandler(ifMono, ifBool), MaxSatGenerator.QUERY_HARD);
			printlnInfo("===============================================");
			printlnInfo("===============================================");
			printlnInfo("Iteration: " + numIter);
			Map<Quad, Integer> abs = absMap.get(q);
			printlnInfo("Abstraction used: " + abs);
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

	private static Set<Tuple> tuples(final ProgramRel r) {
		Set<Tuple> ts = new HashSet<Tuple>();
		for (int[] args : r.getAryNIntTuples())
			ts.add(new Tuple(r, args));
		return ts;
	}

	private final void runAllTasks() {
		for (ITask t : tasks) {
			ClassicProject.g().resetTaskDone(t);
			ClassicProject.g().runTask(t);
		}
		queryRel.load();
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

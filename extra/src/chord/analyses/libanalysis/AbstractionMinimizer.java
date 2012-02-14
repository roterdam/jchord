package chord.analyses.libanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import chord.util.Execution;
import chord.util.Utils;

public class AbstractionMinimizer implements JobDispatcher {

	Execution EX = Execution.v();
	Random random = new Random();
	String defaults;
	String sepMaj,sepMin;

	int C;
	Abstraction[] bottomX, topX;
	QueryFactory qFactory;
	AbstractionFactory aFactory;
	HashSet<String> y2queries;
	HashSet<String> allQueries = null;
	Set<Query> qSet;
	double initIncrProb = 0.5;
	double incrThetaStepSize = 0.1;
	int scanThreshold = 30;

	int numScenarios = 0; // Total number of calls to the analysis oracle
	List<Group> groups = new ArrayList<Group>();

	Set<String> allY() { return y2queries; }

	public AbstractionMinimizer(boolean isScan, Set<Query> qSet, Set<Abstraction> aSet, QueryFactory qFactory, AbstractionFactory aFactory, BlackBox box, String sepMaj, String sepMin) {
		this.sepMaj = sepMaj;
		this.sepMin = sepMin;
		this.C = aSet.size();
		this.qFactory = qFactory;
		this.aFactory = aFactory;
		this.qSet = qSet;
		this.bottomX = new Abstraction[C];
		this.topX = new Abstraction[C];
		int c = 0;
		
		if(qSet != null){
			this.allQueries = new HashSet<String>();
			for(Query q: qSet){
				allQueries.add(q.encode());
			}
		}
		
		for (Abstraction a : aSet) {
			bottomX[c] = aFactory.create(a);
			bottomX[c].minRefine();
			topX[c] = aFactory.create(a);
			topX[c].maxRefine();
			c++;
		}

		Scenario bottomSIn = new Scenario("1",Abstraction.concatAbstractions(encodeX(bottomX),sepMin),null,sepMaj);
		Scenario bottomSOut = new Scenario(box.apply(bottomSIn.toString()),sepMaj);
		HashSet<String> bottomY = new HashSet<String>(Arrays.asList(Query.splitQueries(bottomSOut.getOut(),sepMin)));
		
		Scenario topSIn = new Scenario("1",Abstraction.concatAbstractions(encodeX(topX),sepMin),null,sepMaj);;
		Scenario topSOut = new Scenario(box.apply(topSIn.toString()),sepMaj);
		HashSet<String> topY = new HashSet<String>(Arrays.asList(Query.splitQueries(topSOut.getOut(),sepMin)));
		
		EX.logs("bottom: %s tuples", bottomY.size());
		EX.logs("top: %s tuples", topY.size());


		// Keep only queries that bottom was unable to prove but top was able to prove
		this.y2queries = new HashSet();
		for(String y: topY){
			if (!bottomY.contains(y)){ // Unproven by bottom, proven by top
				if(allQueries == null)
					y2queries.add(y);
				else if(allQueries.contains(y))
					y2queries.add(y);
				
			}
		}

		EX.logs("|Y| = %s", y2queries.size());
		EX.putOutput("numY", y2queries.size());
		EX.putOutput("topComplexity", complexity(topX));

		groups.add(new Group(isScan,y2queries));

		outputStatus();
		loadGroups();
		loadScenarios();
	}

	void loadGroups() {
		String path = EX.path("groups");
		if (!new File(path).exists()) return;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			String line;
			groups.clear();
			while ((line = in.readLine()) != null) {
				// Format: <isScan> ## <step size> ## <lower> ## <upper> ## queries
				String[] tokens = line.split("##");
				Group g = new Group(Boolean.parseBoolean(tokens[0]),new HashSet<String>(Arrays.asList(Query.splitQueries(tokens[4], sepMin))));
				g.incrTheta = invLogistic(Double.parseDouble(tokens[1]));
				g.lowerX = decodeX(Abstraction.splitAbstractions(tokens[2], sepMin));
				g.upperX = decodeX(Abstraction.splitAbstractions(tokens[3], sepMin));
				g.updateStatus();
				g.updateStatus();
				groups.add(g);
			}
			outputStatus();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void loadScenarios() {
		String scenariosPath = EX.path("scenarios");
		if (!new File(scenariosPath).exists()) return;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(scenariosPath)));
			String line;
			while ((line = in.readLine()) != null)
				incorporateScenario(new Scenario(line,sepMaj), false);
			in.close();
			outputStatus();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	String[] encodeX(Abstraction[] X) {
		String[] enc = new String[X.length];
		//buf.append(defaults);
		for (int c = 0; c < C; c++) {
			enc[c] = new String(X[c].encode());
		}
		return enc;
	}
	
	Abstraction[] decodeX(String[] x) {
		Abstraction[] X = new Abstraction[C];
		int c = 0;
		for (String s : x) {
			X[c] = this.aFactory.create(s);
			c++;				
		}
		return X;
	}
	
	/*String encodeX(Abstraction[] X) {
		StringBuilder buf = new StringBuilder();
		//buf.append(defaults);
		for (int c = 0; c < C; c++) {
			if (buf.length() > 0) buf.append(' ');
			buf.append(X[c].encode());
		}
		return buf.toString();
	}
	String encodeY(Set<String> Y) {
		StringBuilder buf = new StringBuilder();
		for (String y : Y) {
			if (buf.length() > 0) buf.append(' ');
			buf.append(y);
		}
		return buf.toString();
	}
	Abstraction[] decodeX(String line) {
		Abstraction[] X = new Abstraction[C];
		int c = 0;
		for (String s : line.split(" ")) {
			X[c] = this.aFactory.create(s);
			c++;
					
		}
		return X;
	}

	HashSet<String> decodeY(String line) {
		HashSet<String> Y = new HashSet<String>();
		for (String y : line.split(" ")) Y.add(y);
		return Y;
	}*/

	// General utilities
	int complexity(Abstraction[] X) {
		int sum = 0;
		for (int c = 0; c < C; c++) {
			assert X[c].getLevel() >= bottomX[c].getLevel() : c + " " + X[c].getLevel() + " " + bottomX[c].getLevel();
			sum += X[c].getLevel() - bottomX[c].getLevel();
		}
		return sum;
	}
	Abstraction[] copy(Abstraction[] X) {
		Abstraction[] newX = new Abstraction[C];
		//System.arraycopy(X, 0, newX, 0, C);
		for(int c = 0; c < C; c++){
			newX[c] = this.aFactory.create(X[c]);
		}
		return newX;
	}
	void set(Abstraction[] X1, Abstraction[] X2) { 
		for(int c = 0; c < C; c++){
			X1[c].copy(X2[c]);
		} 
	}
	boolean eq(Abstraction[] X1, Abstraction[] X2) {
		for (int c = 0; c < C; c++)
			if (!X1[c].equals(X2[c])) return false;
		return true;
	}
	boolean lessThanEq(Abstraction[] X1, Abstraction[] X2) {
		for (int c = 0; c < C; c++)
			if (X1[c].getLevel() > X2[c].getLevel()) return false;
		return true;
	}
	int findUniqueDiff(Abstraction[] X1, Abstraction[] X2) {
		int diffc = -1;
		for (int c = 0; c < C; c++) {
			int d = Math.abs(X1[c].getLevel()-X2[c].getLevel());
			if (d > 1) return -1; // Not allowed
			if (d == 1) {
				if (diffc != -1) return -1; // Can't have two diff
				diffc = c;
			}
		}
		return diffc;
	}

	double logistic(double theta) { return 1/(1+Math.exp(-theta)); }
	double invLogistic(double mu) { return Math.log(mu/(1-mu)); }

	class Group {
		boolean done;
		boolean scanning;
		Abstraction[] lowerX;
		Abstraction[] upperX;
		HashSet<String> Y; // Unproven 
		double incrTheta; // For the step size
		HashMap<Integer,Integer> jobCounts; // job ID -> number of jobs in the queue at the time when this job was created

		boolean inRange(Abstraction[] X) { return lessThanEq(lowerX, X) && lessThanEq(X, upperX); }

		@Override public String toString() {
			String status = done ? "done" : (scanning ? "scan" : "rand");
			return String.format("Group(%s,%s<=|X|<=%s,|Y|=%s,incrProb=%.2f,#wait=%s)",
					status, complexity(lowerX), complexity(upperX), Y.size(), logistic(incrTheta), jobCounts.size());
		}

		public Group(boolean isScan, HashSet<String> Y) {
			this.done = false;
			this.scanning = isScan;
			this.lowerX = copy(bottomX);
			this.upperX = copy(topX);
			this.Y = Y;
			this.incrTheta = invLogistic(initIncrProb);
			this.jobCounts = new HashMap();
		}

		public Group(Group g, HashSet<String> Y) {
			this.done = g.done;
			this.scanning = g.scanning;
			this.lowerX = copy(g.lowerX);
			this.upperX = copy(g.upperX);
			this.Y = Y;
			this.incrTheta = g.incrTheta;
			this.jobCounts = new HashMap(g.jobCounts);
		}

		boolean wantToLaunchJob() {
			if (done) return false;
			if (scanning) return jobCounts.size() == 0; // Don't parallelize
			return true;
		}

		Scenario createNewScenario() {
			double incrProb = logistic(incrTheta);
			EX.logs("createNewScenario %s: incrProb=%.2f", this, incrProb);
			if (scanning) {
				if (jobCounts.size() == 0) { // This is sequential - don't waste energy parallelizing
					int diff = complexity(upperX) - complexity(lowerX);
					assert diff > 0 : diff;
					int target_j = random.nextInt(diff);
					EX.logs("Scanning: dipping target_j=%s of diff=%s", target_j, diff);
					// Sample a minimal dip from upperX
					int j = 0;
					Abstraction[] X = new Abstraction[C];
					for (int c = 0; c < C; c++) {
						X[c] = aFactory.create(lowerX[c]);
						for (int i = lowerX[c].getLevel(); i < upperX[c].getLevel(); i++, j++)
							if (j != target_j) X[c].refine();
					}
					return createScenario(X);
				}
				else {
					EX.logs("Scanning: not starting new job, still waiting for %s (shouldn't happen)", jobCounts.keySet());
					return null;
				}
			}
			else {
				// Sample a random element between the upper and lower bounds
				Abstraction[] X = new Abstraction[C];
				for (int c = 0; c < C; c++) {
					X[c] = aFactory.create(lowerX[c]);
					for (int i = lowerX[c].getLevel(); i < upperX[c].getLevel(); i++)
						if (random.nextDouble() < incrProb) X[c].refine();
				}
				if (!eq(X, lowerX) && !eq(X, upperX)) // Don't waste time
					return createScenario(X);
				else
					return null;
			}
		}

		Scenario createScenario(Abstraction[] X) {
			//Scenario scenario = new Scenario(encodeX(X), encodeY(Y));
			Scenario scenario = new Scenario("1",Abstraction.concatAbstractions(encodeX(X),sepMin), Query.concatQueries(allY().toArray(new String[0]),sepMin),sepMaj); // Always include all the queries, otherwise, it's unclear what the reference set is
			jobCounts.put(scenario.id, 1+jobCounts.size());
			return scenario;
		}

		void update(int id, Abstraction[] X, boolean unproven) {
			if (done) return;

			// Update refinement probability to make p(y=1) reasonable
			// Only update probability if we were responsible for launching this run
			// This is important in the initial iterations when getting data for updateLower to avoid polarization of probabilities.
			if (jobCounts.containsKey(id)) {
				double oldIncrProb = logistic(incrTheta);
				double singleTargetProb = Math.exp(-1); // Desired p(y=1)

				// Exploit parallelism: idea is that probability that two of the number of processors getting y=1 should be approximately p(y=1)
				double numProcessors = jobCounts.size(); // Approximate number of processors (for this group) with the number of things in the queue.
				//double targetProb = 1 - Math.pow(1-singleTargetProb, 1.0/numProcessors);
				double targetProb = 1 - Math.pow(1-singleTargetProb, 1.0/Math.sqrt(numProcessors+1)); // HACK

				// Due to parallelism, we want to temper the amount of probability increment
				double stepSize = incrThetaStepSize; // Simplify
				//double stepSize = incrThetaStepSize / Math.sqrt(jobCounts.get(id)); // Size of jobCounts at the time the job was created
				if (!unproven) incrTheta -= (1-targetProb) * stepSize; // Proven -> cheaper abstractions
				else incrTheta += targetProb * stepSize; // Unproven -> more expensive abstractions

				EX.logs("    targetProb = %.2f (%.2f eff. proc), stepSize = %.2f/sqrt(%d) = %.2f, incrProb : %.2f -> %.2f [unproven=%s]",
						targetProb, numProcessors,
						incrThetaStepSize, jobCounts.get(id), stepSize,
						oldIncrProb, logistic(incrTheta), unproven);
				jobCounts.remove(id);
			}

			// Detect minimal dip: negative scenario that differs by upperX by one site (that site must be necessary)
			// This should only really be done in the scanning part
			if (unproven) {
				int c = findUniqueDiff(X, upperX);
				if (c != -1) {
					EX.logs("    updateLowerX %s: found that c=%s is necessary", this, upperX[c].encode());
					lowerX[c] = upperX[c];
				}
			}
			else { // Proven
				EX.logs("    updateUpperX %s: reduced |upperX|=%s to |upperX|=%s", this, complexity(upperX), complexity(X));
				set(upperX, X);
			}

			updateStatus();
		}

		void updateStatus() {
			if (scanning) {
				if (eq(lowerX, upperX)) {
					EX.logs("    DONE with group %s!", this);
					done = true;
				}
			}
			else {
				int lowerComplexity = complexity(lowerX);
				int upperComplexity = complexity(upperX);
				int diff = upperComplexity-lowerComplexity;

				if (upperComplexity == 1) { // Can't do better than 1
					EX.logs("    DONE with group %s!", this);
					done = true;
				}
				else if (diff <= scanThreshold) {
					EX.logs("    SCAN group %s now!", this);
					scanning = true;
				}
			}
		}
	}

	int sample(double[] weights) {
		double sumWeight = 0;
		for (double w : weights) sumWeight += w;
		double target = random.nextDouble() * sumWeight;
		double accum = 0;
		for (int i = 0; i < weights.length; i++) {
			accum += weights[i];
			if (accum >= target) return i;
		}
		throw new RuntimeException("Bad");
	}

	List<Group> getCandidateGroups() {
		List<Group> candidates = new ArrayList();
		for (Group g : groups)
			if (g.wantToLaunchJob())
				candidates.add(g);
		return candidates;
	}

	public Scenario createJob() {
		List<Group> candidates = getCandidateGroups();
		if (candidates.size() == 0) return null;
		// Sample a group proportional to the number of effects in that group
		// This is important in the beginning to break up the large groups
		double[] weights = new double[candidates.size()];
		for (int i = 0; i < candidates.size(); i++)
			weights[i] = candidates.get(i).Y.size();
		int chosen = sample(weights);
		Group g = candidates.get(chosen);
		return g.createNewScenario();
	}

	public boolean isDone() {
		for (Group g : groups)
			if (!g.done) return false;
		return true;
	}

	public void onJobResult(Scenario scenario) {
		incorporateScenario(scenario, true);
		outputStatus();
	}

	String render(Abstraction[] X, Set<String> Y) { return String.format("|X|=%s,|Y|=%s", complexity(X), Y.size()); }

	// Incorporate the scenario into all groups
	void incorporateScenario(Scenario scenario, boolean saveToDisk) {
		numScenarios++;
		if (saveToDisk) {
			PrintWriter f = Utils.openOutAppend(EX.path("scenarios"));
			f.println(scenario);
			f.close();
		}

		Abstraction[] X = decodeX(Abstraction.splitAbstractions(scenario.getIn(),sepMin));
		Set<String> Y = new HashSet<String>(Arrays.asList(Query.splitQueries(scenario.getOut(),sepMin)));

		EX.logs("Incorporating scenario id=%s,%s into %s groups (numScenarios = %s)", scenario.id, render(X, Y), groups.size(), numScenarios);
		List<Group> newGroups = new ArrayList();
		boolean changed = false;
		for (Group g : groups)
			changed |= incorporateScenario(scenario.id, X, Y, g, newGroups);
		groups = newGroups;
		if (!changed) // Didn't do anything - probably an outdated scenario
			EX.logs("  Useless: |X|=%s,|Y|=%s", complexity(X), Y.size());
	}

	// Incorporate into group g
	boolean incorporateScenario(int id, Abstraction[] X, Set<String> Y, Group g, List<Group> newGroups) {
		// Don't need this since Y is with respect to allY
		// Don't update on jobs we didn't ask for! (Important because we are passing around subset of queries which make sense only with respect to the group that launched the job)
		/*if (!g.jobCounts.containsKey(id)) {
      newGroups.add(g);
      return false;
    }*/

		if (!g.inRange(X)) { // We asked for this job, but now it's useless
			g.jobCounts.remove(id);
			newGroups.add(g);
			return false;
		}

		// Now we can make an impact
		EX.logs("  into %s", g);

		HashSet<String> Y0 = new HashSet<String>();
		HashSet<String> Y1 = new HashSet<String>();
		for (String y : g.Y) {
			if (Y.contains(y)) Y0.add(y);
			else               Y1.add(y);
		}
		if (Y0.size() == 0 || Y1.size() == 0) { // Don't split: all of Y still behaves the same
			assert !(Y0.size() == 0 && Y1.size() == 0); // At least one must be true
			g.update(id, X, Y1.size() > 0);
			newGroups.add(g);
		}
		else {
			Group g0 = new Group(g, Y0);
			Group g1 = new Group(g, Y1);
			g0.update(id, X, false);
			g1.update(id, X, true);
			newGroups.add(g0);
			newGroups.add(g1);
		}
		return true;
	}

	void outputStatus() {
		int numDone = 0, numScanning = 0;
		for (Group g : groups) {
			if (g.done) numDone++;
			else if (g.scanning) numScanning++;
		}

		EX.putOutput("numScenarios", numScenarios);
		EX.putOutput("numDoneGroups", numDone);
		EX.putOutput("numScanGroups", numScanning);
		EX.putOutput("numGroups", groups.size());

		// Print groups
		EX.logs("%s groups", groups.size());
		int sumComplexity = 0;
		Abstraction[] X = new Abstraction[C];
		int trackItr = 0;
		for (Group g : groups) {
			EX.logs("  %s", g);
			sumComplexity += complexity(g.upperX);
			if(trackItr == 0){
				for (int c = 0; c < C; c++)
					X[c] = g.upperX[c];
			}else{
				for (int c = 0; c < C; c++)
					X[c] = X[c].getLevel() > g.upperX[c].getLevel()?X[c]:g.upperX[c];
			}
			trackItr++;
		}
		EX.putOutput("sumComplexity", sumComplexity);
		EX.putOutput("complexity", complexity(X));

		EX.flushOutput();
	}

	public void saveState() {
		// Save to disk
		{
			PrintWriter out = Utils.openOut(EX.path("groups"));
			for (Group g : groups)
				out.println(g.scanning + "##" + logistic(g.incrTheta) + "##" + Abstraction.concatAbstractions(encodeX(g.lowerX),sepMin) + "##" + Abstraction.concatAbstractions(encodeX(g.upperX),sepMin) + "##" + Query.concatQueries(g.Y.toArray(new String[0]), sepMin));
			out.close();
		}
		{
			PrintWriter out = Utils.openOut(EX.path("groups.txt"));
			for (Group g : groups) {
				out.println("=== "+g);
				out.println("Abstractions:");
				for (int c = 0; c < C; c++)
					if (g.upperX[c].getLevel() != bottomX[c].getLevel())
						out.println("  "+g.upperX[c].encode() + "##" + g.upperX[c].toString());
				out.println("Queries:");
				for (String y : g.Y) {
					//Query q = y2queries.get(y);
					out.println("  "+y);
				}
			}
			out.close();
		}
	}

	public int maxWorkersNeeded() {
		// If everyone's scanning, just need one per scan
		// Otherwise, need as many workers as we can get.
		int n = 0;
		for (Group g : groups) {
			if (g.done) continue;
			if (g.scanning) n++;
			else return 10000; // Need a lot
		}
		return n;
	}

}

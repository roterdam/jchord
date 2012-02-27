package chord.analyses.libanalysis;

public interface JobDispatcher {
	Scenario createJob(); // Ask dispatcher for job
	void onJobResult(Scenario scenario); // Return job result
	boolean isDone();
	void saveState();
	int maxWorkersNeeded();
}

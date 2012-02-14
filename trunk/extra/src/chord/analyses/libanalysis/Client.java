package chord.analyses.libanalysis;

public interface Client {
	Scenario createJob(); // Ask client for job
	void onJobResult(Scenario scenario); // Return job result
	boolean isDone();
	void saveState();
	int maxWorkersNeeded();
}

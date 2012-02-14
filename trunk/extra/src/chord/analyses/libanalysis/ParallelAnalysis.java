package chord.analyses.libanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

import chord.project.analyses.JavaAnalysis;
import chord.util.Execution;

interface BlackBox {
	public String apply(String line);
}

abstract class ParallelAnalysis extends JavaAnalysis implements BlackBox {

	Execution X;
	String masterHost;
	int masterPort;
	String mode; // worker or master or null
	Client client = null;

	protected abstract void init();
	protected void finish(){
		finishAnalysis();
		X.finish(null);
	}
	protected abstract void finishAnalysis();
	protected abstract String setMasterHost();
	protected abstract int setMasterPort();
	protected abstract String setMode();
	protected abstract Client setClient();
	
	
	public abstract String apply(String line);
	//public abstract void readQueries(ProgramRel rel, Collection<T> queries, QueryFactory<T> factory);

	void sleep(int seconds) {
		try { Thread.sleep(seconds*1000); } catch(InterruptedException e) { }
	}
		
	
	boolean isMaster() { return mode != null && mode.equals("master"); }
	boolean isWorker() { return mode != null && mode.equals("worker"); }

	public void run() {
		X = Execution.v();
		init();
		masterHost = setMasterHost();
		masterPort = setMasterPort();
		mode = setMode();
		
		if (isWorker()) runWorker();
		else{
			client = setClient();
			if(client == null){
				Exception e = new Exception("Client not initialized in Master");
				throw new RuntimeException(e);
			}
			new Master(masterPort, client);
		}
		finish();
	}
	String callMaster(String line) {
		try {
			Socket master = new Socket(masterHost, masterPort);
			BufferedReader in = new BufferedReader(new InputStreamReader(master.getInputStream()));
			PrintWriter out = new PrintWriter(master.getOutputStream(), true);
			out.println(line);
			out.flush();
			line = in.readLine();
			in.close();
			out.close();
			master.close();
			return line;
		} catch (IOException e) {
			return null;
		}
	}

	void runWorker() {
		X.logs("Starting worker...");
		int numJobs = 0;
		while (true) {
			X.logs("============================================================");
			// Get a job
			String line = callMaster("GET");
			if (line == null) { X.logs("Got null, something bad happened to master..."); this.sleep(5); }
			else if (line.equals("WAIT")) { X.logs("Waiting..."); this.sleep(5); X.putOutput("exec.status", "waiting"); X.flushOutput(); }
			else if (line.equals("EXIT")) { X.logs("Exiting..."); break; }
			else {
				X.putOutput("exec.status", "running"); X.flushOutput();

				String[] tokens = line.split(" ", 2);
				String id = tokens[0];
				String input = tokens[1];
				String output = apply(input);
				line = callMaster("PUT "+id+" "+output);
				X.logs("Sent result to master, got reply: %s", line);
				numJobs++;

				X.putOutput("numJobs", numJobs); X.flushOutput();
			}
		}
	}
}

class Master {
	boolean shouldExit;
	Execution X = Execution.v();
	int port;

	HashMap<Integer,Scenario> inprogress = new HashMap<Integer,Scenario>();
	HashMap<String,Long> lastContact = new HashMap();
	Client client;

	final boolean waitForWorkersToExit = true;

	int numWorkers() { return lastContact.size(); }

	public Master(int port, Client client) {
		this.port = port;
		this.client = client;
		boolean exitFlag = false;

		X.logs("MASTER: listening at port %s", port);
		try {
			ServerSocket master = new ServerSocket(port);
			while (true) {
				if (exitFlag && (!waitForWorkersToExit || lastContact.size() == 0)) break;
				X.putOutput("numWorkers", numWorkers());
				X.flushOutput();
				X.logs("============================================================");
				boolean clientIsDone = client.isDone();
				if (clientIsDone) {
					if (!waitForWorkersToExit || lastContact.size() == 0) break;
					X.logs("Client is done but still waiting for %s workers to exit...", lastContact.size());
				}
				Socket worker = master.accept();
				String hostname = worker.getInetAddress().getHostAddress();

				BufferedReader in = new BufferedReader(new InputStreamReader(worker.getInputStream()));
				PrintWriter out = new PrintWriter(worker.getOutputStream(), true);

				X.logs("MASTER: Got connection from worker %s [hostname=%s]", worker, hostname);
				String cmd = in.readLine();
				if (cmd.equals("GET")) {
					lastContact.put(hostname, System.currentTimeMillis()); // Only add if it's getting stuff
					if (clientIsDone || numWorkers() > client.maxWorkersNeeded() + 1) { // 1 for extra buffer
						// If client is done or we have more workers than we need, then quit
						out.println("EXIT");
						lastContact.remove(hostname);
					}
					else {
						Scenario reqScenario = client.createJob();
						if (reqScenario == null) {
							X.logs("  No job, waiting (%s workers, %s workers needed)", numWorkers(), client.maxWorkersNeeded());
							out.println("WAIT");
						}
						else {
							inprogress.put(reqScenario.id, reqScenario);
							out.println(reqScenario.id + " " + reqScenario); // Response: <ID> <task spec>
							X.logs("  GET => id=%s", reqScenario.id);
						}
					}
				}
				else if (cmd.equals("CLEAR")) {
					lastContact.clear();
					out.println("Cleared workers");
				}
				else if (cmd.equals("SAVE")) {
					client.saveState();
					out.println("Saved");
				}
				else if (cmd.equals("EXIT")) {
					exitFlag = true;
					out.println("Going to exit...");
				}
				else if (cmd.equals("FLUSH")) {
					// Flush dead workers
					HashMap<String,Long> newLastContact = new HashMap();
					for (String name : lastContact.keySet()) {
						long t = lastContact.get(name);
						if (System.currentTimeMillis() - t < 60*60*1000)
							newLastContact.put(name, t);
					}
					lastContact = newLastContact;
					X.logs("%d workers", lastContact.size());
					out.println(lastContact.size()+" workers left");
				}
				else if (cmd.startsWith("PUT")) {
					String[] tokens = cmd.split(" ", 3); // PUT <ID> <task result>
					int id = Integer.parseInt(tokens[1]);
					Scenario scenario = inprogress.remove(id);
					if (scenario == null) {
						X.logs("  PUT id=%s, but doesn't exist", id);
						out.println("INVALID");
					}
					else {
						X.logs("  PUT id=%s", id);
						scenario.decode(tokens[2]);
						client.onJobResult(scenario);
						out.println("OK");
					}
				}

				in.close();
				out.close();
			}
			master.close();
			client.saveState();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}

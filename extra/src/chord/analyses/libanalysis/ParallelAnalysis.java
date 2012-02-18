package chord.analyses.libanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
	//int workerPort;
	String mode; // worker or master or null
	int masterFailureLimit = 20;
	int masterReplyTimeOut = 60000; //milliseconds
	//int workerTimeOut = 30000;
	
	JobDispatcher dispatcher = null;

	protected abstract void init();
	protected void finish(){
		finishAnalysis();
		X.finish(null);
	}
	protected abstract void finishAnalysis();
	protected abstract String setMasterHost();
	protected abstract int setMasterPort();
	protected abstract int setWorkerPort();
	protected abstract String setMode();
	protected abstract JobDispatcher setJobDispatcher();


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
		//workerPort = setWorkerPort();

		if (isWorker()) runWorker();
		else{
			dispatcher = setJobDispatcher();
			if(dispatcher == null){
				Exception e = new Exception("Dispatcher not initialized in Master");
				throw new RuntimeException(e);
			}
			new Master(masterPort, dispatcher);
		}
		finish();
	}
	String callMaster(String line) {
		try {
			InetAddress addrM = InetAddress.getByName(masterHost);
			//Socket master = new Socket(addrM, masterPort,null,workerPort);
			Socket master = new Socket(addrM, masterPort);
			BufferedReader in = new BufferedReader(new InputStreamReader(master.getInputStream()));
			PrintWriter out = new PrintWriter(master.getOutputStream(), true);
			master.setSoTimeout(masterReplyTimeOut);
			out.println(line);
			out.flush();
			line = in.readLine();
			in.close();
			out.close();
			master.close();
			return line;
		}catch(SocketTimeoutException e){
			X.logs("Read from master timed out");
			return null;
		}catch (IOException e) {
			return null;
		}
	}

	void runWorker() {
		int failCount = 0;
		Integer ID = null;

		X.logs("Starting worker...");
		int numJobs = 0;
		while (true) {
			X.logs("============================================================");
			// Get a job
			String line;
			
			if(ID == null){
				line = callMaster("ID");
			}else{
				line = callMaster("GET " + ID.intValue());
			}
			if (line == null) {
				X.logs("Got null, something bad happened to master..."); 
				failCount++;
				this.sleep(5);
				if(failCount == masterFailureLimit){
					X.logs("Master probably exited, exiting..."); break;
				}
			}
			else if (line.equals("WAIT")) { failCount = 0; X.logs("Waiting..."); this.sleep(5); X.putOutput("exec.status", "waiting"); X.flushOutput(); }
			else if (line.equals("EXIT")) { X.logs("Exiting..."); break; }
			else if (line.startsWith("ID")) {
				failCount = 0;
				String[] tokens = line.split(" ", 2);
				ID = new Integer(tokens[1]);
			}
			else if (line.startsWith("APPLY") && ID != null){
				failCount = 0;
				X.putOutput("exec.status", "running"); X.flushOutput();

				String[] tokens = line.split(" ", 3);
				String id = tokens[1];
				String input = tokens[2];
				String output = apply(input);
				line = callMaster("PUT "+ID.intValue()+" "+id+" "+ output);
				X.logs("Sent result to master, got reply: %s", line);
				numJobs++;

				X.putOutput("numJobs", numJobs); X.flushOutput();
			}else{
				if(ID == null)
					X.logs("ID not set for worker. Try again..");
				else
					X.logs("Incorrect command issued by master. Try again..");
			}
		}
	}
}

class Master {
	boolean shouldExit;
	Execution X = Execution.v();
	int port;
	int workerTimeOut = 30000;

	HashMap<Integer,Scenario> inprogress = new HashMap<Integer,Scenario>();
	//HashMap<String,Long> lastContact = new HashMap<String, Long>();
	HashMap<Integer,Long> lastContact = new HashMap<Integer, Long>();
	HashMap<Integer,Integer> workerToScenarioMap = new HashMap<Integer, Integer>();
	JobDispatcher dispatcher;

	final boolean waitForWorkersToExit = true;

	int numWorkers() { return lastContact.size(); }

	public Master(int port, JobDispatcher dispatcher) {
		this.port = port;
		this.dispatcher = dispatcher;
		//boolean exitFlag = false;

		X.logs("MASTER: listening at port %s", port);
		try {
			ServerSocket master = new ServerSocket(port);
			commController(master);
			master.close();
			dispatcher.saveState();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	void commController(ServerSocket master){
		boolean exitFlag = false;
		int cntID = 0;
		while (true) {
			try{
				if (exitFlag && (!waitForWorkersToExit || lastContact.size() == 0)) break;
				X.putOutput("numWorkers", numWorkers());
				X.flushOutput();
				X.logs("============================================================");
				boolean dispatcherIsDone = dispatcher.isDone();
				if (dispatcherIsDone) {
					if (!waitForWorkersToExit || lastContact.size() == 0) break;
					X.logs("Dispatcher is done but still waiting for %s workers to exit...", lastContact.size());
				}
				Socket worker = master.accept();
				String hostname = worker.getInetAddress().getHostAddress() /*+ ":" + worker.getPort()*/;
				BufferedReader in = new BufferedReader(new InputStreamReader(worker.getInputStream()));
				PrintWriter out = new PrintWriter(worker.getOutputStream(), true);
				
				worker.setSoTimeout(workerTimeOut);

				X.logs("MASTER: Got connection from worker %s [hostname=%s]", worker, hostname);
				String cmd = in.readLine();
				if(cmd.equals("ID")){
					if (dispatcherIsDone || numWorkers() > dispatcher.maxWorkersNeeded() + 1) { // 1 for extra buffer
						// If dispatcher is done or we have more workers than we need, then quit
						out.println("EXIT");
					}else{
						lastContact.put(cntID, System.currentTimeMillis()); // Only add if it's getting stuff
						out.println("ID " + cntID);
						cntID++;
					}
					
				}
				else if (cmd.startsWith("GET")) {
					String[] tokens = cmd.split(" ", 2); // GET <WID>
					int wID = Integer.parseInt(tokens[1]);
					lastContact.put(wID, System.currentTimeMillis()); // Only add if it's getting stuff
					
					if (dispatcherIsDone || numWorkers() > dispatcher.maxWorkersNeeded() + 1) { // 1 for extra buffer
						// If dispatcher is done or we have more workers than we need, then quit
						out.println("EXIT");
						workerToScenarioMap.remove(wID);
						lastContact.remove(wID);
					}else if(workerToScenarioMap.containsKey(wID)){
						int sID = workerToScenarioMap.get(wID);
						Scenario scenario = inprogress.get(sID);
						if (scenario == null) {
							workerToScenarioMap.remove(wID);
						}else {
							out.println("APPLY "+scenario.id + " " + scenario);
						}
					}
					
					if(!workerToScenarioMap.containsKey(wID)){  //check again since previous case can modify workerToScenarioMap
						Scenario reqScenario = dispatcher.createJob();
						if (reqScenario == null) {
							X.logs("  No job, waiting (%s workers, %s workers needed)", numWorkers(), dispatcher.maxWorkersNeeded());
							out.println("WAIT");
						}
						else {
							inprogress.put(reqScenario.id, reqScenario);
							workerToScenarioMap.put(wID, reqScenario.id);
							out.println("APPLY "+reqScenario.id + " " + reqScenario); // Response: <ID> <task spec>
							X.logs("  GET => id=%s", reqScenario.id);
						}
					}
				}
				else if (cmd.startsWith("CLEAR")) {
					lastContact.clear();
					out.println("Cleared workers");
				}
				else if (cmd.startsWith("SAVE")) {
					dispatcher.saveState();
					out.println("Saved");
				}
				else if (cmd.startsWith("EXIT")) {
					exitFlag = true;
					out.println("Going to exit...");
				}
				else if (cmd.startsWith("FLUSH")) {
					// Flush dead workers
					HashMap<Integer,Long> newLastContact = new HashMap<Integer, Long>();
					for (Integer wID : lastContact.keySet()) {
						long t = lastContact.get(wID);
						if (System.currentTimeMillis() - t < 60*60*1000)
							newLastContact.put(wID, t);
						else
							workerToScenarioMap.remove(wID);
					}
					lastContact = newLastContact;
					X.logs("%d workers", lastContact.size());
					out.println(lastContact.size()+" workers left");
				}
				else if (cmd.startsWith("PUT")) {
					String[] tokens = cmd.split(" ", 4); // PUT <WID> <SID> <task result>
					int wID = Integer.parseInt(tokens[1]);
					int sID = Integer.parseInt(tokens[2]);
					Scenario scenario = inprogress.remove(sID);
					
					if(workerToScenarioMap.get(wID)!=null)
						if(workerToScenarioMap.get(wID).intValue() == sID)
							workerToScenarioMap.remove(wID);
					
					if (scenario == null) {
						X.logs("  PUT id=%s, but doesn't exist", sID);
						out.println("INVALID");
					}
					else {
						X.logs("  PUT id=%s", sID);
						scenario.decode(tokens[3]);
						dispatcher.onJobResult(scenario);
						out.println("OK");
					}
				}

				in.close();
				out.close();
			}catch(SocketTimeoutException e){
				X.logs("Socket read from worker timed out");
			}catch(IOException e){
				X.logs("Some error in socket comm with a worker. Continuing with other workers");
			}
		}
		return;
	}
}

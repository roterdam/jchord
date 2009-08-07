package chord.project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;

import chord.util.IntBuffer;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.util.ProcessExecutor;

@Chord(
    name = "dyn-java"
)
public class DynamicAnalysis extends JavaAnalysis {
	public boolean handlesNewOrNewArray() { return false; }
	public boolean handlesInstFldRd() { return false; }
	public boolean handlesInstFldWr() { return false; }
	public boolean handlesAryElemRd() { return false; }
	public boolean handlesAryElemWr() { return false; }
	public boolean handlesStatFldWr() { return false; }
	public boolean handlesAcqLock() { return false; }
	public boolean handlesThreadStart() { return false; }
	public boolean handlesThreadSpawn() { return false; }
	public boolean handlesMethodEnter() { return false; }
	public boolean handlesMethodLeave() { return false; }
	public void initPass() {
		// signals beginning of parsing of a new trace
		// do nothing by default; subclasses can override
	}
	public void done() {
		// do nothing by default; subclasses can override
	}

	private IndexMap<String> sHmap = new IndexHashMap<String>();
	private IndexMap<String> dHmap = new IndexHashMap<String>();
	private IndexMap<String> sEmap = new IndexHashMap<String>();
	private IndexMap<String> dEmap = new IndexHashMap<String>();
	private IndexMap<String> sFmap = new IndexHashMap<String>();
	private IndexMap<String> dFmap = new IndexHashMap<String>();
	private IndexMap<String> sMmap = new IndexHashMap<String>();
	private IndexMap<String> dMmap = new IndexHashMap<String>();
	protected IndexMap<String> Hmap;
	protected IndexMap<String> Emap;
	protected IndexMap<String> Fmap;
	protected IndexMap<String> Mmap;

	protected boolean convert;

	public void run() {
		final String mainClassName = Properties.mainClassName;
		assert (mainClassName != null);
		final String classPathName = Properties.classPathName;
		assert (classPathName != null);
		final String classesDirName = Properties.classesDirName;
		assert (classesDirName != null);

		final String crudeTraceFileName =
			getOrMake("chord.crude.trace.file", "crude_trace.txt");
		final String finalTraceFileName =
			getOrMake("chord.final.trace.file", "final_trace.txt");

		final String runIdsStr = System.getProperty("chord.run.ids", "0");

        convert = System.getProperty("chord.convert", "true").equals("true");

		boolean initDomH = false;
		boolean initDomE = false;
		boolean initDomF = false;
		boolean initDomL = false;
		boolean initDomM = false;
		if (handlesNewOrNewArray()) {
			initDomH = true;
		}
		if (handlesInstFldRd() || handlesInstFldWr() ||
			handlesAryElemRd() || handlesAryElemWr() ||
			handlesStatFldWr()) {
			initDomE = true;
			initDomF = true;
		}
		if (handlesAcqLock()) {
			initDomL = true;
			// TODO
		}
		if (handlesMethodEnter() || handlesMethodLeave()) {
			initDomM = true;
		}
		if (initDomH) {
			processDom("H", sHmap, dHmap);
		}
		if (initDomE) {
			processDom("E", sEmap, dEmap);
		}
		if (initDomF) {
			processDom("F", sFmap, dFmap);
		}
		if (initDomM) {
			processDom("M", sMmap, dMmap);
		}
		if (convert) {
			Hmap = sHmap;
			Emap = sEmap;
			Fmap = sFmap;
			Mmap = sMmap;
		} else {
			Hmap = dHmap;
			Emap = dEmap;
			Fmap = dFmap;
			Mmap = dMmap;
		}

		boolean doTracePipe = System.getProperty(
			"chord.trace.pipe", "false").equals("true");

		ProcessExecutor.execute("rm " + crudeTraceFileName);
		ProcessExecutor.execute("rm " + finalTraceFileName);
		if (doTracePipe) {
			ProcessExecutor.execute("mkfifo " + crudeTraceFileName);
			ProcessExecutor.execute("mkfifo " + finalTraceFileName);
		}

		final String[] runIds = runIdsStr.split(",");
		final String cmd = "java -Xbootclasspath/p:" +
			classesDirName + File.pathSeparator + Properties.bootClassPathName +
        	" -Xverify:none" + " -verbose" + 
			" -cp " + classesDirName + File.pathSeparator + classPathName +
        	" -agentpath:" + Properties.instrAgentFileName +
			"=trace_file_name=" + crudeTraceFileName +
			" " + mainClassName + " ";
		final String cmd2 = "java -cp " + Properties.bootClassPathName + // TODO
			" -Dchord.crude.trace.file=" + crudeTraceFileName +
			" -Dchord.final.trace.file=" + finalTraceFileName +
			" chord.project.TraceTransformer";
		for (String runId : runIds) {
			System.out.println("XXXXX: " + runId);
			final String args = System.getProperty("chord.args." + runId, "");
			if (doTracePipe) {
				Thread t1 = new Thread() {
					public void run() {
						ProcessExecutor.execute(cmd + args);
					}
				};
				Thread t2 = new Thread() {
					public void run() {
						ProcessExecutor.execute(cmd2);
					}
				};
				t1.start();
				t2.start();
				processTrace(finalTraceFileName);
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
			} else {
				ProcessExecutor.execute(cmd + args);
				(new TraceTransformer()).run(crudeTraceFileName, finalTraceFileName);
				processTrace(finalTraceFileName);
			}
		}
		done();
	}

	private String getOrMake(String propName, String fileName) {
		String s = System.getProperty(propName);
		if (s == null) {
			s = (new File(Properties.outDirName, fileName)).getAbsolutePath();
		}
		return s;
	}

	private void processDom(String domName, IndexMap<String> sMap,
			IndexMap<String> dMap) {
		if (convert) {
			ProgramDom dom = (ProgramDom) Project.getTrgt(domName);
			Project.runTask(dom);
			dom.saveToIdFile();
			for (int i = 0; i < dom.size(); i++) {
				String s = dom.toUniqueIdString(dom.get(i));
				if (sMap.contains(s))
					throw new RuntimeException(domName +
						"smap already contains: " + s);
				sMap.getOrAdd(s);
			}
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
				new File(Properties.outDirName, domName + ".dynamic.txt")));
			String s;
			while ((s = reader.readLine()) != null) {
				assert (!dMap.contains(s));
				dMap.getOrAdd(s);
			}
			reader.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private int getHidx(int h) {
		String s = dHmap.get(h);
		int hIdx = sHmap.indexOf(s);
		if (hIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sHmap");
		}
		return hIdx;
	}
	private int getFidx(int f) {
		String s = dFmap.get(f);
		int fIdx = sFmap.indexOf(s);
		if (fIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sFmap");
		}
		return fIdx;
	}
	private int getEidx(int e) {
		String s = dEmap.get(e);
		int eIdx = sEmap.indexOf(s);
		if (eIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sEmap");
		}
		return eIdx;
	}
	private int getMidx(int m) {
		String s = dMmap.get(m);
		int mIdx = sMmap.indexOf(s);
		if (mIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sMmap");
		}
		return mIdx;
	}
	private int getLidx(int l) {
		// TODO
		return 0;
	}

	private void processTrace(String fileName) {
		try {
			initPass();
			IntBuffer buffer = new IntBuffer(1024, fileName, true);
			int count = 0;
			while (!buffer.isDone()) {
				int opcode = buffer.get();
				count++;
				switch (opcode) {
				case EventKind.NEW:
				case EventKind.NEW_ARRAY:
				{
					int h = buffer.get();
					int o = buffer.get();
					if (handlesNewOrNewArray()) {
						int hIdx = convert ? getHidx(h) : h;
						processNewOrNewArray(hIdx, o);
					}
					break;
				}
				case EventKind.ACQ_LOCK:
				{
					int l = buffer.get();
					int o = buffer.get();
					if (handlesAcqLock()) {
						int lIdx = convert ? getLidx(l) : l;
						processAcqLock(lIdx, o);
					}
					break;
				}
				case EventKind.INST_FLD_RD:
				{
					int e = buffer.get();
					int b = buffer.get();
					int f = buffer.get();
					if (handlesInstFldRd()) {
						int eIdx = convert ? getEidx(e) : e;
						int fIdx = convert ? getFidx(f) : f;
						processInstFldRd(eIdx, b, fIdx);
					}
					break;
				}
				case EventKind.INST_FLD_WR:
				{
					int e = buffer.get();
					int b = buffer.get();
					int f = buffer.get();
					int r = buffer.get();
					if (handlesInstFldWr()) {
						int eIdx = convert ? getEidx(e) : e;
						int fIdx = convert ? getFidx(f) : f;
						processInstFldWr(eIdx, b, fIdx, r);
					}
					break;
				}
				case EventKind.ARY_ELEM_RD:
				{
					int e = buffer.get();
					int b = buffer.get();
					int i = buffer.get();
					if (handlesAryElemRd()) {
						int eIdx = convert ? getEidx(e) : e;
						processAryElemRd(eIdx, b, i);
					}
					break;
				}
				case EventKind.ARY_ELEM_WR:
				{
					int e = buffer.get();
					int b = buffer.get();
					int i = buffer.get();
					int r = buffer.get();
					if (handlesAryElemWr()) {
						int eIdx = convert ? getEidx(e) : e;
						processAryElemWr(eIdx, b, i, r);
					}
					break;
				}
				case EventKind.STAT_FLD_WR:
				{
					int f = buffer.get();
					int r = buffer.get();
					if (handlesStatFldWr()) {
						int fIdx = convert ? getFidx(f) : f;
						processStatFldWr(fIdx, r);
					}
					break;
				}
				case EventKind.THREAD_START:
				{
					int t = buffer.get();
					if (handlesThreadStart()) {
						processThreadStart(t);
					}
					break;
				}
				case EventKind.THREAD_SPAWN:
				{
					int o = buffer.get();
					if (handlesThreadSpawn()) {
						processThreadSpawn(o);
					}
					break;
				}
				case EventKind.METHOD_ENTER:
				{
					int t = buffer.get();
					int m = buffer.get();
					if (handlesMethodEnter()) {
						int mIdx = convert ? getMidx(m) : m;
						processMethodEnter(t, mIdx);
					}
					break;
				}
				case EventKind.METHOD_LEAVE:
				{
					int t = buffer.get();
					int m = buffer.get();
					if (handlesMethodLeave()) {
						int mIdx = convert ? getMidx(m) : m;
						processMethodLeave(t, mIdx);
					}
					break;
				}
				default:
					throw new RuntimeException("Unknown opcode: " + opcode);
				}
			}
			System.out.println("PROCESS TRACE: " + count);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	public void processNewOrNewArray(int hIdx, int o) { }
	public void processInstFldRd(int eIdx, int b, int fIdx) { }
	public void processInstFldWr(int eIdx, int b, int fIdx, int r) { }
	public void processStatFldWr(int fIdx, int r) { }
	public void processAryElemRd(int eIdx, int b, int i) { }
	public void processAryElemWr(int eIdx, int b, int i, int r) { }
	public void processAcqLock(int lIdx, int o) { }
	public void processThreadStart(int o) { }
	public void processThreadSpawn(int o) { }
	public void processMethodEnter(int t, int mIdx) { }
	public void processMethodLeave(int t, int mIdx) { }
}

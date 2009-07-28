package chord.project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;

import chord.util.IntBuffer;
import chord.util.Assertions;
import chord.util.IndexMap;
import chord.util.PropertyUtils;
import chord.util.ProcessExecutor;

@Chord(
    name = "dyn-java"
)
public class DynamicAnalysis extends JavaAnalysis {
	public boolean handlesObjValAsgnInst() { return false; }
	public boolean handlesInstFldRdInst() { return false; }
	public boolean handlesInstFldWrInst() { return false; }
	public boolean handlesAryElemRdInst() { return false; }
	public boolean handlesAryElemWrInst() { return false; }
	public boolean handlesStatFldWrInst() { return false; }
	public boolean handlesAcqLockInst() { return false; }
	public boolean handlesForkHeadInst() { return false; }
	public void initPass() {
		// signals beginning of parsing of a new trace
		// do nothing by default; subclasses can override
	}
	public void done() {
		// do nothing by default; subclasses can override
	}

	private IndexMap<String> sHmap = new IndexMap<String>();
	private IndexMap<String> dHmap = new IndexMap<String>();
	private IndexMap<String> sEmap = new IndexMap<String>();
	private IndexMap<String> dEmap = new IndexMap<String>();
	private IndexMap<String> sFmap = new IndexMap<String>();
	private IndexMap<String> dFmap = new IndexMap<String>();
	protected IndexMap<String> Hmap;
	protected IndexMap<String> Emap;
	protected IndexMap<String> Fmap;

	protected boolean convert;

	public void run() {
		final String mainClassName = Properties.mainClassName;
		Assertions.Assert(mainClassName != null);
		final String classPathName = Properties.classPathName;
		Assertions.Assert(classPathName != null);
		final String classesDirName = Properties.classesDirName;
		Assertions.Assert(classesDirName != null);

		final String crudeTraceFileName =
			getOrMake("chord.crude.trace.file", "crude_trace.txt");
		final String finalTraceFileName =
			getOrMake("chord.final.trace.file", "final_trace.txt");

		final String runIdsStr = System.getProperty("chord.run.ids", "");

        convert = PropertyUtils.getBoolProperty("chord.convert", true);

		boolean initDomH = false;
		boolean initDomE = false;
		boolean initDomF = false;
		boolean initDomL = false;
		if (handlesObjValAsgnInst()) {
			initDomH = true;
		}
		if (handlesInstFldRdInst() || handlesInstFldWrInst() ||
			handlesAryElemRdInst() || handlesAryElemWrInst() ||
			handlesStatFldWrInst()) {
			initDomE = true;
			initDomF = true;
		}
		if (handlesAcqLockInst()) {
			initDomL = true;
			// TODO
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
		if (convert) {
			Hmap = sHmap;
			Emap = sEmap;
			Fmap = sFmap;
		} else {
			Hmap = dHmap;
			Emap = dEmap;
			Fmap = dFmap;
		}

		boolean doTracePipe = PropertyUtils.getBoolProperty("chord.trace.pipe", true);

		ProcessExecutor.execute("rm " + crudeTraceFileName);
		ProcessExecutor.execute("rm " + finalTraceFileName);
		if (doTracePipe) {
			ProcessExecutor.execute("mkfifo " + crudeTraceFileName);
			ProcessExecutor.execute("mkfifo " + finalTraceFileName);
		}

		final String[] runIds = runIdsStr.split(",");
		final String cmd = "java -Xbootclasspath/p:" + Properties.bootClassPathName +
        	" -Xverify:none " + // " -verbose" + 
			" -cp " + classesDirName + File.pathSeparator + classPathName +
        	" -agentlib:chord_agent" +
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
				sMap.set(s);
			}
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
				new File(Properties.outDirName, domName + ".dynamic.txt")));
			String s;
			while ((s = reader.readLine()) != null) {
				Assertions.Assert(!dMap.contains(s));
				dMap.set(s);
			}
			reader.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private int getHidx(int h) {
		String s = dHmap.get(h);
		int hIdx = sHmap.get(s);
		if (hIdx == -1) {
			// System.err.println("WARNING: could not find `" + s + "` in sHmap");
		}
		return hIdx;
	}
	private int getFidx(int f) {
		String s = dFmap.get(f);
		int fIdx = sFmap.get(s);
		if (fIdx == -1) {
			// System.err.println("WARNING: could not find `" + s + "` in sFmap");
		}
		return fIdx;
	}
	private int getEidx(int e) {
		String s = dEmap.get(e);
		int eIdx = sEmap.get(s);
		if (eIdx == -1) {
			// System.err.println("WARNING: could not find `" + s + "` in sEmap");
		}
		return eIdx;
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
				case InstKind.NEW_INST:
				case InstKind.NEW_ARRAY_INST:
				{
					int h = buffer.get();
					int o = buffer.get();
					if (handlesObjValAsgnInst()) {
						int hIdx = convert ? getHidx(h) : h;
						processObjValAsgnInst(hIdx, o);
					}
					break;
				}
				case InstKind.ACQ_LOCK_INST:
				{
					int l = buffer.get();
					int o = buffer.get();
					if (handlesAcqLockInst()) {
						int lIdx = convert ? getLidx(l) : l;
						processAcqLockInst(lIdx, o);
					}
					break;
				}
				case InstKind.INST_FLD_RD_INST:
				{
					int e = buffer.get();
					int b = buffer.get();
					int f = buffer.get();
					if (handlesInstFldRdInst()) {
						int eIdx = convert ? getEidx(e) : e;
						int fIdx = convert ? getFidx(f) : f;
						processInstFldRdInst(eIdx, b, fIdx);
					}
					break;
				}
				case InstKind.INST_FLD_WR_INST:
				{
					int e = buffer.get();
					int b = buffer.get();
					int f = buffer.get();
					int r = buffer.get();
					if (handlesInstFldWrInst()) {
						int eIdx = convert ? getEidx(e) : e;
						int fIdx = convert ? getFidx(f) : f;
						processInstFldWrInst(eIdx, b, fIdx, r);
					}
					break;
				}
				case InstKind.ARY_ELEM_RD_INST:
				{
					int e = buffer.get();
					int b = buffer.get();
					int i = buffer.get();
					if (handlesAryElemRdInst()) {
						int eIdx = convert ? getEidx(e) : e;
						processAryElemRdInst(eIdx, b, i);
					}
					break;
				}
				case InstKind.ARY_ELEM_WR_INST:
				{
					int e = buffer.get();
					int b = buffer.get();
					int i = buffer.get();
					int r = buffer.get();
					if (handlesAryElemWrInst()) {
						int eIdx = convert ? getEidx(e) : e;
						processAryElemWrInst(eIdx, b, i, r);
					}
					break;
				}
				case InstKind.STAT_FLD_WR_INST:
				{
					int f = buffer.get();
					int r = buffer.get();
					if (handlesStatFldWrInst()) {
						int fIdx = convert ? getFidx(f) : f;
						processStatFldWrInst(fIdx, r);
					}
					break;
				}
				case InstKind.FORK_HEAD_INST:
				{
					int o = buffer.get();
					if (handlesForkHeadInst()) {
						processForkHeadInst(o);
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
	public void processObjValAsgnInst(int hIdx, int o) { }
	public void processInstFldRdInst(int eIdx, int b, int fIdx) { }
	public void processInstFldWrInst(int eIdx, int b, int fIdx, int r) { }
	public void processStatFldWrInst(int fIdx, int r) { }
	public void processAryElemRdInst(int eIdx, int b, int idx) { }
	public void processAryElemWrInst(int eIdx, int b, int idx, int r) { }
	public void processAcqLockInst(int lIdx, int l) { }
	public void processForkHeadInst(int o) { }
}

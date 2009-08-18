package chord.project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;

import chord.instr.EventKind;
import chord.instr.InstrScheme;
import chord.instr.TraceTransformer;
import chord.instr.InstrScheme.EventFormat;
import chord.util.ByteBufferedFile;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.util.ProcessExecutor;
import chord.util.ReadException;

@Chord(
    name = "dyn-java"
)
public class DynamicAnalysis extends JavaAnalysis {
	private IndexMap<String> sHmap = new IndexHashMap<String>();
	private IndexMap<String> dHmap = new IndexHashMap<String>();
	private IndexMap<String> sEmap = new IndexHashMap<String>();
	private IndexMap<String> dEmap = new IndexHashMap<String>();
	private IndexMap<String> sFmap = new IndexHashMap<String>();
	private IndexMap<String> dFmap = new IndexHashMap<String>();
	private IndexMap<String> sMmap = new IndexHashMap<String>();
	private IndexMap<String> dMmap = new IndexHashMap<String>();
	private IndexMap<String> sPmap = new IndexHashMap<String>();
	private IndexMap<String> dPmap = new IndexHashMap<String>();
	protected IndexMap<String> Hmap;
	protected IndexMap<String> Emap;
	protected IndexMap<String> Fmap;
	protected IndexMap<String> Mmap;
	protected IndexMap<String> Pmap;
	protected InstrScheme globalInstrScheme;
	protected InstrScheme clientInstrScheme;
	protected boolean convert;

	public void initPass() {
		// signals beginning of parsing of a new trace
		// do nothing by default; subclasses can override
	}
	public void done() {
		// do nothing by default; subclasses can override
	}
	// subclass must override
	public InstrScheme getInstrScheme() {
		throw new ChordRuntimeException();
	}
	public void run() {
		globalInstrScheme = InstrScheme.v();
		clientInstrScheme = getInstrScheme();
		boolean needsTraceTransform = clientInstrScheme.needsTraceTransform();
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
		final String runIdsStr =
			System.getProperty("chord.run.ids", "0");
        convert = System.getProperty(
        	"chord.convert", "true").equals("true");
		boolean doTracePipe = System.getProperty(
			"chord.trace.pipe", "false").equals("true");
		if (clientInstrScheme.needsHmap())
        	processDom("H", sHmap, dHmap);
		if (clientInstrScheme.needsEmap())
        	processDom("E", sEmap, dEmap);
		if (clientInstrScheme.needsPmap())
        	processDom("P", sPmap, dPmap);
		if (clientInstrScheme.needsFmap())
        	processDom("F", sFmap, dFmap);
        if (clientInstrScheme.needsMmap())
        	processDom("M", sMmap, dMmap);
        if (convert) {
        	Hmap = sHmap;
        	Emap = sEmap;
        	Pmap = sPmap;
        	Fmap = sFmap;
        	Mmap = sMmap;
        } else {
        	Hmap = dHmap;
        	Emap = dEmap;
        	Pmap = dPmap;
        	Fmap = dFmap;
        	Mmap = dMmap;
        }
		ProcessExecutor.execute("rm " + crudeTraceFileName);
		ProcessExecutor.execute("rm " + finalTraceFileName);
		if (doTracePipe) {
			ProcessExecutor.execute("mkfifo " + crudeTraceFileName);
			ProcessExecutor.execute("mkfifo " + finalTraceFileName);
		}
		int numMeths = getNum("numMeths.txt");
		int numLoops = getNum("numLoops.txt");
		final String[] runIds = runIdsStr.split(",");
		final String cmd = "java -ea -Xbootclasspath/p:" +
			classesDirName + File.pathSeparator + Properties.bootClassPathName +
        	" -Xverify:none" + " -verbose" + 
        	" -cp " + classesDirName + File.pathSeparator + classPathName +
        	" -agentpath:" + Properties.instrAgentFileName +
			"=trace_file_name=" + crudeTraceFileName +
			"=num_meths=" + numMeths +
			"=num_loops=" + numLoops +
			"=instr_bound=" + Properties.instrBound +
			" " + mainClassName + " ";
		final String traceTransformCmd = needsTraceTransform ?
			"java -ea -cp " + Properties.bootClassPathName +
			" -Dchord.crude.trace.file=" + crudeTraceFileName +
			" -Dchord.final.trace.file=" + finalTraceFileName +
			" chord.project.TraceTransformer" : null;
		for (String runId : runIds) {
			System.out.println("Processing Run ID: " + runId);
			final String args = System.getProperty("chord.args." + runId, "");
			if (doTracePipe) {
				Thread t1 = new Thread() {
					public void run() {
						ProcessExecutor.execute(cmd + args);
					}
				};
				if (needsTraceTransform) {
					Thread t2 = new Thread() {
						public void run() {
							ProcessExecutor.execute(traceTransformCmd);
						}
					};
					t1.start();
					t2.start();
					processTrace(finalTraceFileName);
					try {
						t1.join();
						t2.join();
					} catch (InterruptedException ex) {
						throw new ChordRuntimeException(ex);
					}
				} else {
					t1.start();
					processTrace(crudeTraceFileName);
					try {
						t1.join();
					} catch (InterruptedException ex) {
						throw new ChordRuntimeException(ex);
					}
				}
			} else {
				ProcessExecutor.execute(cmd + args);
				if (needsTraceTransform) {
					(new TraceTransformer()).run(crudeTraceFileName, finalTraceFileName);
					processTrace(finalTraceFileName);
				} else {
					processTrace(crudeTraceFileName);
				}
			}
		}
		done();
	}

	private static int getNum(String fileName) {
		File file = new File(Properties.classesDirName, fileName);
		if (!file.exists())
			return 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String s = reader.readLine();
			assert (s != null);
			reader.close();
			return Integer.parseInt(s);
		} catch (IOException ex) { 
			throw new RuntimeException(ex);
		}
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
			throw new ChordRuntimeException(ex);
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
	private int getEidx(int e) {
		String s = dEmap.get(e);
		int eIdx = sEmap.indexOf(s);
		if (eIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sEmap");
		}
		return eIdx;
	}
	private int getPidx(int p) {
		String s = dPmap.get(p);
		int pIdx = sPmap.indexOf(s);
		if (pIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sPmap");
		}
		return pIdx;
	}
	private int getFidx(int f) {
		String s = dFmap.get(f);
		int fIdx = sFmap.indexOf(s);
		if (fIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sFmap");
		}
		return fIdx;
	}
	private int getMidx(int m) {
		String s = dMmap.get(m);
		int mIdx = sMmap.indexOf(s);
		if (mIdx == -1) {
			System.err.println("WARNING: could not find `" + s + "` in sMmap");
		}
		return mIdx;
	}

	private void processTrace(String fileName) {
		try {
			initPass();
			ByteBufferedFile buffer = new ByteBufferedFile(1024, fileName, true);
			int count = 0;
			while (!buffer.isDone()) {
				byte opcode = buffer.getByte();
				count++;
				switch (opcode) {
				case EventKind.ENTER_METHOD:
				{
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
	            	if (lef.present()) {
		                int m;
		                if (lef.hasMid()) {
		                	m = buffer.getInt();
		                	if (convert)
		                		m = getMidx(m);
		                } else {
		                	m = -1;
		                	if (gef.hasMid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
						processEnterMethod(m, t);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
					break;
				}
				case EventKind.LEAVE_METHOD:
				{
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ENTER_AND_LEAVE_METHOD);
	            	if (lef.present()) {
		                int m;
		                if (lef.hasMid()) {
		                	m = buffer.getInt();
		                	if (convert)
		                		m = getMidx(m);
		                } else {
		                	m = -1;
		                	if (gef.hasMid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
						processLeaveMethod(m, t);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	            	break;
				}
				case EventKind.NEW:
				case EventKind.NEW_ARRAY:
				{
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.NEW_AND_NEWARRAY);
	            	if (lef.present()) {
		                int h;
		                if (lef.hasPid()) {
		                	h = buffer.getInt();
		                	if (convert)
		                		h = getHidx(h);
		                } else {
		                	h = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
		                processNewOrNewArray(h, t, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
					break;
				}
	            case EventKind.GETSTATIC_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.GETSTATIC_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
	                	processGetstaticPrimitive(e, t, f);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.GETSTATIC_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.GETSTATIC_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
		            	processGetstaticReference(e, t, f, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.PUTSTATIC_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.PUTSTATIC_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
	                	processGetstaticPrimitive(e, t, f);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.PUTSTATIC_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.PUTSTATIC_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
		            	processGetstaticReference(e, t, f, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.GETFIELD_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.GETFIELD_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
	                	processGetfieldPrimitive(e, t, b, f);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.GETFIELD_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.GETFIELD_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processGetfieldReference(e, t, b, f, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.PUTFIELD_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.PUTFIELD_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
	                	processPutfieldPrimitive(e, t, b, f);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.PUTFIELD_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.PUTFIELD_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int f;
		                if (lef.hasFid()) {
		                	f = buffer.getInt();
		                	if (convert)
		                		f = getFidx(f);
		                } else {
		                	f = -1;
		                	if (gef.hasFid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processPutfieldReference(e, t, b, f, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.ALOAD_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ALOAD_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int i;
		                if (lef.hasIid()) {
		                	i = buffer.getInt();
		                } else {
		                	i = -1;
		                	if (gef.hasIid())
		                		buffer.eatInt();
		                }
	                	processAloadPrimitive(e, t, b, i);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.ALOAD_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ALOAD_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ALOAD_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int i;
		                if (lef.hasIid()) {
		                	i = buffer.getInt();
		                } else {
		                	i = -1;
		                	if (gef.hasIid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processAloadReference(e, t, b, i, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.ASTORE_PRIMITIVE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ASTORE_PRIMITIVE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int i;
		                if (lef.hasIid()) {
		                	i = buffer.getInt();
		                } else {
		                	i = -1;
		                	if (gef.hasIid())
		                		buffer.eatInt();
		                }
	                	processAstorePrimitive(e, t, b, i);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.ASTORE_REFERENCE:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ASTORE_REFERENCE);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ASTORE_REFERENCE);
	            	if (lef.present()) {
		                int e;
		                if (lef.hasPid()) {
		                	e = buffer.getInt();
		                	if (convert)
		                		e = getEidx(e);
		                } else {
		                	e = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int b;
		                if (lef.hasBid()) {
		                	b = buffer.getInt();
		                } else {
		                	b = -1;
		                	if (gef.hasBid())
		                		buffer.eatInt();
		                }
		                int i;
		                if (lef.hasIid()) {
		                	i = buffer.getInt();
		                } else {
		                	i = -1;
		                	if (gef.hasIid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processAstoreReference(e, t, b, i, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.THREAD_START:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.THREAD_START);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.THREAD_START);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processWait(p, t, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.THREAD_JOIN:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.THREAD_JOIN);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.THREAD_JOIN);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int o;
		                if (lef.hasOid()) {
		                	o = buffer.getInt();
		                } else {
		                	o = -1;
		                	if (gef.hasOid())
		                		buffer.eatInt();
		                }
	                	processWait(p, t, o);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.ACQUIRE_LOCK:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.ACQUIRE_LOCK);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.ACQUIRE_LOCK);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int l;
		                if (lef.hasLid()) {
		                	l = buffer.getInt();
		                } else {
		                	l = -1;
		                	if (gef.hasLid())
		                		buffer.eatInt();
		                }
	                	processWait(p, t, l);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.RELEASE_LOCK:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.RELEASE_LOCK);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.RELEASE_LOCK);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int l;
		                if (lef.hasLid()) {
		                	l = buffer.getInt();
		                } else {
		                	l = -1;
		                	if (gef.hasLid())
		                		buffer.eatInt();
		                }
	                	processWait(p, t, l);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.WAIT:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.WAIT);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.WAIT);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int l;
		                if (lef.hasLid()) {
		                	l = buffer.getInt();
		                } else {
		                	l = -1;
		                	if (gef.hasLid())
		                		buffer.eatInt();
		                }
	                	processWait(p, t, l);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
	            case EventKind.NOTIFY:
	            {
					EventFormat lef = clientInstrScheme.getEvent(InstrScheme.NOTIFY);
					EventFormat gef = globalInstrScheme.getEvent(InstrScheme.NOTIFY);
	            	if (lef.present()) {
		                int p;
		                if (lef.hasPid()) {
		                	p = buffer.getInt();
		                	if (convert)
		                		p = getPidx(p);
		                } else {
		                	p = -1;
		                	if (gef.hasPid())
		                		buffer.eatInt();
		                }
		                int t;
		                if (lef.hasTid()) {
		                	t = buffer.getInt();
		                } else {
		                	t = -1;
		                	if (gef.hasTid())
		                		buffer.eatInt();
		                }
		                int l;
		                if (lef.hasLid()) {
		                	l = buffer.getInt();
		                } else {
		                	l = -1;
		                	if (gef.hasLid())
		                		buffer.eatInt();
		                }
	                	processNotify(p, t, l);
	            	} else {
	            		int n = gef.size();
	            		buffer.eat(n);
	            	}
	                break;
	            }
				default:
					throw new RuntimeException("Unknown opcode: " + opcode);
				}
			}
			System.out.println("PROCESS TRACE: " + count);
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		} catch (ReadException ex) {
			throw new ChordRuntimeException(ex);
		}
	}
	public void processEnterMethod(int m, int t) { }
	public void processLeaveMethod(int m, int t) { }
	public void processNewOrNewArray(int h, int t, int o) { }
	public void processGetstaticPrimitive(int e, int t, int f) { }
	public void processGetstaticReference(int e, int t, int f, int o) { }
	public void processPutstaticPrimitive(int e, int t, int f) { }
	public void processPutstaticReference(int e, int t, int f, int o) { }
	public void processGetfieldPrimitive(int e, int t, int b, int f) { }
	public void processGetfieldReference(int e, int t, int b, int f, int o) { }
	public void processPutfieldPrimitive(int e, int t, int b, int f) { }
	public void processPutfieldReference(int e, int t, int b, int f, int o) { }
	public void processAloadPrimitive(int e, int t, int b, int i) { }
	public void processAloadReference(int e, int t, int b, int i, int o) { }
	public void processAstorePrimitive(int e, int t, int b, int i) { }
	public void processAstoreReference(int e, int t, int b, int i, int o) { }
	public void processThreadStart(int p, int t, int o) { }
	public void processThreadJoin(int p, int t, int o) { }
	public void processAcquireLock(int p, int t, int l) { }
	public void processReleaseLock(int p, int t, int l) { }
	public void processWait(int p, int t, int l) { }
	public void processNotify(int p, int t, int l) { }
}

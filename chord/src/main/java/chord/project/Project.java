/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.project;

import java.io.PrintStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Stack;

import org.scannotation.AnnotationDB;

import chord.util.ArraySet;
import chord.util.ArrayUtils;
import chord.util.ClassUtils;
import chord.util.FileUtils;
import chord.util.StringUtils;
import chord.util.Timer;
import chord.util.tuple.object.Pair;
import chord.analyses.alias.CtxtsAnalysis;
import chord.bddbddb.RelSign;

/**
 * A project.
 * 
 * It encapsulates all program analyses in scope.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Project {
	private static boolean isInited;
	private static boolean hasNoErrors = true;
	private static Map<String, Set<TrgtInfo>> nameToTrgtsDebugMap;
	private static Map<ITask, Set<String>> taskToConsumedNamesMap;
	private static Map<ITask, Set<String>> taskToProducedNamesMap;
	private static Map<String, ITask> nameToTaskMap;
	private static Map<String, Object> nameToTrgtMap;
	private static Map<ITask, Set<Object>> taskToProducedTrgtsMap;
	private static Map<ITask, Set<Object>> taskToConsumedTrgtsMap;
	private static Map<Object, Set<ITask>> trgtToProducerTasksMap;
	private static Map<Object, Set<ITask>> trgtToConsumerTasksMap;
	private static Map<Set<ITask>, ITask> resolverMap =
		new HashMap<Set<ITask>, ITask>();
	private static Set<ITask> doneTasks = new HashSet<ITask>();
	private static Set<Object> doneTrgts = new HashSet<Object>();
	private static Stack<Timer> timers = new Stack<Timer>();
	private static Timer currTimer;

	private Project() { }
	
	public static void main(String[] args) {
        PrintStream outStream = null;
        PrintStream errStream = null;
        try {
            String outDirName = Properties.outDirName;
            assert (outDirName != null);
            String outFileName = Properties.outFileName;
            assert (outFileName != null);
            String errFileName = Properties.outFileName;
            assert (errFileName != null);

            outFileName = FileUtils.getAbsolutePath(outFileName, outDirName);
            errFileName = FileUtils.getAbsolutePath(errFileName, outDirName);
            File outFile = new File(outFileName);
            outStream = new PrintStream(outFile);
            System.setOut(outStream);
            File errFile = new File(errFileName);
            if (errFile.equals(outFile))
                errStream = outStream;
            else
                errStream = new PrintStream(errFile);
            System.setErr(errStream);

			System.out.println("ENTER: chord");
			currTimer = new Timer("chord");
			currTimer.init();

            Properties.print();
            Project.init();
            Program.v().init();

			boolean doInstr = Properties.doInstr;
			
			if (doInstr) {
				Instrumentor instrumentor = new Instrumentor();
				instrumentor.visit(Program.v());
			}
			
            String analyses = Properties.analyses;
            if (analyses != null) {
                String[] analysisNames = analyses.split(" |,|:|;");
				for (String name : analysisNames)
					runTask(name);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (outStream != null)
                outStream.close();
            if (errStream != null && errStream != outStream)
                errStream.close();
        } finally {
			System.out.println("LEAVE: chord");
			if (currTimer != null) {
				currTimer.done();
				printCurrTimer();
			}
		}
	}

	public static Object getTrgt(String name) {
		Object trgt = nameToTrgtMap.get(name);
		if (trgt == null) {
			throw new RuntimeException("Trgt '" + name +
				"' not found.");
		}
		return trgt;
	}
	public static ITask getTask(String name) {
		ITask task = nameToTaskMap.get(name);
		if (task == null) {
			throw new RuntimeException("Task '" + name +
				"' not found.");
		}
		return task;
	}

	private static void printCurrTimer() {
		System.out.println("Exclusive time: " + currTimer.getExclusiveTimeStr());
		System.out.println("Inclusive time: " + currTimer.getInclusiveTimeStr());
	}

	public static void runTask(ITask task) {
		System.out.println("ENTER: " + task);
		if (isTaskDone(task)) {
			System.out.println("ALREADY DONE.");
			return;
		}
		currTimer.pause();
		timers.push(currTimer);
		currTimer = new Timer(task.getName());
		currTimer.init();
		Set<Object> consumedTrgts = taskToConsumedTrgtsMap.get(task);
		for (Object trgt : consumedTrgts) {
			if (isTrgtDone(trgt))
				continue;
			Set<ITask> tasks = trgtToProducerTasksMap.get(trgt);
			if (tasks.size() != 1) {
				throw new RuntimeException("Task producing trgt '" +
					trgt + "' consumed by task '" + task +
					"' not found.");
			}
			ITask task2 = tasks.iterator().next();
			runTask(task2);
		}
		task.run();
        System.out.println("LEAVE: " + task);
        currTimer.done();
		printCurrTimer();
		currTimer = timers.pop();
		currTimer.resume();
		setTaskDone(task);
		Set<Object> producedTrgts = taskToProducedTrgtsMap.get(task);
		assert(producedTrgts != null);
		for (Object trgt : producedTrgts) {
			setTrgtDone(trgt);
		}
	}

	public static ITask runTask(String name) {
		ITask task = getTask(name);
		runTask(task);
		return task;
	}

	public static boolean isTrgtDone(Object trgt) {
		return doneTrgts.contains(trgt);
	}

	public static boolean isTrgtDone(String name) {
		return isTrgtDone(getTrgt(name));
	}

	public static void setTrgtDone(Object trgt) {
		doneTrgts.add(trgt);
	}

	public static void setTrgtDone(String name) {
		setTrgtDone(getTrgt(name));
	}

	public static void resetTrgtDone(Object trgt) {
		if (doneTrgts.remove(trgt)) {
			for (ITask task : trgtToConsumerTasksMap.get(trgt)) {
				resetTaskDone(task);
			}
		}
	}

	public static void runTasksWithVisitors(Collection<ITask> tasks) {
		// TODO
	}

	public static void runTaskWithVisitors(ITask task) {
		// TODO
	}

	public static void resetAll() {
		doneTrgts.clear();
		doneTasks.clear();
	}

	public static void resetTrgtDone(String name) {
		resetTrgtDone(getTrgt(name));
	}

	public static boolean isTaskDone(ITask task) {
		return doneTasks.contains(task);
	}

	public static boolean isTaskDone(String name) {
		return isTaskDone(getTask(name));
	}

	public static void setTaskDone(ITask task) {
		doneTasks.add(task);
	}

	public static void setTaskDone(String name) {
		setTaskDone(getTask(name));
	}

	public static void resetTaskDone(ITask task) {
		if (doneTasks.remove(task)) {
			for (Object trgt : taskToProducedTrgtsMap.get(task)) {
				resetTrgtDone(trgt);
			}
		}
	}

	public static void resetTaskDone(String name) {
		resetTaskDone(getTask(name));
	}

	public static void init() {
		if (isInited)
			return;
		nameToTaskMap = new HashMap<String, ITask>();
		nameToTrgtsDebugMap = new HashMap<String, Set<TrgtInfo>>();
		taskToConsumedNamesMap =
			new HashMap<ITask, Set<String>>();
		taskToProducedNamesMap =
			new HashMap<ITask, Set<String>>();
		
		buildDlogAnalysisMap();
		buildJavaAnalysisMap();

		nameToTrgtMap = new HashMap<String, Object>();
		List<Pair<ProgramRel, RelSign>> todo =
			new ArrayList<Pair<ProgramRel, RelSign>>();
		for (Map.Entry<String, Set<TrgtInfo>> e :
				nameToTrgtsDebugMap.entrySet()) {
			String name = e.getKey();
			Set<TrgtInfo> infos = e.getValue();
			Iterator<TrgtInfo> it = infos.iterator();
			TrgtInfo fstInfo = it.next();
			Class resType = fstInfo.type;
			String resTypeLoc = fstInfo.location;
			String[] resDomNames;
			String resDomOrder;
			if (fstInfo.sign != null) {
				resDomNames = fstInfo.sign.val0;
				resDomOrder = fstInfo.sign.val1;
			} else {
				resDomNames = null;
				resDomOrder = null;
			}
			String resDomNamesLoc = fstInfo.location;
			String resDomOrderLoc = fstInfo.location;
			boolean corrupt = false;
			while (it.hasNext()) {
				TrgtInfo info = it.next();
				Class curType = info.type;
				if (curType != null) {
					if (resType == null) {
						resType = curType;
						resTypeLoc = info.location;
					} else if (ClassUtils.isSubclass(curType, resType)) {
						resType = curType;
						resTypeLoc = info.location;
					} else if (!ClassUtils.isSubclass(resType, curType)) {
						inconsistentTypes(name,
							resType.toString(), curType.toString(),
							resTypeLoc, info.location);
						corrupt = true;
						break;
					}
				}
				RelSign curSign = info.sign;
				if (curSign != null) {
					String[] curDomNames = curSign.val0;
					if (resDomNames == null) {
						resDomNames = curDomNames;
						resDomNamesLoc = info.location;
					} else if (!Arrays.equals(resDomNames, curDomNames)) {
						inconsistentDomNames(name,
							ArrayUtils.toString(resDomNames),
							ArrayUtils.toString(curDomNames),
							resDomNamesLoc, info.location);
						corrupt = true;
						break;
					}
					String curDomOrder = curSign.val1;
					if (curDomOrder != null) {
						if (resDomOrder == null) {
							resDomOrder = curDomOrder;
							resDomOrderLoc = info.location;
						} else if (!resDomOrder.equals(curDomOrder)) {
							inconsistentDomOrders(name,
								resDomOrder, curDomOrder,
								resDomOrderLoc, info.location);
						}
					}
				}
			}
			if (corrupt)
				continue;
			if (resType == null) {
				unknownType(name);
				continue;
			}
			RelSign sign = null;
			if (ClassUtils.isSubclass(resType, ProgramRel.class)) {
				if (resDomNames == null) {
					unknownSign(name);
					continue;
				}
				if (resDomOrder == null) {
					unknownOrder(name);
					continue;
				}
				sign = new RelSign(resDomNames, resDomOrder);
			}
			Object trgt = nameToTaskMap.get(name);
			if (trgt == null) {
				trgt = instantiate(resType.getName(), resType);
				if (trgt instanceof ITask) {
					ITask analysis = (ITask) trgt;
					analysis.setName(name);
				}
			}
			nameToTrgtMap.put(name, trgt);
			if (sign != null) {
				ProgramRel rel = (ProgramRel) trgt;
				todo.add(new Pair<ProgramRel, RelSign>(rel, sign));
			}
		}

		checkErrors();

		for (Pair<ProgramRel, RelSign> tuple : todo) {
			ProgramRel rel = tuple.val0;
			RelSign sign = tuple.val1;
			String[] domNames = sign.getDomNames();
			ProgramDom[] doms = new ProgramDom[domNames.length];
			for (int i = 0; i < domNames.length; i++) {
				String domName = StringUtils.trimNumSuffix(domNames[i]);
				Object trgt = nameToTrgtMap.get(domName);
				if (trgt == null) {
					undefinedDom(domName, rel.getName());
					continue;
				}
				if (!(trgt instanceof ProgramDom)) {
					illtypedDom(domName, rel.getName(),
						trgt.getClass().getName());
					continue;
				}
				doms[i] = (ProgramDom) trgt;
			}
			rel.setSign(sign);
			rel.setDoms(doms);
		}

		checkErrors();

		trgtToConsumerTasksMap = new HashMap<Object, Set<ITask>>();
		trgtToProducerTasksMap = new HashMap<Object, Set<ITask>>();
		for (Object trgt : nameToTrgtMap.values()) {
			Set<ITask> consumerTasks = new HashSet<ITask>();
			trgtToConsumerTasksMap.put(trgt, consumerTasks);
			Set<ITask> producerTasks = new HashSet<ITask>();
			trgtToProducerTasksMap.put(trgt, producerTasks);
		}
		taskToConsumedTrgtsMap = new HashMap<ITask, Set<Object>>();
		taskToProducedTrgtsMap = new HashMap<ITask, Set<Object>>();
		for (ITask task : nameToTaskMap.values()) {
			Set<String> consumedNames = taskToConsumedNamesMap.get(task);
			Set<Object> consumedTrgts =
				new HashSet<Object>(consumedNames.size());
			for (String name : consumedNames) {
				Object trgt = nameToTrgtMap.get(name);
				assert (trgt != null);
				consumedTrgts.add(trgt);
				Set<ITask> consumerTasks =
					trgtToConsumerTasksMap.get(trgt);
				consumerTasks.add(task);
			}
			taskToConsumedTrgtsMap.put(task, consumedTrgts);
			Set<String> producedNames =
				taskToProducedNamesMap.get(task);
			Set<Object> producedTrgts =
				new HashSet<Object>(producedNames.size());
			for (String name : producedNames) {
				Object trgt = nameToTrgtMap.get(name);
				assert (trgt != null);
				producedTrgts.add(trgt);
				Set<ITask> producerTasks =
					trgtToProducerTasksMap.get(trgt);
				producerTasks.add(task);
			}
			taskToProducedTrgtsMap.put(task, producedTrgts);
		}
		for (String trgtName : nameToTrgtMap.keySet()) {
			Object trgt = nameToTrgtMap.get(trgtName);
			Set<ITask> producerTasks = trgtToProducerTasksMap.get(trgt);
			int size = producerTasks.size();
			if (size == 0) {
				Set<ITask> consumerTasks =
					trgtToConsumerTasksMap.get(trgt);
				List<String> consumerTaskNames =
					new ArraySet<String>(consumerTasks.size());
				for (ITask task : consumerTasks) {
					consumerTaskNames.add(getSourceName(task));
				}
				undefinedTarget(trgtName, consumerTaskNames);
			} else if (size > 1) {
				ITask task2 = resolve(producerTasks);
				if (task2 != null) {
					Set<ITask> tasks2 = new ArraySet<ITask>(1);
					tasks2.add(task2);
					trgtToProducerTasksMap.put(trgt, tasks2);
					continue;
				}
				List<String> producerTaskNames =
					new ArraySet<String>(producerTasks.size());
				for (ITask task : producerTasks) {
					producerTaskNames.add(getSourceName(task));
				}
				redefinedTarget(trgtName, producerTaskNames);
			}
		}
		isInited = true;
	}

	private static ITask resolve(Set<ITask> tasks) {
		if (resolverMap.containsKey(tasks))
			return resolverMap.get(tasks);
		ITask resolver = null;
		if (tasks.size() == 4) {
			ITask cspa0cfaDlogTask = null;
			ITask cspaKcfaDlogTask = null;
			ITask cspaKobjDlogTask = null;
			ITask cspaHybridDlogTask = null;
			for (ITask task : tasks) {
				String name = task.getName();
				if (name.equals("cspa-0cfa-dlog"))
					cspa0cfaDlogTask = task;
				else if (name.equals("cspa-kobj-dlog"))
					cspaKobjDlogTask = task;
				else if (name.equals("cspa-kcfa-dlog"))
					cspaKcfaDlogTask = task;
				else if (name.equals("cspa-hybrid-dlog"))
					cspaHybridDlogTask = task;
			}
			if (cspa0cfaDlogTask != null && cspaKcfaDlogTask != null &&
				cspaKobjDlogTask != null && cspaHybridDlogTask != null) {
				String ctxtKindStr = System.getProperty(
					"chord.ctxt.kind", "ci");
				String instCtxtKindStr = System.getProperty(
					"chord.inst.ctxt.kind", ctxtKindStr);
				String statCtxtKindStr = System.getProperty(
					"chord.stat.ctxt.kind", ctxtKindStr);
				int instCtxtKind, statCtxtKind;
				if (instCtxtKindStr.equals("ci")) {
					instCtxtKind = CtxtsAnalysis.CTXTINS;
				} else if (instCtxtKindStr.equals("cs")) {
					instCtxtKind = CtxtsAnalysis.KCFASEN;
				} else if (instCtxtKindStr.equals("co")) {
					instCtxtKind = CtxtsAnalysis.KOBJSEN;
				} else
					throw new RuntimeException();
				if (statCtxtKindStr.equals("ci")) {
					statCtxtKind = CtxtsAnalysis.CTXTINS;
				} else if (statCtxtKindStr.equals("cs")) {
					statCtxtKind = CtxtsAnalysis.KCFASEN;
				} else if (statCtxtKindStr.equals("cc")) {
					statCtxtKind = CtxtsAnalysis.CTXTCPY;
				} else
					throw new RuntimeException();
				if (instCtxtKind == CtxtsAnalysis.CTXTINS &&
						statCtxtKind == CtxtsAnalysis.CTXTINS)
					resolver = cspa0cfaDlogTask;
				else if (instCtxtKind == CtxtsAnalysis.KOBJSEN &&
						statCtxtKind == CtxtsAnalysis.CTXTCPY)
					resolver = cspaKobjDlogTask;
				else if (instCtxtKind == CtxtsAnalysis.KCFASEN &&
						statCtxtKind == CtxtsAnalysis.KCFASEN)
					resolver = cspaKcfaDlogTask;
				else
					resolver = cspaHybridDlogTask;
			}
		}
		if (resolver != null) {
			System.out.println("Using task '" + getSourceName(resolver) +
				"' to resolve following tasks:");
			for (ITask task : tasks) {
				System.out.println("\t'" + getSourceName(task) + "'");
			}
		}
		resolverMap.put(tasks, resolver);
		return resolver;
	}

	private static void checkErrors() {
		if (!hasNoErrors) {
			System.err.println("Found errors (see above). Exiting ...");
			System.exit(1);
		}
	}

	private static void createTrgt(String name, Class type, String location) {
		TrgtInfo info = new TrgtInfo(type, location, null);
		createTrgt(name, info);
	}
	
	private static void createTrgt(String name, Class type, String location,
			RelSign relSign) {
		for (String name2 : relSign.getDomKinds()) {
			createTrgt(name2, ProgramDom.class, location); 
		}
		TrgtInfo info = new TrgtInfo(type, location, relSign);
		createTrgt(name, info);
	}

	private static void createTrgt(String name, TrgtInfo info) {
		Set<TrgtInfo> infos = nameToTrgtsDebugMap.get(name);
		if (infos == null) {
			infos = new HashSet<TrgtInfo>();
			nameToTrgtsDebugMap.put(name, infos);
		}
		infos.add(info);
	}

	private static class TrgtInfo {
		public Class type;
		public final String location;
		public RelSign sign;
		public TrgtInfo(Class type, String location, RelSign sign) {
			this.type = type;
			this.location = location;
			this.sign = sign;
		}
	};

	private static void buildDlogAnalysisMap() {
		String[] fileNames =
			Properties.dlogAnalysisPathName.split(File.pathSeparator);
		for (String fileName : fileNames) {
			File file = new File(fileName);
			if (!file.exists()) {
				nonexistentPathElem(fileName, "chord.dlog.analysis.path");
				continue;
			}
			processDlogAnalysis(file);
		}
	}

	private static void buildJavaAnalysisMap() {
		ArrayList<URL> list = new ArrayList<URL>();
		String[] fileNames =
			Properties.javaAnalysisPathName.split(File.pathSeparator);
		for (String fileName : fileNames) {
			File file = new File(fileName);
			if (!file.exists()) {
				nonexistentPathElem(fileName, "chord.java.analysis.path");
				continue;
			}
			try {
               list.add(file.toURL());
            } catch (MalformedURLException ex) {
            	malformedPathElem(fileName, "chord.java.analysis.path",
            		ex.getMessage());
				continue;
           }
        }
		URL[] urls = new URL[list.size()];
		list.toArray(urls);
		AnnotationDB db = new AnnotationDB();
		try {
			db.scanArchives(urls);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		Map<String, Set<String>> index = db.getAnnotationIndex();
		if (index == null)
			return;
		Set<String> classNames = index.get(Chord.class.getName());
		if (classNames == null)
			return;
		for (String className : classNames) {
			processJavaAnalysis(className);
		}
	}

    private static final FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			if (name.startsWith("."))
				return false;
			return true;
		}
	};

	private static void processDlogAnalysis(File file) {
		if (file.isDirectory()) {
			File[] subFiles = file.listFiles(filter);
			for (File subFile : subFiles)
				processDlogAnalysis(subFile);
			return;
		}
		String fileName = file.getAbsolutePath();
		if (!fileName.endsWith(".dlog") && !fileName.endsWith(".datalog"))
			return;
		DlogAnalysis task = new DlogAnalysis();
		boolean ret = task.parse(fileName);
		if (!ret) {
			ignoreDlogAnalysis(fileName);
			return;
		}
		String name = task.getDlogName();
		if (name == null) {
			anonDlogAnalysis(fileName);
			name = fileName;
		}
		ITask task2 = nameToTaskMap.get(name);
		if (task2 != null) {
			redefinedDlogTask(fileName, name, getSourceName(task2));
			return;
		}
		for (String domName : task.getDomNames()) {
			createTrgt(domName, ProgramDom.class, fileName);
		}
		Map<String, RelSign> consumedRelsMap =
			task.getConsumedRels();
		for (Map.Entry<String, RelSign> e : consumedRelsMap.entrySet()) {
			String relName = e.getKey();
			RelSign relSign = e.getValue();
			createTrgt(relName, ProgramRel.class, fileName, relSign);
		}
		Map<String, RelSign> producedRelsMap =
			task.getProducedRels();
		for (Map.Entry<String, RelSign> e : producedRelsMap.entrySet()) {
			String relName = e.getKey();
			RelSign relSign = e.getValue();
			createTrgt(relName, ProgramRel.class, fileName, relSign);
		}
		taskToConsumedNamesMap.put(task,
			consumedRelsMap.keySet());
		taskToProducedNamesMap.put(task,
			producedRelsMap.keySet());
		task.setName(name);
		nameToTaskMap.put(name, task);
	}
	
	private static void processJavaAnalysis(String className) {
		Class type;
		try {
			type = Class.forName(className);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		ChordAnnotParser info = new ChordAnnotParser(type);
		boolean ret = info.parse();
		if (!ret) {
			ignoreJavaAnalysis(className);
			return;
		}
		String name = info.getName();
		if (name.equals("")) {
			anonJavaAnalysis(className);
			name = className;
		}
		ITask task = nameToTaskMap.get(name);
		if (task != null) {
			redefinedJavaTask(className, name, getSourceName(task));
			return;
		}
		try {
			task = instantiate(className, ITask.class);
		} catch (RuntimeException ex) {
			nonInstantiableJavaAnalysis(className, ex.getMessage());
			return;
		}
		Map<String, Class  > nameToTypeMap = info.getNameToTypeMap();
		Map<String, RelSign> nameToSignMap = info.getNameToSignMap();
		for (Map.Entry<String, Class> e : nameToTypeMap.entrySet()) {
			String name2 = e.getKey();
			Class type2 = e.getValue();
			RelSign sign2 = nameToSignMap.get(name2);
			if (sign2 != null)
				createTrgt(name2, type2, className, sign2);
			else
				createTrgt(name2, type2, className);
		}
		for (Map.Entry<String, RelSign> e :
				nameToSignMap.entrySet()) {
			String name2 = e.getKey();
			if (nameToTypeMap.containsKey(name2))
				continue;
			RelSign sign2 = e.getValue();
			createTrgt(name2, ProgramRel.class, className, sign2);
		}
		Set<String> consumedNames = info.getConsumedNames();
		Set<String> producedNames = info.getProducedNames();
		taskToConsumedNamesMap.put(task, consumedNames);
		taskToProducedNamesMap.put(task, producedNames);
		for (String consumedName : consumedNames) {
			if (!nameToTypeMap.containsKey(consumedName) &&
				!nameToSignMap.containsKey(consumedName)) {
				createTrgt(consumedName, null, className);
			}
		}
		for (String producedName : producedNames) {
			if (!nameToTypeMap.containsKey(producedName) &&
				!nameToSignMap.containsKey(producedName)) {
				createTrgt(producedName, null, className);
			}
		}
		task.setName(name);
		nameToTaskMap.put(name, task);
	}

	// create an instance of the class named className and cast
	// it to the class named clazz
	private static <T> T instantiate(String className, Class<T> clazz) {
		try {
			Object obj = Class.forName(className).newInstance();
			return clazz.cast(obj);
		} catch (InstantiationException e) {
			throw new RuntimeException(
				"Class '" + className + "' cannot be instantiated.");
		} catch (IllegalAccessException e) {
			throw new RuntimeException(
				"Class '" + className + "' cannot be accessed.");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
				"Class '" + className + "' not found.");
		} catch (ClassCastException e) {
			throw new RuntimeException(
				"Class '" + className + "' must be a subclass of " +
				clazz.getName() + ".");
		}
	}
	
	private static String getSourceName(ITask analysis) {
		Class clazz = analysis.getClass();
		if (clazz == DlogAnalysis.class)
			return ((DlogAnalysis) analysis).getFileName();
		return clazz.getName();
	}
	
	private static void anonJavaAnalysis(String name) {
		System.err.println("WARNING: Java analysis '" + name +
			"' is not named via a @Chord(name=\"...\") annotation; " +
			"using its classname itself as its name.");
	}
	
	private static void anonDlogAnalysis(String name) {
		System.err.println("WARNING: Dlog analysis '" + name +
			"' is not named via a # name=... line; " +
			"using its filename itself as its name.");
	}

	private static void ignoreDlogAnalysis(String name) {
		System.err.println("ERROR: Ignoring Dlog analysis '" + name +
			"'; errors were found while parsing it (see above).");
		hasNoErrors = false;
	}
	
	private static void ignoreJavaAnalysis(String name) {
		System.err.println("ERROR: Ignoring Java analysis '" + name +
			"'; errors were found in its @Chord annotation (see above).");
		hasNoErrors = false;
	}
	
	private static void undefinedDom(String domName, String relName) {
		System.err.println("ERROR: '" + domName +
			"' declared as a domain of relation '" + relName +
			"' not declared as a produced name of any task.");
		hasNoErrors = false;
	}
	
	private static void illtypedDom(String domName, String relName,
			String type) {
		System.err.println("ERROR: '" + domName +
			"' declared as a domain of relation '" + relName +
			"' has type '" + type + "' which is not a subclass of '" +
			ProgramDom.class.getName() + "'.");
		hasNoErrors = false;
	}
	
	private static void undefinedTarget(String name,
			List<String> consumerTaskNames) {
		System.err.println("WARNING: '" + name +
			"' not declared as produced name of any task; " +
			"declared as consumed name of following tasks:");
		for (String taskName : consumerTaskNames) {
			System.err.println("\t'" + taskName + "'");
		}
	}
	
	private static void redefinedTarget(String name,
			List<String> producerTaskNames) {
		System.err.println("WARNING: '" + name +
			"' declared as produced name of multiple tasks:");
		for (String taskName : producerTaskNames) {
			System.err.println("\t'" + taskName + "'");
		}
	}
	
	private static void inconsistentDomNames(String relName, String names1,
			String names2, String loc1, String loc2) {
		System.err.println("ERROR: Relation '" + relName +
			"' declared with different domain names '" + names1 +
			"' and '" + names2 + "' in '" + loc1 + "' and '" + loc2 +
			"' respectively.");
		hasNoErrors = false;
	}
	
	private static void inconsistentDomOrders(String relName, String order1,
			String order2, String loc1, String loc2) {
		System.err.println("WARNING: Relation '" + relName +
			"' declared with different domain orders '" + order1 +
			"' and '" + order2 + "' in '" + loc1 + "' and '" + loc2 +
			"' respectively.");
	}
	
	private static void inconsistentTypes(String name, String type1,
			String type2, String loc1, String loc2) {
		System.err.println("ERROR: '" + name +
			"' declared with inconsistent types '" + type1 +
			"' and '" + type2 + "' in '" + loc1 + "' and '" + loc2 +
			"' respectively.");
		hasNoErrors = false;
	}
	
	private static void unknownSign(String name) {
		System.err.println("ERROR: sign of relation '" + name +
			"' unknown.");
		hasNoErrors = false;
	}
	
	private static void unknownOrder(String name) {
		System.err.println("ERROR: order of relation '" + name +
			"' unknown.");
		hasNoErrors = false;
	}
	
	private static void unknownType(String name) {
		System.err.println("ERROR: type of target '" + name +
			"' unknown.");
		hasNoErrors = false;
	}
	
	private static void redefinedJavaTask(String newTaskName, String name,
			String oldTaskName) {
		System.err.println("ERROR: Ignoring Java analysis '" +
			newTaskName +
			"': its @Chord(name=\"...\") annotation uses name '" +
			name + "' that is also used for another task '" +
			oldTaskName + "'.");
		hasNoErrors = false;
	}
	private static void redefinedDlogTask(String newTaskName, String name,
			String oldTaskName) {
		System.err.println("ERROR: Ignoring Dlog analysis '" +
			newTaskName +
			"': its # name=\"...\" line uses name '" +
			name + "' that is also used for another task '" +
			oldTaskName + "'.");
		hasNoErrors = false;
	}
	
	private static void malformedPathElem(String elem, String path,
			String msg) {
		System.err.println("WARNING: Ignoring malformed entry '" +
			elem + "' in '" + path + "': " + msg + ".");
	}
	
	private static void nonexistentPathElem(String elem, String path) {
		System.err.println("WARNING: Ignoring non-existent entry '" +
			elem + "' in '" + path + "'.");
	}
	
	private static void nonInstantiableJavaAnalysis(String name, String msg) {
		System.err.println("ERROR: Ignoring Java analysis task '" +
			name + "': " + msg + ".");
		hasNoErrors = false;
	}
}

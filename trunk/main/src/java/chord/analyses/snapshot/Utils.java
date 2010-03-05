package chord.analyses.snapshot;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Collections;

import joeq.Compiler.Quad.Quad;
import chord.doms.DomT;

import chord.util.IntArraySet;
import chord.util.ChordRuntimeException;
import chord.project.Properties;
import chord.util.IndexMap;
import chord.util.IndexHashMap;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.analyses.DynamicAnalysis;
import chord.project.analyses.ProgramRel;
import chord.project.Project;
import chord.program.Program;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TLongIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntArrayList;

/**
 * General utilities.
 *
 * @author Percy Liang (pliang@cs.berkeley.edu)
 */
public class Utils {
  public static <S, T> void add(Map<S, List<T>> map, S key1, T key2) {
    List<T> s = map.get(key1);
    if(s == null) map.put(key1, s = new ArrayList<T>());
    s.add(key2);
  }

  public static PrintWriter openOut(String path) {
    try {
      return new PrintWriter(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

class Execution {
  HashMap<Object,Object> output = new LinkedHashMap<Object,Object>(); // For statistics, which get dumped
  int numErrors = 0;
  StopWatch watch = new StopWatch();

  String path(String name) { return name == null ? basePath : basePath+"/"+name; }

  public Execution() {
    basePath = Properties.outDirName;
    System.out.println("Execution directory: "+basePath);
    //logOut = Utils.openOut(path("log"));
    logOut = new PrintWriter(System.out);
    
    String view = System.getProperty("chord.partition.addToView", null);
    if (view != null) {
      PrintWriter out = Utils.openOut(path("addToView"));
      out.println(view);
      out.close();
    }
    watch.start();
  }

  public void logs(String format, Object... args) {
    logOut.println(String.format(format, args));
    logOut.flush();
  }

  public void errors(String format, Object... args) {
    numErrors++;
    logOut.print("ERROR: ");
    logOut.println(String.format(format, args));
    logOut.flush();
  }

  public void writeMap(String name, HashMap<Object,Object> map) {
    PrintWriter out = Utils.openOut(path(name));
    for (Object key : map.keySet()) {
      out.println(key+"\t"+map.get(key));
    }
    out.close();
  }

  public void finish() {
    watch.stop();
    output.put("exec.time", watch);
    output.put("exec.errors", numErrors);
    writeMap("output.map", output);
  }

  String basePath;
	PrintWriter logOut;
}

class StatFig {
  double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, sum = 0;
  int n = 0;

  double mean() { return sum/n; }

  void add(double x) {
    sum += x;
    n += 1;
    min = Math.min(min, x);
    max = Math.max(max, x);
  }

  @Override public String toString() {
    return String.format("%.2f / %.2f / %.2f (%d)", min, sum/n, max, n);
  }
}

interface GraphMonitor {
  public void finish();
  public void addNode(int a, String label);
  public void deleteEdge(int a, int b);
  public void addEdge(int a, int b, String label);
  public void setNodeLabel(int a, String label);
  public void setNodeColor(int a, String color);
  public void setNodeColor(int a, int color);
}

class SerializingGraphMonitor implements GraphMonitor {
  PrintWriter out;
  int max;
  int n = 0;
  public SerializingGraphMonitor(String path, int max) {
    this.out = Utils.openOut(path);
    this.max = max;
  } 
  boolean c() {
    if (n >= max) return false;
    n++;
    return true;
  }
  public void finish() { out.close(); }
  public void addNode(int a, String label) { if (c()) out.printf("n %s%s\n", a, label == null ? "" : " "+label); }
  public void deleteEdge(int a, int b) { if (c()) out.printf("-e %s %s\n", a, b); }
  public void addEdge(int a, int b, String label) { if (c()) out.printf("e %s %s%s\n", a, b, label == null ? "" : " "+label); }
  public void setNodeLabel(int a, String label) { if (c()) out.printf("nl %s %s\n", a, label); }
  public void setNodeColor(int a, String color) { if (c()) out.printf("nc %s %s\n", a, color); }
  public void setNodeColor(int a, int color) { }
}

/*class UbiGraphMonitor implements GraphMonitor {
  UbigraphClient graph = new UbigraphClient();
  TIntIntHashMap nodes = new TIntIntHashMap();
  TLongIntHashMap edges = new TLongIntHashMap();

  String[] colors;

  long encode(int a, int b) { return a*1000000000+b; }

  public UbiGraphMonitor() {
    graph.clear();
    Random random = new Random(1);
    colors = new String[1000];
    for (int i = 0; i < colors.length; i++)
      colors[i] = String.format("#%06x", random.nextInt(1<<(4*6)));
    graph.setEdgeStyleAttribute(0, "arrow", "true"); // Default
  }
  public void finish() { }
  public void addNode(int a, String label) {
    int v = graph.newVertex();
    if (label != null) graph.setVertexAttribute(v, "label", label);
    nodes.put(a, v);
  }
  public void setNodeLabel(int a, String label) {
    graph.setVertexAttribute(nodes.get(a), "label", label);
  }
  public void setNodeColor(int a, int color) {
    graph.setVertexAttribute(nodes.get(a), "color", colors[color%colors.length]);
  }
  public void setNodeColor(int a, String color) {
    graph.setVertexAttribute(nodes.get(a), "color", color);
  }
  public void deleteEdge(int a, int b) {
    int e = edges.remove(encode(a, b));
    graph.removeEdge(e);
  }
  public void addEdge(int a, int b, String label) {
    if (a == b) return; // Ubigraph doesn't support self-loops
    int e = graph.newEdge(nodes.get(a), nodes.get(b));
    //if (label != null) graph.setEdgeAttribute(e, "label", label); // Can't do this now - need to create edge style
    edges.put(encode(a, b), e);
  }
}*/

/**
 * Simple class for measuring elapsed time.
 */
class StopWatch
{
	public StopWatch()
	{
	}

	public StopWatch(long ms)
	{
		startTime = 0;
		endTime = ms;
		this.ms = ms;
	}

	public void reset()
	{
		ms = 0;
		isRunning = false;
	}

	public StopWatch start()
	{
    assert !isRunning;
		isRunning = true;
		startTime = System.currentTimeMillis();

		return this;
	}

	public StopWatch stop()
	{
		assert isRunning;
		endTime = System.currentTimeMillis();
		isRunning = false;
		ms = endTime - startTime;
		n = 1;
		return this;
	}

	public StopWatch accumStop()
	{
    // Stop and accumulate time
		assert isRunning;
		endTime = System.currentTimeMillis();
		isRunning = false;
		ms += endTime - startTime;
		n++;
		return this;
	}

  public void add(StopWatch w) {
    assert !isRunning && !w.isRunning;
    ms += w.ms;
    n += w.n;
  }

	public long getCurrTimeLong()
	{
		return ms + (isRunning() ? System.currentTimeMillis() - startTime : 0);
	}

	@Override
	public String toString()
	{
		long msCopy = ms;
		long m = msCopy / 60000;
		msCopy %= 60000;
		long h = m / 60;
		m %= 60;
		long d = h / 24;
		h %= 24;
		long y = d / 365;
		d %= 365;
		long s = msCopy / 1000;

		StringBuilder sb = new StringBuilder();

		if (y > 0)
		{
			sb.append(y);
			sb.append('y');
			sb.append(d);
			sb.append('d');
		}
		if (d > 0)
		{
			sb.append(d);
			sb.append('d');
			sb.append(h);
			sb.append('h');
		}
		else if (h > 0)
		{
			sb.append(h);
			sb.append('h');
			sb.append(m);
			sb.append('m');
		}
		else if (m > 0)
		{
			sb.append(m);
			sb.append('m');
			sb.append(s);
			sb.append('s');
		}
		else if (s > 9)
		{
			sb.append(s);
			sb.append('s');
		}
		else if (s > 0)
		{
			sb.append((ms / 100) / 10.0);
			sb.append('s');
		}
		else
		{
			sb.append(ms / 1000.0);
			sb.append('s');
		}
		return sb.toString();
	}

	public long startTime, endTime, ms;

	public int n;

	private boolean isRunning = false;

	public boolean isRunning()
	{
		return isRunning;
	}
}

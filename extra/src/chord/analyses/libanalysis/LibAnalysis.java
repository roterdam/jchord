/*
 * Copyright (c) 2008-2010, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 * Licensed under the terms of the New BSD License.
 *
 */
package chord.analyses.libanalysis;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.net.Socket;
import java.net.ServerSocket;

import joeq.Class.jq_ClassInitializer;
import joeq.Class.jq_Class;
import joeq.Class.jq_Type;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Reference;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Inst;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import chord.util.Execution;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.DomC;
import chord.analyses.alias.CtxtsAnalysis;
import chord.bddbddb.Dom;
import chord.bddbddb.Rel.AryNIterable;
import chord.bddbddb.Rel.IntPairIterable;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.TrioIterable;
import chord.bddbddb.Rel.QuadIterable;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.HextIterable;
import chord.bddbddb.Rel.RelView;
import chord.analyses.alloc.DomH;
import chord.analyses.invk.DomI;
import chord.analyses.method.DomM;
import chord.analyses.heapacc.DomE;
import chord.analyses.point.DomP;
import chord.analyses.var.DomV;
import chord.program.Program;
import chord.project.Chord;
import chord.project.Messages;
import chord.project.OutDirUtils;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.DlogAnalysis;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.IndexMap;
import chord.util.ArraySet;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.tuple.integer.IntPair;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.Utils;
import chord.util.StopWatch;

class QueryGen extends Query {
	private Dom[] doms;
	private Object[] tuple;
	private String enc;
	private int mod;
	
	public Dom[] getDoms() {
		return doms;
	}

	public Object[] getTuple() {
		return tuple;
	}

	public String getEnc() {
		return enc;
	}

	public QueryGen(Dom[] doms, Object[] tuple) {
		mod = 1;
		this.doms = doms;
		this.tuple = tuple;
		this.enc = this.encode();
	}
	
	public QueryGen(String enc) {
		mod = 1;
		this.doms = null;
		this.tuple = null;
		this.decode(enc);
	}
	
	public QueryGen() {
	}
	
	public QueryGen(QueryGen q){
		this.doms = q.doms;
		this.tuple = q.tuple;
		this.enc = new String(q.enc);
		this.mod = q.mod;
	}
	
	@Override public int hashCode() { return enc.hashCode(); }
	@Override public String toString() { 
		String retStr = "";
		for(int c = 0; c < tuple.length; c++){
			//retStr += tuple[c].toString();
			retStr += doms[c].toUniqueString(tuple[c]);
			retStr += " # ";
		}
		return retStr;
		//return enc;
	}
	
	@Override
	public boolean equals(Object q){
		if(this.encode().equalsIgnoreCase(((QueryGen)q).encode()))
			return true;
		return false;
	}
	
	public String encode(){
		if(mod == 1){
			int n = doms.length;
			enc = new String("");
			for (int i = 0; i < n; i++) {
				Object o = tuple[i];
				enc += doms[i].getName() + ":" + doms[i].indexOf(o);
				//enc += doms[i].toUniqueString(o);
				if (i < n - 1){
					enc += ",";
					//enc += "||,||";
				}
			}
			//enc =  "Q?"+enc;
			mod = 0;
		}
		return enc;
	}
	
	public void decode(String s){
		//if(s.charAt(0)!='Q' || s.charAt(1)!='?'){
			//return;
		//}
		
		mod = 1;
		String[] types = s.split(",");
		//String[] row = s.substring(2).split("||,||");
		if(doms==null)
			doms = new Dom[types.length];
		if(tuple == null)
			tuple = new Object[types.length];
		
		int c = 0;
		for(String type : types){
			String[] parts = type.split(":");
			ProgramDom dom = (ProgramDom) ClassicProject.g().getTrgt(parts[0]);
			Object obj = dom.get(Integer.parseInt(parts[1]));
			doms[c] = dom;
			tuple[c] = obj;
			c++;
		}
		this.enc = s;
		mod = 0;
	}

	@Override
	public int compareTo(Query q) {
		
		return this.encode().compareToIgnoreCase(q.encode());
	}
	
	public static String encodeStat(Dom[] doms, Object[] tuple){
		int n = doms.length;
		String enc = new String("");
		for (int i = 0; i < n; i++) {
			Object o = tuple[i];
			enc += doms[i].getName() + ":" + doms[i].indexOf(o);
			//enc += doms[i].toUniqueString(o);
			if (i < n - 1){
				enc += ",";
				//enc += "||,||";
			}
		}
		return enc;
	}

}

class QueryGenFactory implements QueryFactory{
	@Override
	public QueryGen create(Object[] objs) {
		if(objs.length != 2)
			return null;
		if(objs[0].getClass().isArray() && objs[1].getClass().isArray()){
			if(Dom.class.isAssignableFrom(objs[0].getClass().getComponentType()) && Object.class.isAssignableFrom(objs[1].getClass().getComponentType())) 
			
				return new QueryGen((Dom[])objs[0],(Object[])objs[1]);
		}
		return null;
		
			
	}

	@Override
	public QueryGen create(Query q) {
		if(!QueryGen.class.isInstance(q))
			return null;
		return new QueryGen((QueryGen)q);
		
	}

	@Override
	public QueryGen create() {
		return new QueryGen();
	}

	@Override
	public QueryGen create(String enc) {
		// TODO Auto-generated method stub
		return new QueryGen(enc);
	}
}

class MethodAbstraction extends Abstraction {
	private DomM domM;
	private jq_Method abs;
	private String enc;
	
	public DomM getDomM() {
		return domM;
	}

	public jq_Method getAbs() {
		return abs;
	}

	public String getEnc() {
		return enc;
	}
	
	public MethodAbstraction(jq_Method m) {
		this.domM = (DomM) ClassicProject.g().getTrgt("M"); 
		if(!ClassicProject.g().isTaskDone(domM))
			ClassicProject.g().runTask(domM);
		this.abs = m;
		this.setLevel(0);
		this.setMaxLevel(1);
		this.setMinLevel(0);
		this.mod = 1;
		this.enc = this.encode();
	}
	
	public MethodAbstraction(String enc) { 
		//this.domM = (DomM) ClassicProject.g().getTrgt("M"); ClassicProject.g().runTask(domM);
		//this.abs = null;
		mod = 1;
		this.setMinLevel(0);
		this.setMaxLevel(1);
		this.decode(enc);
		
	}
	
	public MethodAbstraction(){
		this.domM = (DomM) ClassicProject.g().getTrgt("M"); 
		if(!ClassicProject.g().isTaskDone(domM))
			ClassicProject.g().runTask(domM);
		this.abs = null;
		this.enc = null;
		this.setMinLevel(0);
		this.setMaxLevel(1);
		this.setLevel(0);
		mod = 1;
	}
	
	public MethodAbstraction(MethodAbstraction a){
		this.domM = (DomM) ClassicProject.g().getTrgt("M"); 
		if(!ClassicProject.g().isTaskDone(domM))
			ClassicProject.g().runTask(domM);
		
		this.abs = a.abs;
		this.enc = new String(a.enc);
		this.setMinLevel(a.getMinLevel());
		this.setMaxLevel(a.getMaxLevel());
		this.setLevel(a.getLevel());
		this.mod = a.mod;
	}
	
	@Override public int hashCode() { return enc.hashCode(); }
	@Override public String toString() { 
		return abs.toString();
	}
	
	@Override
	public boolean equals(Object q){
		if(this.encode().equalsIgnoreCase(((MethodAbstraction)q).encode()))
			return true;
		return false;
	}
	public String encode(){
		if(mod == 1){
			enc = new String("");
			enc += domM.getName() + ":" + domM.indexOf(abs) + ":" + this.getLevel();
			mod = 0;
		}
		return enc;
	}
	
	public void decode(String s){
			String[] parts = s.split(":");
			//ProgramDom dom = (ProgramDom) ClassicProject.g().getTrgt("M");
			mod = 1;
			domM = (DomM) ClassicProject.g().getTrgt("M"); 
			if(!ClassicProject.g().isTaskDone(domM))
				ClassicProject.g().runTask(domM);
			if(domM.getName().equalsIgnoreCase(parts[0])){
				abs = domM.get(Integer.parseInt(parts[1]));
				this.setLevel(Integer.parseInt(parts[2]));
				this.enc = s;
				mod = 0;
			}
	}

	@Override
	public int compareTo(Abstraction a) {
		
		return this.encode().compareToIgnoreCase(a.encode());
	}
	
	@Override
	public void refine() {
		this.setLevel(1);	
	}

	@Override
	public void maxRefine() { 
		this.setLevel(this.getMaxLevel());
	}

	@Override
	public void minRefine() {
		this.setLevel(this.getMinLevel());
	}
	
	@Override
	public void copy(Abstraction a) {
		if(!MethodAbstraction.class.isInstance(a))
			return;
		
		this.domM = (DomM) ClassicProject.g().getTrgt("M"); 
		if(!ClassicProject.g().isTaskDone(domM))
			ClassicProject.g().runTask(domM);
		
		this.abs = ((MethodAbstraction)a).abs;
		this.enc = new String(((MethodAbstraction)a).encode());
		this.setMinLevel(a.getMinLevel());
		this.setMaxLevel(a.getMaxLevel());
		this.setLevel(a.getLevel());
		mod = 0;
		
	}

}

class MethodAbstractionFactory implements AbstractionFactory{

	@Override
	public MethodAbstraction create() {
		// TODO Auto-generated method stub
		return new MethodAbstraction();
	}

	@Override
	public MethodAbstraction create(Object[] objs) {
		if(objs.length != 1)
			return null;
		if(!jq_Method.class.isInstance(objs[0]))
			return null;
		return new MethodAbstraction((jq_Method)objs[0]);
	}

	@Override
	public MethodAbstraction create(Abstraction a) {
		if(!MethodAbstraction.class.isInstance(a))
			return null;
		
		return new MethodAbstraction((MethodAbstraction)a);
	}

	@Override
	public MethodAbstraction create(String enc) {
		// TODO Auto-generated method stub
		return new MethodAbstraction(enc);
	}
	
}

@Chord(
		name = "libanalysis-java"
		)
public class LibAnalysis extends ParallelAnalysis {

	String coarseningStrategy;
	boolean isScan;
	String staticAnalysis;
	String trackedRelation;
	String excludedLib;
	String[] excludedLibArr;
	String sepMajor;
	String sepMin;

	Set<Abstraction> mSet = new HashSet<Abstraction>();
	Set<QueryGen> qSet = new HashSet<QueryGen>();
	MethodAbstractionFactory mFactory = new MethodAbstractionFactory();
	QueryGenFactory qFactory = new QueryGenFactory();
	

	
	// Initialization to do anything.
	protected void init() {
		this.sepMajor = new String("##");
		this.sepMin = new String("<<,>>");
		this.coarseningStrategy		 = X.getStringArg("coarseningStrategy", "scan");
		if(this.coarseningStrategy.equalsIgnoreCase("scan")) isScan = true;
		this.staticAnalysis          = X.getStringArg("staticAnalysis", "");
		this.trackedRelation        = X.getStringArg("trackedRelation", "");
		this.excludedLib             = Config.scopeExcludeStr;
		this.excludedLibArr          = Utils.toArray(this.excludedLib);
		
		this.masterHost              = X.getStringArg("masterHost", null);
		this.masterPort              = X.getIntArg("masterPort", 8888);
		this.mode                    = X.getStringArg("mode", null);

		ClassicProject.g().runTask("libM");
		// Excluded Methods
		{
			ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt("libM"); rel.load();
			System.out.println(rel.size());
			Iterable<jq_Method> result = rel.getAry1ValTuples();
			for (jq_Method m : result) {
				Object[] objs = new Object[1];
				objs[0] = m;
				mSet.add(mFactory.create(objs));
			}
			rel.close();
		}
	}

	@Override
	protected String setMasterHost() {
		return this.masterHost;
	}

	@Override
	protected int setMasterPort() {
		return this.masterPort;
	}

	@Override
	protected String setMode() {
		return this.mode;
	}

	@Override
	protected JobDispatcher setJobDispatcher() {
		return new AbstractionMinimizer(isScan,(Set<Query>)null,mSet,qFactory, mFactory,this,sepMajor,sepMin);
	}
	
	protected void finishAnalysis() {
		DomV domV = (DomV) ClassicProject.g().getTrgt("V"); 
		DomH domH = (DomH) ClassicProject.g().getTrgt("H");
		if(!ClassicProject.g().isTaskDone(domV))
			ClassicProject.g().runTask(domV);
		if(!ClassicProject.g().isTaskDone(domH))
			ClassicProject.g().runTask(domH);
		
		ClassicProject.g().resetTaskDone("allVH");
		ClassicProject.g().runTask("allVH");
		ProgramRel relAllVH = (ProgramRel)ClassicProject.g().getTrgt("allVH"); 
		relAllVH.load();
		PrintWriter out = Utils.openOut(X.path("queries_all.txt"));
		out.println("Queries:");
		out.println("Num:" + relAllVH.size());
		IntPairIterable itr = relAllVH.getAry2IntTuples();
		for(IntPair it : itr){
			out.println("  "+ domV.toUniqueString(it.idx0) + " # " + domH.toUniqueString(it.idx1));
		}
		out.close();

	}
	
	public void readQueries(ProgramRel rel, Collection<String> queries) {

		Dom[] doms = rel.getDoms();
		AryNIterable tuples = rel.getAryNValTuples();
		X.logs("Tracked relation size: " + rel.size());
		for (Object[] tuple : tuples) {
			//QueryGen temp = new QueryGen(doms,tuple);
			//queries.add(temp.encode());
			queries.add(QueryGen.encodeStat(doms, tuple));
		}
	}
	
	public String apply(String line) {
		//If no queries contained is incoming request, we assume that all valid queries are to be returned
			X.logs("Setting methods to be treated as no-ops");
			Set<String> queries;
			Set<MethodAbstraction> abstractions = new HashSet<MethodAbstraction>();
			//String type = decodeScenario(line, (Set)queries, (Set)abstractions, qFactory, mFactory);
			Scenario inScenario = new Scenario(line,sepMajor);
			
			if(!inScenario.getType().equalsIgnoreCase("1"))
				return inScenario.toString();
			
			ClassicProject.g().resetTaskDone("libM");
			ClassicProject.g().runTask("libM");
			ProgramRel relM = (ProgramRel)ClassicProject.g().getTrgt("libM"); relM.load();
			
			if(!inScenario.getIn().equalsIgnoreCase("")){
				for (String s : Abstraction.splitAbstractions(inScenario.getIn(),sepMin)) {
					MethodAbstraction a  = new MethodAbstraction(s);
					//a.decode(s);
					abstractions.add(a);
				}
			}
			
			for (MethodAbstraction m : abstractions) {
				if(m.getLevel() == 0)
					relM.remove(m.getAbs());
			}
			relM.save();
			//relM.close();
			
			ClassicProject.g().resetTaskDone(this.staticAnalysis);
			ClassicProject.g().runTask(this.staticAnalysis);

			Set<String> allUnproven = new HashSet<String>();
			ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(this.trackedRelation); rel.load();
			readQueries(rel, allUnproven);
			rel.close();

			if(inScenario.getOut().equalsIgnoreCase("")){
				String[] arr = allUnproven.toArray(new String[0]);
				inScenario.setOut(Query.concatQueries(arr, sepMin));
				return inScenario.toString();
			}
			
			queries = new HashSet<String>(Arrays.asList(Query.splitQueries(inScenario.getOut(),sepMin)));
			Set<String> unproven = new HashSet<String>();
			for(String q : allUnproven){
				if(queries.contains(q))
					unproven.add(q);
			}
			
			inScenario.setOut(Query.concatQueries(allUnproven.toArray(new String[0]),sepMin));
			return inScenario.toString();
	}
}






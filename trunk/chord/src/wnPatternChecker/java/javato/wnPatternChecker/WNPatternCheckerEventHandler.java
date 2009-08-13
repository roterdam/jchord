package javato.wnPatternChecker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javato.observer.Observer;
import javato.utils.Parameters;

/**
 * Copyright (c) 2007-2008,
 * Pallavi Joshi	<pallavi@cs.berkeley.edu>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class WNPatternCheckerEventHandler {
	public static ArrayList<String> iidToLineMap = Observer.getIidToLineMap(Parameters.iidToLineMapFile);
	private Database db;
	private ThreadBase tb = new ThreadBase();
	
	public WNPatternCheckerEventHandler(){
		db = new Database();
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(db));
	}
	
	public static ThreadLocal lockStack = new ThreadLocal() {
        protected Object initialValue() {
            return new Stack<LockStackElemInfo>();
        }
    };
	
    //need not be synchronized since it deals with thread local variables
	public void Lock(int iid, int t, int l){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		LockStackElemInfo lsElemInfo = new LockStackElemInfo(iid, l);
		ls.push(lsElemInfo);
	}
	
	public synchronized void Unlock(int iid, int t, int l){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		if(ls.size() == 1){
			List<WrListElemInfo> wrList = ls.peek().wrList;
			for(WrListElemInfo wrListElem : wrList){
				wrListElem.lockSet.add(l);
				db.addToDatabase(wrListElem.iids, wrListElem.m, t, wrListElem.lockSet, wrListElem.notifySet, tb.getVC(t));
			}
			ls.pop();
		}
		else{
			LockStackElemInfo topLsElem = ls.peek();
			LockStackElemInfo topButOneLsElem = ls.elementAt(ls.size() - 2);
			topButOneLsElem.updateRdList(topLsElem.rdList);
			for(WrListElemInfo wrElem : topLsElem.wrList){
				wrElem.addToLockSet(topLsElem.l);
			}
			topButOneLsElem.updateWrList(topLsElem.wrList);
			ls.pop();
		}
	}
	
	//need not be synchronized since it deals with thread local variables
	public void Read(int iid, int t, long m){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		RdListElemInfo rdElem = new RdListElemInfo(iid, m);

		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToRdList(rdElem);
		}
	}
	
	public synchronized void Write(int iid, int t, long m){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		WrListElemInfo wrElem = new WrListElemInfo(iid, m);
		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToWrList(wrElem);
		}
		else{
			List iids = new LinkedList<Integer>();
			iids.add(iid);
			db.addToDatabase(iids, m, t, new HashSet<Integer>(), new HashSet<Integer>(), tb.getVC(t));
		}
	}
	
	public synchronized void Wait(int iid, int t, int l){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		int indxL = getIndexForL(ls, l);
		
		List<RdListElemInfo> rdListForL = new LinkedList<RdListElemInfo>();
		for(int i = indxL; i < ls.size(); i++){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(i);
			rdListForL = unionOfRdLists(rdListForL, lsElem.rdList);
		}
		for(RdListElemInfo rdElem : rdListForL){
			db.addToDatabase(rdElem.iids, rdElem.m, t, l, tb.getVC(t));
		}
	}
	
	public  List<RdListElemInfo> unionOfRdLists(List<RdListElemInfo> rdList1, List<RdListElemInfo> rdList2){
		List unionList = new LinkedList<RdListElemInfo>(rdList1);
		for(RdListElemInfo rdElem : rdList2){
			if(unionList.contains(rdElem)){
				int indxOfRdElemInUnionList = unionList.indexOf(rdElem);
				RdListElemInfo rdElemInUnionList = (RdListElemInfo)unionList.get(indxOfRdElemInUnionList);
				rdElemInUnionList.iids.addAll(rdElem.iids);
				rdElemInUnionList.iids = Database.removeDupsFromList(rdElemInUnionList.iids);
			}
			else{
				unionList.add(rdElem);
			}
		}
		return unionList;
	}
	
	//need not be synchronized since it deals with thread local variables
	public void Notify(int iid, int t, int l){
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		int indxL = getIndexForL(ls, l);
		
		List<WrListElemInfo> wrListForL = new LinkedList<WrListElemInfo>();
		for(int i = indxL; i < ls.size(); i++){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(i);
			wrListForL = unionOfWrLists(wrListForL, lsElem.wrList);
		}
		for(WrListElemInfo wrElem : wrListForL){
			wrElem.addToNotifySet(l);
		}
	}
	
	public  List<WrListElemInfo> unionOfWrLists(List<WrListElemInfo> wrList1, List<WrListElemInfo> wrList2){
		List unionList = new LinkedList<WrListElemInfo>(wrList1);
		for(WrListElemInfo wrElem : wrList2){
			if(unionList.contains(wrElem)){
				int indxOfWrElemInUnionList = unionList.indexOf(wrElem);
				WrListElemInfo wrElemInUnionList = (WrListElemInfo)unionList.get(indxOfWrElemInUnionList);
				wrElemInUnionList.iids.addAll(wrElem.iids);
				wrElemInUnionList.iids = Database.removeDupsFromList(wrElemInUnionList.iids);
			}
			else{
				unionList.add(wrElem);
			}
		}
		return unionList;
	}
	
	public synchronized void Start(int iid, int parent, int child){
		tb.Start(parent, child);
	}
	
	public synchronized void Join(int iid, int parent, int child){
		tb.Join(parent, child);
	}
	
	int getIndexForL(Stack<LockStackElemInfo> ls, int l){
		int indxL = 0;
		for(indxL = ls.size() - 1; indxL >= 0; indxL--){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(indxL);
			if(lsElem.l == l){
				break;
			}
		}
		assert(indxL >= 0);
		return indxL;
	}
	
}

class ShutdownThread extends Thread {
	private Database db;
	public ShutdownThread(Database db){
		this.db = db;
	}
    
    public void run() {
        db.checkForErrors();
    }
}



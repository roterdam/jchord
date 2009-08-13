package javato.wnPatternChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javato.utils.VectorClock;

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


public class Database {
	private Map<Long, Map<Integer, List<DBElemInfo>>> eBase;
	private Set<ErrorInfo> errors;
	
	public Database(){
		eBase = new HashMap<Long, Map<Integer, List<DBElemInfo>>>();
		errors = new HashSet<ErrorInfo>();
	}
	
	public void addToDatabase(List iids, long m, int t, Set<Integer> lSet, Set<Integer> nSet, VectorClock vc){
		DBElemInfo elem = new DBElemInfo(iids, lSet, nSet, new VectorClock(vc));
		
		List<DBElemInfo> tValueInMValueInEBase = getValueForTInValueForMInEBase(m, t);
		addToDBElemInfoList(tValueInMValueInEBase, elem, iids);
	}
	
	public void addToDatabase(List iids, long m, int t, int l, VectorClock vc){
		DBElemInfo elem = new DBElemInfo(iids, l, new VectorClock(vc));
		
		List<DBElemInfo> tValueInMValueInEBase = getValueForTInValueForMInEBase(m, t);
		addToDBElemInfoList(tValueInMValueInEBase, elem, iids);	
	}
	
	public List<DBElemInfo> getValueForTInValueForMInEBase(long m, int t){
		Map<Integer, List<DBElemInfo>> mValueInEBase = eBase.get(m);
		if(mValueInEBase == null){
			mValueInEBase = new HashMap<Integer, List<DBElemInfo>>();
			eBase.put(m, mValueInEBase);
		}
		List<DBElemInfo> tValueInMValueInEBase = mValueInEBase.get(t);
		if(tValueInMValueInEBase == null){
			tValueInMValueInEBase = new LinkedList<DBElemInfo>();
			mValueInEBase.put(t, tValueInMValueInEBase);
		}
		return tValueInMValueInEBase;
	}
	
	public void addToDBElemInfoList(List<DBElemInfo> tValueInMValueInEBase, DBElemInfo elem, List iids){
		if(tValueInMValueInEBase.contains(elem)){
			int index = tValueInMValueInEBase.indexOf(elem);
			assert (index != -1);
			DBElemInfo elemInList = tValueInMValueInEBase.get(index);
			elemInList.iids.addAll(iids);
			elemInList.iids = removeDupsFromList(elemInList.iids);
		}
		else{
			tValueInMValueInEBase.add(elem);
		}
	}
	
	public static List<Integer> removeDupsFromList(List<Integer> iids){
		Set<Integer> iidsSet = new HashSet<Integer>(iids);
		List<Integer> iidsListWithoutDups = new LinkedList<Integer>(iidsSet);
		return iidsListWithoutDups;
	}
	
	public void checkForErrors(){
		Iterator eBaseItr = eBase.entrySet().iterator();
		while(eBaseItr.hasNext()){
			Map.Entry eBaseEntry = (Map.Entry)eBaseItr.next();
			Long m = (Long)(eBaseEntry.getKey());
			Map<Integer, Set<DBElemInfo>> eBaseEntryForM = 
				(Map<Integer, Set<DBElemInfo>>)eBaseEntry.getValue();
			
			Iterator eBaseEntryForMItr1 = eBaseEntryForM.entrySet().iterator();
			while(eBaseEntryForMItr1.hasNext()){
				Map.Entry eBaseEntryForMEntry1 = (Map.Entry)eBaseEntryForMItr1.next();
				Integer t1 = (Integer)eBaseEntryForMEntry1.getKey();
				List<DBElemInfo> t1DBElems = (List<DBElemInfo>)eBaseEntryForMEntry1.getValue();
				
				Iterator eBaseEntryForMItr2 = eBaseEntryForM.entrySet().iterator();
				while(eBaseEntryForMItr2.hasNext()){
					Map.Entry eBaseEntryForMEntry2 = (Map.Entry)eBaseEntryForMItr2.next();
					Integer t2 = (Integer)eBaseEntryForMEntry2.getKey();
					List<DBElemInfo> t2DBElems = (List<DBElemInfo>)eBaseEntryForMEntry2.getValue();
					if(t1 != t2){
						checkForErrors(m, t1, t1DBElems, t2, t2DBElems);
					}
				}
			}
		}
	}
	
	public void checkForErrors(Long m, Integer t1, List<DBElemInfo> t1DBElems, Integer t2, 
			List<DBElemInfo> t2DBElems){
		
		assert(t1.intValue() != t2.intValue());
		for(DBElemInfo e1 : t1DBElems){
			if(e1.isReadElem){
				for(DBElemInfo e2 : t2DBElems){
					if(!e2.isReadElem){
						Integer l = e1.l;
						VectorClock vc1 = e1.vc;
						Set<Integer> lSet = e2.lockSet;
						Set<Integer> nSet = e2.notifySet;
						VectorClock vc2 = e2.vc;
						
						if(areEventsParallel(vc1, vc2) && !lSet.contains(l)){
							ErrorInfo errInfo = new ErrorInfo(e2.iids, e1.iids, false);
							
							if(!(errors.contains(errInfo))){
								System.err.println("-----------------------------ERROR-----------------------------");
								System.err.println("Writes to memory at "+printIIDs(e2.iids)+
										" without holding the right " +
										"lock.  Reads of the same memory involved in the computation of the " +
										"condition of the wait are at "+printIIDs(e1.iids));
								System.err.println("---------------------------------------------------------------");
								errors.add(errInfo);
							}
						}
						else if(areEventsParallel(vc1, vc2) && !nSet.contains(l)){
							ErrorInfo errInfo = new ErrorInfo(e2.iids, e1.iids, true);
							
							if(!errors.contains(errInfo)){
								System.err.println("-----------------------------ERROR-----------------------------");
								System.err.println("Writes to memory at "+printIIDs(e2.iids)+
										" holding the right lock " +
										"but without a notification on it before releasing it. Reads of the same " +
										"memory involved in the computation of the condition of the wait are at "+
										printIIDs(e1.iids));
								System.err.println("---------------------------------------------------------------");
								errors.add(errInfo);
							}
							
						}
					}
				}
			}
		}
	  
	}
	
	public String printIIDs(List<Integer> iids){
		ArrayList<String> iidToLineMap = WNPatternCheckerEventHandler.iidToLineMap;
		String res = "[ ";
		for(Integer iid : iids){
			res += iidToLineMap.get(iid)+" ";
		}
		res += "]";
		return res;
	}
	
	
	boolean areEventsParallel(VectorClock vc1, VectorClock vc2){
		if((VectorClock.isVC1LessThanOrEqualToVC2(vc1, vc2)) || (VectorClock.isVC1LessThanOrEqualToVC2(vc2, vc1))){
			return false;
		}
		return true;
	}
	
}


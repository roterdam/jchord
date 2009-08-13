package javato.wnPatternChecker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

public class LockStackElemInfo {
	public int l;
	public List<RdListElemInfo> rdList;
	public List<WrListElemInfo> wrList;
	public int iid;
	
	public LockStackElemInfo(int iid, int lock){
		this.l = lock;
		this.rdList = new LinkedList<RdListElemInfo>();
		this.wrList = new LinkedList<WrListElemInfo>();
		this.iid = iid;
	}
	
	public void updateWrList(List<WrListElemInfo> wrListToBeAdded){
		for(WrListElemInfo wrElem : wrListToBeAdded){
			if(wrList.contains(wrElem)){
				int wrElemIndxInWrList = wrList.indexOf(wrElem);
				WrListElemInfo wrElemInWrList = (WrListElemInfo)wrList.get(wrElemIndxInWrList);
				wrElemInWrList.iids.addAll(wrElem.iids);
				wrElemInWrList.iids = Database.removeDupsFromList(wrElemInWrList.iids);
			}
			else{
				wrList.add(wrElem);
			}
		}
	}
	
	public void updateRdList(List<RdListElemInfo> rdListToBeAdded){
		for(RdListElemInfo rdElem : rdListToBeAdded){
			if(rdList.contains(rdElem)){
				int rdElemIndxInRdList = rdList.indexOf(rdElem);
				RdListElemInfo rdElemInRdList = (RdListElemInfo)rdList.get(rdElemIndxInRdList);
				rdElemInRdList.iids.addAll(rdElem.iids);
				rdElemInRdList.iids = Database.removeDupsFromList(rdElemInRdList.iids);
			}
			else{
				rdList.add(rdElem);
			}
		}
		
	}
	
	public void addToRdList(RdListElemInfo rdElem){
		if(rdList.contains(rdElem)){
			int rdElemIndxInRdList = rdList.indexOf(rdElem);
			RdListElemInfo rdElemInRdList = (RdListElemInfo)rdList.get(rdElemIndxInRdList);
			rdElemInRdList.iids.addAll(rdElem.iids);
			rdElemInRdList.iids = Database.removeDupsFromList(rdElemInRdList.iids);
		}
		else{
			rdList.add(rdElem);		
		}
	}
	
	public void addToWrList(WrListElemInfo wrElem){
		if(wrList.contains(wrElem)){
			int wrElemIndxInWrList = wrList.indexOf(wrElem);
			WrListElemInfo wrElemInWrList = (WrListElemInfo)wrList.get(wrElemIndxInWrList);
			wrElemInWrList.iids.addAll(wrElem.iids);
			wrElemInWrList.iids = Database.removeDupsFromList(wrElemInWrList.iids);
		}
		else{
			wrList.add(wrElem);		
		}
	}
}

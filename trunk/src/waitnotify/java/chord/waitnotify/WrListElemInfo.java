package chord.waitnotify;

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

public class WrListElemInfo {
	public long m;
	public Set<Integer> lockSet;
	public Set<Integer> notifySet;
	public List<Integer> iids;
	
	public WrListElemInfo(int iid, long m){
		iids = new LinkedList<Integer>();
		iids.add(iid);
		this.m = m;
		lockSet = new HashSet<Integer>();
		notifySet = new HashSet<Integer>();
	}
	
	
	public void addToLockSet(int l){
		lockSet.add(l);
	}
	
	public void addToNotifySet(int l){
		notifySet.add(l);
	}
	
	public boolean equals(Object other){
		if(!(other instanceof WrListElemInfo)){
			return false;
		}
		WrListElemInfo otherWrSetElemInfo = (WrListElemInfo)other;
		if((m == otherWrSetElemInfo.m) && (lockSet.equals(otherWrSetElemInfo.lockSet)) && 
				(notifySet.equals(otherWrSetElemInfo.notifySet))){
			return true;
		}
		return false;
	}
	
	public int hashCode(){
		int hash = 1;
		hash = hash*31 + (new Long(m)).intValue();
		hash = hash*31 + lockSet.hashCode();
		hash = hash*31 + notifySet.hashCode();
		return hash;
	}
}

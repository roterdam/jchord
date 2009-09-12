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


public class DBElemInfo {
	int l;
	int waitIID;
	Set<Integer> lockSet;
	Set<Integer> notifySet;
	boolean isReadElem;
	VectorClock vc;
	List<Integer> iids;
	
	public String toString() {
		String s = "lockSet: ";
		if (lockSet == null)
			s += "null";
		else {
			for (Integer x : lockSet)
				s += x + " ";
		}
		s += "notifySet: ";
		if (notifySet == null)
			s += "null";
		else {
			for (Integer x : notifySet)
				s += x + " ";
		}
		s += "isReadElem: " + isReadElem + " iids: ";
		if (iids == null)
			s += "null";
		else {
			for (Integer x : iids)
				s += x + " ";
		}
		return s;
	}
	
	public DBElemInfo(List iids, int lock, int waitIID, VectorClock vc){
		l = lock;
		this.waitIID = waitIID;
		this.vc = vc; 
		isReadElem = true;
		this.iids = new LinkedList<Integer>(iids);
		lockSet = null;
		notifySet = null;
	}
	
	
	public DBElemInfo(List iids, Set<Integer> lSet, Set<Integer> nSet, VectorClock vc){
		lockSet = new HashSet<Integer>(lSet);
		notifySet = new HashSet<Integer>(nSet);
		this.vc = vc;
		isReadElem = false;
		this.iids = new LinkedList<Integer>(iids);
		l = 0;
		waitIID = 0;
	}
	
	public boolean equals(Object other){
		if(!(other instanceof DBElemInfo)){
			return false;
		}
		DBElemInfo otherDBElem = (DBElemInfo)other;
		if(this.isReadElem){
			if((otherDBElem.isReadElem == true) && (otherDBElem.l == l) && (otherDBElem.waitIID == waitIID) && 
					(vc.equals(otherDBElem.vc))){
				return true;
			}
		}
		else{
			if((otherDBElem.isReadElem == false) && (lockSet.equals(otherDBElem.lockSet)) && 
					(notifySet.equals(otherDBElem.notifySet)) && (vc.equals(otherDBElem.vc))){
				return true;
			}
		}
		return false;
	}
	
	public int hashCode(){
		int hash = 1;
		if(this.isReadElem){
			hash = hash*31 + l;
			hash = hash*31 + waitIID;
			hash = hash*31 + vc.hashCode();
		}
		else{
			hash = hash*31 + lockSet.hashCode();
			hash = hash*31 + notifySet.hashCode();
			hash = hash*31 + vc.hashCode();
		}
		return hash;
	}
}

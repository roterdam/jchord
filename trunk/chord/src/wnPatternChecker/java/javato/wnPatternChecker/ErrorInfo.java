package javato.wnPatternChecker;

import java.util.HashSet;
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

public class ErrorInfo {
	List<Integer> wrIIDs;
	List<Integer> rdIIDs;
	
	boolean isProperLockHeld;
	
	public ErrorInfo(List<Integer> wrIIDsList, List<Integer> rdIIDsList, boolean isProperLockHeld){
		assert (wrIIDsList != null);
		assert (rdIIDsList != null);
		wrIIDs = wrIIDsList;
		rdIIDs = rdIIDsList;
		this.isProperLockHeld = isProperLockHeld;
	}
	
	public boolean equals(Object other){
		//System.out.println("in equals");
		if(!(other instanceof ErrorInfo)){
			return false;
		}
		ErrorInfo otherErrInfo = (ErrorInfo)other;
		Set<Integer> wrIIDsSet = new HashSet<Integer>(wrIIDs);
		Set<Integer> otherWrIIDsSet = new HashSet<Integer>(otherErrInfo.wrIIDs);
		Set<Integer> rdIIDsSet = new HashSet<Integer>(rdIIDs);
		Set<Integer> otherRdIIDsSet = new HashSet<Integer>(otherErrInfo.rdIIDs);
		
		if(wrIIDsSet.equals(otherWrIIDsSet) && rdIIDsSet.equals(otherRdIIDsSet) && 
				(isProperLockHeld == otherErrInfo.isProperLockHeld)){
			return true;
		}
		
		return false;
	}
	
	public int hashCode(){
		int hashCode = 1;
		Set<Integer> wrIIDsSet = new HashSet<Integer>(wrIIDs);
		hashCode = hashCode*31 + wrIIDsSet.hashCode();
		Set<Integer> rdIIDsSet = new HashSet<Integer>(rdIIDs);
		hashCode = hashCode*31 + rdIIDsSet.hashCode();
		if(isProperLockHeld){
			hashCode = hashCode*2;
		}
		else{
			hashCode = hashCode*2 + 1;
		}
		return hashCode;
	}
	
}

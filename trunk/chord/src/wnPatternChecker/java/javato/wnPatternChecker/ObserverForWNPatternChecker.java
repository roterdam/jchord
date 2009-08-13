package javato.wnPatternChecker;

import javato.observer.Observer;

import java.util.Iterator;
import java.util.Stack;
import javato.wnPatternChecker.Pair;

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
public class ObserverForWNPatternChecker extends Observer {
	
	private static WNPatternCheckerEventHandler wnEh = new WNPatternCheckerEventHandler();
    
	private static ThreadLocal lStack = new ThreadLocal(){
		protected Object initialValue() {
            return new Stack<Pair<Integer, Integer>>();
        }
	};
	
	public static void myLock(int iid, Object l){
		Stack<Pair<Integer, Integer>> ls = (Stack<Pair<Integer, Integer>>)(lStack.get());
		int lid = uniqueId(l);
		boolean addToStack = true;
		
		if(!ls.isEmpty()){
			Iterator lsItr = ls.iterator();
			while(lsItr.hasNext()){
				Pair<Integer, Integer> lsElem = (Pair<Integer, Integer>)lsItr.next();
				if(lsElem.fst == lid){
					lsElem.snd += 1;
					addToStack = false;
					break;
				}
			}
		}
		
		if(addToStack){
			Pair<Integer, Integer> lsElem = new Pair<Integer, Integer>(lid, 1);
			ls.push(lsElem);
			wnEh.Lock(iid, uniqueId(Thread.currentThread()), lid);
		}
	}
	
	public static void myUnlock(int iid, Object l){
		Stack<Pair<Integer, Integer>> ls = (Stack<Pair<Integer, Integer>>)(lStack.get());
		int lid = uniqueId(l);
		
		if(!ls.isEmpty()){
			Iterator lsItr = ls.iterator();
			while(lsItr.hasNext()){
				Pair<Integer, Integer> lsElem = (Pair<Integer, Integer>)lsItr.next();
				if(lsElem.fst == lid){
					lsElem.snd -= 1;
					break;
				}
			}
		}
		
		Pair<Integer, Integer> topElem = (Pair<Integer, Integer>)ls.peek();
		if(topElem.snd == 0){
			assert(topElem.fst == lid);
			ls.pop();
			wnEh.Unlock(iid, uniqueId(Thread.currentThread()), lid);
		}
	}
	
	public static void myReadAfter(int iid, Object o, int fld){
		wnEh.Read(iid, uniqueId(Thread.currentThread()), id(o, fld));
	}
	
	public static void myReadAfter(int iid, int clss, int fld){
		wnEh.Read(iid, uniqueId(Thread.currentThread()), id(clss, fld));
	}
	
	public static void myWriteAfter(int iid, Object o, int fld){
		wnEh.Write(iid, uniqueId(Thread.currentThread()), id(o, fld));
	}
	
	public static void myWriteAfter(int iid, int clss, int fld){
		wnEh.Write(iid, uniqueId(Thread.currentThread()), id(clss, fld));
	}
	
	public static void myWaitBefore(int iid, Object l){
		wnEh.Wait(iid, uniqueId(Thread.currentThread()), uniqueId(l));
	}
	
	public static void myNotifyAfter(int iid, Object l){
		wnEh.Notify(iid, uniqueId(Thread.currentThread()), uniqueId(l));
	}
	
	public static void myNotifyAllAfter(int iid, Object l){
		wnEh.Notify(iid, uniqueId(Thread.currentThread()), uniqueId(l));
	}
	
	public static void myStartBefore(int iid, Object t){
		wnEh.Start(iid, uniqueId(Thread.currentThread()), uniqueId(t));
	}
	
	public static void myJoinAfter(int iid, Object t){
		wnEh.Join(iid, uniqueId(Thread.currentThread()), uniqueId(t));
	}
}

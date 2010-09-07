package waitnotify;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntStack;
import java.util.Set;
import java.util.HashSet;

public class LockStack {
	TIntStack lStack = new TIntStack();
	TIntIntHashMap lAcqCounts = new TIntIntHashMap();
	//Set<Integer> locksToConsider = null;
	int lastLockPopped = -1;

	//public void setLocksToConsider(Set<Integer> s){
		//locksToConsider = new HashSet<Integer>(s);
	//}
	
	public boolean lock(int lId){
		int nAcq = lAcqCounts.adjustOrPutValue(lId, 1, 1);
		lStack.push(lId);
		//if(!locksToConsider.contains(lId)){
			//return false;		
		//}
		boolean rval = (nAcq == 1)? true : false;
		return rval;
	}
	
	public boolean unLock(){
		int lId = lStack.peek();
		lAcqCounts.adjustValue(lId, -1);
		lStack.pop();
		lastLockPopped = lId;
		if(lAcqCounts.get(lId) == 0){
			lAcqCounts.remove(lId);
			//if(locksToConsider.contains(lId))
				return true;
		}
		return false;
	}
	
	public boolean isEmpty(){
		return (lStack.size() == 0);
	}
}

package javato.wnPatternChecker;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.DynamicAnalysis;
import chord.project.Runtime;

import chord.util.tuple.integer.IntPair;

@Chord(
	name = "wn-java"
)
public class WNPatternAnalysis extends DynamicAnalysis {
	private static ThreadLocal<Stack<IntPair>> lStack =
			new ThreadLocal<Stack<IntPair>>(){
		protected Stack<IntPair> initialValue() {
            return new Stack<IntPair>();
        }
	};
	private Database db = new Database();
	private ThreadBase tb = new ThreadBase();
	public static ThreadLocal<Stack<LockStackElemInfo>> lockStack =
			new ThreadLocal<Stack<LockStackElemInfo>>() {
        protected Stack<LockStackElemInfo> initialValue() {
            return new Stack<LockStackElemInfo>();
        }
    };
    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();

    	instrScheme.setGetstaticPrimitiveEvent(true, true, true);
    	instrScheme.setPutstaticPrimitiveEvent(true, true, true);
    	instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setAloadPrimitiveEvent(true, true, true, true);
    	instrScheme.setAstorePrimitiveEvent(true, true, true, true);

    	instrScheme.setGetstaticReferenceEvent(true, true, false, true);
    	instrScheme.setPutstaticReferenceEvent(true, true, false, true);
    	instrScheme.setGetfieldReferenceEvent(true, true, false, false, true);
    	instrScheme.setPutfieldReferenceEvent(true, true, false, false, true);
    	instrScheme.setAloadReferenceEvent(true, true, false, false, true);
    	instrScheme.setAstoreReferenceEvent(true, true, false, false, true);

    	instrScheme.setThreadStartEvent(true, true, true);
    	instrScheme.setThreadJoinEvent(true, true, true);
    	instrScheme.setAcquireLockEvent(true, true, true);
    	instrScheme.setReleaseLockEvent(true, true, true);
    	instrScheme.setWaitEvent(true, true, true);
    	instrScheme.setNotifyEvent(true, true, true);
    	return instrScheme;
    }
	public void initPass() {
		// do nothing for now
	}
	public void done() {
        db.checkForErrors();
	}

	public void processGetstaticPrimitive(int eId, int tId, int fId) { 
		processHeapRd(eId, tId, Runtime.getPrimitiveId(1, fId));
	}
	public void processPutstaticPrimitive(int eId, int tId, int fId) {
		processHeapWr(eId, tId, Runtime.getPrimitiveId(1, fId));
	}
	public void processGetfieldPrimitive(int eId, int tId, int bId, int fId) {
		processHeapRd(eId, tId, Runtime.getPrimitiveId(bId, fId));
	}
	public void processPutfieldPrimitive(int eId, int tId, int bId, int fId) {
		processHeapWr(eId, tId, Runtime.getPrimitiveId(bId, fId));
	}
	public void processAloadPrimitive(int eId, int tId, int bId, int iId) {
		processHeapRd(eId, tId, Runtime.getPrimitiveId(bId, iId));
	}
	public void processAstorePrimitive(int eId, int tId, int bId, int iId) {
		processHeapWr(eId, tId, Runtime.getPrimitiveId(bId, iId));
	}

	public void processGetstaticReference(int eId, int tId, int fId, int oId) { 
		processHeapRd(eId, tId, oId);
	}
	public void processPutstaticReference(int eId, int tId, int fIdx, int oId) {
		processHeapWr(eId, tId, oId);
	}
	public void processGetfieldReference(int eId, int tId, int bId, int fId, int oId) { 
		processHeapRd(eId, tId, oId);
	}
	public void processPutfieldReference(int eId, int tId, int bId, int fId, int oId) { 
		processHeapWr(eId, tId, oId);
	}
	public void processAloadReference(int eId, int tId, int bId, int iId, int oId) {
		processHeapRd(eId, tId, oId);
	}
	public void processAstoreReference(int eId, int tId, int bId, int iId, int oId) {
		processHeapWr(eId, tId, oId);
	}

	public void processAcquireLock(int pId, int tId, int lId) {
		Stack<IntPair> ls = lStack.get();
		boolean addToStack = true;
		if(!ls.isEmpty()){
			Iterator<IntPair> lsItr = ls.iterator();
			while(lsItr.hasNext()){
				IntPair lsElem = lsItr.next();
				if(lsElem.idx0 == lId){
					lsElem.idx1 += 1;
					addToStack = false;
					break;
				}
			}
		}
		if(addToStack){
			IntPair lsElem = new IntPair(lId, 1);
			ls.push(lsElem);
			Stack<LockStackElemInfo> ls2 = lockStack.get();
			LockStackElemInfo lsElemInfo = new LockStackElemInfo(pId, lId);
			ls2.push(lsElemInfo);
		}
	}
	public void processReleaseLock(int pId, int tId, int lId) {
		Stack<IntPair> ls = (Stack<IntPair>)(lStack.get());
		if(!ls.isEmpty()){
			Iterator<IntPair> lsItr = ls.iterator();
			while(lsItr.hasNext()){
				IntPair lsElem = lsItr.next();
				if(lsElem.idx0 == lId){
					lsElem.idx1 -= 1;
					break;
				}
			}
		}
		IntPair topElem = ls.peek();
		if(topElem.idx1 == 0){
			assert(topElem.idx0 == lId);
			ls.pop();
			Stack<LockStackElemInfo> ls2 = (Stack<LockStackElemInfo>)(lockStack.get());
			if(ls2.size() == 1){
				List<WrListElemInfo> wrList = ls2.peek().wrList;
				for(WrListElemInfo wrListElem : wrList){
					wrListElem.lockSet.add(lId);
					db.addToDatabase(wrListElem.iids, wrListElem.m, tId, wrListElem.lockSet, wrListElem.notifySet, tb.getVC(tId));
				}
				ls2.pop();
			}
			else{
				LockStackElemInfo topLsElem = ls2.peek();
				LockStackElemInfo topButOneLsElem = ls2.elementAt(ls2.size() - 2);
				topButOneLsElem.updateRdList(topLsElem.rdList);
				for(WrListElemInfo wrElem : topLsElem.wrList){
					wrElem.addToLockSet(topLsElem.l);
				}
				topButOneLsElem.updateWrList(topLsElem.wrList);
				ls2.pop();
			}
		}
	}
	private void processHeapRd(int eId, int tId, long oId) {
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		RdListElemInfo rdElem = new RdListElemInfo(eId, oId);
		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToRdList(rdElem);
		}
	}
	private void processHeapWr(int pId, int tId, long oId) {
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		WrListElemInfo wrElem = new WrListElemInfo(pId, oId);
		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToWrList(wrElem);
		}
		else {
			List<Integer> iids = new LinkedList<Integer>();
			iids.add(new Integer(pId));
			db.addToDatabase(iids, oId, tId, new HashSet<Integer>(), new HashSet<Integer>(), tb.getVC(tId));
		}
	}
	public void processThreadStart(int pId, int tId, int oId) { 
		tb.Start(tId, oId);
	}
	public void processThreadJoin(int pId, int tId, int oId) {
		tb.Join(tId, oId);
	}
	public void processWait(int pId, int tId, int lId) { 
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		int indxL = getIndexForL(ls, lId);
		
		List<RdListElemInfo> rdListForL = new LinkedList<RdListElemInfo>();
		for(int i = indxL; i < ls.size(); i++){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(i);
			rdListForL = unionOfRdLists(rdListForL, lsElem.rdList);
		}
		for(RdListElemInfo rdElem : rdListForL){
			db.addToDatabase(rdElem.iids, rdElem.m, tId, lId, tb.getVC(tId));
		}
	}
	public void processNotify(int pId, int tId, int lId) { 
		Stack<LockStackElemInfo> ls = (Stack<LockStackElemInfo>)(lockStack.get());
		int indxL = getIndexForL(ls, lId);
		List<WrListElemInfo> wrListForL = new LinkedList<WrListElemInfo>();
		for(int i = indxL; i < ls.size(); i++){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(i);
			wrListForL = unionOfWrLists(wrListForL, lsElem.wrList);
		}
		for(WrListElemInfo wrElem : wrListForL){
			wrElem.addToNotifySet(lId);
		}
	}
	private static List<RdListElemInfo> unionOfRdLists(List<RdListElemInfo> rdList1, List<RdListElemInfo> rdList2){
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
	private static List<WrListElemInfo> unionOfWrLists(List<WrListElemInfo> wrList1, List<WrListElemInfo> wrList2){
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
	private static int getIndexForL(Stack<LockStackElemInfo> ls, int l){
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

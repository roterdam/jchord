package chord.waitnotify;

import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.TreeMap;

import chord.doms.DomM;
import chord.project.ChordRuntimeException;
import chord.project.Properties;
import chord.project.Utils;
import chord.instr.InstrScheme;
import chord.project.Chord;
import chord.project.Program;
import chord.project.Project;
import chord.project.ProgramDom;
import chord.project.DynamicAnalysis;
import chord.project.Runtime;

import chord.util.tuple.integer.IntPair;

@Chord(
	name = "wn-java"
)
public class WNPatternAnalysis extends DynamicAnalysis {
	private static Map<Integer, Stack<IntPair>> lStacks = new TreeMap<Integer, Stack<IntPair>>();
	private Database db = new Database();
	private ThreadBase tb = new ThreadBase();
	public static Map<Integer, Stack<LockStackElemInfo>> lockStacks = new TreeMap<Integer, Stack<LockStackElemInfo>>();

    protected InstrScheme instrScheme;
    public InstrScheme getInstrScheme() {
    	if (instrScheme != null)
    		return instrScheme;
    	instrScheme = new InstrScheme();
		instrScheme.setConvert();

    	instrScheme.setGetstaticPrimitiveEvent(true, true, true);
    	instrScheme.setPutstaticPrimitiveEvent(true, true, true);
    	instrScheme.setGetfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setPutfieldPrimitiveEvent(true, true, true, true);
    	instrScheme.setAloadPrimitiveEvent(true, true, true, true);
    	instrScheme.setAstorePrimitiveEvent(true, true, true, true);

    	instrScheme.setGetstaticReferenceEvent(true, true, true, false);
    	instrScheme.setPutstaticReferenceEvent(true, true, true, false);
    	instrScheme.setGetfieldReferenceEvent(true, true, true, true, false);
    	instrScheme.setPutfieldReferenceEvent(true, true, true, true, false);
    	instrScheme.setAloadReferenceEvent(true, true, true, true, false);
    	instrScheme.setAstoreReferenceEvent(true, true, true, true, false);

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
	public void donePass() {
		System.out.println("in done...going to call checkForErrors");
        db.checkForErrors(instrumentor.getEmap(), instrumentor.getImap());
	}
	public void doneAllPasses() {
		try {
			PrintWriter writer = new PrintWriter(new File(
				Properties.outDirName, "waitNotifyErrorList.xml"));
			writer.println("<waitNotifyErrorList>");
			for (ErrorInfo info : db.errors) {
				List<Integer> e1idList = info.rdIIDs;
				List<Integer> e2idList = info.wrIIDs;
				String e1idListStr = "";
				int n1 = e1idList.size();
				for (int i = 0; i < n1; i++) {
					Integer e = e1idList.get(i);
					e1idListStr += "E" + e;
					if (i < n1 - 1)
						e1idListStr += " ";
				}
				String e2idListStr = "";
				int n2 = e2idList.size();
				for (int i = 0; i < n2; i++) {
					Integer e = e2idList.get(i);
					e2idListStr += "E" + e;
					if (i < n2 - 1)
						e2idListStr += " ";
				}
				String kind = info.isProperLockHeld ? "Missing Notify" : "Missing Lock";
				writer.println("<waitNotifyError e1idList=\"" + e1idListStr +
					"\" e2idList=\"" + e2idListStr + "\" kind=\"" + kind + "\"/>");
			}
			writer.println("</waitNotifyErrorList>");
			writer.close();
		} catch (IOException ex) {
			throw new ChordRuntimeException(ex);
		}
		DomM domM = (DomM) Project.getTrgt("M");
		Project.runTask(domM);
		domM.saveToXMLFile();
        Utils.copyFile("src/main/web/Mlist.dtd");

		instrumentor.getDomE().saveToXMLFile();
        Utils.copyFile("src/main/web/Elist.dtd");

        Utils.copyFile("src/waitnotify/web/results.dtd");
        Utils.copyFile("src/waitnotify/web/results.xml");
        Utils.copyFile("src/waitnotify/web/results.xsl");
        Utils.copyFile("src/main/web/style.css");
        Utils.copyFile("src/main/web/misc.xsl");

        Utils.runSaxon("results.xml", "results.xsl");
		Program.v().HTMLizeJavaSrcFiles();
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
		processHeapRd(eId, tId, Runtime.getPrimitiveId(1, fId));
	}
	public void processPutstaticReference(int eId, int tId, int fId, int oId) {
		processHeapWr(eId, tId, Runtime.getPrimitiveId(1, fId));
	}
	public void processGetfieldReference(int eId, int tId, int bId, int fId, int oId) { 
		processHeapRd(eId, tId, Runtime.getPrimitiveId(bId, fId));
	}
	public void processPutfieldReference(int eId, int tId, int bId, int fId, int oId) { 
		processHeapWr(eId, tId, Runtime.getPrimitiveId(bId, fId));
	}
	public void processAloadReference(int eId, int tId, int bId, int iId, int oId) {
		processHeapRd(eId, tId, Runtime.getPrimitiveId(bId, iId));
	}
	public void processAstoreReference(int eId, int tId, int bId, int iId, int oId) {
		processHeapWr(eId, tId, Runtime.getPrimitiveId(bId, iId));
	}

	public void processAcquireLock(int pId, int tId, int lId) {
		//System.out.println("in lock acquire...tId is "+tId+" lId is "+lId);
		Stack<IntPair> ls = getLStack(tId);
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
			Stack<LockStackElemInfo> ls2 = getLockStack(tId);
			LockStackElemInfo lsElemInfo = new LockStackElemInfo(pId, lId);
			ls2.push(lsElemInfo);
		}
	}

	public static Stack<IntPair> getLStack(int tId){
		Stack<IntPair> ls = lStacks.get(tId);
		if(ls == null){
			ls = new Stack<IntPair>();
			lStacks.put(tId, ls);
		}
		return ls;
	}	

	public static Stack<LockStackElemInfo> getLockStack(int tId){
		Stack<LockStackElemInfo> ls = lockStacks.get(tId);
		if(ls == null){
			ls = new Stack<LockStackElemInfo>();
			lockStacks.put(tId, ls);
		}
		return ls;
	}	
	public void processReleaseLock(int pId, int tId, int lId) {
		//System.out.println("in lock release...tId is "+tId+" lId is "+lId);
		Stack<IntPair> ls = getLStack(tId);
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
			Stack<LockStackElemInfo> ls2 = getLockStack(tId);
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
		//System.out.println("Rd: " + eId + " " + tId + " " + oId);
		Stack<LockStackElemInfo> ls = getLockStack(tId);
		RdListElemInfo rdElem = new RdListElemInfo(eId, oId);
		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToRdList(rdElem);
		}
	}
	private void processHeapWr(int eId, int tId, long oId) {
		//System.out.println("Wr: " + eId + " " + tId + " " + oId);
		Stack<LockStackElemInfo> ls = getLockStack(tId);
		WrListElemInfo wrElem = new WrListElemInfo(eId, oId);
		if(!ls.isEmpty()){
			LockStackElemInfo topLsElem = ls.peek();
			topLsElem.addToWrList(wrElem);
		}
		else {
			List<Integer> iids = new LinkedList<Integer>();
			iids.add(new Integer(eId));
			db.addToDatabase(iids, oId, tId, new HashSet<Integer>(), new HashSet<Integer>(), tb.getVC(tId));
		}
	}
	public void processThreadStart(int pId, int tId, int oId) { 
		//System.out.println("in thread start...tId is "+tId+" oId is "+oId);
		tb.Start(tId, oId);
	}
	public void processThreadJoin(int pId, int tId, int oId) {
		//System.out.println("in thread join...tId is "+tId+" oId is "+oId);
		tb.Join(tId, oId);
	}
	public void processWait(int pId, int tId, int lId) { 
		//System.out.println("in wait...tId is "+tId+" lId is "+lId);
		Stack<LockStackElemInfo> ls = getLockStack(tId);
		int indxL = getIndexForL(ls, lId);
		
		List<RdListElemInfo> rdListForL = new LinkedList<RdListElemInfo>();
		for(int i = indxL; i < ls.size(); i++){
			LockStackElemInfo lsElem = (LockStackElemInfo)ls.elementAt(i);
			rdListForL = unionOfRdLists(rdListForL, lsElem.rdList);
		}
		for(RdListElemInfo rdElem : rdListForL){
			db.addToDatabase(rdElem.iids, rdElem.m, tId, lId, pId, tb.getVC(tId));
		}
	}
	public void processNotify(int pId, int tId, int lId) { 
		//System.out.println("in notify...tId is "+tId+" lId is "+lId);
		Stack<LockStackElemInfo> ls = getLockStack(tId);
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
package chord.analyses.typestate;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.alias.CIObj;
import chord.analyses.alias.CIPAAnalysis;
import chord.util.ArraySet;
import chord.util.graph.MutableLabeledGraph;
import chord.util.tuple.object.Pair;

public class Helper {
    public static boolean hasAnyGlobalAccessPath(AbstractState ab) {
        for (AccessPath ap : ab.ms) {
            if (ap instanceof GlobalAccessPath)
                return true;
        }
        return false;
    }

    public static int getIndexInAP(ArraySet<AccessPath> ms, Register r) {
        for (AccessPath ap : ms) {
            if (ap instanceof RegisterAccessPath && ((RegisterAccessPath) ap).var == r && ap.fields.isEmpty())
                return ms.indexOf(ap);
        }
        return -1;
    }

    public static int getPrefixIndexInAP(ArraySet<AccessPath> ms, Register r, int minIndex) {
        int currIndex = 0;
        for (AccessPath ap : ms) {
            if (currIndex > minIndex && ap instanceof RegisterAccessPath && ((RegisterAccessPath) ap).var == r)
                return currIndex;
            currIndex++;
        }
        return -1;
    }

    public static int getPrefixIndexInAP(ArraySet<AccessPath> ms, jq_Field g, int minIndex) {
        int currIndex = 0;
        for (AccessPath ap : ms) {
            if (currIndex > minIndex && ap instanceof GlobalAccessPath && ((GlobalAccessPath) ap).global == g)
                return currIndex;
            currIndex++;
        }
        return -1;
    }

    public static int getPrefixIndexInAP(ArraySet<AccessPath> ms, Register r, jq_Field f, int minIndex) {
        int currIndex = 0;
        for (AccessPath ap : ms) {
            if (currIndex > minIndex && ap instanceof RegisterAccessPath) {
                RegisterAccessPath rap = (RegisterAccessPath) ap;
                if (rap.var == r && !rap.fields.isEmpty() && rap.fields.get(0) == f)
                    return currIndex;
            }
            currIndex++;
        }
        return -1;
    }

    public static int getPrefixIndexInAP(ArraySet<AccessPath> ms, jq_Field g, jq_Field f, int minIndex) {
        int currIndex = 0;
        for (AccessPath ap : ms) {
            if (currIndex > minIndex && ap instanceof GlobalAccessPath) {
                GlobalAccessPath gap = (GlobalAccessPath) ap;
                if (gap.global == g && !gap.fields.isEmpty() && gap.fields.get(0) == f)
                    return currIndex;
            }
            currIndex++;
        }
        return -1;
    }
    
    /**
     * does there exist e, e' such that access path ap of the form e.f.e' and may-alias(e,r) holds?
     */
    public static boolean mayPointsTo(AccessPath ap, Register r, jq_Field f, CIPAAnalysis cipa) {
        if (!ap.fields.contains(f))  // quick test
            return false;
        Set<Quad> pts1;
        if (ap instanceof GlobalAccessPath)
            pts1 = pointsTo(((GlobalAccessPath) ap).global, cipa);
        else
            pts1 = pointsTo(((RegisterAccessPath) ap).var,cipa);
        for (jq_Field f2 : ap.fields) {
            if (f2 == f) break;
            pts1 = pointsTo(pts1, f2, cipa);
        }
        Set<Quad> pts2 = pointsTo(r,cipa);
        for (Quad q : pts2) {
            if (pts1.contains(q))
                return true;
        }
        return false;
    }

    public static boolean mayPointsTo(Register var, Quad alloc, CIPAAnalysis cipa) {
        //CIObj obj = cipa.pointsTo(var);
        //return obj.pts.contains(alloc);
        return pointsTo(var, alloc, cipa);
    }
    
    private static Map<Register, Set<Quad>> VpointsToMap = new HashMap<Register, Set<Quad>>();
    private static Map<jq_Field, Set<Quad>> FpointsToMap = new HashMap<jq_Field, Set<Quad>>();
    private static Map<Pair<Quad, jq_Field>, Set<Quad>> HFHMap = new HashMap<Pair<Quad, jq_Field>, Set<Quad>>();

    private static Set<Quad> pointsTo(Set<Quad> quads, jq_Field f, CIPAAnalysis cipa) {
        Set<Quad> retQuads = new HashSet<Quad>();
        Set<Quad> temp = new HashSet<Quad>(1);
        for(Quad q : quads){
            Pair<Quad,jq_Field> p = new Pair<Quad,jq_Field>(q,f);
            Set<Quad> pts = HFHMap.get(p);
            if (pts == null) {
                temp.clear();
                temp.add(q);
                CIObj obj = new CIObj(temp);
                pts = cipa.pointsTo(obj,f).pts;
                HFHMap.put(p, pts);
            }
            retQuads.addAll(pts);    
        }
        return retQuads;
    }
    
    private static boolean pointsTo(Register v, Quad q, CIPAAnalysis cipa) {
        Set<Quad> pts = VpointsToMap.get(v);
        if (pts == null) {
            pts = cipa.pointsTo(v).pts;
            VpointsToMap.put(v, pts);
        }
        return pts.contains(q);
    }
    
    private static Set<Quad> pointsTo(Register v, CIPAAnalysis cipa) {
        Set<Quad> pts = VpointsToMap.get(v);
        if (pts == null) {
            pts = cipa.pointsTo(v).pts;
            VpointsToMap.put(v, pts);
        }
        return pts;
    }
    
    private static Set<Quad> pointsTo(jq_Field f, CIPAAnalysis cipa) {
        Set<Quad> pts = FpointsToMap.get(f);
        if (pts == null) {
            pts = cipa.pointsTo(f).pts;
            FpointsToMap.put(f, pts);
        }
        return pts;
    }
	
    private static boolean traverseGraphAndCheck(MutableLabeledGraph<Object, Object> graphedHeap, ArraySet<AccessPath> ms, 
    		Object q, List accessPath, Set<List<jq_Field>> pathSuffixes){
    	Set<Object> preds = graphedHeap.getPreds(q);
    	
    	if(preds.isEmpty() || preds == null){ //Reached the root node 
    		Object root = accessPath.remove(0); 
    		if(root instanceof jq_Field){
    			for(List<jq_Field> suffix : pathSuffixes){
	    			List<jq_Field> fields = new ArrayList<jq_Field>((List<jq_Field>)accessPath);
	    			fields.addAll(suffix);
	    			GlobalAccessPath gAP = new GlobalAccessPath((jq_Field)root, fields);
	    			if(!ms.contains(gAP))
	    				return true;
    			}
    		}else{ //RegisterAccessPath

    			for(List<jq_Field> suffix : pathSuffixes){
    				List<jq_Field> fields = new ArrayList<jq_Field>((List<jq_Field>)accessPath);
	    			fields.addAll(suffix);
	    			RegisterAccessPath rAP = new RegisterAccessPath((Register)root, fields);
    				if(!ms.contains(rAP))
    					return true;
    			}
    		}
    		return false;
    	}else{
    		for(Object pred: preds){
    			List modAccessPath = new ArrayList(accessPath);
    			Set<Object> fields = graphedHeap.getLabels(pred, q);
    			for(Object f : fields) //Should always loop just once
    				modAccessPath.add(0, f);
    			
    			if(traverseGraphAndCheck(graphedHeap, ms, pred, modAccessPath, pathSuffixes))
    				return true;	
    		}
    		return false;
    	}
    	
    }
    
	public static boolean isAliasMissing(ArraySet<AccessPath> ms, Register v, jq_Field f, CIPAAnalysis cipa){
		Set<List<jq_Field>> pathSuffixes = new ArraySet<List<jq_Field>>();
		for (AccessPath ap : ms) {
			if(ap instanceof RegisterAccessPath)
				if(((RegisterAccessPath)ap).var.equals(v) && ap.fields.get(0).equals(f))
					pathSuffixes.add(ap.fields.subList(1, ap.fields.size()));
        }
		MutableLabeledGraph<Object, Object> graphedHeap = cipa.getGraphedHeap();
		Set<Quad> trackedAllocs = pointsTo(v, cipa);
		for(Quad q : trackedAllocs){
			if(traverseGraphAndCheck(graphedHeap, ms, q, new ArrayList(), pathSuffixes))
				return true;
		}
		return false;
	}

	public static boolean doesAliasExist(Register v, CIPAAnalysis cipa){
		Set<Quad> trackedAllocs = pointsTo(v, cipa);
		for(Quad q : trackedAllocs){
			if(cipa.doesAliasExist(q))
				return true;
		}
		return false;
	}
    
    public static ArraySet<AccessPath> removeReference(ArraySet<AccessPath> oldMS, Register r) {
        ArraySet<AccessPath> newMS = null;
        int i = -1;
        while ((i = getPrefixIndexInAP(oldMS, r, i)) >= 0) {
            if (newMS == null) newMS = new ArraySet<AccessPath>(oldMS);
            newMS.remove(oldMS.get(i));
        }
        return newMS;
    }

    public static ArraySet<AccessPath> removeReference(ArraySet<AccessPath> oldMS, jq_Field g) {
        ArraySet<AccessPath> newMS = null;
        int i = -1;
        while ((i = getPrefixIndexInAP(oldMS, g, i)) >= 0) {
            if (newMS == null) newMS = new ArraySet<AccessPath>(oldMS);
            newMS.remove(oldMS.get(i));
        }
        return newMS;
    }

    public static void addAllGlobalAccessPath(ArraySet<AccessPath> newMS, ArraySet<AccessPath> oldMS) {
        for (AccessPath ap : oldMS) {
            if (ap instanceof GlobalAccessPath)
                newMS.add(ap);
        }
    }

    public static void addAllLocalAccessPath(ArraySet<AccessPath> newMS, ArraySet<AccessPath> oldMS) {
        for (AccessPath ap : oldMS) {
            if (ap instanceof RegisterAccessPath)
                newMS.add(ap);
        }
    }
    
    public static void removeAllGlobalAccessPaths(ArraySet<AccessPath> MS) {
        for (Iterator<AccessPath> i = MS.iterator(); i.hasNext();) {
            AccessPath ap = i.next();
            if (ap instanceof GlobalAccessPath)
                i.remove();
        }
    }
 
    public static boolean removeModifiableAccessPaths(Set<jq_Field> modFields, ArraySet<AccessPath> MS) {
        boolean modified = false;
    	if (modFields == null)
            return modified;
        
        for (Iterator<AccessPath> i = MS.iterator(); i.hasNext();) {
            AccessPath ap = i.next();
            boolean mod = false;
            for (jq_Field f : ap.fields) {
                if (modFields.contains(f)) {
                    mod = true;
                    break;
                }
            }
            if (mod){
                i.remove();
                modified = true;
            }
        }
        
        return modified;
    }
    
    public static void removeAllExceptLocalVariables(ArraySet<AccessPath> MS){
        removeAllGlobalAccessPaths(MS);
        for (Iterator<AccessPath> i = MS.iterator(); i.hasNext();) {
            AccessPath ap = i.next();
            if (!(ap.fields == Collections.EMPTY_LIST))
                i.remove();
        }
        
    }
}

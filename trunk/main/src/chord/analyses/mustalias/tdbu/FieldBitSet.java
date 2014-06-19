package chord.analyses.mustalias.tdbu;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import joeq.Class.jq_Field;
import chord.analyses.field.DomF;

public class FieldBitSet extends BitSet implements Set<jq_Field> {
public static DomF domF;
	public FieldBitSet(){
		super(domF.size());
	}
	public FieldBitSet(Set<jq_Field> notInFSet) {
		this();
		if(notInFSet instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)notInFSet;;
			this.addAll(fbs);
			return;
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		if(!(o instanceof jq_Field))
			return false;
		jq_Field f = (jq_Field)o;
		int fIndx = domF.indexOf(f);
		return this.get(fIndx);
	}

	@Override
	public Iterator<jq_Field> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(jq_Field e) {
		int fIndx = domF.indexOf(e);
		if(this.get(fIndx))
			return false;
		this.set(fIndx);
		return true;
	}

	@Override
	public boolean remove(Object o) {
		if(!(o instanceof jq_Field))
			return false;
		jq_Field f = (jq_Field)o;
		int fIndx = domF.indexOf(f);
		if(this.get(fIndx)){
			this.set(fIndx);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)c;
			FieldBitSet cCopy = (FieldBitSet)fbs.clone();
			cCopy.andNot(this);
			if(cCopy.isEmpty())
				return true;
			return false;
		}
		throw new UnsupportedOperationException();
	}

	
	
	public boolean intersects(FieldBitSet set) {
		FieldBitSet cCopy = (FieldBitSet)set.clone();
		cCopy.and(this);
		if(cCopy.isEmpty())
			return false;
		return true;
	}
	@Override
	public boolean addAll(Collection<? extends jq_Field> c) {
		if(c instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)c;;
			FieldBitSet thisCopy = (FieldBitSet)this.clone();
			this.or(fbs);
			return !this.equals(thisCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)c;;
			FieldBitSet thisCopy = (FieldBitSet)this.clone();
			this.and(fbs);
			return !this.equals(thisCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if(c instanceof FieldBitSet){
			FieldBitSet fbs = (FieldBitSet)c;;
			FieldBitSet thisCopy = (FieldBitSet)this.clone();
			this.andNot(fbs);
			return !this.equals(thisCopy);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		StringBuffer sv = new StringBuffer();
		sv.append("FieldBitSet [");
		int startIndex = super.nextSetBit(0);
		while(startIndex >= 0){
			sv.append(domF.get(startIndex)+",");
			startIndex = super.nextSetBit(startIndex+1);
		}
		sv.append("]");
		return sv.toString();
	}
	@Override
	public int size() {
		return this.cardinality();
	}

	
	
}

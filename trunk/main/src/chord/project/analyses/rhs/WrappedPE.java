package chord.project.analyses.rhs;

import chord.util.Utils;
import joeq.Compiler.Quad.Inst;

public class WrappedPE<PE extends IEdge, SE extends IEdge> implements IWrappedPE<PE, SE> {
    public final Inst i;
    public final PE pe;
    public final WrappedPE<PE, SE> wpe;
    public final WrappedSE<PE, SE> wse;
    public WrappedPE(Inst i, PE pe, WrappedPE<PE, SE> wpe, WrappedSE<PE, SE> wse) {
        this.i = i;
        this.pe = pe;
        this.wpe = wpe;
        this.wse = wse;
    }
    
	@Override
	public Inst getInst() { return i; }

	@Override
    public PE getPE() { return pe; }

	@Override
    public IWrappedPE<PE, SE> getWPE() { return wpe; }

	@Override
    public IWrappedSE<PE, SE> getWSE() { return wse; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((i == null) ? 0 : i.hashCode());
        result = prime * result + ((pe == null) ? 0 : pe.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
		if (!(obj instanceof WrappedPE)) return false;
        WrappedPE that = (WrappedPE) obj;
		return this.i == that.i && Utils.areEqual(this.pe, that.pe);
    }

    @Override
    public String toString() {
        return "WrappedEdge [Inst=" + i + ", PE="+pe+"]";
    }
}

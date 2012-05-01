package chord.project.analyses.rhs;

import chord.util.Utils;

public class WrappedSE<PE extends IEdge, SE extends IEdge> implements IWrappedSE<PE, SE> {
    public final SE se;
    public final IWrappedPE<PE, SE> wpe;

    public WrappedSE(SE se, IWrappedPE<PE, SE> pe) {
        this.se = se;
        this.wpe = pe;
    }

    @Override
    public SE getSE() { return se; }

    @Override
    public IWrappedPE<PE, SE> getWPE() { return wpe; }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((se == null) ? 0 : se.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WrappedSE)) return false;
        WrappedSE that = (WrappedSE) obj;
		return Utils.areEqual(this.se, that.se);
    }
    @Override
    public String toString() {
        return "WrappedSE [se=" + se + "]";
    }
}

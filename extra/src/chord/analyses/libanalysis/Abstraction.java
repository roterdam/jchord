package chord.analyses.libanalysis;

public abstract class Abstraction implements Comparable<Abstraction>{
	private int level;
	private int minLevel;
	private int maxLevel;
	protected int mod = 0;
	
	public void setLevel(int level) {
		this.level = level;
		mod = 1;
	}

	public void setMinLevel(int minLevel) {
		this.minLevel = minLevel;
		mod = 1;
	}

	public void setMaxLevel(int maxLevel) {
		this.maxLevel = maxLevel;
		mod = 1;
	}
	
	public int getLevel() {
		return level;
	}

	public int getMinLevel() {
		return minLevel;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	@Override
	public abstract boolean equals(Object q);
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract int compareTo(Abstraction a);
	
	public abstract void refine();
	public abstract void maxRefine();
	public abstract void minRefine();
	public abstract String encode();
	public abstract void decode(String s);
	
	public abstract void copy(Abstraction a);
	
	@Override
	public abstract String toString();
	
	public static String concatAbstractions(String[] abstractions,String sep){
		if(abstractions == null)
			return new String("");
		
		StringBuilder buf = new StringBuilder();
		//buf.append(defaults);
		for (int c = 0; c < abstractions.length; c++) {
			if (buf.length() > 0) buf.append(sep);
			buf.append(abstractions[c]);
		}
		return buf.toString();
		
	}
	
	public static String[] splitAbstractions(String abstractions, String sep){
		if(abstractions==null)
			return null;
		return abstractions.split(sep);
	}
	
}

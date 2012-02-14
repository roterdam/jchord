package chord.analyses.libanalysis;

public abstract class Query implements Comparable<Query> {

	@Override
	public abstract boolean equals(Object q);
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract int compareTo(Query q);
	
	@Override
	public abstract String toString();
	
	public abstract String encode();
	
	public abstract void decode(String s);
	
	public static String concatQueries(String[] queries,String sep){
		if(queries == null)
			return new String("");
		
		StringBuilder buf = new StringBuilder();
		//buf.append(defaults);
		for (int c = 0; c < queries.length; c++) {
			if (buf.length() > 0) buf.append(sep);
			buf.append(queries[c]);
		}
		return buf.toString();
		
	}
	
	public static String[] splitQueries(String queries, String sep){
		if(queries==null)
			return null;
		return queries.split(sep);
	}
}
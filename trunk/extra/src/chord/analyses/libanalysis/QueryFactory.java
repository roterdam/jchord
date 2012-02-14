package chord.analyses.libanalysis;

public interface QueryFactory {
	//T create();
	//T create(Object[] objs);
	Query create();
	Query create(Object[] objs);
	Query create(String enc);
	Query create(Query q);
}

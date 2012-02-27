package chord.analyses.libanalysis;

public interface AbstractionFactory {
	//T create();
	//T create(Object[] objs);
	Abstraction create();
	Abstraction create(Object[] objs);
	Abstraction create(Abstraction a);
	Abstraction create(String enc);
}

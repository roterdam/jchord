package chord.analyses.escape.metaback;

import chord.analyses.heapacc.DomE;
import chord.project.analyses.metaback.Query;
import chord.project.analyses.metaback.QueryFactory;
import chord.project.analyses.metaback.QueryResult;
import chord.project.analyses.metaback.dnf.ClauseSizeCMP;
import chord.project.analyses.metaback.dnf.DNF;
import chord.util.Utils;

public class EscQueryFactory implements QueryFactory {
	private final static EscQueryFactory singleton = new EscQueryFactory();
	private static DomE dom;
	
	public static void setDomE(DomE obj){
		dom = obj;
	}
	
	private EscQueryFactory(){}
	
	public static EscQueryFactory getSingleton(){
		return singleton;
	}
	
	@Override
	public QueryResult genResultFromStr(String s) {
		String tokens[] = Utils.split(s, EscQueryResult.SEP, true, true, -1);
		EscQuery q = (EscQuery)this.getQueryFromStr(tokens[0]);
		int result = Integer.parseInt(tokens[1]);
		DNF nc =null;
		if(result==QueryResult.REFINE&&tokens.length>=3)
		nc = new DNF(new LNumCMP(),EscDNFFactory.getSingleton(),tokens[2]);
		return new EscQueryResult(q,result,nc);
	}

	@Override
	public Query getQueryFromStr(String s) {
		EscQuery ret = new EscQuery(s,dom);
		return ret;
	}

}

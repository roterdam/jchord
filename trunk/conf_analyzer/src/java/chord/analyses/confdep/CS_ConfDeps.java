package chord.analyses.confdep;

import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alloc.DomH;
import chord.analyses.confdep.optnames.DomOpts;
import chord.analyses.confdep.rels.RelFailurePath;
import chord.analyses.field.DomF;
import chord.analyses.invk.DomI;
import chord.analyses.primtrack.DomUV;
import chord.analyses.string.DomStrConst;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.analyses.JavaAnalysis;

/**
 * Context-sensitive version of ConfDeps
 * @author asrabkin
 *
 */
@Chord(
		name = "CSConfDeps"
	)
public class CS_ConfDeps extends ConfDeps {

	public boolean STATIC = false;
	public boolean DYNTRACK = false;
	public boolean SUPERCONTEXT = false;
	public static final String CONFDEP_SCANLOGS_OPT = "confdep.scanlogs";
	public static final String CONFDEP_DYNAMIC_OPT = "confdep.dynamic"; //should be either static, dynamic-load or dynamic-track
	DomH domH;
	DomI domI;
	DomV domV;
	DomUV domUV;
	DomF domF;
	DomStrConst domConst;
	public boolean lookAtLogs = false;
	boolean fakeExec;

	public void run() {
		ClassicProject Project = ClassicProject.g();

		fakeExec = Config.buildBoolProperty("programUnchanged", false);
		lookAtLogs = Config.buildBoolProperty(CONFDEP_SCANLOGS_OPT, true);
		String dynamism = System.getProperty(CONFDEP_DYNAMIC_OPT, "static");
		if(dynamism.equals("static")) {
			STATIC = true;
			DYNTRACK = false;
			SUPERCONTEXT = System.getProperty(RelFailurePath.FAILTRACE_OPT, "").length() > 0;
		} else if (dynamism.equals("dynamic-track")) {
			STATIC = false;
			DYNTRACK = true;
		} else if(dynamism.equals("dynamic-load")) {
			STATIC = false;
			DYNTRACK = false;
		} else {
			System.err.println("ERR: " + CONFDEP_DYNAMIC_OPT + " must be 'static', 'dynamic-track', or dynamic-load");
			System.exit(-1);
		}
		boolean miniStrings = Config.buildBoolProperty("useMiniStrings", false);
		boolean dumpIntermediates = Config.buildBoolProperty("dumpArgTaints", false);

		boolean wideCallModel = Config.buildBoolProperty("externalCallsReachEverything", true);
		if(!wideCallModel)
			makeEmptyRelation(Project, "externalThis");
		
		
		//Start by doing points-to, string processing and opt-finding.
		//are linked together because  
		if(STATIC) {
			Project.runTask("cipa-0cfa-arr-dlog");

			maybeRun(Project,"findconf-dlog");

			if(miniStrings) //runs on context-insensitive graph
				maybeRun(Project,"mini-str-dlog");
			else
				maybeRun(Project,"strcomponents-dlog");

			Project.runTask("CnfNodeSucc");

			Project.runTask("Opt");
		} else {
			Project.runTask("dynamic-cdep-java");	  
			Project.runTask("cipa-0cfa-arr-dlog");
		}
		
		if(DYNTRACK) {
			Project.runTask("dyn-datadep");
				//no need for another points-to here. Only datadep-cs uses
				//the context-sensitive points-to
		} else {
			
			Project.runTask("ctxts-java"); //do the context-sensitive points-to
					//need to do this unconditionally to build the domain
			maybeRun(Project, CtxtsAnalysis.getCspaKind());

			maybeRun(Project,"datadep-func-cs-dlog");
		}
	
		if(SUPERCONTEXT) {
			//    	Project.runTask("PobjVarAsgnInst"); //to avoid counting domP time in SCS
			maybeRun(Project,"confdep-dlog"); //to mark primRefDep as done
			Project.runTask("scs-datadep-dlog");
			Project.runTask("scs-confdep-dlog");
		} else {
			Project.runTask("confdep-dlog");
		}
	
		DomOpts domOpt  = (DomOpts) Project.getTrgt("Opt");
		slurpDoms();
		dumpOptUses(domOpt);

		if(DYNTRACK)
			dumpFieldTaints(domOpt, "instHF", "statHF");
		else
			dumpFieldTaints(domOpt, "instFOpt", "statFOpt");

		if(STATIC)
			dumpOptRegexes("conf_regex.txt", DomOpts.optSites());

		if(dumpIntermediates)  {
//			Project.runTask("datadep-debug-dlog");
//			dumpArgDTaints();
		}

		
	}

}

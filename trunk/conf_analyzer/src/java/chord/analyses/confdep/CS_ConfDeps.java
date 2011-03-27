package chord.analyses.confdep;

import chord.analyses.alias.CtxtsAnalysis;
import chord.analyses.alloc.DomH;
import chord.analyses.confdep.optnames.DomOpts;
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
		} else {
			
			Project.runTask("ctxts-java"); //do the context-sensitive points-to
			ClassicProject.g().runTask(CtxtsAnalysis.getCspaKind());

			maybeRun(Project,"datadep-func-cs-dlog");
		}
		
		Project.runTask("confdep-dlog");

		DomOpts domOpt  = (DomOpts) Project.getTrgt("Opt");
		slurpDoms();
		dumpOptUses(domOpt);
		dumpFieldTaints(domOpt, "instFOpt", "statFOpt");
		if(dumpIntermediates)  {
			if(STATIC)
				dumpOptRegexes("conf_regex.txt", DomOpts.optSites());
//			Project.runTask("datadep-debug-dlog");
//			dumpArgDTaints();
		}

		
	}

}

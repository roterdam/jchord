package chord.analyses.escape;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import chord.util.FileUtils;
import chord.program.Program;
import chord.program.MethodElem;
import joeq.Compiler.Quad.Quad;
import chord.analyses.alloc.DomH;
import chord.analyses.heapacc.DomE;
import chord.project.analyses.ProgramRel;
import chord.project.Chord;
import chord.project.Config;

@Chord(name="locEH", sign="E0,H0:E0_H0")
public class LocEH extends ProgramRel {
	@Override
	public void fill() {
        DomE domE = (DomE) doms[0];
        DomH domH = (DomH) doms[1];
		Program program = Program.g();
		File file = new File(Config.outDirName, "locEH.txt");
		List<String> list = new ArrayList<String>();
		FileUtils.readFileToList(file, list);
		for (String s : list) {
			String[] a = s.split(" ");
			MethodElem me1 = MethodElem.parse(a[0]);
			Quad q1 = program.getQuad(me1);
			int e = domE.indexOf(q1);
			assert (e >= 0);
			MethodElem me2 = MethodElem.parse(a[1]);
			Quad q2 = program.getQuad(me2);
			int h = domH.indexOf(q2);
			assert (h >= 0);
			add(e, h);
		}
	}
}

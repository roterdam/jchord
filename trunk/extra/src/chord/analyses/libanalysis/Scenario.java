package chord.analyses.libanalysis;

import java.util.Random;

public final class Scenario {
	//private static int newId = 0;
	//int id = newId++;

	private static Random random = new Random();
	int id = random.nextInt();

	private String in, out;
	private String type;
	private String sep;
	
	
	public String getIn() {
		return in;
	}

	public String getOut() {
		return out;
	}

	public String getType() {
		return type;
	}

	public void setIn(String in) {
		this.in = in;
	}

	public void setOut(String out) {
		this.out = out;
	}

	public void setType(String type) {
		this.type = type;
	}
	


	public Scenario(String line, String sep) {
		this.sep = sep;
		String[] tokens = line.split(sep,-1);
		type = tokens[0];
		in = tokens[1];
		out = tokens[2];
	}
	
	public Scenario(String type, String in, String out, String sep) { 
		this.in = in; this.out = out; this.type = type; this.sep = sep; 
	}
	
	@Override public String toString() { return type + sep + in + sep + out; }
	
	/*void copy(Scenario inScenario){
		this.in = inScenario.in;
		this.out = inScenario.out;
		this.type = inScenario.type;
		this.sepMaj = inScenario.sepMaj;
		this.sepMin = inScenario.sepMin;
	}*/
		
	void decode(String line){
		String[] tokens = line.split(sep,-1);
		type = tokens[0];
		in = tokens[1];
		out = tokens[2];
	}

}

import java.io.*;

public class Test{
	public static void main(String[] args){
	    try{	
		String runActiveMQ = "../bin/activemq";
		Process p1 = Runtime.getRuntime().exec(runActiveMQ);
		writeToStdOutput(p1, "ACTIVEMQ");
		String runProducer = "ant producer";
		Process p2 = Runtime.getRuntime().exec(runProducer);
		writeToStdOutput(p2, "PRODUCER");
		String runConsumer = "ant consumer";
		Process p3 = Runtime.getRuntime().exec(runConsumer);
		writeToStdOutput(p3, "CONSUMER");
		p1.destroy();
	    }
	    catch(Exception e){
		System.err.println("Exception while running the activemq benchmark");
		e.printStackTrace();
	    }
	}

	private static void writeToStdOutput(Process p, String errsrc){
		try{
			BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader pErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	        	
			boolean writeErrStream = true;
			String pInStr;
			while((pInStr = pIn.readLine()) != null){
				System.out.println(errsrc + ">OUT> "+pInStr);
				if(errsrc.equals("ACTIVEMQ") && (pInStr.indexOf("Successfully connected to tcp://localhost:61616") != -1)){
					writeErrStream = false;
					break;
				}
			}	

			if(writeErrStream){
				String pErrStr;
				while((pErrStr = pErr.readLine()) != null){
					System.out.println(errsrc + ">ERR> "+pErrStr);
				}
			}
		}	
		catch(Exception e){
			System.err.println("Exception while writing the output of subprocesses to the standard output");
			e.printStackTrace();
		}
	}
}

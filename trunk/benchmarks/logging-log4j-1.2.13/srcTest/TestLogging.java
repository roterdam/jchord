//code inspired by the example in http://www.ibm.com/developerworks/library/j-tracemt.html?ca=dgr-lnxw1alog4j

import java.util.Random;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;

import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;

import java.io.IOException;
import java.util.Enumeration;


class TestThread implements Runnable{
	private static Logger logger = Logger.getLogger(TestLogging.class);

    public void run(){
	   Random rnd = new Random();
       int rndid = rnd.nextInt();

       boolean changeBufSize = rnd.nextBoolean();
	   if(changeBufSize){
	      Enumeration e = logger.getAllAppenders();
		  while(e.hasMoreElements()){
			Appender a = (Appender)e.nextElement();
			if(a instanceof AsyncAppender){
				AsyncAppender aa = (AsyncAppender)a;
				aa.setBufferSize(50);
			}
		  }
       }
      
       logger.info("[" +rndid +"]"+ " Program Running");
       logger.debug("[" +rndid +"]" + " Debug message!!");
       logger.warn("[" +rndid +"]" + " Warning this is a warning");
       logger.error("[" +rndid +"]" + " Error Message!!");
       logger.fatal("[" +rndid +"]" + "A Non-Fatal FATAL message!!");
    }
}

//invoke it as : java TestLogging <#threads> 
public class TestLogging{
    private static Logger logger = Logger.getLogger(TestLogging.class);
	private static ThreadGroup myGroup = new ThreadGroup("Group");
	private static TestThread ttRunnable = new TestThread();

	public static void main(String[] args){
		try{
			FileAppender appender1 = new FileAppender(new SimpleLayout(),"log1");
			FileAppender appender2 = new FileAppender(new SimpleLayout(),"log2");
			
			AsyncAppender asAppender = new AsyncAppender();
			asAppender.addAppender(appender1);
			asAppender.addAppender(appender2);

            logger.addAppender(asAppender);			

			logger.info("About to start threads");
		}
		catch(IOException e){
			System.err.println("IOException while opening FileAppender appender");
		}
		
        //int tCount = Integer.parseInt(args[0]);
	int tCount = 20;
        for(int i = 0; i < tCount; i++){
			Thread thr = new Thread(myGroup, ttRunnable);
            thr.start();
        }
    }
}


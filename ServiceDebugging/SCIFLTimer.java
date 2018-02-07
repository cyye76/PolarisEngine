package ServiceDebugging;

import java.util.Timer;
import java.util.TimerTask;

public class SCIFLTimer extends TimerTask {

	Timer timer;
	AnalysisInfo info=null;
	
	public SCIFLTimer() {
		timer = new Timer();
	}

	public SCIFLTimer(AnalysisInfo io) {
		timer = new Timer();
		info = io;
	}

	public void setTimer(long timeout) {
		timer.schedule(this, timeout);
	}
	
	@Override
	public void run() {		
		//ServiceDebugging.prepare4AbortedQuit(); 
		//ServiceDebugging.copyResults();		
		
		ServiceDebugging.setStopFlags();
        timer.cancel();
        if(info!=null) 
        	 ServiceDebugging.copyIntermediateResults(info);
	}
	
	public void abort() {
		timer.cancel();
	}

}

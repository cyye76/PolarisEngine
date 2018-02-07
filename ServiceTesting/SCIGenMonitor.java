package ServiceTesting;

import scripts.TestingScript.TestingScriptDefinition;
import ServiceTesting.Monitoring.CoverageMonitor;

public class SCIGenMonitor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	    String copyOutDir = "/scratch/j/jacobsen/cyye/ST/";
	    
	    TestingScriptDefinition tsd = ServiceTesting.loadTestingScript("Applications/Auction/testscript.xml");
	    
	    ServiceTesting.isBaseline = true;
	    CoverageMonitor coverage_monitor = ServiceTesting.constructCoverageMonitor(tsd, ServiceTesting.PathCoverage);		
		
	    String source = copyOutDir + "PathCoverageMonitor"+args[0];
	    
	    ServiceTesting.savePathCoverageMonitor(coverage_monitor, source);	    	    
	}

}

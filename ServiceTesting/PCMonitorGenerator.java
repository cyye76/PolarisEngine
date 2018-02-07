package ServiceTesting;

import java.util.ArrayList;

import scripts.TestingScript.TestingScriptDefinition;
import Service.AbstractService;
import Service.Choreography;
import ServiceTesting.Monitoring.CoverageMonitor;
import ServiceTesting.Monitoring.GlobalObservation;
import ServiceTesting.Monitoring.Observation;
import ServiceTesting.Monitoring.ObservationUnit;
import ServiceTesting.Monitoring.Oracle;
import ServiceTesting.Monitoring.TestingMonitor;
import ServiceTesting.TestCase.ServiceTestCase;
import engine.engine;

public class PCMonitorGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//start the engine
		engine.getEngine().startEngine();
				
		PCMonitorGenerator.MyTesting(args);
				
		//close the engine
		try {
		 	Thread.sleep(10000);
		} catch(Exception e) {}	
	
		engine.getEngine().closeEngine();

    }
	
	private static int timeout = 60;
	private static String appID = null;
	
	//main testing procedure
	public static void MyTesting(String[] args) { 
						
		if(args.length < 4) {
		
			System.out.println("Usage: testscriptName testnum timeout ID");		
		
		} else {
			//0. set parameter
			timeout = new Integer(args[2]);
			ServiceTesting.isBaseline = true;
	        appID = args[3];
	    
	     	//1. load testing script		
	       	TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(args[0]);
			
	       	//2. load test oracle
	       	Oracle m_oracle = ServiceTesting.loadOracle(tsd);
						
            //3. testcase num
	       	int testnum = new Integer(args[1]);	       	
	        testOneApp(tsd, m_oracle, testnum);
	        	        	       		       	        	       
	    }
	} 

	private static void testOneApp(TestingScriptDefinition tsd,
			Oracle m_oracle, int testnum) {
        //0. load monitor
		CoverageMonitor coverage_monitor = new CoverageMonitor();
	    
		ArrayList<String> selectedServiceName = new ArrayList<String>();
	    selectedServiceName.add("Buyer1");
	    selectedServiceName.add("Saler");
		ServiceTesting.constructPartialPathCoverageMonitor(coverage_monitor, tsd, selectedServiceName);
		//ServiceTesting.constructPathCoverageMonitor(coverage_monitor, tsd);

		int testcaseNum=0;
		while(testcaseNum<testnum) {
			//1. load services
			ArrayList<AbstractService> services = ServiceTesting.loadTestingService(tsd);
		
	
			//2. load test case
			int testcase_policy = ServiceTestCase.RANDOM;
			ServiceTestCase m_testcase = ServiceTesting.loadServiceTestCase(tsd, testcase_policy);
			services.add(m_testcase.getService());//add the test case service into the service list
	
			//3. load choreography		
			Choreography cho = ServiceTesting.loadChoreography(tsd, services);
		
			//4. register service choreography
			engine.getEngine().getServiceCorrelation().register(cho);	        
        
			//5. register testing_monitor       
			coverage_monitor.register(services);
			TestingMonitor testing_monitor = new TestingMonitor();
			testing_monitor.register(services);
			testing_monitor.registerCompleteConditions(m_testcase.getService());//use test case to indicate whether the service is complete
	    
			//6. execute services
			for(AbstractService service: services)
				engine.getEngine().executeService(service);
	
			//wait the services complete
			int time = timeout;
			while(time>0 && !testing_monitor.terminated) {
				try {
					Thread.sleep(1000);
				} catch(Exception e) {}	        		
				time--;
			}
        
			//not terminated, force it terminate immediately
			if(!testing_monitor.terminated) {
				System.out.println("Time out!");	        	
			}
			
			        
			//terminate all the remaining running services
			ArrayList<AbstractService> running_services = testing_monitor.getRemainingServices(services);
			engine.getEngine().terminateServices(running_services);
			
			coverage_monitor.matchGlobalObservation();			
			//remove observation events
	        coverage_monitor.clearObservationEvents();		                 			
			
			//garbage collecting for the services
	        coverage_monitor.unregister(services);
	        engine.getEngine().getServiceCorrelation().unregister(cho);	        
	        engine.getEngine().unregister(services);
	        testing_monitor.gabarageCollection();			
			
			testcaseNum++;
		}
        
		CoverageMonitor saved_monitor = new CoverageMonitor();
		ArrayList<ObservationUnit> feasibleObservationUnits = coverage_monitor.getGlobalObservation().getCoveredObservationUnit();
		saved_monitor.setTestingObservation(coverage_monitor.getTestingObservation());
		
		Observation global_observation = new GlobalObservation();
		for(ObservationUnit unit: feasibleObservationUnits)
			global_observation.addObservationUnit(unit);
		saved_monitor.setGlobalObservation(global_observation);
		
		
		//String copyOutDir = "/scratch/j/jacobsen/cyye/ST/PathCoverageMonitor_"+appID;
		//ServiceTesting.savePathCoverageMonitor(saved_monitor, copyOutDir);
		String source = "tmp/PathCoverageMonitor_"+appID;
		ServiceTesting.savePathCoverageMonitor(saved_monitor, source);
	}
	
}

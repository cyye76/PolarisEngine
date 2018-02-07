package ServiceTesting;

import java.util.ArrayList;

import scripts.TestingScript.TestingScriptDefinition;
import Service.AbstractService;
import Service.Choreography;
import ServiceTesting.Monitoring.Oracle;
import ServiceTesting.Monitoring.TestingMonitor;
import ServiceTesting.TestCase.ServiceTestCase;
import engine.engine;

public class AppTesting {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//start the engine
		engine.getEngine().startEngine();
				
		AppTesting.MyTesting(args);
				
		//close the engine
		try {
		 	Thread.sleep(10000);
		} catch(Exception e) {}	
	
		engine.getEngine().closeEngine();

    }
	
	private static int timeout = 60;
	
	//main testing procedure
	public static void MyTesting(String[] args) { 
						
		if(args.length < 3) {
		
			System.out.println("Usage: testscriptName testnum timeout");		
		
		} else {
			//0. set parameter
			timeout = new Integer(args[2]);
	    
	     	//1. load testing script		
	       	TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(args[0]);
			
	       	//2. load test oracle
	       	Oracle m_oracle = ServiceTesting.loadOracle(tsd);
						
            //3. test app
	       	int testnum = new Integer(args[1]);
	       	for(int i=0;i<=testnum;i++) {
	           boolean passed = testOneApp(tsd, m_oracle);
	           if(!passed) {
	        	   System.out.println("Fail!");
	           } else
	        	   System.out.println("Pass!");
	       	}
	       		       	        	       
	    }
	}

	private static boolean testOneApp(TestingScriptDefinition tsd,
			Oracle m_oracle) {

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
        	
        //output results	        	                
        boolean result = m_oracle.checkOracle(services); 
                
        //garbage collecting for the services
        engine.getEngine().getServiceCorrelation().unregister(cho);	        
        engine.getEngine().unregister(services);
        testing_monitor.gabarageCollection();
        
        return result;
	}
	
}

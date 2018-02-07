package ServiceTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import engine.engine;
import engine.expression.PortExpression.PortVariable;
import Deployment.LoadServiceScript;
import Service.AbstractService;
import Service.Choreography;
import Service.Task;
import Service.Transition;
import ServiceTesting.EventInterface.ActivityCoverageEventInterface;
import ServiceTesting.EventInterface.BranchCoverageEventInterface;
import ServiceTesting.EventInterface.DUCoverageEventInterface;
import ServiceTesting.EventInterface.PathCoverageEventInterface;
import ServiceTesting.Monitoring.CoverageMonitor;
import ServiceTesting.Monitoring.LocalObservation;
import ServiceTesting.Monitoring.Observation;
import ServiceTesting.Monitoring.ObservationUnit;
import ServiceTesting.Monitoring.EventSequenceObservationUnit;
import ServiceTesting.Monitoring.Oracle;
import ServiceTesting.Monitoring.TestingMonitor;
import ServiceTesting.TestCase.RandomServiceTestCase;
import ServiceTesting.TestCase.ServiceTestCase;
import Utils.XMLProcessing;

import scripts.MutationScript.MutationDefinition;
import scripts.MutationScript.MutationType;
import scripts.TestingScript.ChoreographyType;
import scripts.TestingScript.MutationsType;
import scripts.TestingScript.OracleType;
import scripts.TestingScript.ServicesType;
import scripts.TestingScript.TestCaseType;
import scripts.TestingScript.TestingScriptDefinition;

public class ServiceTesting {
	
	public final static int ActivityCoverage = 1;
	public final static int BranchCoverage = 2;
	public final static int DUCoverage = 3;
	public final static int PathCoverage = 4;
	
	public final static int Baseline_DUCoverage_Random = 1;
	public final static int OA_DUCoverage_Random = 2;
	public final static int Baseline_PathCoverage_Random = 3;
	public final static int OA_PathCoverage_Random = 4;
	
	
	private static boolean isInconsistencyFault;
	private static int approach;
	public static boolean isBaseline;
	private static int coverageStrategy;//coverage strategy
	private static int testcase_policy;	//testcase generation policy	
	private static int exposed_percentage;//percentage of exposed events
	private static int maxTestCaseNum;//maximum test case for each mutation
	private static int test_count;//number of rounds for each mutation
	private static double coverageThreshold;//stop condition for each testing
	private static String testscript_filename;//test script filename
	private static String appendix; //used to differentiate different runs
	private static String outdir;//output directory
	private static String copyOutDir = null;
	private static long duration=-1;
	private static int mutationIndex = 0;
	private static int round=0;
	private static int workIndex = 0;
	private static int workTerminateIndex = 0;
	private static String coreID=null;
    private static long start=0;
    private static ArrayList<String> needCopyFiles=null;
    private static int timeout=60;
	private static ArrayList<String> selectedServiceName = null;
	private static String savedmonitor = null;
    
	public static boolean handleInputParameter(String[] args) {		
		if(args.length<8) {
			System.out.println("Usage: approach maxTestCaseNum test_count coverageThreshold testscript_filename appendix outdir isInconsistencyFault");
			return false;
		} 
		
		try {
			approach = new Integer(args[0]);
			initializeTestPolicy();
			
		    maxTestCaseNum = new Integer(args[1]);
		    test_count = new Integer(args[2]);
		    coverageThreshold = new Double(args[3]);
		    testscript_filename = args[4];
		    appendix = args[5];//generateAppendix(testcase_policy, coverage_strategy, args[5]);
		    outdir = args[6];
		    
		    isInconsistencyFault = new Boolean(args[7]);
		    
		    if(args.length > 8)
		        timeout = new Integer(args[8]);
		    
		    if(args.length > 9) copyOutDir = args[9];
		    		    
		    if(args.length>10) 
		    	duration = new Long(args[10]);
		    
		    //selectedServiceName = new ArrayList<String>();
		    //selectedServiceName.add("Buyer1");
		    //selectedServiceName.add("Buyer2");
		    //selectedServiceName.add("Saler");
		    if(testscript_filename.equals("Applications/Insurance/testscript.xml"))
		        savedmonitor = "Applications/Insurance/PathCoverageMonitor";
		    
		    if(testscript_filename.equals("Applications/Auction/testscript.xml"))
		        savedmonitor = "Applications/Auction/PathCoverageMonitor";
		    
		    return true;
		
		}catch(Exception e) {
			System.out.println("Usage: approach maxTestCaseNum test_count coverageThreshold testscript_filename appendix  isInconsistencyFault");
			return false;
		}
		
	}

    private static void initializeTestPolicy() {	
    	if(approach<=4)			
    		testcase_policy = ServiceTestCase.RANDOM;//random			
										
    	if(approach == 1 || approach == 3) 				
    		isBaseline = true;
				
    	if(approach == 2 || approach == 4) 
    		isBaseline = false;
	
    	if(approach == 1 || approach == 2)
    		coverageStrategy = 3;
	
    	if(approach == 3 || approach == 4)
    		coverageStrategy = 4;
	}
	
	public static void main(String[] args) {
		//start the engine
		engine.getEngine().startEngine();
		
		ServiceTesting.MyTesting(args);
		//ServiceTesting.MyTestingReverse(args);
		
		//close the engine
		try {
    		Thread.sleep(10000);
    	} catch(Exception e) {}	
		engine.getEngine().closeEngine();
	}

	//main testing procedure
	public static void MyTesting(String[] args) { 
			
		//0. handle input parameter
        boolean validParameter = ServiceTesting.handleInputParameter(args);  
		
        if(validParameter) {
        	//1. load testing script		
        	TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript_filename);
		
        	//2. load test oracle
        	Oracle m_oracle = ServiceTesting.loadOracle(tsd);
		
        	//3. load mutation category
        	ArrayList<String[]> mutations = ServiceTesting.loadMutationList(tsd);
		
        	//4. load saveParameters
        	String[] saveParameters = ServiceTesting.loadSaveParameters();
        	//int round = 0;
        	//int mutationIndex = 0;
        	if(saveParameters!=null && saveParameters.length>=2) {
        		round = new Integer(saveParameters[0]);
        		mutationIndex = new Integer(saveParameters[1]);
        	}
        	
        	//4. test each mutation
        	for(int i=round;i<test_count;i++) {
        	 
        		while(mutationIndex<mutations.size()) {
        	    	String[] mt = mutations.get(mutationIndex);
        			ServiceTesting.testOneMutation(tsd, mt, m_oracle, mutationIndex);
        			mutationIndex++;
        			        			
        			recordCompleteOne(i + "   " + mutationIndex);        			
        	    }        		        	    
        		
        		mutationIndex = 0;
        	     
        	}
        	
        	//5. copy results if running in sciNet HPC
        	if(copyOutDir!=null) copyResults();
        }
    }
	
	//main testing procedure
	public static void MyTestingReverse(String[] args) { 
			
		//0. handle input parameter
        boolean validParameter = ServiceTesting.handleInputParameter(args);  
		
        if(validParameter) {
        	//1. load testing script		
        	TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript_filename);
		
        	//2. load test oracle
        	Oracle m_oracle = ServiceTesting.loadOracle(tsd);
		
        	//3. load mutation category
        	ArrayList<String[]> mutations = ServiceTesting.loadMutationList(tsd);
        	int mutationIndex = mutations.size()-1;
		
        	//4. load saveParameters
        	String[] saveParameters = ServiceTesting.loadSaveParameters();
        	//int round = 0;
        	//int mutationIndex = 0;
        	if(saveParameters!=null && saveParameters.length>=2) {
        		round = new Integer(saveParameters[0]);
        		mutationIndex = new Integer(saveParameters[1]);
        	}
        	
        	//4. test each mutation
        	for(int i=round;i<test_count;i++) {
        	 
        		while(mutationIndex>=0) {
        	    	String[] mt = mutations.get(mutationIndex);
        			ServiceTesting.testOneMutation(tsd, mt, m_oracle, mutationIndex);
        			mutationIndex--;
        			        			
        			recordCompleteOne(i + "   " + mutationIndex);        			
        	    }        		        	    
        		
        		mutationIndex =mutations.size()-1;
        	     
        	}
        	
        	//5. copy results if running in sciNet HPC
        	if(copyOutDir!=null) copyResults();
        }
    }	
	
	private static void HandleSerialTaskArguments(String[] args) {
		//args[0]:startWorkIndex
		//args[1]:endWorkIndex
		//args[2]:CoreID
		//args[3]:duration
		
		//fix the following parameters
	    outdir = "/dev/shm";
	    copyOutDir = "/scratch/j/jacobsen/cyye/ST";
		
	    //outdir = "tmp/tmp1";
	    //copyOutDir = "tmp/tmp2";
	    maxTestCaseNum = 200;
	    test_count = 1;
	    coverageThreshold = 0.90;
	    	    
		workIndex = new Integer(args[0]);
		workTerminateIndex = new Integer(args[1]);
		coreID = args[2];		
		duration = new Long(args[3]);//172800000;
		mutationIndex=0;
		
		timeout = new Integer(args[4]);
		
		if(args.length>5)
			copyOutDir = args[5];
		
		//If parameters are saved, load them
		String[] saveParameters = ServiceTesting.loadSaveParameters();
		if(saveParameters!=null && saveParameters.length>=2) {
			workIndex = new Integer(saveParameters[0]);
			mutationIndex = new Integer(saveParameters[1]);
		}
	}
	
	//for running a serial of tasks in scinet hpc
	public static void TestSerialTasks(String[] args) {
		start = System.currentTimeMillis(); 
        needCopyFiles = new ArrayList<String>();
		
		//handle Parameters
		HandleSerialTaskArguments(args);
		
		while(workIndex <= workTerminateIndex && workIndex< SCIST.tasks.length) {						
			
			int cg = SCIST.tasks[workIndex][0];
			int ft = SCIST.tasks[workIndex][1];
			approach = SCIST.tasks[workIndex][2];
			
			initializeTestPolicy();
		    testscript_filename = SCIST.tscripts[cg];		    	    	    	    
	        isInconsistencyFault = ft==0;
	        appendix = SCIST.CG[cg] + SCIST.FT[ft] + coreID;
	        	        
	        savedmonitor = SCIST.savedPathMonitor[cg];
	    
		    boolean workCompleted = MySCITesting();
		    if(!workCompleted)  break;

		    workIndex++; //execute next task
		}
		
		//copy results and quit	        		    
        copyResults();
        
		//if all the tasks are finished, set the finish flag
		if(workIndex > workTerminateIndex) {
			setTerminateFlags();
		}
	}
	
	public static void TestEqualEffectTasks(int app, int index, int end,
			String appd, String od, String cpd, long dura) {
		
		start = System.currentTimeMillis(); 
        needCopyFiles = new ArrayList<String>();
		
		appendix = appd;
        outdir = od;
        copyOutDir = cpd;   
        duration = dura;
        coreID = appd + app;
        isBaseline = true;
        coverageStrategy = app + 3;
        testcase_policy = ServiceTestCase.RANDOM;//random
        
        maxTestCaseNum = 50;//200;	    
	    coverageThreshold = 1;
        
       //If parameters are saved, load them
		String[] saveParameters = ServiceTesting.loadSaveParameters();
		if(saveParameters!=null && saveParameters.length>=2) {
			index = new Integer(saveParameters[0]);	
			end = new Integer(saveParameters[1]);
		}        
		workTerminateIndex = end; 
		
        while(index<=end) {
        	int[] assign = SCIEXP2.getAssignment(index);
        	testscript_filename = SCIEXP2.tscripts[assign[0]];		    	    	    	    
	        isInconsistencyFault = assign[1]==0;
	        mutationIndex = assign[2];	        
	        workIndex = index;	        
	        
	        boolean workCompleted = MyEqualEffectTesting();
		    if(!workCompleted)  break;

		    index++; //execute next task
		}
		
		//copy results and quit	        		    
        copyResultsSE();
        
		//if all the tasks are finished, set the finish flag
		if(index > end) {
			setTerminateFlags();
		}
		
	}
	
	private static boolean MyEqualEffectTesting() {
		//1. load testing script		
        TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript_filename);
		
        //2. load test oracle
        Oracle m_oracle = ServiceTesting.loadOracle(tsd);
		
        //3. load mutation category
        ArrayList<String[]> mutations = ServiceTesting.loadMutationList(tsd);
		        	
        //4. test each mutation        	         
        String[] mt = mutations.get(mutationIndex);
                
        boolean completed = ServiceTesting.testOneMutationSameEffect(tsd, mt, m_oracle, workIndex);

        if(!completed) return false;
        	                   			        			
        recordCompleteOne((workIndex+1) + "  " + workTerminateIndex);        	   	                	    
                
        return true;
	}	

	//adapt it for running in SciNet HPC
	private static boolean MySCITesting() {
							        
        //1. load testing script		
        TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript_filename);
		
        //2. load test oracle
        Oracle m_oracle = ServiceTesting.loadOracle(tsd);
		
        //3. load mutation category
        ArrayList<String[]> mutations = ServiceTesting.loadMutationList(tsd);
		        	
        //4. test each mutation        	 
        while(mutationIndex<mutations.size()) {
           	String[] mt = mutations.get(mutationIndex);
        	boolean completed = ServiceTesting.testOneMutation(tsd, mt, m_oracle, mutationIndex);

            if(!completed) return false;
        	   
            mutationIndex++;
        			        			
            recordCompleteOne(workIndex + "   " + mutationIndex);
        	   	
        }        		        	    
        
        mutationIndex=0;
        return true;
    }

	private static String[] loadSaveParameters() {
		String filename ;
		if(copyOutDir==null)
		   filename = getCompleteFileName();
		else 
		   filename = getSaveParameterFileName();
		
		String[] conts = null;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();			
			if(line!=null)
				conts = line.split("\\s+");
			reader.close();			
		} catch(Exception e) {}
					
		return conts;
	}			

	private static void recordCompleteOne(String mt) {
		String filename = getCompleteFileName();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(mt);			
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean testOneMutation(TestingScriptDefinition tsd, String mt,
			Oracle m_oracle, int mt_index) {
		String[] mts = new String[1];
		mts[0] = mt;
		return testOneMutation(tsd, mts, m_oracle, mt_index);
	}
		
	private static boolean testOneMutation(TestingScriptDefinition tsd, String[] mts,
				Oracle m_oracle, int mt_index) {	
		
		//0. create monitor	
		CoverageMonitor coverage_monitor = constructCoverageMonitor(tsd, coverageStrategy);		
		
		int testcaseNum = 0;
		double coverage = 0;
		double real_coverage = 0;
		boolean faultdetected = false;
		int tonum=0;
		
		while(testcaseNum<maxTestCaseNum && coverage < coverageThreshold) {
			//1. load services
			ArrayList<AbstractService> services = loadTestingService(tsd);
		
			//2. load mutation into services
			for(String mt: mts)
			   loadMutation(mt, services);
		
			//3. load test case
			ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
			services.add(m_testcase.getService());//add the test case service into the service list
		
			//4. load choreography		
			Choreography cho = loadChoreography(tsd, services);
			
			//5. register service choreography
	        engine.getEngine().getServiceCorrelation().register(cho);	        
	        
	        //6. register coverage_monitor and testing_monitor
	        coverage_monitor.register(services);
	        TestingMonitor testing_monitor = new TestingMonitor();
	        testing_monitor.register(services);
	        testing_monitor.registerCompleteConditions(m_testcase.getService());//use test case to indicate whether the service is complete
		    
	        //7. execute services
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
	        	tonum++;	        	
	        }
	        
	        //terminate all the remaining running services
	        ArrayList<AbstractService> running_services = testing_monitor.getRemainingServices(services);
	        engine.getEngine().terminateServices(running_services);
	        	
	        //calculate the statistic results	        	        
	        testcaseNum++;
	        coverage_monitor.matchTestingObservation();
	        double new_coverage = coverage_monitor.calculateTestingCoverage();
	        if(new_coverage>coverage) { 	        
	        	faultdetected = faultdetected || !m_oracle.checkOracle(services);
	        	coverage = new_coverage;
	        	
	        	//calculate the global coverage
	        	coverage_monitor.matchGlobalObservation();
	        	
	        	//for debug
	        	//System.out.println("Local Coverage:"+ coverage);
	        	//System.out.println("Global Coverage:" + coverage_monitor.calculateCoverage(false));
	        }
	        
	        //remove observation events
	        coverage_monitor.clearObservationEvents();
	       
	        //garbage collecting for the services
	        coverage_monitor.unregister(services);
	        engine.getEngine().getServiceCorrelation().unregister(cho);	        
	        engine.getEngine().unregister(services);
	        testing_monitor.gabarageCollection();

            if(duration > 0) {
    			long current = System.currentTimeMillis();
    			if(start + duration < current + 30 *60 * 1000) //in advance of 1/2 hour to end the task
    				return false;
    		} 
            
            if(tonum>10) {
            	System.out.println("Quit for more than 10 time out!");
            	break;//more than 10 timeout, quit 
            }
		}
		
		//output the result of the testing
		real_coverage = coverage_monitor.calculateRealCoverage();
		ReportTestStatisticsResult(mt_index, real_coverage, faultdetected);

        return true;
	}
	
	private static boolean testOneMutationSameEffect(
			TestingScriptDefinition tsd, String[] mts,
			Oracle m_oracle, int index) {
		
		//0. create monitor	
		CoverageMonitor coverage_monitor_EA = constructCoverageMonitor(tsd, coverageStrategy);		
		CoverageMonitor coverage_monitor_OA = constructCoverageMonitor(tsd, coverageStrategy);
		
		int testcaseNum = 0;
		double OA_coverage = 0;
		double EA_coverage = 0;
		double EA_real_coverage = 0;
		boolean faultdetected_EA = false;
		boolean faultdetected_OA = false;
		
		int tcn_OA=0;
		int tcn_SC_EA=0;
		int tcn_SF_EA=0;		
		int extra_EA = 0;
		
		boolean terminated_OA = false;
		boolean terminated_SC_EA = false;
		boolean terminated_SF_EA = false;
		
		boolean terminated = false;
		while(!terminated) {
			//1. load services
			ArrayList<AbstractService> services = loadTestingService(tsd);
		
			//2. load mutation into services
			for(String mt: mts)
			   loadMutation(mt, services);
		
			//3. load test case
			ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
			services.add(m_testcase.getService());//add the test case service into the service list
		
			//4. load choreography		
			Choreography cho = loadChoreography(tsd, services);
			
			//5. register service choreography
	        engine.getEngine().getServiceCorrelation().register(cho);	        
	        
	        //6. register coverage_monitor and testing_monitor
	        coverage_monitor_EA.register(services);
	        coverage_monitor_OA.register(services);
	        TestingMonitor testing_monitor = new TestingMonitor();
	        testing_monitor.register(services);
	        testing_monitor.registerCompleteConditions(m_testcase.getService());//use test case to indicate whether the service is complete
		    
	        //7. execute services
	        for(AbstractService service: services)
		       engine.getEngine().executeService(service);
		
	        //wait the services complete
	        int time = 60;
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
	        
	        testcaseNum++;

	        //calculate the local coverage	        	        	        
	        coverage_monitor_EA.matchTestingObservation();
	        double new_coverage_EA = coverage_monitor_EA.calculateTestingCoverage();
	        
	        //calculate the global coverage
        	coverage_monitor_OA.matchGlobalObservation();
        	double new_coverage_OA = coverage_monitor_OA.calculateRealCoverage();
        	
        	//faultdetected
        	boolean faultdetected = !m_oracle.checkOracle(services);
        	
	        if(new_coverage_EA > EA_coverage && EA_coverage < coverageThreshold) {
	        	
	        	coverage_monitor_EA.matchGlobalObservation();
	        	EA_real_coverage = coverage_monitor_EA.calculateRealCoverage();
	        	
	        	faultdetected_EA = faultdetected_EA || faultdetected;
	        	EA_coverage = new_coverage_EA;
	        	tcn_SC_EA ++;
	        	tcn_SF_EA ++;
	        }	
	        	
	        if(new_coverage_OA > OA_coverage && !terminated_OA) { 	        
	        	faultdetected_OA = faultdetected_OA || faultdetected;
	        	OA_coverage = new_coverage_OA;
	        	tcn_OA ++;	        	
	        }		        	
	        
	        if(terminated_OA) {
	        	extra_EA++;
	        	
	        	if(!terminated_SC_EA) {	          
	        		coverage_monitor_EA.matchGlobalObservation();
		        	EA_real_coverage = coverage_monitor_EA.calculateRealCoverage();
	        	    tcn_SC_EA ++;
	        	}
	        	
	        	if(!terminated_SF_EA) {
	        		faultdetected_EA = faultdetected_EA || faultdetected;
	        		tcn_SF_EA ++;
	        	}
	        }
	        
	        //remove observation events
	        coverage_monitor_EA.clearObservationEvents();
	        coverage_monitor_OA.clearObservationEvents();
	        
	        //garbage collecting for the services
	        coverage_monitor_EA.unregister(services);
	        coverage_monitor_OA.unregister(services);
	        engine.getEngine().getServiceCorrelation().unregister(cho);	        
	        engine.getEngine().unregister(services);
	        testing_monitor.gabarageCollection();

            if(duration > 0) {
    			long current = System.currentTimeMillis();
    			if(start + duration < current + 30 *60 * 1000) //in advance of 1/2 hour to end the task
    				return false;
    		} 
            
            terminated_OA = testcaseNum > maxTestCaseNum || OA_coverage >= coverageThreshold;
            terminated_SC_EA = terminated_OA && (EA_real_coverage >= OA_coverage || extra_EA > 200);
            terminated_SF_EA = terminated_OA && (!faultdetected_OA || faultdetected_EA || extra_EA >200);
            terminated = terminated_OA && terminated_SC_EA && terminated_SF_EA;
		}
		
		//output the result of the testing		
		ReportTestStatisticsResultSE(index, tcn_OA, tcn_SC_EA, tcn_SF_EA);

        return true;
	}
	
	public static CoverageMonitor constructCoverageMonitor(
			TestingScriptDefinition tsd, int coverageStrategy) {
		
		CoverageMonitor monitor = new CoverageMonitor();						
		
		switch(coverageStrategy) {
		case ActivityCoverage:
			 constructActivityCoverageMonitor(monitor, tsd);
			 break;
		case BranchCoverage:
			 constructBranchCoverageMonitor(monitor, tsd);
			 break;
		case DUCoverage:
			 constructDUCoverageMonitor(monitor, tsd);
			 break;
		case PathCoverage:
			 constructPCMonitorFactory(monitor, tsd);
			 break;
		}
		
		return monitor;
	}
	
	private static void constructPCMonitorFactory(CoverageMonitor monitor, TestingScriptDefinition tsd) {
		if(savedmonitor!=null) 
		    loadPathCoverageMonitor(monitor, savedmonitor);
		else
		
		if(selectedServiceName!=null) 
		   constructPartialPathCoverageMonitor(monitor, tsd, selectedServiceName);
		else
			constructPathCoverageMonitor(monitor, tsd);
	}

	public static void constructPathCoverageMonitor(CoverageMonitor monitor,
			TestingScriptDefinition tsd) {		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		
		ServiceTestCase testcase = loadServiceTestCase(tsd,testcase_policy);
		services.add(testcase.getService());
		
		ArrayList<PathCoverageEventInterface> eventInterfaces = 
			new ArrayList<PathCoverageEventInterface>();
		for(AbstractService service: services) {
			PathCoverageEventInterface pcei = new PathCoverageEventInterface();
			pcei.deriveEventInterface(service);
			eventInterfaces.add(pcei);
		}
		
		String choName = tsd.getChoreography().getName();
		HashMap<String, ArrayList<String>> ports = LoadServiceScript.loadChoreography(choName);
		
		HashMap<String, ArrayList<Task>> porttasks = 
			new HashMap<String, ArrayList<Task>>();
		for(AbstractService service: services)
		    porttasks.put(service.getName(), getPortTasks(service));		
		
		Observation testing_observation, global_observation;
		//global_observation = Observation.constructPathCoverageObservation(eventInterfaces, ports, porttasks);
		global_observation = Observation.constructStaticGlobalPathCoverageObservation(eventInterfaces,ports, porttasks);
		
		if(isBaseline)
		    testing_observation = Observation.constructLocalPathCoverageObservation(eventInterfaces);
		else
			testing_observation = global_observation;
				
		monitor.setTestingObservation(testing_observation);
		monitor.setGlobalObservation(global_observation);
	}
	
	public static void loadPathCoverageMonitor(CoverageMonitor monitor,
			String filename) {		
		Observation testing_observation = null;
		Observation global_observation = null;
		
		try{
			ObjectInputStream oi = new ObjectInputStream(new FileInputStream(filename));
			global_observation = (Observation) oi.readObject();
			
			ArrayList<ObservationUnit> unitlist = global_observation.getObservationUnit();
			for(ObservationUnit unit: unitlist)
				unit.rollback();
			
			if(isBaseline) {
			    testing_observation = (Observation) oi.readObject();
			    filterObservation(testing_observation, global_observation);
			} else
				testing_observation = global_observation; 
			oi.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		monitor.setTestingObservation(testing_observation);
		monitor.setGlobalObservation(global_observation);
	}
	
	private static void filterObservation(Observation testing_observation,
			Observation global_observation) {
		
		ArrayList<ObservationUnit> unitlist = testing_observation.getObservationUnit();
		ArrayList<ObservationUnit> gulist = global_observation.getObservationUnit();
		ArrayList<ObservationUnit> filter = new ArrayList<ObservationUnit>();
		for(ObservationUnit unit: unitlist) {
		   HashMap<String, ArrayList<String>> paths = ((EventSequenceObservationUnit) unit).getSequence();
		   Set<String> sn_list = paths.keySet();
		   for(String sn: sn_list) {
			   ArrayList<String> ph = paths.get(sn);
			   if(NotCover(gulist, ph,testing_observation, unit)) 
				   filter.add(unit);
		   }
		}
		
		unitlist.removeAll(filter);
		
	}

	private static boolean NotCover(ArrayList<ObservationUnit> gulist,
			ArrayList<String> ph, Observation testing_observation,ObservationUnit testing_unit) {
		
		for(ObservationUnit unit: gulist) {
			HashMap<String, ArrayList<String>> paths = ((EventSequenceObservationUnit) unit).getSequence();
			   Set<String> sn_list = paths.keySet();
			   for(String sn: sn_list) {
				   ArrayList<String> path = paths.get(sn);
				   if(isSamePath(path, ph)) {
					   ((LocalObservation)testing_observation).addMapping(sn, testing_unit);
					   return false;
				   }
			   }
		}
		
		return true;
	}

	public static void savePathCoverageMonitor(CoverageMonitor monitor,
			String filename) {		
		Observation testing_observation = monitor.getTestingObservation();
		Observation global_observation = monitor.getGlobalObservation();
		
		try{
			ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(filename));
			oo.writeObject(global_observation);
			oo.writeObject(testing_observation);			
			oo.close();
		} catch(Exception e) {
			e.printStackTrace();
		}				
	}
	
	public static void constructPartialPathCoverageMonitor(CoverageMonitor monitor,
			TestingScriptDefinition tsd, ArrayList<String> selectedServices) {		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		
		ServiceTestCase testcase = loadServiceTestCase(tsd,testcase_policy);
		services.add(testcase.getService());
		
		ArrayList<PathCoverageEventInterface> eventInterfaces = 
			new ArrayList<PathCoverageEventInterface>();
		for(AbstractService service: services) {
			String sn = service.getName();
			if(!selectedServices.contains(sn)) continue;
			
			PathCoverageEventInterface pcei = new PathCoverageEventInterface();
			pcei.deriveEventInterface(service);
			eventInterfaces.add(pcei);
		}
		
		String choName = tsd.getChoreography().getName();
		HashMap<String, ArrayList<String>> ports = LoadServiceScript.loadChoreography(choName);
		filterPorts(ports, selectedServices);
		
		HashMap<String, ArrayList<Task>> porttasks = 
			new HashMap<String, ArrayList<Task>>();
		for(AbstractService service: services) {
			String sn = service.getName();
			if(!selectedServices.contains(sn)) continue;
			
		    porttasks.put(sn, getPartialPortTasks(service, selectedServices, ports));
		}
		
		Observation testing_observation, global_observation;
		global_observation = Observation.constructPathCoverageObservation(eventInterfaces, ports, porttasks);
		//global_observation = Observation.constructStaticGlobalPathCoverageObservation(eventInterfaces);
		
		if(isBaseline)
		    testing_observation = constructPartialLocalPathCoverageObservation(global_observation, eventInterfaces);
		else
			testing_observation = global_observation;
				
		monitor.setTestingObservation(testing_observation);
		monitor.setGlobalObservation(global_observation);
	}	
	

	private static Observation constructPartialLocalPathCoverageObservation(
			Observation global_observation, ArrayList<PathCoverageEventInterface> eventInterfaces) {

        
		LocalObservation observation = new LocalObservation();
		HashMap<ObservationUnit, String> mapping = new HashMap<ObservationUnit, String>();
		
		ArrayList<String> exposedEvent = Observation.getExposedEvents(eventInterfaces);
		
		
		HashMap<String, ArrayList<ArrayList<String>>> buf = new HashMap<String, ArrayList<ArrayList<String>>>();
		ArrayList<ObservationUnit> ulist = global_observation.getObservationUnit();
		for(ObservationUnit unit:ulist) {
			HashMap<String, ArrayList<String>> sequence = ((EventSequenceObservationUnit)unit).getSequence();
			Set<String> snlist = sequence.keySet();
			for(String sn: snlist) {
				ArrayList<String> path = sequence.get(sn);
				addLocalPath(buf, sn, path);
			}
			
		}
		
		Set<String> snlist = buf.keySet();
		for(String sn: snlist) {
			ArrayList<ArrayList<String>> pathlist = buf.get(sn);
			for(ArrayList<String> ph: pathlist) {
				HashMap<String, ArrayList<String>> history = new HashMap<String, ArrayList<String>>();
				history.put(sn, ph);
				
				EventSequenceObservationUnit unit = new EventSequenceObservationUnit();
				unit.setSequence(history);								
				unit.setExposedEvents(exposedEvent);
				
                mapping.put(unit, sn);
				observation.addObservationUnit(unit);
			}
		}
		
		return observation;
	}

	private static void addLocalPath(
			HashMap<String, ArrayList<ArrayList<String>>> buf, String sn,
			ArrayList<String> path) {
		
		ArrayList<ArrayList<String>> pathlist = buf.get(sn);
		if(pathlist == null) {
			pathlist = new ArrayList<ArrayList<String>>();
			buf.put(sn, pathlist);
		}
		
		boolean existing = false;
		for(ArrayList<String> ph: pathlist) {
			if(isSamePath(ph, path)) {
				existing = true;
				break;
			}
		}
		
		if(!existing) pathlist.add(path);
		
	}

	private static boolean isSamePath(ArrayList<String> ph,
			ArrayList<String> path) {

		if(ph.size()!=path.size())
		  return false;
		
		for(int i=0;i<ph.size();i++)
			if(!ph.get(i).equals(path.get(i))) return false;
		
		return true;
	}

	private static void filterPorts(HashMap<String, ArrayList<String>> ports,
			ArrayList<String> selectedServices) {
		
		for(String sn1: selectedServices) {
			ArrayList<String> ps1 = ports.get(sn1);
			if(ps1!=null) {
				ArrayList<String> notshared = new ArrayList<String>();
				for(String pv: ps1) {
					boolean shared = false;
					for(String sn2: selectedServices) {
						if(sn1.equals(sn2)) continue;
						ArrayList<String> ps2 = ports.get(sn2);
						if(ps2.contains(pv)) {
							shared = true;
							break;
						}
					}
					
					if(!shared) notshared.add(pv);
				}
				
				ps1.removeAll(notshared);
			}
		}
		
		Set<String> keys = ports.keySet();
		ArrayList<String> removeKeys = new ArrayList<String>();
		removeKeys.addAll(keys);
		removeKeys.removeAll(selectedServices);
		
		for(String key: removeKeys) {
			if(!selectedServices.contains(key))
				ports.remove(key);
		}
	}

	private static ArrayList<Task> getPartialPortTasks(AbstractService service,
			ArrayList<String> selectedServices,
			HashMap<String, ArrayList<String>> ports) {
		ArrayList<Task> result = new ArrayList<Task>();
        ArrayList<Task> tasks = service.getTasks();
        for(Task task: tasks) {
        	if(task.isPort()) { 
        		
        		ArrayList<PortVariable> pvs = task.getPortexp().getPortVariables();
        		ArrayList<String> mps = ports.get(service.getName());
        		ArrayList<PortVariable> filteredpvs = new ArrayList<PortVariable>();
        		
        		for(PortVariable pv: pvs)
        			if(!mps.contains(pv.getName())) filteredpvs.add(pv);
        		pvs.removeAll(filteredpvs);
        	
        		if(!pvs.isEmpty()) {
        			String revised_effect = task.isSend()? "send":"receive";
        			for(PortVariable pv: pvs)
        				revised_effect += " " + pv.getName();
        			task.setEffect(revised_effect);
        			
        		    result.add(task);
        		}
        	}
        }
		
		return result;
	}

	private static ArrayList<Task> getPortTasks(AbstractService service) {
        ArrayList<Task> result = new ArrayList<Task>();
        ArrayList<Task> tasks = service.getTasks();
        for(Task task: tasks) {
        	if(task.isPort()) 
        		result.add(task);        	
        }
		
		return result;
	}

	private static void constructDUCoverageMonitor(CoverageMonitor monitor,
			TestingScriptDefinition tsd) {
        
		ArrayList<AbstractService> services = loadTestingService(tsd);
		ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
		services.add(m_testcase.getService());//add the test case service into the service list
		
		ArrayList<DUCoverageEventInterface> eventInterfaces = 
			new ArrayList<DUCoverageEventInterface>();
		for(AbstractService service: services) {
			DUCoverageEventInterface ducei = new DUCoverageEventInterface();
			ducei.deriveEventInterface(service);
			eventInterfaces.add(ducei);
		}
		
		Observation testing_observation, global_observation;
		global_observation = Observation.constructGlobalDUCoverageObservation(eventInterfaces, services);
		
		if(isBaseline)
			testing_observation = Observation.constructLocalDUCoverageObservation(eventInterfaces);
		else
			testing_observation = global_observation;
		
		monitor.setTestingObservation(testing_observation);		
		monitor.setGlobalObservation(global_observation);
	}

	private static void constructBranchCoverageMonitor(CoverageMonitor monitor,
			TestingScriptDefinition tsd) {
		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		
		ArrayList<BranchCoverageEventInterface> eventInterfaces = 
			new ArrayList<BranchCoverageEventInterface>();
		for(AbstractService service: services) {
			BranchCoverageEventInterface bcei = new BranchCoverageEventInterface();
			bcei.deriveEventInterface(service);
			eventInterfaces.add(bcei);
		}
		
		Observation observation = Observation.constructBranchCoverageObservation(eventInterfaces);
		monitor.setTestingObservation(observation);		
		monitor.setGlobalObservation(observation);	
		
	}

	private static void constructActivityCoverageMonitor(
			CoverageMonitor monitor, TestingScriptDefinition tsd) {
		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		
		ArrayList<ActivityCoverageEventInterface> eventInterfaces = 
			new ArrayList<ActivityCoverageEventInterface>();
		for(AbstractService service: services) {
			ActivityCoverageEventInterface acei = new ActivityCoverageEventInterface();
			acei.deriveEventInterface(service);
			eventInterfaces.add(acei);
		}
		
		Observation observation = Observation.constructActivityCoverageObservation(eventInterfaces);
		monitor.setTestingObservation(observation);		
		monitor.setGlobalObservation(observation);
		
	}

	private static void setTerminateFlags () {
		String filename = getFinishFlagFileName();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(filename);
			writer.close();
		} catch(Exception e) {
			
		}
	}
	
	private static void copyResults() {
		copyComputationResult();
		copySavedParameters();		
	}
	
	private static void copyResultsSE() {
		copyComputationResult();
		copySavedParametersSE();		
	}
	
	private static void copySavedParametersSE() {
		//String tmp_filename = getCompleteFileName();
		String des_filename = getSaveParameterFileName();
		//copyFiles(tmp_filename, des_filename, false);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(des_filename));
            String line = (workIndex+1) + "   " + workTerminateIndex;
            writer.write(line);
            writer.close();
        } catch(Exception e ) {}
	}

	private static void copySavedParameters() {
		//String tmp_filename = getCompleteFileName();
		String des_filename = getSaveParameterFileName();
		//copyFiles(tmp_filename, des_filename, false);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(des_filename));
            String line = workIndex + "   " +mutationIndex;
            writer.write(line);
            writer.close();
        } catch(Exception e ) {}
	}

	private static void copyComputationResult() {

        if(needCopyFiles!=null) 
            for(String tmp_filename: needCopyFiles) {
		        String des_filename = getCopyResultFileName(tmp_filename);
	            copyFiles(tmp_filename, des_filename, true);
            }
	}
	
	public static void copyFiles(String source, String destination, boolean append) {	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(source));			
			ArrayList<String> contents = new ArrayList<String>();
			String line = reader.readLine();
			while(line!=null) {
				contents.add(line);
				line = reader.readLine();
			}
			reader.close();
						
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination, append));
			for(String row: contents) {
				writer.write(row);
				writer.newLine();
			}
			writer.close();	
		} catch(Exception e) {
			
		}
	}

	private static void ReportTestStatisticsResultSE(int index, int tnc_OA,
			int tnc_SC_EA, int tnc_SF_EA) {
		
		String filename = getTestResultFileName();		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content = formatString("" + index) + formatString("" + tnc_OA) 
			               + formatString("" + tnc_SC_EA) + formatString("" + tnc_SF_EA);
			writer.write(content);
			writer.newLine();
			writer.close();

            if(needCopyFiles!=null && !needCopyFiles.contains(filename)) needCopyFiles.add(filename);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	private static void ReportTestStatisticsResult(int mt_index, double coverage,
			boolean faultdetected) {
		
		String filename = getTestResultFileName();		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content = formatString(""+mt_index) + formatDouble(coverage) + formatBoolean(faultdetected);
			writer.write(content);
			writer.newLine();
			writer.close();

            if(needCopyFiles!=null && !needCopyFiles.contains(filename)) needCopyFiles.add(filename);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}	

	public static String formatDouble(double coverage) {
		if(Math.abs(coverage)<0.001) 
			return formatString("0");
		
		String value = ""+coverage;
		int length = value.length()>6? 6: value.length();
		value = value.substring(0, length);
		return formatString(value);
	}

	public static String formatString(String value) {
		String result = value;
		for(int i=value.length();i<10;i++)
			result += " ";
		
		return result;
	}

	private static String formatBoolean(boolean faultdetected) {

		if(faultdetected)
		   return formatString("1");
		
		return formatString("0");
	}

	public static TestingScriptDefinition loadTestingScript(String filename) {
		 try {
			 FileInputStream input = new FileInputStream(filename);			
			 return XMLProcessing.unmarshal(TestingScriptDefinition.class, input);
		 } catch(Exception e) {
			 e.printStackTrace();
		 }
		 
		 return null;
	}
	
	public static ArrayList<AbstractService> loadTestingService(TestingScriptDefinition tsd) {
		ServicesType sts = tsd.getServices();
		assert(sts!=null);
		
		List<String> serviceNames = sts.getServiceName();
		assert(serviceNames!=null);
		
		ArrayList<AbstractService> result = new ArrayList<AbstractService>();
		for(String name: serviceNames) {
			AbstractService service = LoadServiceScript.loadService(name);
			assert(service!=null);
			
			result.add(service);
		}
		
		return result;
	}
	
	public static ServiceTestCase loadServiceTestCase(TestingScriptDefinition tsd, int strategy) {
	
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);
		
		ServiceTestCase testcase;
		switch(strategy) {
		case ServiceTestCase.RANDOM:
		case ServiceTestCase.CONSTRAINT:
		case ServiceTestCase.HYBRID:
		default:
			testcase = new RandomServiceTestCase(service);
			testcase.initializeTestCase();
		}
		
		return testcase;
	}
	
	public static Choreography loadChoreography(TestingScriptDefinition tsd, ArrayList<AbstractService> services) {
		ChoreographyType  ct = tsd.getChoreography();
		assert(ct!=null);
		
		String filename = ct.getName();
        HashMap<String, ArrayList<String>> ports = LoadServiceScript.loadChoreography(filename);
        
        Choreography cho = new Choreography();
        for(AbstractService service:services)
            cho.bind(service.getInstanceID(), ports.get(service.getName()));
        
        return cho;
	}
	
	private static ArrayList<String[]> loadMutationList(TestingScriptDefinition tsd) {
		MutationsType mutt; 
		
		String filename;
		if(isInconsistencyFault) 
			mutt = tsd.getInconsistencyMutations();			
		else 
			mutt = tsd.getMutations();
		
		assert(mutt!=null);
		filename = mutt.getCategoryName();					
		assert(filename!=null);
				
		//String completeFileName = getCompleteFileName(); 
		
		ArrayList<String[]> result = new ArrayList<String[]>();
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
		
			while(line!=null) {
				if(!line.isEmpty()) {
					String[] conts = line.split("\\s+");
					result.add(conts);
				}
				
				line = reader.readLine();
			}
			reader.close();				
		
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		return result;
	}		

	private static String getCompleteFileName() {		
		return outdir + "/" + appendix +"_Complete_"+ approach +".txt";
	}
	
	private static String getSaveParameterFileName() {
		return copyOutDir + "/params/" + coreID +"_Complete_"+ workTerminateIndex +".txt";
	}
	
	private static String getTestResultFileName() {		
		return outdir + "/" + appendix + "_" + approach + ".txt";
	}
	
	private static String getCopyResultFileName(String filename) {

        int index = filename.lastIndexOf('/');
        String tfn = filename.substring(index+1);
		return copyOutDir + "/results/" + tfn;		
	}
	
	private static String getFinishFlagFileName() {
		return copyOutDir + "/flags/" + coreID + workTerminateIndex;
	}

	private static void loadMutation(String filename, ArrayList<AbstractService> services) {
					
		try {
			FileInputStream input = new FileInputStream(filename);			
			MutationDefinition md = XMLProcessing.unmarshal(MutationDefinition.class, input);
			assert(md!=null);
			
			String serviceName = md.getServiceName();
			String mutationName = md.getMutationName();
			MutationType mt = md.getType();
			String expression = md.getContents();
			assert(serviceName!=null);
			assert(mutationName!=null);
			assert(mt!=null);
			
			if(mt.equals(MutationType.TASK_MUTATION)) {
				Task task = getServiceTask(services, serviceName, mutationName);
				assert(task!=null);
				
				//update the task with the mutation
				task.setEffect(expression);
				
			} else 
				
			if(mt.equals(MutationType.TRANSITION_MUTATION)) {
				Transition transition = getServiceTransition(services, serviceName, mutationName);
				assert(transition!=null);
				
				//update the transition with mutation
				transition.getGuard().setGuard(expression);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	private static Transition getServiceTransition(
			ArrayList<AbstractService> services, String serviceName,
			String mutationName) {
		
		for(AbstractService service: services) {
			if(serviceName.equals(service.getName())) {
				String tranName = getTransName(mutationName);
				return service.getTransitionbyName(tranName);
			}
		}
		
		return null;
	}


	private static Task getServiceTask(ArrayList<AbstractService> services,
			String serviceName, String mutationName) {
		
		for(AbstractService service: services) {
			if(serviceName.equals(service.getName())) {
				String tkName = getTaskName(mutationName);
				return service.getTaskbyName(tkName);
			}
		}
		
		return null;
	}
	
	private static String getTaskName(String mutationName) {
		int index = mutationName.lastIndexOf("_");
		return mutationName.substring(index+1);		
	}

	private static String getTransName(String mutationName) {
		int index = mutationName.lastIndexOf("_");
		return mutationName.substring(index+1);		
	}
	
	public static Oracle loadOracle(TestingScriptDefinition tsd) {
		OracleType ot = tsd.getOracle();
		assert(ot!=null);
		String filename = ot.getName();
		assert(filename!=null);
		
		return new Oracle(filename);
	}

	
	//main testing procedure
	public static void filterMutations(String[] args) {
		
		//0. handle input parameter
        boolean validParameter = ServiceTesting.handleInputParameter(args);  
		
        if(validParameter) {
        	//1. load testing script		
        	TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript_filename);
		
        	//2. load test oracle
        	Oracle m_oracle = ServiceTesting.loadOracle(tsd);
		
        	//3. load mutation category
        	ArrayList<String[]> mutations = ServiceTesting.loadMutationList(tsd);
		
        	//4. test each mutation		
        	for(String mt[]: mutations) {
        		boolean found = ServiceTesting.filterOneMutation(tsd, mt, m_oracle);
        		
        		if(!found)         		
        		   recordUnDetectedMutation(mt);
        	}
        }		
	}
	
	private static void recordUnDetectedMutation(String[] mts) {
		String filename = getUnDetectedMutationFileName();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			for(String mt: mts) 
			   writer.write(mt + "   ");			   		
			writer.newLine();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getUnDetectedMutationFileName() {		
		return outdir + "/UndetectedMutations.txt";
	}
	
	private static boolean filterOneMutation(TestingScriptDefinition tsd, String[] mts,
			Oracle m_oracle) {
				
		int testcaseNum = 0;		
		
		while(testcaseNum<test_count) {
			//1. load services
			ArrayList<AbstractService> services = loadTestingService(tsd);
		
			//2. load mutation into services
			for(String mt: mts)
			   loadMutation(mt, services);
		
			//3. load test case
			ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
			services.add(m_testcase.getService());//add the test case service into the service list
		
			//4. load choreography		
			Choreography cho = loadChoreography(tsd, services);
			
			//5. register service choreography
	        engine.getEngine().getServiceCorrelation().register(cho);
	        //System.out.println();
	        
	        //6. register coverage_monitor and testing_monitor	        
	        TestingMonitor testing_monitor = new TestingMonitor();
	        testing_monitor.register(services);
	        testing_monitor.registerCompleteConditions(m_testcase.getService());//use test case to indicate whether the service is complete
		    
	        //7. execute services
	        for(AbstractService service: services)
		       engine.getEngine().executeService(service);
		
	        //wait the services complete
	        int time = 50;
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
	        	
	        //calculate the statistic results	        	        
	        testcaseNum++;
            if(!m_oracle.checkOracle(services))
	        	return true;
	        
	        //garbage collecting for the services	        
	        engine.getEngine().getServiceCorrelation().unregister(cho);	        
	        engine.getEngine().unregister(services);
	        testing_monitor.gabarageCollection();
		}	
		
		return false;
	}	

}

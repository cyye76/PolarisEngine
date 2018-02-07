package ServiceTesting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;

import scripts.TestingScript.TestCaseType;
import scripts.TestingScript.TestingScriptDefinition;
import Deployment.LoadServiceScript;
import Service.AbstractService;
import Service.Choreography;
import ServiceTesting.ConstraintSolver.CSLVariable;
import ServiceTesting.ConstraintSolver.MyConstraint;
import ServiceTesting.InformationLeakage.InformationLeakage;
import ServiceTesting.Monitoring.CoverageMonitor;
import ServiceTesting.Monitoring.TestingMonitor;
import ServiceTesting.TestCase.ConstraintServiceTestCase;
import ServiceTesting.TestCase.HybridServiceTestCase;
import ServiceTesting.TestCase.ServiceTestCase;
import engine.engine;

public class TestCaseExp {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TestCaseExp tse = new TestCaseExp();
		if(tse.handleParameter(args)) {		
			
			//start the engine
			engine.getEngine().startEngine();
			
			TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(tse.testscript);
			
			tse.testOneService(tsd);
			
			//close the engine
			try {
	    		Thread.sleep(10000);
	    	} catch(Exception e) {}	
			engine.getEngine().closeEngine();						
		}
	}
	
    public static void SCIRun(String[] args, ArrayList<String> copyfiles) {
		
		TestCaseExp tse = new TestCaseExp();
		tse.copyfiles = copyfiles;
		if(tse.handleParameter(args)) {							
			TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(tse.testscript);			
			tse.testOneService(tsd);											
		}
	}
    
	//args[0] testscript
	//args[1] maxTestCaseNum;
	//args[2] testcase_policy
	//args[3] coverageStrategy
	//args[4] outdir
	//args[5] appendix
	private boolean handleParameter(String[] args) {
		if(args.length>=6) {
			testscript = args[0];
			maxTestCaseNum = new Integer(args[1]);
			testcase_policy = new Integer(args[2]);
			coverageStrategy = new Integer(args[3]);
			outdir = args[4];
			appendix = args[5];
			
			if(args.length>=7) {
				//Config.getConfig().variableDomain = new Integer(args[6]);
				exposedPercentage = new Double(args[6]);
			}
			return true;
		}

		return false;
	}

	private int maxTestCaseNum = 200;
	private int testcase_policy;
	private String testscript;
	private double exposedPercentage=1;
	
	private ArrayList<ServiceTestCase> testcasepool = new ArrayList<ServiceTestCase>();
	private void generateTestcases(ArrayList<AbstractService> services, CoverageMonitor coverage_monitor, long[] gts) {
		TestingScriptDefinition tsd = ServiceTesting.loadTestingScript(testscript);
		constraint_list = coverage_monitor.getConstraints(coverageStrategy, testscript);
		int[] weight = coverage_monitor.getConstraintWeights();
		
		if(testcase_policy==1)//random 
			generateRandomTestcases(tsd, gts);
		
		if(testcase_policy==2) //constraint
			generateConstraintTestcases(tsd, services, weight, gts);
		
		if(testcase_policy==3) //hybrid 			
			generateHybridTestcases(tsd, services, weight, gts);
					
	}
	
	private void generateHybridTestcases(TestingScriptDefinition tsd, ArrayList<AbstractService> services, int[] weight, long[] gts) {
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);						

		
		HybridServiceTestCase hst = new HybridServiceTestCase(service);
		hst.setConstraint_list(constraint_list);
		hst.addServiceSchema(service.getName(), service.getVariableSchema());
			
		for(AbstractService as: services)
			hst.addServiceSchema(as.getName(), as.getVariableSchema());
		
		hst.generateTestCasePool2(testcasepool, maxTestCaseNum, tsd, weight, gts);
	}

	private void generateConstraintTestcases(TestingScriptDefinition tsd, ArrayList<AbstractService> services, int[] weight, long[] gts) {
		/*
		long start = System.currentTimeMillis();
		for(int i=0;i<maxTestCaseNum;i++) {
			ServiceTestCase tc = loadConstraintTestCase(tsd, services);
			testcasepool.add(tc);
			long current = System.currentTimeMillis();
			gts[i] = current - start;
		}*/
		
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);

		
		ConstraintServiceTestCase ctc = new ConstraintServiceTestCase(service);
		ctc.setConstraint_list(constraint_list);
		ctc.addServiceSchema(service.getName(), service.getVariableSchema());
			
		for(AbstractService as: services)
			ctc.addServiceSchema(as.getName(), as.getVariableSchema());
				
		ctc.generateTestCasePool(tsd, testcasepool, weight, maxTestCaseNum, gts);		
		
	}

	private void generateRandomTestcases(TestingScriptDefinition tsd, long[] gts) {
		long start = System.currentTimeMillis();
		for(int i=0;i<maxTestCaseNum;i++) {
			ServiceTestCase tc = ServiceTesting.loadServiceTestCase(tsd, ServiceTestCase.RANDOM);
			testcasepool.add(tc);
			long current = System.currentTimeMillis();
			gts[i] = current - start;
		}		
	}	

	private void testOneService(TestingScriptDefinition tsd) {	
		long start = System.currentTimeMillis(); 	

		//0. create monitor	
		ServiceTesting.isBaseline = false;
		CoverageMonitor coverage_monitor = ServiceTesting.constructCoverageMonitor(tsd, coverageStrategy);
		InformationLeakage leakage = new InformationLeakage();
		leakage.setNeedExposed(testcase_policy!=ServiceTestCase.RANDOM);
		coverage_monitor.setExposedPercentage(exposedPercentage);
			
		boolean generatedTestCases = false;
		int testcaseNum = 1;	
		double coverage = 0;	
		long testGT = 0;
		
		double[] coverage_buffer = new double[maxTestCaseNum];
		long[] duration_buffer = new long[maxTestCaseNum];
		long[] gts = new long[maxTestCaseNum];
	
		while(testcaseNum<=maxTestCaseNum) {
			//1. load services
			ArrayList<AbstractService> services = ServiceTesting.loadTestingService(tsd);
			
			//2. load test case	
			if(!generatedTestCases) {
				long gt_start = System.currentTimeMillis();
				generateTestcases(services, coverage_monitor, gts);
				generatedTestCases = true;
				long gt_end = System.currentTimeMillis();
				testGT = gt_end - gt_start;
			}
			
			//ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy, services, coverage_monitor);
			ServiceTestCase m_testcase = testcasepool.get(testcaseNum-1);
			if(m_testcase==null) break;
			services.add(m_testcase.getService());//add the test case service into the service list
	
			//3. load choreography		
			Choreography cho = ServiceTesting.loadChoreography(tsd, services);
		
			//4. register service choreography
			engine.getEngine().getServiceCorrelation().register(cho);	        
        
			//5. register coverage_monitor and testing_monitor
			coverage_monitor.register(services);
			TestingMonitor testing_monitor = new TestingMonitor();
			testing_monitor.register(services);
			testing_monitor.registerCompleteConditions(m_testcase.getService());//use test case to indicate whether the service is complete
	    
			//6. execute services
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
        
			//7.not terminated, force it terminate immediately
			if(!testing_monitor.terminated) {
				System.out.println("Time out!");	        	
			}
        
			//terminate all the remaining running services
			ArrayList<AbstractService> running_services = testing_monitor.getRemainingServices(services);
			engine.getEngine().terminateServices(running_services);
        	
			//calculate the statistic results	        	        			
			coverage_monitor.matchTestingObservation();
			coverage = coverage_monitor.calculateRealCoverage();
            long duration = System.currentTimeMillis() - start;
            //double ls_state = 0, ls_transition = 0;
            
            //calculate information leakage
            leakage.addEvents(coverage_monitor.getObservationEvents());
            //ls_state = leakage.getInformationLeakage();
            
            int index =testcaseNum-1;             
            coverage_buffer[index] = coverage;
            duration_buffer[index] = duration;
            //ReportTestStatisticsResult(testcaseNum, coverage, duration, ls_state, ls_transition);
        
			testcaseNum++;						
			
			//remove observation events
			coverage_monitor.clearObservationEvents();
        
			//garbage collecting for the services
			coverage_monitor.unregister(services);
			engine.getEngine().getServiceCorrelation().unregister(cho);	        
			engine.getEngine().unregister(services);
			testing_monitor.gabarageCollection();						
		}			    
		
		//output results
		double[] ls_state = leakage.getInformationLeakage();
		for(int i=0;i<maxTestCaseNum;i++) {
			//ReportTestStatisticsResult(i+1, coverage_buffer[i], duration_buffer[i], ls_state[i], testGT/1000.0);
			ReportTestStatisticsResult(i+1, coverage_buffer[i], duration_buffer[i], ls_state[i], gts[i]/1000.0);
		}
    
	}

	private ArrayList<ArrayList<MyConstraint>> constraint_list = null;
	private ServiceTestCase loadServiceTestCase(
			TestingScriptDefinition tsd, int tpolicy, 
			ArrayList<AbstractService> services, CoverageMonitor coverage_monitor) {

		if(tpolicy == ServiceTestCase.RANDOM) 
			return ServiceTesting.loadServiceTestCase(tsd, tpolicy);
				
		if(constraint_list == null) {
			constraint_list = coverage_monitor.getConstraints(coverageStrategy, testscript);
		}				
		
		if(tpolicy == ServiceTestCase.CONSTRAINT)
			return loadConstraintTestCase(tsd, services);
		
		if(tpolicy == ServiceTestCase.HYBRID) {			
			return loadHybridTestCase(tsd, services);
		}

		return null;
	}	

	private HybridServiceTestCase m_hst = null; 
	private ServiceTestCase loadHybridTestCase(TestingScriptDefinition tsd,
			ArrayList<AbstractService> services) {
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);						

		if(m_hst==null) {
			m_hst = new HybridServiceTestCase(service);
			m_hst.setConstraint_list(constraint_list);
			m_hst.addServiceSchema(service.getName(), service.getVariableSchema());
			
			for(AbstractService as: services)
				m_hst.addServiceSchema(as.getName(), as.getVariableSchema());
		}
		
		//if(m_hst.nextTestCase(service)) return m_hst;
		//if(m_hst.nextTestCase1(service)) return m_hst;
		if(m_hst.nextTestCase2(service)) return m_hst;
		
		return null;
	}

	private ConstraintServiceTestCase m_ctc = null; 
	private ServiceTestCase loadConstraintTestCase(TestingScriptDefinition tsd,
			ArrayList<AbstractService> services) {
		
		TestCaseType tct = tsd.getTestCase();
		assert(tct!=null);
		
		String filename = tct.getName();
		assert(filename!=null);
		
		AbstractService service = LoadServiceScript.loadService(filename);
		assert(service!=null);

		if(m_ctc==null) {
			m_ctc = new ConstraintServiceTestCase(service);
			m_ctc.setConstraint_list(constraint_list);
			m_ctc.addServiceSchema(service.getName(), service.getVariableSchema());
			
			for(AbstractService as: services)
				m_ctc.addServiceSchema(as.getName(), as.getVariableSchema());
		}
		
		if(m_ctc.nextTestCase(service)) return new ConstraintServiceTestCase(service);
		
		return null;
	}

	private ArrayList<String> copyfiles = null;
	private void ReportTestStatisticsResult(int testcaseNum,
			double coverage, long duration, double ls_state, double ls_transition) {
        
		String filename = getTestResultFileName();
		if(copyfiles!=null && !copyfiles.contains(filename))
			copyfiles.add(filename);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content = formatString(""+testcaseNum) + formatDouble(coverage)  
			                 + formatString(""+duration) + formatDouble(ls_state) + formatDouble(ls_transition);
			writer.write(content);
			writer.newLine();
			writer.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static String formatDouble(double coverage) {
		if(coverage<0.0001) 
			return formatString("0");
		
		String value = ""+coverage;
		int length = value.length()>6? 6: value.length();
		value = value.substring(0, length);
		return formatString(value);
	}

	private static String formatString(String value) {
		String result = value;
		for(int i=value.length();i<10;i++)
			result += " ";
		
		return result;
	}


	private String outdir;
	private String appendix;
	private int coverageStrategy;
	private String getTestResultFileName() {
		int num = (int)(exposedPercentage *  100);
		return outdir + "/" + appendix + "_" + coverageStrategy + "_" + testcase_policy + "_" + num + ".txt";
	}

}

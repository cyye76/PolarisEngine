package ServiceDebugging;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import engine.engine;
import engine.Event.Event;
import Deployment.LoadServiceScript;
import Service.AbstractService;
import Service.Choreography;
import Service.Task;
import Service.Transition;
import ServiceDebugging.FaultLocalization.Encapsulation;
import ServiceDebugging.FaultLocalization.ExecutionRecord;
import ServiceDebugging.FaultLocalization.FLTimeoutException;
import ServiceDebugging.FaultLocalization.FaultLocalizationAlgorithm;
import ServiceDebugging.FaultLocalization.FaultLocalizationMonitoring;
import ServiceTesting.Monitoring.Oracle;
import ServiceTesting.Monitoring.TestingMonitor;
import ServiceTesting.TestCase.RandomServiceTestCase;
import ServiceTesting.TestCase.ServiceTestCase;
import Utils.TarArchiveFiles;
import Utils.XMLProcessing;
import Configuration.Config;

import scripts.MutationScript.MutationDefinition;
import scripts.MutationScript.MutationType;
import scripts.TestingScript.ChoreographyType;
import scripts.TestingScript.MutationsType;
import scripts.TestingScript.OracleType;
import scripts.TestingScript.ServicesType;
import scripts.TestingScript.TestCaseType;
import scripts.TestingScript.TestingScriptDefinition;


public class ServiceDebugging {		
	
	private static int faultType;
	private static int testcase_policy;	//testcase generation policy	
	private static int maxTestCaseNum;//maximum test case for each mutation
	private static int totalTestCaseNum;//from the parameters
	private static int test_count;//number of rounds for each mutation
	private static String testscript_filename;//test script filename
	private static String appendix; //used to differentiate different runs
	private static String outdir;//output directory
	private static String copyOutDir = null;
	private static long duration=-1;
	private static int mutationIndex = 0;
	private static int mutationIndexTerminate = 0;
	private static int round=0;
	private static int workIndex = 0;
	private static int workTerminateIndex = 0;
	private static int approachIndex = -1;
	private static long totalEventsNum = 0;
	private static String coreID=null;
    private static long start=0;
    private static ArrayList<String> needCopyFiles=null;
    private static int timeout = 60; //default one minute 
    private static SCIFLTimer m_timer=null;
    
	
	public static boolean handleInputParameter(String[] args) {		
		if(args.length<6) {
			System.out.println("Usage: maxTestCaseNum test_count testscript_filename appendix outdir isInconsistencyFault [copyOutDir] [duration] [timeout]");
			return false;
		}
		
		try {			
			initializeTestPolicy();
			
		    maxTestCaseNum = new Integer(args[0]);
		    test_count = new Integer(args[1]);
		    testscript_filename = args[2];
		    appendix = args[3];
		    outdir = args[4];
		    
		    faultType = new Integer(args[5]);
		    
		    if(args.length > 6) copyOutDir = args[6];
		    		    
		    if(args.length>7) 
		    	duration = new Long(args[7]);
		    
		    if(args.length>8) 
		    	timeout = new Integer(args[8]);
		    
		    return true;
		
		}catch(Exception e) {
			System.out.println("Usage: maxTestCaseNum test_count testscript_filename appendix outdir isInconsistencyFault [copyOutDir] [duration] [timeout]");
			return false;
		}
		
	}  

    private static void initializeTestPolicy() {	
    	
    	testcase_policy = ServiceTestCase.RANDOM;//random													
    	
	}       
	
	public static void main(String[] args) {
		//start the engine
		engine.getEngine().startEngine();
		
		start = System.currentTimeMillis();
		
		ServiceDebugging.MyTesting(args);
		
		//close the engine
		try {
    		Thread.sleep(10000);
    	} catch(Exception e) {}	
		engine.getEngine().closeEngine();
	}

	//main testing procedure
	public static void MyTesting(String[] args) { 
			
		//0. handle input parameter
        boolean validParameter = ServiceDebugging.handleInputParameter(args);  
		
        if(validParameter) {
        	//1. load testing script		
        	TestingScriptDefinition tsd = ServiceDebugging.loadTestingScript(testscript_filename);
		
        	//2. load test oracle
        	Oracle m_oracle = ServiceDebugging.loadOracle(tsd); 
		
        	//3. load mutation category
        	ArrayList<String[]> mutations = ServiceDebugging.loadMutationList(tsd, faultType);
		
        	//4. load saveParameters
        	String[] saveParameters = ServiceDebugging.loadSaveParameters();
        	        	
        	if(saveParameters!=null && saveParameters.length>=2) {
        		round = new Integer(saveParameters[0]);
        		mutationIndex = new Integer(saveParameters[1]);
        	}
        	
        	//4. test each mutation
        	for(int i=round;i<test_count;i++) {
        	 
        		while(mutationIndex<mutations.size()) {
        	    	String[] mt = mutations.get(mutationIndex);
        	    	ServiceDebugging.testOneMutation(tsd, mt, m_oracle, mutationIndex);
        			mutationIndex++;
        			        			
        			recordCompleteOne(i + "   " + mutationIndex);        			
        	    }        		        	    
        		
        		mutationIndex = 0;
        	     
        	}
        	
        	//5. copy results if running in sciNet HPC
        	if(copyOutDir!=null) copyResults();
        }
    }
	
	private static void HandleSerialTaskArguments(String[] args) {
		//args[0]:CoreID
		//args[1]:duration
		
		//fix the following parameters
	    //outdir = "/dev/shm";
	    //copyOutDir = "/scratch/j/jacobsen/cyye/FL";
		//maxTestCaseNum = 100;
		
	    outdir = "tmp/tmp1";
	    copyOutDir = "tmp/tmp2";
	    maxTestCaseNum = 100;
	    
	    test_count = 1;
	    	    	    
	    //workIndex = 0;	    
		workIndex = new Integer(args[0]);
		workTerminateIndex = new Integer(args[1]);
		
		coreID = args[2];		
		duration = new Long(args[3]);//172800000;
								
		if(args.length>4)
		   timeout = new Integer(args[4]);
		
		if(args.length>5)
			copyOutDir = args[5];
		
		if(args.length>6) {
			int[][] ltask = loadTasks(args[6]);
			if(ltask!=null) SCIFL.tasks = ltask;
		}
				
		mutationIndex= SCIFL.tasks[workIndex][2];
		mutationIndexTerminate = SCIFL.tasks[workIndex][3];

		
		//If parameters are saved, load them
		String[] saveParameters = ServiceDebugging.loadSaveParameters();
		if(saveParameters!=null && saveParameters.length>=4) {
			workIndex = new Integer(saveParameters[0]);
			mutationIndex = new Integer(saveParameters[1]);
			approachIndex = new Integer(saveParameters[2]);
			mutationIndexTerminate = SCIFL.tasks[workIndex][3];
			totalEventsNum = new Long(saveParameters[3]);
		}  
		
		//set timer
		m_timer = new SCIFLTimer();  
		m_timer.setTimer(duration - 1800000);//in advance of half an hour 
	}  
	
	private static void HandleSerialTaskArgumentsOnly(String[] args, boolean analysis) {
		
		//fix the following parameters
	    outdir = "/dev/shm";
	    copyOutDir = "/scratch/j/jacobsen/cyye/FL";
		
	    //outdir = "tmp/tmp1";
	    //copyOutDir = "tmp/tmp2";
  
		workIndex = new Integer(args[0]);
		workTerminateIndex = new Integer(args[1]);
		
		coreID = args[2];		
		duration = new Long(args[3]);//172800000;
		totalTestCaseNum = new Integer(args[4]);
		maxTestCaseNum = totalTestCaseNum;
								
		if(args.length>5)
		   timeout = new Integer(args[5]);
		
		if(args.length>6)
			copyOutDir = args[6];
		
		if(args.length>7) {
			int[][] ltask = loadTasks(args[7]);
			if(ltask!=null) SCIFL.tasks = ltask;
		}
				
		mutationIndex= SCIFL.tasks[workIndex][2];
		mutationIndexTerminate = SCIFL.tasks[workIndex][3];

		
		//If parameters are saved, load them
		String[] saveParameters = ServiceDebugging.loadSaveParameters();
		if(saveParameters!=null && saveParameters.length>=4) {
			workIndex = new Integer(saveParameters[0]);
			mutationIndex = new Integer(saveParameters[1]);
			if(analysis) {
				  approachIndex = new Integer(saveParameters[2]);
			} else {
			      int testcaseNum = new Integer(saveParameters[2]);
			      if(testcaseNum>0) maxTestCaseNum = testcaseNum;
			}
			mutationIndexTerminate = SCIFL.tasks[workIndex][3];
			totalEventsNum = new Long(saveParameters[3]);
		}  
		
		//set timer
		m_timer = new SCIFLTimer();  
		m_timer.setTimer(duration - 1800000);//in advance of half an hour 
		//m_timer.setTimer(duration - 5400000);//in advance of one and a half hour 
	}  
	 
	/**
	 * Load tasks from a file
	 * @param string
	 * @return
	 */
	public static int[][] loadTasks(String filename) {
		ArrayList<int[]> buffer = new ArrayList<int[]>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();
			String[] conts;
			while(line!=null) {				
				conts = line.split("\\s+");				
				if(conts!=null && conts.length>=4 && !conts[0].startsWith("#")) {
					int[] item = new int[4];
					for(int i=0;i<item.length;i++)
						item[i] = new Integer(conts[i]);
					
					buffer.add(item);					
				}
				
				line = reader.readLine();
			}
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		int[][] result = null;
		if(buffer.size()>0) {
			result = new int[buffer.size()][];
			for(int i=0;i<result.length;i++)
				result[i] = buffer.get(i);				
		}
		
		return result;
	}

	//for running a serial of tasks in scinet hpc
	public static void TestSerialTasks(String[] args) {
		start = System.currentTimeMillis(); 
        needCopyFiles = new ArrayList<String>();
		
		//handle Parameters
		HandleSerialTaskArguments(args);
		
		while(workIndex <= workTerminateIndex &&  workIndex < SCIFL.tasks.length) {						
			
			int cg = SCIFL.tasks[workIndex][0];
			int ft = SCIFL.tasks[workIndex][1];
			
			initializeTestPolicy();
		    testscript_filename = SCIFL.tscripts[cg];		    	    	    	    
	        faultType = ft;
	        appendix = SCIFL.CG[cg] + SCIFL.FT[ft] + coreID;
	        totalEventsNum = SCIFL.TN[cg];
	    
		    boolean workCompleted = MySCITesting();
		    if(!workCompleted)  break;

		    workIndex++; //execute next task
		    
		    if(workIndex < SCIFL.tasks.length) {
				mutationIndex= SCIFL.tasks[workIndex][2];
				mutationIndexTerminate = SCIFL.tasks[workIndex][3];		    	
		    }
		   
		}
		
		//copy results and quit
		if(!needTerminated) copyResults();		
        
		//if all the tasks are finished, set the finish flag
		if(workIndex > workTerminateIndex && !needTerminated) {
			setTerminateFlags();
			if(m_timer!=null) m_timer.abort();//cancel the timer
		}
	}	


	//adapt it for running in SciNet HPC
	private static boolean MySCITesting() {
							        
        //1. load testing script		
        TestingScriptDefinition tsd = ServiceDebugging.loadTestingScript(testscript_filename);
		
        //2. load test oracle
        Oracle m_oracle = ServiceDebugging.loadOracle(tsd);
		
        //3. load mutation category
        ArrayList<String[]> mutations = ServiceDebugging.loadMutationList(tsd, faultType);
		        	
        //4. test each mutation        	 
        //while(mutationIndex < mutations.size()) {
        while(mutationIndex <= mutationIndexTerminate) {
           	String[] mt = mutations.get(mutationIndex);           	
        	boolean completed;
        	Encapsulation eps = null;
        	if(approachIndex>=0 && approachIndex<=70) eps = loadEncapsulation();
        	
            if(eps==null)
        	    completed = testOneMutation(tsd, mt, m_oracle, mutationIndex);
            else
            	completed = testOneMutation(eps, mutationIndex, approachIndex);

            if(!completed) return false;
        	   
            mutationIndex++;
            approachIndex = -1;
            //totalEventsNum = 0;
        			        			
            //recordCompleteOne(workIndex + "   " + mutationIndex + "    " + approachIndex + "   " + totalEventsNum);        	   	
            recordCompleteOne(workIndex + "   " + mutationIndex + "    " + approachIndex + "   " + 0);
        }        		        	    
        
        //mutationIndex=0;
        return true;
    }
	
	//for running a serial of tasks to generate events only in scinet hpc
	public static void TestSerialTasksOnly(String[] args) {
			start = System.currentTimeMillis(); 
	        needCopyFiles = new ArrayList<String>();
			
			//handle Parameters
			HandleSerialTaskArgumentsOnly(args, false);
			
			while(workIndex <= workTerminateIndex &&  workIndex < SCIFL.tasks.length) {						
				
				int cg = SCIFL.tasks[workIndex][0];
				int ft = SCIFL.tasks[workIndex][1];
				
				initializeTestPolicy();
			    testscript_filename = SCIFL.tscripts[cg];		    	    	    	    
		        faultType = ft;
		        appendix = SCIFL.CG[cg] + SCIFL.FT[ft] + coreID;
		        totalEventsNum = SCIFL.TN[cg];
		    
			    boolean workCompleted = MySCITestingOnly();
			    if(!workCompleted)  break;
			    
			    //added on 2016.04.25 to avoid running into the loop again when workcompleted = true and needTerminated = true
			    if(needTerminated) break;

			    workIndex++; //execute next task
			    
			    if(workIndex < SCIFL.tasks.length) {
					mutationIndex= SCIFL.tasks[workIndex][2];
					mutationIndexTerminate = SCIFL.tasks[workIndex][3];		    	
			    }
			   
			}
			
			//copy results and quit
			copyTestingResultsOnly();		
	        
			//if all the tasks are finished, set the finish flag
			if(workIndex > workTerminateIndex && !needTerminated) {
				setTerminateFlags();
				if(m_timer!=null) m_timer.abort();//cancel the timer
			}
		}	
	
	//for running a serial of tasks to analyze events only in scinet hpc
	public static void analyzeSerialTasksOnly(String[] args) {
		start = System.currentTimeMillis(); 
		needCopyFiles = new ArrayList<String>();
				
		//handle Parameters
		HandleSerialTaskArgumentsOnly(args, true);
				
		while(workIndex <= workTerminateIndex &&  workIndex < SCIFL.tasks.length) {						
					
			int cg = SCIFL.tasks[workIndex][0];
			int ft = SCIFL.tasks[workIndex][1];
					
			//initializeTestPolicy();
		    testscript_filename = SCIFL.tscripts[cg];		    	    	    	    
			faultType = ft;
			appendix = SCIFL.CG[cg] + SCIFL.FT[ft] + coreID;
			totalEventsNum = SCIFL.TN[cg];
			    
		    boolean workCompleted = MySCIAnalysisOnly();
		    if(!workCompleted)  break;

		    workIndex++; //execute next task
				    
			if(workIndex < SCIFL.tasks.length) {
				mutationIndex= SCIFL.tasks[workIndex][2];
				mutationIndexTerminate = SCIFL.tasks[workIndex][3];		    	
			}
				   
		}
				
		//copy results and quit
		copyResults();		
		        
		//if all the tasks are finished, set the finish flag
		if(workIndex > workTerminateIndex && !needTerminated) {
			setTerminateFlags();
			if(m_timer!=null) m_timer.abort();//cancel the timer
		}
	}	
	
	//for debugging in scinet
	public static void analyzeSerialTasks4Debug(String[] args) {
		AnalysisInfo info = new AnalysisInfo();
		info.needCopyFiles = new ArrayList<String>();
					
		//handle Parameters
		if(args.length<7) {
			  System.out.println("Invalid parameter list!");
			  return;
		}
		
	    info.outdir = "/scratch/j/jacobsen/cyye/FL/tmp/";
		//info.outdir = "/dev/shm";
	    info.copyOutDir = "/scratch/j/jacobsen/cyye/FL";
		
	    //info.outdir = "tmp/tmp1";
	    //info.copyOutDir = "tmp/tmp2";
  
		info.workIndex = new Integer(args[0]);
		info.workTerminateIndex = new Integer(args[1]);		
		info.coreID = args[2];		
		info.duration = new Long(args[3]);//172800000;
		info.copyOutDir = args[4];
		int[][] ltask = ServiceDebugging.loadTasks(args[5]);
		if(ltask!=null) SCIFL.tasks = ltask;
		int appNum = new Integer(args[6]);	
		info.savedFileName = info.copyOutDir + "/history/" + info.coreID +"_Complete_"+ info.workTerminateIndex +".txt"; 		
					
		//get candidate task list
		int[] tklist = getCandidateTaskList(info.workIndex, info.workTerminateIndex);
		info.savedAnalysis = loadSavedAnalysisParamters(info.savedFileName);
				
		for(int i=0;i<tklist.length;i++) {
			int tkvalue = tklist[i];
			info.tkvalue = tkvalue;
			int appIndex = getAppIndex(tkvalue, info.savedAnalysis);
			if(appIndex >= appNum) continue;
			
			//invoke the analysis from appIndex
			int mtIndex = getMutationIndex(tkvalue);
			int ft = getFaultType(tkvalue);
			info.totalEventsNum = getTotalEventNum(tkvalue);
			info.appendix = getAppendix(tkvalue, info.coreID);
			info.mtIndex = mtIndex;
			TestingScriptDefinition tsd = getTestingScriptDefinition(tkvalue);
			String[] mt = getMutations(tsd, ft, mtIndex);
			
			String filename = getAnalyzedEventsFileName(info);
			if(!isCompleteXMLFile(filename + ".tar.gz", info)) continue;
			String xmlFilename = getEventXMLFileName(filename+ ".tar.gz", info);
			
			boolean completed = analyzeOneMutationOnlyNew(tsd, mt,  appIndex, xmlFilename, info);
		    if(!completed) break;
			
		    /*
			Encapsulation eps = Encapsulation.loadEncapsulationFromSavedEvents(xmlFilename);			
			 //delete the xml file
		     File file = new File(xmlFilename);
		     file.delete();
			
			ArrayList<AbstractService> services = loadTestingService(tsd);
			eps.snapshotServiceInstance(services);
			
			
			ArrayList<ExecutionRecord> erlist = eps.getInstances();
			for(ExecutionRecord er: erlist) {
				  ArrayList<Event> eventlist = er.getAllEvents();
				  for(Event evt: eventlist) {
					   evt.printMessage();
				  }
			}
		
			
			for(String mt1: mt) {
				String mToken = loadMutation(mt1, services);
				eps.addMutation(mToken);
			}*/

			break;
		    //boolean completed = analyzeOneMutationOnlyNew(tsd, mt,  appIndex, xmlFilename, info);
			//if(!completed) break;					
		}											       		
	}		
	
	
	//for running a serial of tasks to analyze events only in scinet hpc
	public static void analyzeSerialTasksOnlyNew(String[] args) {
		AnalysisInfo info = new AnalysisInfo();
		info.needCopyFiles = new ArrayList<String>();
					
		//handle Parameters
		if(args.length<7) {
			  System.out.println("Invalid parameter list!");
			  return;
		}
		
	    info.outdir = "/dev/shm";
	    info.copyOutDir = "/scratch/j/jacobsen/cyye/FL";
		
	    //info.outdir = "tmp/tmp1";
	    //info.copyOutDir = "tmp/tmp2";
  
		info.workIndex = new Integer(args[0]);
		info.workTerminateIndex = new Integer(args[1]);		
		info.coreID = args[2];		
		info.duration = new Long(args[3]);//172800000;
		info.copyOutDir = args[4];
		int[][] ltask = loadTasks(args[5]);
		if(ltask!=null) SCIFL.tasks = ltask;
		int appNum = new Integer(args[6]);	
		info.savedFileName = info.copyOutDir + "/history/" + info.coreID +"_Complete_"+ info.workTerminateIndex +".txt"; 		
					
		//get candidate task list
		int[] tklist = getCandidateTaskList(info.workIndex, info.workTerminateIndex);
		info.savedAnalysis = loadSavedAnalysisParamters(info.savedFileName);
		
		//set timer
		m_timer = new SCIFLTimer(info);  
		m_timer.setTimer(info.duration - 1800000);//in advance of half an hour
		
		for(int i=0;i<tklist.length;i++) {
			int tkvalue = tklist[i];
			info.tkvalue = tkvalue;
			int appIndex = getAppIndex(tkvalue, info.savedAnalysis);
			if(appIndex >= appNum) continue;
			
			//invoke the analysis from appIndex
			int mtIndex = getMutationIndex(tkvalue);
			int ft = getFaultType(tkvalue);
			info.totalEventsNum = getTotalEventNum(tkvalue);
			info.appendix = getAppendix(tkvalue, info.coreID);
			info.mtIndex = mtIndex;
			TestingScriptDefinition tsd = getTestingScriptDefinition(tkvalue);
			String[] mt = getMutations(tsd, ft, mtIndex);
			
			String filename = getAnalyzedEventsFileName(info);
			if(!isCompleteXMLFile(filename + ".tar.gz", info)) continue;
			String xmlFilename = getEventXMLFileName(filename+ ".tar.gz", info);
			
		    boolean completed = analyzeOneMutationOnlyNew(tsd, mt,  appIndex, xmlFilename, info);
			if(!completed) break;		
			
			//if all the tasks are finished, set the finish flag
			if(i>= tklist.length-1 && !needTerminated) {
				setTerminateFlags(info);
				if(m_timer!=null) m_timer.abort();//cancel the timer
				copyIntermediateResults(info);
			}
		}											       		
	}		

	private static String getAppendix(int tkvalue, String cID) {
		int cg = tkvalue/10000;
		int ft = (tkvalue- cg*10000)/1000;
		return SCIFL.CG[cg] + SCIFL.FT[ft] + cID;		
	}

	private static long getTotalEventNum(int tkvalue) {
		int cg = tkvalue/10000;		
		return SCIFL.TN[cg];
	}

	private static String extractFilename(String fn) {		
		if(fn==null) return null;			
		int index = fn.lastIndexOf('/') + 1;
		return fn.substring(index);
	}
	
	private static String getEventXMLFileName(String fn, AnalysisInfo info) {
		String filename = extractFilename(fn);
		String destination = info.outdir+"/"+filename;
		//String destination = "/scratch/j/jacobsen/cyye/FL/tmp/" + filename;
		int endIndex = destination.length() - ".tar.gz".length();
		String xmlfile = destination.substring(0, endIndex);
		return xmlfile;
	}
	
	private static boolean isCompleteXMLFile(String fn, AnalysisInfo info) {
		File file = new File(fn);
		if(!file.exists()) return false;
		
		//first, copy the file to tmp
		String filename = extractFilename(fn);
		String destination = info.outdir+"/"+filename;
		//String destination = "/scratch/j/jacobsen/cyye/FL/tmp/"+filename;
		copyArchivedFile(fn, destination);
		
		//next, dearchive and uncompress the file
		dearchiveDestFile(destination, true);
		int endIndex = destination.length() - ".tar.gz".length();
		String xmlfile = destination.substring(0, endIndex);
		
		//check whether the xml file is complete
		boolean iscomplete = false;
		String endflags = "</Instances>";
		try{
			RandomAccessFile raf = new RandomAccessFile(xmlfile, "r");
			long length = raf.length() - endflags.length()-1;
			if(length<0) length=0;
			raf.seek(length);
			String content = raf.readLine();
			iscomplete = endflags.equals(content);
			raf.close();
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		//delete the incomplete xml files
		if(!iscomplete) {
			file = new File(xmlfile);
			file.delete();
		}
		
		return iscomplete;
	}
	
	private static int getFaultType(int tkvalue) {
		int cg = tkvalue/10000;
		int ft = (tkvalue- cg*10000)/1000;
		return ft;
	}

	private static ArrayList<String[]> current_mutations = null; 
	private static boolean reloadMutationList = true;		
	private static int current_ft = -1; 
	private static String[] getMutations(TestingScriptDefinition tsd,
			int ft, int mtIndex) {
        if(current_mutations==null)  reloadMutationList = true;
        if(ft!=current_ft) {
        	reloadMutationList = true;
        	current_ft = ft;
        }
		
		if(reloadMutationList) {
			    current_mutations = ServiceDebugging.loadMutationList(tsd, ft);
			    reloadMutationList = false;
		}
		
		if(current_mutations!=null && mtIndex<current_mutations.size()) return current_mutations.get(mtIndex);
		
		return null;
	}

	private static int getMutationIndex(int tkvalue) {
		int cg = tkvalue/10000;
		int ft = (tkvalue- cg*10000)/1000;
		int mutIndex = tkvalue - cg*10000 - ft * 1000;
		return mutIndex;
	}

	private static String current_tstfn = "";
	private static TestingScriptDefinition tsd = null; 
	private static TestingScriptDefinition getTestingScriptDefinition(
			int tkvalue) {
		int cg = tkvalue/10000; 
		String tscript_fn = SCIFL.tscripts[cg];
		if(tscript_fn.equals(current_tstfn) && tsd!=null) return tsd;
		
		tsd = ServiceDebugging.loadTestingScript(tscript_fn);
		current_tstfn = tscript_fn;
		reloadMutationList = true;
		
		return tsd;
	}

	private static int getAppIndex(int tkvalue,
			HashMap<Integer, Integer> savedAnalysis) {
		Integer result = savedAnalysis.get(tkvalue);
		if(result!=null) return result;
		
		return 0;
	}

	private static int[] getCandidateTaskList(int startIndex,
			int terminateIndex) {
		
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		for(int i=startIndex;i<=terminateIndex && i < SCIFL.tasks.length;i++) {
			int cg = SCIFL.tasks[i][0];
			int ft = SCIFL.tasks[i][1];
			int mstart = SCIFL.tasks[i][2];
			int mend = SCIFL.tasks[i][3];
			for(int j=mstart;j<=mend;j++) {
				int value = cg * 10000 + ft * 1000 + j;
				result.add(value);
			}
		}
		
		int[] buffer = new int[result.size()];
		for(int i=0;i<buffer.length;i++)
			buffer[i] = result.get(i);
		
		return buffer;
	}
	
	private static HashMap<Integer, Integer> loadSavedAnalysisParamters(String filename) {
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		
		try {
			String[] conts = null;
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line = reader.readLine();			
			while(line!=null) {
				conts = line.split("\\s+");
				if(conts.length>=2) {
					int mutindex = new Integer(conts[0]);
					int appindex = new Integer(conts[1]);
					result.put(mutindex, appindex);
				}
				line = reader.readLine();
			}
			reader.close();			
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		return result;
	}

	/*
	 *  mt_index: fault index
	 *  faulttype: the type of fault 0: inconsistency, 1: single, 2: two faults
	 *  category: the category of applications. 0: LA, 1: BO, 2:SC, 3:IN, 4: AC
	 *  filename: the name of event source file
	 *  savedFile: the file name to save the analysis results
	 *  CPEFileName: the file name to save the intermediate ranking records for debugging
	 *  return: complete or not
	 */
	public static boolean LocalAnalyzeOneTask(int mt_index, int faulttype, int category, 
			String filename, String savedFile, String CPEFileName) {
			
        //1. load testing script	
		String testscriptfilename = SCIFL.tscripts[category];
        TestingScriptDefinition tsd = ServiceDebugging.loadTestingScript(testscriptfilename);
		
        //3. load mutation category
        ArrayList<String[]> mutations = ServiceDebugging.loadMutationList(tsd, faulttype);
        String[] mt = mutations.get(mt_index);  
        
        totalEventsNum = SCIFL.TN[category];
        
        boolean completed = analyzeOneEventFileOnly(tsd, mt,filename, 
    			                mt_index, savedFile, CPEFileName);
        
        return completed;
	}
	
	public static boolean LocalAnalyzeBPELTask( 
			String filename, int evtnum, boolean detailedoutput) {
		
	     Encapsulation eps = Encapsulation.loadEncapsulationFromSavedEvents(filename);
	     boolean completed =  true;
	     
	     //filter duplicated event
	     filterDuplicatedEvent(eps);
	
	     totalEventsNum = evtnum;
	     algorithm = new FaultLocalizationAlgorithm(eps, totalEventsNum);
	     
	     //set the solution to include only one for the case study
	     //comment this out if it is not used for case study
	     ArrayList<Integer> keeps= new ArrayList<Integer>();
	     keeps.add(3);
	     algorithm.FilterSolutions(keeps);
	
	     //debugging
	     String CPEFileName = "tmp/BPELAnalysis.txt";
	     algorithm.setCPEFileName(CPEFileName);		
	
	     try{
	         algorithm.faultLocalizationBPEL(detailedoutput);
	     } catch(FLTimeoutException e) {
		     System.out.println("Timeout for terminating the task!");
	         completed = false;		    
	     }
	
	 	 //output result
	     String result = algorithm.getFLResult();
	     System.out.println(result);
		
        return completed;
	}	

	private static void filterDuplicatedEvent(Encapsulation eps) {
		ArrayList<ExecutionRecord> rdlist = eps.getInstances();
		for(ExecutionRecord record: rdlist) 
			 record.removeDuplicatedEvents();		
	}

	//adapt it for running in SciNet HPC
	private static boolean MySCITestingOnly() {
								        
	        //1. load testing script		
	        TestingScriptDefinition tsd = ServiceDebugging.loadTestingScript(testscript_filename);
			
	        //2. load test oracle
	        Oracle m_oracle = ServiceDebugging.loadOracle(tsd);
			
	        //3. load mutation category
	        ArrayList<String[]> mutations = ServiceDebugging.loadMutationList(tsd, faultType);
			        	
	        //4. test each mutation        	 
	        //while(mutationIndex < mutations.size()) {
	        
	        //fixed the bug when needTerminated=true saving empty event files via adding !needTerminated to quit the loop
	        //2016.04.25
	        while(mutationIndex <= mutationIndexTerminate && !needTerminated) {
	           	String[] mt = mutations.get(mutationIndex);           	
	        	
	           	boolean completed = testOneMutationOnly(tsd, mt, m_oracle, mutationIndex);
	            
	            if(!completed) return false;
	        	   
	            mutationIndex++;
	            //approachIndex = -1;
	            maxTestCaseNum = totalTestCaseNum;
	            
	            recordCompleteOne(workIndex + "   " + mutationIndex + "    " + totalTestCaseNum + "   " + 0);
	        }        		        	    
	        
	        return true;
	}
	
	//adapt it for running in SciNet HPC
	private static boolean MySCIAnalysisOnly() {
										        
			 //1. load testing script		
			 TestingScriptDefinition tsd = ServiceDebugging.loadTestingScript(testscript_filename);
					
			 //2. load test oracle
			 //Oracle m_oracle = ServiceDebugging.loadOracle(tsd);
					
			 //3. load mutation category
			 ArrayList<String[]> mutations = ServiceDebugging.loadMutationList(tsd, faultType);
					        	
			 //4. test each mutation        	 
			 //while(mutationIndex < mutations.size()) {
			 while(mutationIndex <= mutationIndexTerminate && !needTerminated) {
			       String[] mt = mutations.get(mutationIndex);   
			       
			       isResume = approachIndex>=0;
			       if(approachIndex<0) approachIndex=0;
			        	
			       boolean completed = analyzeOneMutationOnly(tsd, mt, mutationIndex, approachIndex);
			            
			       if(!completed) return false;
			        	   
			       mutationIndex++;
			       approachIndex = -1;
			            
			       recordCompleteOne(workIndex + "   " + mutationIndex + "    " + approachIndex + "   " + 0);
			 }        		        	    
			        
	     return true;
	}		

	private static boolean analyzeOneMutationOnly(
				TestingScriptDefinition tsd, String[] mts, int mt_index, int appindex) {
				
		String filename = getAnalyzedEventsFileName(mt_index);
		dearchiveDestFile(filename+".tar.gz", false); //keep the original file
		Encapsulation eps = Encapsulation.loadEncapsulationFromSavedEvents(filename);
		
		//delete the xml file
		File file = new File(filename);
		file.delete();
		
		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		eps.snapshotServiceInstance(services);
		
		for(String mt: mts) {
			String mToken = loadMutation(mt, services);
			eps.addMutation(mToken);
		}

		boolean completed =  true;
		
		algorithm = new FaultLocalizationAlgorithm(eps, totalEventsNum);
		
		//debugging
		algorithm.setCPEFileName(getCompositeEventOutputFileName(mt_index));		
		
		try{
		    algorithm.faultLocalization(appindex);
		} catch(FLTimeoutException e) {
			System.out.println("Timeout for terminating the task!");
		    completed = false;		    
		}
				
		prepare4QuitAnalysis(mt_index, completed);
		
        return completed;
	}
	
	private static boolean analyzeOneMutationOnlyNew(
			TestingScriptDefinition tsd, String[] mts, int appindex, String filename, AnalysisInfo info) {
		
		Encapsulation eps = Encapsulation.loadEncapsulationFromSavedEvents(filename);			
		//delete the xml file
	    File file = new File(filename);
	    file.delete();
		
	    /*
		ArrayList<ExecutionRecord> instances= eps.getInstances();
		for(ExecutionRecord record:instances) {
			if("313_1450686677024".equals(record.getInstanceID())) {
			
				 ArrayList<Event> events = record.getAllEvents();
				 for(Event evt: events) {
					 if("LoanProcess_1450686677040".equals(evt.getInstanceID())) 
					 evt.printMessage();
				 }
			}
		}*/
		
		ArrayList<AbstractService> services = loadTestingService(tsd);
		eps.snapshotServiceInstance(services);
	
		for(String mt: mts) {
			String mToken = loadMutation(mt, services);
			eps.addMutation(mToken);
		}

		boolean completed =  true;
	
		algorithm = new FaultLocalizationAlgorithm(eps, info.totalEventsNum);
	
		//debugging
		//algorithm.setCPEFileName(getCompositeEventOutputFileName(mt_index));		
	
		try{
			algorithm.faultLocalizationNew(appindex, info);
		} catch(FLTimeoutException e) {
			System.out.println("Timeout for terminating the task!");
			completed = false;		    
		}				
		
		return completed;
	}	

	private static boolean analyzeOneEventFileOnly(
			TestingScriptDefinition tsd, String[] mts, String filename, 
			int mt_index, String savedFile, String CPEFileName) {
			
	     Encapsulation eps = Encapsulation.loadEncapsulationFromSavedEvents(filename);
	
	     //delete the xml file
	     //File file = new File(filename);
	     //file.delete();
	
	     ArrayList<AbstractService> services = loadTestingService(tsd);
	     eps.snapshotServiceInstance(services);
	
	     for(String mt: mts) {
		      String mToken = loadMutation(mt, services);
		      eps.addMutation(mToken);
	     }

	     boolean completed =  true;
	
	     algorithm = new FaultLocalizationAlgorithm(eps, totalEventsNum);
	     
	     //set the solution to include only one for the case study
	     //comment this out if it is not used for case study
	     //ArrayList<Integer> keeps= new ArrayList<Integer>();
	     //keeps.add(3);
	     //algorithm.FilterSolutions(keeps);
	
	     //debugging
	     algorithm.setCPEFileName(CPEFileName);		
	
	     try{
	         algorithm.faultLocalization(0);
	     } catch(FLTimeoutException e) {
		     System.out.println("Timeout for terminating the task!");
	         completed = false;		    
	     }
	
	 	 //output result
	     String result = algorithm.getFLResult();
	     ReportLocalFaultLocalizationResult(mt_index, result, completed, savedFile);	
		
         return completed;
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

	private static boolean isResume;
	private static FaultLocalizationAlgorithm algorithm;
	private static FaultLocalizationMonitoring fault_monitor;
	/**
	 * This approach is to resume the handling of event ranking based on saved events
	 * @param eps 
	 * @param mt_index
	 * @param app_Index
	 * @return
	 */
	private static boolean testOneMutation(Encapsulation eps, int mt_index, int app_index) {
		
		isResume = true;
		
		//fault localization based on exposed events
		algorithm = new FaultLocalizationAlgorithm(eps,totalEventsNum);
		
		//debugging
		algorithm.setCPEFileName(getCompositeEventOutputFileName(mt_index));
		
		isPartialSaved = false;
		
		boolean completed =  true;
							
		try{
		    algorithm.faultLocalization(app_index);
		} catch(FLTimeoutException e) {
			System.out.println("Timeout for terminating the task!");
			//e.printStackTrace();
		    completed = false;
		}
		
		prepare4Quit(mt_index, completed);
				
		return completed;
	}
	
	private static boolean testOneMutation(TestingScriptDefinition tsd, String[] mts,
				Oracle m_oracle, int mt_index) {
		
		isResume = false;
		
		//0. create monitor	
		//FaultLocalizationMonitoring fault_monitor = new FaultLocalizationMonitoring();		
		fault_monitor = new FaultLocalizationMonitoring();
		
		int testcaseNum = 0;
		int accumulatedDelay = 0;
		int maxAccumulated = 1800;//half an hour
		boolean completed =  true;
		
		while(testcaseNum<maxTestCaseNum) {
			//1. load services
			ArrayList<AbstractService> services = loadTestingService(tsd);			
		
			//2. load mutation into services
			for(String mt: mts) {
			   String mToken = loadMutation(mt, services);
			   if(fault_monitor.notRegisterMutation()) fault_monitor.registerMutation(mToken);
			}
						
			fault_monitor.setMutationRegisterFlag();
		
			//3. load test case
			ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
			services.add(m_testcase.getService());//add the test case service into the service list
		
			//4. load choreography		
			Choreography cho = loadChoreography(tsd, services);
			
			//5. register service choreography
	        engine.getEngine().getServiceCorrelation().register(cho);	        
	        
	        //6. register coverage_monitor and testing_monitor
	        fault_monitor.register(services);
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
	        	accumulatedDelay += timeout;
	        }
	        
	        //terminate all the remaining running services
	        ArrayList<AbstractService> running_services = testing_monitor.getRemainingServices(services);
	        engine.getEngine().terminateServices(running_services);
	        
	        testcaseNum++;
	                
	        //record passed or failed for the testing
	        fault_monitor.setFinalResult(m_oracle.checkOracle(services));
	        
	        //save received events
	        fault_monitor.recordExecutionInstances();
	        
	        	        
	        //garbage collecting for the services
	        fault_monitor.unregister(services);
	        engine.getEngine().getServiceCorrelation().unregister(cho);	        
	        engine.getEngine().unregister(services);
	        testing_monitor.gabarageCollection();

	        //This part of code is used to pause and save the experiment due to the
	        //time limit for CPU usage.
	        if(needTerminated) return false;	        
            
            if(accumulatedDelay > maxAccumulated) break;
		}
		
		//fault localization based on exposed events
		Encapsulation eps = fault_monitor.getEncapsulation();
		//FaultLocalizationAlgorithm algorithm = new FaultLocalizationAlgorithm(eps, 0);
		//algorithm = new FaultLocalizationAlgorithm(eps, 0);
		algorithm = new FaultLocalizationAlgorithm(eps, totalEventsNum);
		
		//debugging
		algorithm.setCPEFileName(getCompositeEventOutputFileName(mt_index));		
			
		isPartialSaved = false;
		
		try{
		    algorithm.faultLocalization();
		} catch(FLTimeoutException e) {
			System.out.println("Timeout for terminating the task!");
			//e.printStackTrace();
		    completed = false;		    
		}
		
		prepare4Quit(mt_index, completed);
		
        return completed;
	}
	
	private static boolean testOneMutationOnly(TestingScriptDefinition tsd, String[] mts,
			Oracle m_oracle, int mt_index) {
	
	    //0. create monitor	
	    //FaultLocalizationMonitoring fault_monitor = new FaultLocalizationMonitoring();		
	    fault_monitor = new FaultLocalizationMonitoring();
	
	    boolean completed =  true;
	
	    while(maxTestCaseNum >0 && !needTerminated) {
		      //1. load services
		      ArrayList<AbstractService> services = loadTestingService(tsd);			
	
		      //2. load mutation into services
		      for(String mt: mts) {
		           String mToken = loadMutation(mt, services);
		           if(fault_monitor.notRegisterMutation()) fault_monitor.registerMutation(mToken);
		      }
					
		      fault_monitor.setMutationRegisterFlag();
	
		      //3. load test case
		      ServiceTestCase m_testcase = loadServiceTestCase(tsd, testcase_policy);
		      services.add(m_testcase.getService());//add the test case service into the service list
	
		      //4. load choreography		
		      Choreography cho = loadChoreography(tsd, services);
		
		      //5. register service choreography
              engine.getEngine().getServiceCorrelation().register(cho);	        
        
              //6. register coverage_monitor and testing_monitor
              fault_monitor.register(services);
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
        	        //accumulatedDelay += timeout;
              }
        
              //terminate all the remaining running services
              ArrayList<AbstractService> running_services = testing_monitor.getRemainingServices(services);
              engine.getEngine().terminateServices(running_services);
        
              maxTestCaseNum--;
                
              //record passed or failed for the testing
              fault_monitor.setFinalResult(m_oracle.checkOracle(services));
        
              //save received events
              fault_monitor.recordExecutionInstances();
        
        	        
              //garbage collecting for the services
              fault_monitor.unregister(services);
              engine.getEngine().getServiceCorrelation().unregister(cho);	        
              engine.getEngine().unregister(services);
              testing_monitor.gabarageCollection();

              //This part of code is used to pause and save the experiment due to the
              //time limit for CPU usage.
              if(needTerminated) completed = false;	        
        
              //if(accumulatedDelay > maxAccumulated) break;
	     }
	
	     //save all the exposed events
	     Encapsulation eps = fault_monitor.getEncapsulation();
	     String filename = getSavedEventsFileName(mt_index);
	     eps.saveEvents(filename, completed);
	     needCopyFiles.add(filename);
	
         return completed;
    }	
	
	public static boolean isPartialSaved=true;
	public static void prepare4AbortedQuit() {
		prepare4Quit(mutationIndex, false);			
	}
	
	private static synchronized void prepare4Quit(int mt_index, boolean completed) {
		
		if(!needTerminated && !isPartialSaved) {//if terminated, no need to invoke this function again

			String result = algorithm.getFLResult();
			approachIndex = algorithm.getNextAppIndex();
			if(totalEventsNum==0) totalEventsNum = algorithm.totalEventsNum;
	    
			//save encapsulation for resuming the task
			if(!isResume && !completed) saveEncapsulation(fault_monitor.getEncapsulation());

		
			//output the result of the testing
			ReportFaultLocalizationResult(mt_index, result, isResume, completed, "");	
			
			isPartialSaved = true;
		}
		
	}
	
    private static synchronized void prepare4QuitAnalysis(int mt_index, boolean completed) {
		
		//if(!needTerminated) {//if terminated, no need to invoke this function again
    	    
    	    String description = algorithm.getSolutionDescriptions();
    	
			//String result = algorithm.getFLResult();
    	    String result = algorithm.getFLResult();
			approachIndex = algorithm.getNextAppIndex();
			if(totalEventsNum==0) totalEventsNum = algorithm.totalEventsNum;
		
			
			//output the result of the testing
			ReportFaultLocalizationResult(mt_index, result, isResume, completed, description);	
			
			//isPartialSaved = true;
		//}
		
	}
	
	/**
	 * Save encapsulation for later resuming the ranking task
	 * @param encapsulation
	 */
	private static void saveEncapsulation(Encapsulation encapsulation) {
		String des_filename = getSaveEncapsulationFileName();
		try {
			ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(des_filename));
			writer.writeObject(encapsulation);
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Encapsulation loadEncapsulation() {
		String des_filename = getSaveEncapsulationFileName();
		Encapsulation eps = null;
		try {
			ObjectInputStream reader = new ObjectInputStream(new FileInputStream(des_filename));
			eps = (Encapsulation)reader.readObject();
			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return eps;
	}

	private static void setTerminateFlags(AnalysisInfo info) {
		String filename = getFinishFlagFileName(info);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(filename);
			writer.close();
		} catch(Exception e) {
			
		}
	}
	
	private static void setTerminateFlags() {
		String filename = getFinishFlagFileName();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write(filename);
			writer.close();
		} catch(Exception e) {
			
		}
	}	
	public static synchronized void copyResults() {
	
	   copyComputationResult();
	   copySavedParameters();
	}
	
	public static synchronized void copyTestingResultsOnly() {
		
		copySavedEvents();
		copySavedParametersOnly();
	}
	
	private static void copySavedParametersOnly() {
		//String tmp_filename = getCompleteFileName();
		String des_filename = getSaveParameterFileName();
		//copyFiles(tmp_filename, des_filename, false);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(des_filename));
            String line = workIndex + "   " + mutationIndex + "    " + maxTestCaseNum + "   " + totalEventsNum;
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
            String line = workIndex + "   " + mutationIndex + "    " + approachIndex + "   " + totalEventsNum;
            writer.write(line);
            writer.close();
        } catch(Exception e ) {}
	}

	private static void copyComputationResult() {

        if(needCopyFiles!=null) {
            for(String tmp_filename: needCopyFiles) {
		        String des_filename = getCopyResultFileName(tmp_filename);
	            copyFiles(tmp_filename, des_filename, true);
            }
            needCopyFiles.clear();
        }
	}
	
	private static void copyComputationResult(AnalysisInfo info) {

        if(info.needCopyFiles!=null) {
            for(String tmp_filename: info.needCopyFiles) {
		        String des_filename = getCopyResultFileName(tmp_filename, info);
	            copyFiles(tmp_filename, des_filename, true);
            }
            info.needCopyFiles.clear();
        }
	}	
	
	private static void copySavedEvents() {

        if(needCopyFiles!=null) {
            for(String tmp_filename: needCopyFiles) {
		        String des_filename = getCopyEventFileName(tmp_filename);
		        
		        //copy or append the files
	            copySavedEventFiles(tmp_filename, des_filename);
	            
	            //archive the files
	            archiveDestFile(des_filename);
            }
            needCopyFiles.clear();
        }
	}
	
	public static void archiveDestFile(String des_filename) {
		try {
		     String dfn = TarArchiveFiles.archive(des_filename);
		     File file = new File(des_filename);
		     file.delete();
		     TarArchiveFiles.compress(dfn);
		     file = new File(dfn);
		     file.delete();
		     
		} catch(Exception e) {
			 e.printStackTrace();
		}
	}

	public static void dearchiveDestFile(String des_filename, boolean delete) {
		try{
			File tmp = TarArchiveFiles.uncompress(des_filename);
		    TarArchiveFiles.dearchive(tmp);
		    tmp.delete();
		    
		    if(delete) {
		    	File file = new File(des_filename);
		    	file.delete();
		    }
		    
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void copyArchivedFile(String source, String destination) {
		 int byteread = 0; // 读取的字节数  
         InputStream in = null;  
         OutputStream out = null;  
		 try{
			 byte[] buffer = new byte[1024];  
			 in = new FileInputStream(source);
			 out = new FileOutputStream(destination);	
			 
			 while ((byteread = in.read(buffer)) != -1) {  
	                out.write(buffer, 0, byteread);  
	         }  
			 
		 } catch(Exception e) {
			 e.printStackTrace();
		 } finally {  
	            try {  
	                if (out != null)  
	                    out.close();  
	                if (in != null)  
	                    in.close();  
	            } catch (Exception e) {  
	                e.printStackTrace();  
	            }  
	        }  
	}

	public static void copySavedEventFiles(String source, String destination) {	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(source));	
			
			//if the .tar.gz file exists, dearchive the file first
			File gz = new File(destination+".tar.gz");
			if(gz.exists())
				dearchiveDestFile(destination+".tar.gz", true);
			
			//check whether the destination exists
			boolean exist = false;
			File tmp = new File(destination);
		    exist = tmp.exists() && tmp.length()>0;
		    
			BufferedWriter writer = new BufferedWriter(new FileWriter(destination, exist));			
			
			//if the file exists, skip the first two lines
			int index = exist?2:0;
			for(int i=0;i<index;i++) 
				reader.readLine();
			
			String line = reader.readLine();
			while(line!=null) {
				writer.write(line);
				writer.newLine();
				line = reader.readLine();
			}
			reader.close();			
			writer.close();	
		} catch(Exception e) {
			e.printStackTrace();
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
	
	public static synchronized void copyIntermediateResults(AnalysisInfo info) {
		//1. save parameters
		copyParameters(info);
		
		//2. copy results
		copyComputationResult(info);
	}
	
	public static synchronized void saveIntermediateResults(AnalysisInfo info, String result, int appIndex, boolean completed) {
		int mt_index = info.mtIndex;		
		
		//1. update the HashMap
		info.savedAnalysis.put(info.tkvalue, appIndex+1);
		
		//2. save the results
        String filename = getFaultLocalizationResultFileName(info);
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content ="";			
			
			//if(appIndex>0)
			//if(appIndex>9) //for running only partial solutions (index >=9)
			if(appIndex>11)
			    content += result;
			else 
				content += ""+mt_index + newline + result;				
			
			writer.write(content);
			if(completed) writer.newLine();
			writer.close();

            if(info.needCopyFiles!=null && !info.needCopyFiles.contains(filename)) info.needCopyFiles.add(filename);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	public static String newline = System.getProperty("line.separator");
	private static void ReportFaultLocalizationResult(int mt_index, String result, boolean partial, boolean completed, String description) {
		
		String filename = getFaultLocalizationResultFileName();		
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content ="";
			if(mt_index==0) content += description;
			
			if(partial)
				content += result;
			else 
				content += ""+mt_index + newline + result;				
			
			writer.write(content);
			if(completed) writer.newLine();
			writer.close();

            if(needCopyFiles!=null && !needCopyFiles.contains(filename)) needCopyFiles.add(filename);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}	

	private static void ReportLocalFaultLocalizationResult(int mt_index, String result, boolean completed, String filename) {
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
			String content ="";
			
			content += ""+mt_index + newline + result;				
			
			writer.write(content);
			if(completed) writer.newLine();
			writer.close();

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
			 //return XMLProcessing.unmarshal(TestingScriptDefinition.class, filename);
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
	
	public static ArrayList<String[]> loadMutationList(TestingScriptDefinition tsd, int ft) {
		MutationsType mutt;

        //set the mutation ratio in order to differentiate the multiple faults and single/inconsistency faults when calculating expense
        Config.getConfig().mutationRatio = ft==2? 2:1;
		
		String filename;
		if(ft==0) //inconsistency fault 
			mutt = tsd.getInconsistencyMutations();			
		else //single or two faults
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
		
		//handle two faults
		if(ft==2) 
		   return constructTwoFaults(result);     	
		else		
		   return result;
	}		

	private static ArrayList<String[]> constructTwoFaults(
			ArrayList<String[]> singfaults) {

		ArrayList<String[]> result = new ArrayList<String[]>();
		
		int associatedIndex = singfaults.size() -1;
		for(int i=0;i<singfaults.size();i++) {
			String sf1 = singfaults.get(i)[0];
			associatedIndex = getNextAssociatedIndex(associatedIndex, sf1, singfaults, result);
			String sf2 = singfaults.get(associatedIndex)[0];
			
			String[] item = new String[2];
			item[0] = sf1;
			item[1] = sf2;
			result.add(item);
			
			associatedIndex--;
		}
		
/*		
		ArrayList<String> abssf = extractFaults(singfaults, 1);
				
		for(int i=0;i<abssf.size();i++) {
			String sf1 = abssf.get(i);
			for(int j=i+1;j<abssf.size();j++) {
				String sf2 = abssf.get(j);
				if(mutationFromSameLocation(sf1, sf2)) continue;
				
				String[] item = new String[2];
				item[0] = sf1;
				item[1] = sf2;
				result.add(item);
			}
		}
*/		
		return result;
		
	/*	
		ArrayList<ArrayList<String>> visited = new ArrayList<ArrayList<String>>();
		
		
		
		for(int i=0;i<singfaults.size();i++) {
			String[] sf1 = singfaults.get(i);
			for(int j=i+1;j<singfaults.size();j++) {
				String[] sf2 = singfaults.get(j);
				if(mutationFromSameLocation(sf1, sf2, visited)) continue;
				String[] item = new String[2];
				item[0] = sf1[0];
				item[1] = sf2[0];
				result.add(item);
			}
		}
				
		return result;
	*/
		
	}

	private static int getNextAssociatedIndex(int associatedIndex, String sf1,
			ArrayList<String[]> singfaults, ArrayList<String[]> result) {

		while(true) {
		   if(associatedIndex<0) associatedIndex = singfaults.size() - 1;
		
		   String sf2 = singfaults.get(associatedIndex)[0];
		   		   
		   if(!mutationFromSameLocation(sf1, sf2) && !existPair(result, sf1, sf2)) break;
		   
		   associatedIndex --;
		}
		
		return associatedIndex;
	}

	private static boolean existPair(ArrayList<String[]> result, String sf1,
			String sf2) {
		
		for(String[] item: result) {
			if(item[0].equals(sf1) && item[1].equals(sf2)) return true;
			
			if(item[0].equals(sf2) && item[1].equals(sf1)) return true;
		}
		
		return false;
	}

	private static ArrayList<String> extractFaults(
			ArrayList<String[]> singfaults, int num) {

		ArrayList<String> result = new ArrayList<String>();
		
		HashMap<String, Integer> buffer = new HashMap<String, Integer>();
		for(String[] item: singfaults) {
			if(item.length==0) continue;
			
			String subitem = extractMutationLocation(item[0]);
			Integer vn = buffer.get(subitem);
			if(vn==null) vn = 0;
			if(vn>=num) continue;
			
			vn++;
			buffer.put(subitem, vn);
			result.add(item[0]);
		}
		
		return result;
	}

	private static boolean mutationFromSameLocation(String sf1, String sf2) {
		
		//skip the empty cases
		if(sf1==null || sf2 == null) return true;
		
		//compare the first item of both arrays
		String item1 = extractMutationLocation(sf1);
		String item2 = extractMutationLocation(sf2);	

		/*
		for(ArrayList<String> item: visited) {
			if(item.contains(item1) && item.contains(item2)) return true;
		}*/
		
		if(item1.equals(item2)) return true;
		/*
		ArrayList<String> item = new ArrayList<String>();
		item.add(item1);
		item.add(item2);
		visited.add(item);
		*/
		
		return false;
	}

	private static String extractMutationLocation(String mtname) {

		int loc1 = mtname.indexOf('_');
		if(loc1>=0) return mtname.substring(loc1+1);
		return "";
	}

	private static String getCompleteFileName() {		
		return outdir + "/" + appendix +"_Complete.txt";
	}
	
	private static String getSaveParameterFileName() {
		return copyOutDir + "/params/" + coreID +"_Complete_"+ workTerminateIndex +".txt";
	}
	
	private static String getSaveEncapsulationFileName() {
		if(copyOutDir==null)
			return outdir + "/" + appendix +"_Encapsulation";
	    else
		    return copyOutDir + "/params/" + coreID +"_"+ workTerminateIndex +"_Encapsulation";
	}
	
	private static String getFaultLocalizationResultFileName(AnalysisInfo info) {
	    //return info.outdir + "/" + info.appendix + "_"+ info.workTerminateIndex +".txt";
		return info.outdir + "/" + info.appendix + "_result.txt" + info.workTerminateIndex;
	}
	
	private static String getFaultLocalizationResultFileName() {		
		return outdir + "/" + appendix + "_"+ workTerminateIndex +".txt";
	}
	
	private static String getCompositeEventOutputFileName(int mt_index) {
		return outdir + "/" + appendix + "_cpe_"+ mt_index +".txt";
	}
	
	private static String getSavedEventsFileName(int mt_index) {
		return outdir + "/" + appendix + "_events_"+ mt_index +".xml";
	}
	
	private static String getAnalyzedEventsFileName(int mt_index) {
		return copyOutDir + "/events/" + appendix + "_events_"+ mt_index +".xml";
	}
	
	private static String getAnalyzedEventsFileName(AnalysisInfo info) {
		//return copyOutDir + "/events/" + appendix + "_events_"+ mt_index +".xml";
		return info.copyOutDir + "/saved/" + info.appendix + "_events_"+ info.mtIndex +".xml";
	}
	
	private static String getCopyResultFileName(String filename) {

        int index = filename.lastIndexOf('/');
        String tfn = filename.substring(index+1);
		return copyOutDir + "/results/" + tfn;		
	}
	
	private static String getCopyResultFileName(String filename, AnalysisInfo info) {
        int index = filename.lastIndexOf('/');
        String tfn = filename.substring(index+1);
		return info.copyOutDir + "/results/" + tfn;		
	}
	
	private static String getCopyEventFileName(String filename) {

        int index = filename.lastIndexOf('/');
        String tfn = filename.substring(index+1);
		return copyOutDir + "/events/" + tfn;		
	}
	
	private static String getFinishFlagFileName() {
		return copyOutDir + "/flags/" + coreID + workTerminateIndex;
	}
	
	private static String getFinishFlagFileName(AnalysisInfo info) {
		return info.copyOutDir + "/flags1/" + info.coreID + info.workTerminateIndex;
	}
	
	private static void copyParameters(AnalysisInfo info) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(info.savedFileName)));
			Set<Integer> keys = info.savedAnalysis.keySet();
			for(Integer key: keys) {
				  Integer value = info.savedAnalysis.get(key);
				  String line = key + "   " + value;
				  writer.write(line);
				  writer.newLine();
			}			
			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	//return mutation token for calculate distance
	private static String loadMutation(String filename, ArrayList<AbstractService> services) {
		String mutationToken = "";
					
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
				
				mutationToken += serviceName +":Task:" + task.getName();
				
			} else 
				
			if(mt.equals(MutationType.TRANSITION_MUTATION)) {
				Transition transition = getServiceTransition(services, serviceName, mutationName);
				assert(transition!=null);
				
				//update the transition with mutation
				transition.getGuard().setGuard(expression);
				
				mutationToken +=serviceName +":Transition:" + transition.getName();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return mutationToken;
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

	public static boolean needTerminated = false;
	public static void setStopFlags() {
		needTerminated = true;
		
	}

	

}

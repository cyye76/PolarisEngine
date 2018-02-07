package ServiceTesting;

import java.util.ArrayList;

import engine.engine;

public class SCITCE {

	/**
	 * TestCaseExp running in the scinet
	 * @param args
	 */
	public static void main(String[] args) {
		
		SCITCE tce = new SCITCE();
		tce.handleParameter(args);
		
		//start the engine
		engine.getEngine().startEngine();
		
		tce.runtasks();
		tce.copyResults();
			
		//close the engine
		try {
	    	Thread.sleep(10000);
	    	
	    } catch(Exception e) {}
		
		engine.getEngine().closeEngine();								
    }

	private String cpdir = "/scratch/j/jacobsen/cyye/exp6";
	private void copyResults() {
		for(String tmp_filename: copyfiles) {
	        String des_filename = getCopyResultFileName(tmp_filename);
            ServiceTesting.copyFiles(tmp_filename, des_filename, true);
        }		
	}
	
	private String getCopyResultFileName(String filename) {
        int index = filename.lastIndexOf('/');
        String tfn = filename.substring(index+1);
		return cpdir + "/" + tfn;		
	}
	
	private int taskID;
	private void handleParameter(String[] args) {		
		coreID = args[0];
		taskID = new Integer(args[1]);
		cpdir = args[2];
	}
	
	private void runtasks() {
		//for(String[] task: tasks) {
		    String[] task = tasks[taskID];
			task[5] += coreID;
			TestCaseExp.SCIRun(task, copyfiles);
		//}
	}

	private ArrayList<String> copyfiles = new ArrayList<String>();
	private String coreID="";
//	private String[][] tasks = {
//			{"Applications/LoanApproval/testscript.xml", "200", "1", "4", "/dev/shm", "LA", "1"},	
//			{"Applications/LoanApproval/testscript.xml", "200", "2", "4", "/dev/shm", "LA", "1"},
//			{"Applications/LoanApproval/testscript.xml", "200", "3", "4", "/dev/shm", "LA", "0.67"},
//			{"Applications/LoanApproval/testscript.xml", "200", "3", "4", "/dev/shm", "LA", "0.34"},
//			
//			{"Applications/BookOrdering/testscript.xml", "200", "1", "4", "/dev/shm", "BO", "1"},	
//			{"Applications/BookOrdering/testscript.xml", "200", "2", "4", "/dev/shm", "BO", "1"},
//			{"Applications/BookOrdering/testscript.xml", "200", "3", "4", "/dev/shm", "BO", "0.67"},
//			{"Applications/BookOrdering/testscript.xml", "200", "3", "4", "/dev/shm", "BO", "0.34"},
//			
//			{"Applications/SupplyChain/testscript.xml", "200", "1", "4", "/dev/shm", "SC", "1"},	
//			{"Applications/SupplyChain/testscript.xml", "200", "2", "4", "/dev/shm", "SC", "1"},
//			{"Applications/SupplyChain/testscript.xml", "200", "3", "4", "/dev/shm", "SC", "0.67"},
//			{"Applications/SupplyChain/testscript.xml", "200", "3", "4", "/dev/shm", "SC", "0.34"}
			
//			{"Applications/LoanApproval/testscript.xml", "200", "1", "3", "/dev/shm", "LA", "1"},	
//			{"Applications/LoanApproval/testscript.xml", "200", "2", "3", "/dev/shm", "LA", "1"},
//			{"Applications/LoanApproval/testscript.xml", "200", "3", "3", "/dev/shm", "LA", "0.67"},
//			{"Applications/LoanApproval/testscript.xml", "200", "3", "3", "/dev/shm", "LA", "0.34"},
//			
//			{"Applications/BookOrdering/testscript.xml", "200", "1", "3", "/dev/shm", "BO", "1"},	
//			{"Applications/BookOrdering/testscript.xml", "200", "2", "3", "/dev/shm", "BO", "1"},
//			{"Applications/BookOrdering/testscript.xml", "200", "3", "3", "/dev/shm", "BO", "0.67"},
//			{"Applications/BookOrdering/testscript.xml", "200", "3", "3", "/dev/shm", "BO", "0.34"},
//			
//			{"Applications/SupplyChain/testscript.xml", "200", "1", "3", "/dev/shm", "SC", "1"},	
//			{"Applications/SupplyChain/testscript.xml", "200", "2", "3", "/dev/shm", "SC", "1"},
//			{"Applications/SupplyChain/testscript.xml", "200", "3", "3", "/dev/shm", "SC", "0.67"},
//			{"Applications/SupplyChain/testscript.xml", "200", "3", "3", "/dev/shm", "SC", "0.34"}
//	};
	
	private String[][] tasks = {
			{"Applications/LoanApproval/testscript.xml", "200", "1", "4", "/dev/shm", "LA", "1"},	
			{"Applications/LoanApproval/testscript.xml", "200", "2", "4", "/dev/shm", "LA", "1"},
			{"Applications/LoanApproval/testscript.xml", "200", "3", "4", "/dev/shm", "LA", "0.75"},
			{"Applications/LoanApproval/testscript.xml", "200", "3", "4", "/dev/shm", "LA", "0.50"},
			
			{"Applications/BookOrdering/testscript.xml", "200", "1", "4", "/dev/shm", "BO", "1"},	
			{"Applications/BookOrdering/testscript.xml", "200", "2", "4", "/dev/shm", "BO", "1"},
			{"Applications/BookOrdering/testscript.xml", "200", "3", "4", "/dev/shm", "BO", "0.75"},
			{"Applications/BookOrdering/testscript.xml", "200", "3", "4", "/dev/shm", "BO", "0.50"},
			
			{"Applications/SupplyChain/testscript.xml", "200", "1", "4", "/dev/shm", "SC", "1"},	
			{"Applications/SupplyChain/testscript.xml", "200", "2", "4", "/dev/shm", "SC", "1"},
			{"Applications/SupplyChain/testscript.xml", "200", "3", "4", "/dev/shm", "SC", "0.75"},
			{"Applications/SupplyChain/testscript.xml", "200", "3", "4", "/dev/shm", "SC", "0.50"},
		
			{"Applications/LoanApproval/testscript.xml", "200", "1", "3", "/dev/shm", "LA", "1"},	
			{"Applications/LoanApproval/testscript.xml", "200", "2", "3", "/dev/shm", "LA", "1"},
			{"Applications/LoanApproval/testscript.xml", "200", "3", "3", "/dev/shm", "LA", "0.75"},
			{"Applications/LoanApproval/testscript.xml", "200", "3", "3", "/dev/shm", "LA", "0.50"},
			
			{"Applications/BookOrdering/testscript.xml", "200", "1", "3", "/dev/shm", "BO", "1"},	
			{"Applications/BookOrdering/testscript.xml", "200", "2", "3", "/dev/shm", "BO", "1"},
			{"Applications/BookOrdering/testscript.xml", "200", "3", "3", "/dev/shm", "BO", "0.75"},
			{"Applications/BookOrdering/testscript.xml", "200", "3", "3", "/dev/shm", "BO", "0.50"},
			
			{"Applications/SupplyChain/testscript.xml", "200", "1", "3", "/dev/shm", "SC", "1"},	
			{"Applications/SupplyChain/testscript.xml", "200", "2", "3", "/dev/shm", "SC", "1"},
			{"Applications/SupplyChain/testscript.xml", "200", "3", "3", "/dev/shm", "SC", "0.75"},
			{"Applications/SupplyChain/testscript.xml", "200", "3", "3", "/dev/shm", "SC", "0.50"}
	};
}

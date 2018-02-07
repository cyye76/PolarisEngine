package ServiceTesting;

import engine.engine;

public class SCIST {
	//CG: LA = 0, BO = 1, SC=2
	//FT: IF = 0, SF = 1
	//App: 1 .. 4
	// approachIndex = CG * 8 + IF * 4 + App (1-24)
	
	public static String[] CG= {
			"LA",
			"BO",
			"SC",
			"IN",
			"AC"			
	};
	
	public static String[] FT={
		"IF",
		"SF"
	};
		
	public static int [][] tasks = {
		//Split the tasks into parallel running
		//Part 1: 0-11
			{0, 0, 2},//LAIF2 0
			{0, 0, 4},//LAIF4 1
			{0, 1, 2},//LASF2 2
			{0, 1, 4},//LASF4 3
			{1, 0, 1},//BOIF1 4
			{1, 0, 3},//BOIF3 5
			{1, 1, 1},//BOSF1 6
			{1, 1, 3},//BOSF3 7
			{2, 0, 2},//SCIF2 8
			{2, 0, 4},//SCIF4 9
			{2, 1, 2},//SCSF2 10
			{2, 1, 4}, //SCSF4 11
			
		//Part 2: 12-23	
			{0, 0, 1},//LAIF1 12
			{0, 0, 3},//LAIF3 13
			{0, 1, 1},//LASF1 14
			{0, 1, 3},//LASF3 15
			{1, 0, 2},//BOIF2 16
			{1, 0, 4},//BOIF4 17
			{1, 1, 2},//BOSF2 18 
			{1, 1, 4},//BOSF4 19
			{2, 0, 1},//SCIF1 20
			{2, 0, 3},//SCIF3 21
			{2, 1, 1},//SCSF1 22
			{2, 1, 3}, //SCSF3 23
			
		//additional exps
			{3, 0, 1}, //INIF1 24
			{3, 0, 2}, //INIF2 25
			{3, 0, 3}, //INIF3 26
			{3, 0, 4}, //INIF4 27
			{3, 1, 1}, //INSF1 28
			{3, 1, 2}, //INSF2 29
			{3, 1, 3}, //INSF3 30
			{3, 1, 4}, //INSF4 31	 		
			{4, 0, 1}, //ACIF1 32
			{4, 0, 2}, //ACIF2 33
			{4, 0, 3}, //ACIF3 34
			{4, 0, 4}, //ACIF4 35
			{4, 1, 1}, //ACSF1 36
			{4, 1, 2}, //ACSF2 37
			{4, 1, 3}, //ACSF3 38
			{4, 1, 4}, //ACSF4 39	 		
	};
	
	public static String[] tscripts = {
			"Applications/LoanApproval/testscript.xml",
			"Applications/BookOrdering/testscript.xml",
			"Applications/SupplyChain/testscript.xml",
			"Applications/Insurance/testscript.xml",
			"Applications/Auction/testscript.xml"			
	};
	
	public static String[] savedPathMonitor = {
		null,
		null,
		null,
		"Applications/Insurance/PathCoverageMonitor",
		"Applications/Auction/PathCoverageMonitor"
	};


	public static void main(String[] args) {
		//start the engine
		engine.getEngine().startEngine();
		
		ServiceTesting.TestSerialTasks(args);
		
		//close the engine
		try {
    		Thread.sleep(10000);
    	} catch(Exception e) {}	
		engine.getEngine().closeEngine();
	}

}

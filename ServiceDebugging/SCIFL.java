package ServiceDebugging;

import engine.engine;

public class SCIFL {

	//CG: LA = 0, BO = 1, SC=2
	//FT: IF = 0, SF = 1	
	
	public static String[] CG= {
			"LA",
			"BO",
			"SC",
			"IN",
			"AC"			
	};
	
	public static String[] FT={
		"IF",
		"SF",
		"TF"
	};
	
	public static int[] TN = { //total number of activities/transitions 
		42,//LA
		66,//BO
		57,//SC
		110,//IN
		213//AC
	};
		
	public static int [][] tasks = {
			{0, 0, 0, 5},//LAIF 0 (6 inconsistency faults)
			
			{0, 1, 0, 45},//LASF 1 (46 single faults)
			
			{1, 0, 0, 31},//BOIF 2 (32 inconsistency faults)
			
			{1, 1, 0, 151},//BOSF 3 (152 single faults)
			
			{2, 0, 0, 19},//SCIF 4 (20 inconsistency faults)
			
			{2, 1, 0, 244},//SCSF 5	(245 single faults)								

			{3, 0, 0, 130},//INIF 6 (131 inconsistency faults)
			
			{3, 1, 0, 181},//INSF 7 (182 single faults)
			
			{4, 0, 0, 65},//ACIF 8 (66 inconsistency faults)			
			
			{4, 1, 0, 84} //ACSF 9 (85 single faults) 
			
	};
/*	
	public static int [][] tasks = {
		{0, 0, 0, 11},//LAIF 0
		
		{0, 1, 0, 68},//LASF 1
		
		{1, 0, 0, 48},//BOIF 2
		
		{1, 1, 0, 199},//BOSF 3
		
		{2, 0, 0, 34},//SCIF 4
		
		{2, 1, 0, 291},//SCSF 5									

		{3, 0, 0, 143},//INIF 6
		
		{3, 1, 0, 270},//INSF 7
		
		{4, 0, 0, 59},//ACIF 8
		
		{4, 0, 60, 119},//ACIF 9
		
		{4, 1, 0, 149},//ACSF 10
		
		{4, 1, 150, 299},//ACSF 11
		
		{4, 1, 300, 444},//ACSF 12
   }; */	
	
	public static String[] tscripts = {
			"Applications/LoanApproval/testscript.xml",
			"Applications/BookOrdering/testscript.xml",
			"Applications/SupplyChain/testscript.xml",
			"Applications/Insurance/testscript.xml",
			"Applications/Auction/testscript.xml"
	};

 
	public static void main(String[] args) {
		//start the engine
		engine.getEngine().startEngine();
		
		ServiceDebugging.TestSerialTasksOnly(args);
		
		//close the engine
		try {
    		Thread.sleep(10000);
    	} catch(Exception e) {}	
		engine.getEngine().closeEngine();
	}

}

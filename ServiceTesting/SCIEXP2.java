package ServiceTesting;

import engine.engine;

public class SCIEXP2 {
	
	/*
	 * Each mutation run 16 times
	 */
	
	public static String[] CG= {
		"LA",
		"BO",
		"SC"
	};

	public static String[] FT={
		"IF",
		"SF"
	};
	
	public static String[] tscripts = {
		"Applications/LoanApproval/testscript.xml",
		"Applications/BookOrdering/testscript.xml",
		"Applications/SupplyChain/testscript.xml"
	};

	int LAIF=12;//0-11
	int LASF=69;//12-80
	int BOIF=49;//81-129
	int BOSF=200;//130-329
	int SCIF=35;//330-364
	int SCSF=292;//365-656
	
	/*
	 * result[0]:tscript index
	 * result[1]:faulttype (0:IF, 1:SF)
	 * result[2]:mutation index
	 * result[3]:category index(0:LA, 1: BO, 2:SC) 
	 */
	public static int[] getAssignment(int index) {
		int[] result = new int[4];
		int cg=-1, ft=-1, ts=-1, mi=-1; 
		
		if(index>=0 && index<=11) {//LAIF
			cg=0;//LA
			ft=0;//IF
			ts=0;//LA
			mi=index;
		}
		
		if(index>=12 && index<=80) {//LASF
			cg=0;//LA
			ft=1;//SF
			ts=0;//LA
			mi=index-12;
		}
		
		if(index>=81 && index<=129) {//BOIF
			cg=1;//BO
			ft=0;//IF
			ts=1;//BO
			mi=index-81;
		}
		
		if(index>=130 && index<=329) {//BOSF
			cg=1;//BO
			ft=1;//IF
			ts=1;//BO
			mi=index-130;
		}
		
		if(index>=330 && index<=364) {//SCIF
			cg=2;//SC
			ft=0;//IF
			ts=2;//SC
			mi=index-330;
		}
		
		if(index>=365 && index<=656) {//SCSF
			cg=2;//SC
			ft=1;//SF
			ts=2;//SC
			mi=index-365;
		}
		
		result[0] = ts;
		result[1] = ft;
		result[2] = mi;
		result[3] = cg;
		
		return result;
	}

	/**
	 * @param args
	 * args[0]: approach (0: DUC, 1:PC)
	 * args[1]: index
	 * args[2]: end
	 * args[3]: appendix
	 * args[4]: outdir
	 * args[5]: cpdir
	 * args[6]: duration
	 */
	public static void main(String[] args) {
		int approach = new Integer(args[0]);
		int index = new Integer(args[1]);
		int end = new Integer(args[2]);
		String appendix = args[3];
		String outdir = args[4];
		String cpdir = args[5];
		long duration = new Long(args[6]);
		
		//start the engine
		engine.getEngine().startEngine();
		
		ServiceTesting.TestEqualEffectTasks(approach, index, end, appendix, outdir, cpdir, duration);
		
		//close the engine
		try {
    		Thread.sleep(10000);
    	} catch(Exception e) {}	
		engine.getEngine().closeEngine();

	}
	
	

}

package ServiceDebugging;

public class BPELAnalysis {

	public static void main(String[] args) {
		
		if(args.length>=3) {
			String filename = args[0];
			int evtnum = new Integer(args[1]);
			boolean detailedoutput = new Boolean(args[2]);
			analyzeOneFile(filename, evtnum, detailedoutput);
		}

	}

	/*
	 * filename: the name of the xml file to analyze
	 */
	private static boolean analyzeOneFile(String filename, int evtnum, boolean detailedoutput) {
		 
		 boolean complete = ServiceDebugging.LocalAnalyzeBPELTask(filename, evtnum, detailedoutput);
		 
		 return complete;
	}

	
}

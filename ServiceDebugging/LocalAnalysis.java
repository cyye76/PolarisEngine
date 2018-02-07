package ServiceDebugging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class LocalAnalysis {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length<4) return;
		init(args);
		analyzeLocalFilewithPattens();
		//analyzeAllLocalFiles();
		//analyzeLocalFileOntheFly();
		//analyzeOneFile("tmp/tmp7/LAIF101_events_0.xml");
		//analyzeOneFile("tmp/tmp7/LASF101_events_13.xml");
		//analyzeOneFile("tmp/tmp7/LAIF101_events_3.xml");
		//analyzeOneFile("tmp/tmp7/BOIF101_events_91.xml");
		//analyzeOneFile("tmp/tmp7/SCSF101_events_1.xml");
	}
	
	private static void analyzeLocalFilewithPattens() {
		int maxTerminate = 10; 
 		
					int terminate = maxTerminate;
					for(int ti=0;ti<=terminate;ti++) {
					    String filename =  pattern+"_events_" + ti+ ".xml.tar.gz";
					    //prefix
					    //String prefix = SCIFL.CG[category]+ SCIFL.FT[ftype];
					    					    
					    //check whether the file exists and is complete
					    boolean isCompleted = isCompleteFile(filename);
					    if(!isCompleted) continue;
					    
					    //analyze the file
					    String xmlfile = getEventXMLFileName(filename);
		        	    boolean complete = analyzeOneFile(xmlfile);
		        	    //if(complete) updateSavedParameter(prefix, filename);
					}

	}
	
	private static void analyzeLocalFiles() {
		int maxTerminate = 10; 
 		
		for(int category=0;category<SCIFL.CG.length;category++) //category
			for(int ftype=0;ftype<SCIFL.FT.length;ftype++)  {//fault types						
					int terminate = maxTerminate;
					for(int ti=0;ti<=terminate;ti++) {
					    String filename = SCIFL.CG[category]+ SCIFL.FT[ftype]
					    		+ pattern+"_events_" + ti+ ".xml.tar.gz";
					    //prefix
					    String prefix = SCIFL.CG[category]+ SCIFL.FT[ftype];
					    					    
					    //check whether the file exists and is complete
					    boolean isCompleted = isCompleteFile(filename);
					    if(!isCompleted) continue;
					    
					    //analyze the file
					    String xmlfile = getEventXMLFileName(filename);
		        	    boolean complete = analyzeOneFile(xmlfile);
		        	    if(complete) updateSavedParameter(prefix, filename);
					}
				}
	}
	
	private static void analyzeLocalFileOntheFly() {
		int[][] maxTerminate = { //
				{23, 45, 45}, //LAIF, LASF, LATF
				{97, 151, 151}, //BOIF, BOSF, BOTF
				{69, 244, 244}, //SCIF, SCSF, SCTF
				{383, 181, 181}, //INIF, INSF, INTF
				{119, 84, 84}  //ACIF, ACSF, ACTF
		};
 		
		for(int category=0;category<SCIFL.CG.length;category++) //category
			for(int ftype=0;ftype<SCIFL.FT.length;ftype++) //fault types			
				for(int core=1;core<=8;core++) {
					int terminate = maxTerminate[category][ftype];
					for(int ti=0;ti<=terminate;ti++) {
					    String filename = SCIFL.CG[category]+ SCIFL.FT[ftype]
					    		+core+pattern+"_events_" + ti+ ".xml.tar.gz";
					    //prefix
					    String prefix = SCIFL.CG[category]+ SCIFL.FT[ftype];
					    
					    //check whether this filename has been analyzed
					    boolean isAnalyzed = isAnalysis(prefix, filename);
					    if(isAnalyzed) continue;
					    
					    //check whether the file exists and is complete
					    boolean isCompleted = isCompleteFile(filename);
					    if(!isCompleted) continue;
					    
					    //analyze the file
					    String xmlfile = getEventXMLFileName(filename);
		        	    boolean complete = analyzeOneFile(xmlfile);
		        	    if(complete) updateSavedParameter(prefix, filename);
					}
				}
	}
		
	private static boolean isCompleteFile(String filename) {
		//first, check whether the file exists
		File file = new File(sourcedir + "/" + filename);
		if(!file.exists()) return false;
		return isCompleteXMLFile(sourcedir+"/"+filename);	
	}

	private static boolean isAnalysis(String prefix, String filename) {
		
		try{
			String param = getParameterFile(prefix);
			BufferedReader reader = new BufferedReader(new FileReader(param));
			String line = reader.readLine();
			while(line!=null) {
				if(line.equals(filename)) {
					reader.close();
					return true;
				}
				line = reader.readLine();
			}
			reader.close();
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		return false;
	}

/*	
	private static void analyzeAllLocalFiles() {	
        ArrayList<String> availablefiles = getAvailableEventFiles(sourcedir, pattern, param);
		
        for(String fn: availablefiles) {
        	 //Check whether the file is complete 
      	     if(isCompleteXMLFile(sourcedir+"/"+fn)) {
        	     String xmlfile = getEventXMLFileName(fn);
        	     boolean complete = analyzeOneFile(xmlfile);
        	     if(complete) updateSavedParameter(fn);
      	     }
        }
	}
*/	
	private static void updateSavedParameter(String prefix, String fn) {
		try {
			String param = getParameterFile(prefix);
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(param), true));
			writer.append(fn+ServiceDebugging.newline);
			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void init(String[] args) {
		sourcedir= args[0]; //the directory of the source event files
		workingdir=args[1]; //the tmp directory to check xml file and save results
		pattern = args[2]; //the pattern to list the source files 
		appd = args[3];
	}
	
	private static String getParameterFile(String prefix) {
		String param = workingdir+ "/" + prefix + pattern+"_Complete.txt"; //the file to save the list of source event files that have been handled
	    return param;
	}
	
	/*
	 * filename: the name of the xml file to analyze
	 */
	private static boolean analyzeOneFile(String filename) {
		 int mt_index = extractMutationIndex(filename);
		 if(mt_index<0) {
			 System.out.println("Invalid mutation index!");
			 return false;
		 }
		 
		 int faulttype = extractFaultType(filename);
		 if(faulttype<0) {
			 System.out.println("Invalid fault type!");
			 return false;
		 }
		 
		 int category = extractCategory(filename);
		 if(category<0) {
			 System.out.println("Invalid application category!");
			 return false;
		 }
		 
		 String savedFile = getLocalAnalysisResultFile(filename);
		 String CPEFileName = getLocalAnalysisCPEFileName(filename, mt_index);
		 boolean complete = ServiceDebugging.LocalAnalyzeOneTask(mt_index, faulttype, 
				 category, filename, savedFile, CPEFileName);
		 
		 return complete;
	}
	
	
	private static int extractCategory(String filename) {
		int category = 0;
		while(category<SCIFL.CG.length) {
		    if(filename.contains(SCIFL.CG[category])) return category;
		    category++;
		}
		return -1;
	}

	private static int extractFaultType(String filename) {
		int faulttype=0;
		while(faulttype<SCIFL.FT.length) {
		    if(filename.contains(SCIFL.FT[faulttype])) return faulttype;
		    faulttype++;
		}
		return -1;
	}

	private static int extractMutationIndex(String filename) {
		
		if(filename!=null) {
		     int start = filename.lastIndexOf('_')+1;
		     int end = filename.length() - ".xml".length();
		     String value = filename.substring(start,end);
		     return new Integer(value);
		}
		
		return -1;
	}


	private static String sourcedir=null; //the directory of the source event files
	private static String workingdir=null; //the tmp directory to check xml file and save results
	//private static String param = null; //the file to save the list of source event files that have been handled 
	private static String pattern = null; //the pattern to list the source files
	private static String appd = null; //the appd to differentiate different handling
	
	/*
	 *  Get all the available files to analyze
	 *  dirname: the directory of event source files
	 *  pattern: the pattern to list the source files
	 *  param: the file to save the param, i.e., the list of source files that have been analyzed
	 *  Return the list of file names matching the pattern 
	 */
	private static ArrayList<String> getAvailableEventFiles(String dirname, String pattern, String param) {
		 ArrayList<String> result = new ArrayList<String>();
		 
		 //1. List all the files in the directory matching with the pattern
		 File dir = new File(dirname);
		 if(dir.isDirectory()) {
			 
		      FilenameFilter filter = new MyFilter(pattern);
			  String[] filelist = dir.list(filter);
			 
		      //2. Check whether the file is already analyzed
			  ArrayList<String> analyzedfiles = getAnalysisFiles(param);
			  for(String fn: filelist) {
		             if(!analyzedfiles.contains(fn)) {		                  
		            		  result.add(fn);
		             }
			  }
		 }
		 
		 return result;
	}
	
	private static String getEventXMLFileName(String fn) {
		String filename = extractFilename(fn);
		String destination = workingdir+"/"+filename;
		int endIndex = destination.length() - ".tar.gz".length();
		String xmlfile = destination.substring(0, endIndex);
		return xmlfile;
	}
	
	private static String getLocalAnalysisResultFile(String filename) {
		int endindex=filename.indexOf("_events_");
		//return filename.substring(0, endindex)+"_result.txt";
		return filename.substring(0, endindex) + "_" + appd + "_result.txt";
	}
	
	private static String getLocalAnalysisCPEFileName(String filename, int mt_index) {
		int endindex=filename.indexOf("_events_");
		return filename.substring(0, endindex)+"_cpe_"+mt_index + ".txt";
	}

	private static boolean isCompleteXMLFile(String fn) {
		//first, copy the file to tmp
		String filename = extractFilename(fn);
		String destination = workingdir+"/"+filename;
		ServiceDebugging.copyArchivedFile(fn, destination);
		
		//next, dearchive and uncompress the file
		ServiceDebugging.dearchiveDestFile(destination, true);
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
			File file = new File(xmlfile);
			file.delete();
		}
		
		return iscomplete;
	}

	private static String extractFilename(String fn) {
		
		if(fn==null) return null;
			
		int index = fn.lastIndexOf('/') + 1;
		
		return fn.substring(index);
	}



	private static ArrayList<String> getAnalysisFiles(String param) {
		ArrayList<String> result = new ArrayList<String>();
		try{
			BufferedReader reader = new BufferedReader(new FileReader(param));
			String line = reader.readLine();
			while(line!=null) {
				result.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch(Exception e) {
			//e.printStackTrace();
		}
		
		return result;
	}

}

class MyFilter implements FilenameFilter{  
    private String pattern;  
    public MyFilter(String pattern){  
        this.pattern = pattern;  
    }  
    public boolean accept(File dir,String name){  
        return name.contains(pattern+"_events_");  
    }  
}  

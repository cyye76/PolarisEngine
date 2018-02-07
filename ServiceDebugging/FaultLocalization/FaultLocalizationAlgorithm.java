package ServiceDebugging.FaultLocalization;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.DataField;

import ServiceDebugging.AnalysisInfo;
import ServiceDebugging.ServiceDebugging;
import Configuration.Config;

public class FaultLocalizationAlgorithm {
	
	
	private Encapsulation m_encapsulation;
	private long totalLocations = 0;
	public FaultLocalizationAlgorithm(Encapsulation encap, long tn) {
		m_encapsulation = encap;
		//totalEventsNum = tn; //for resuming the ranking from a saved encapsulation
		totalLocations = tn;
		//solutions = FaultLocalizationSolution.init();
		//solutions = FaultLocalizationSolution.initGalton();
		//solutions = FaultLocalizationSolution.initGaltonAllEvents();

		m_encapsulation.setTopK((int) (totalLocations*0.15));
		
		solutions = FaultLocalizationSolution.initGaltonCompositeEvents();
	}
	
	public void FilterSolutions(ArrayList<Integer> keeps) {
		ArrayList<FaultLocalizationSolution> buffer = new ArrayList<FaultLocalizationSolution>(); 
        buffer.addAll(solutions);
        solutions.clear();
        for(Integer index: keeps) {
        	if(index<buffer.size()) {
        	   FaultLocalizationSolution sl = buffer.get(index);
        	   solutions.add(sl);
        	}
        }

	}
	
	private double percentage = 0;
	public void setPercentage(double value) {
		percentage = value;
	}
	

	private String formatDouble(double distance) {
		String sd = ""+ distance;
		int length = sd.length()>6? 6: sd.length();			
		
		return sd.substring(0, length);
	}
	
	public static String newline = System.getProperty("line.separator");
	
	private int approachindex;

	private String FLResult;

	public String faultLocalization(int appindex) throws FLTimeoutException{
		FLResult = "";				
		approachindex = appindex;
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		
		while(approachindex<solutions.size()) {
            FaultLocalizationSolution solution = solutions.get(approachindex);
            FLResult += faultLocalization(solution);
			approachindex++;
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		}
		
		return FLResult;
	}
	
	public String faultLocalizationBPEL(boolean detailedoutput) throws FLTimeoutException{
		
        FaultLocalizationSolution solution = solutions.get(0);
        m_encapsulation.setSolution(solution);
        SuspiciousEventRank rank = m_encapsulation.markSuspiciousEvents(); 		
		totalEventsNum = rank.totalEventsNum;
		
		//output the ranked event list
		FLResult = "";
		int num = (int)(totalEventsNum * 0.15);
		for(int i=0;i<num && i< rank.ranklist.length;i++) {
			RankRecord rd = rank.ranklist[i];
			for(int j=0;j<rd.eventIDs.length;j++) FLResult += rd.eventIDs[j] + "   " + rd.rankValue ;
			FLResult += newline;
			
			//output the corresponding event values in the cluster 2016.11.10
			if(rd.events!=null && detailedoutput) {
			   //FLResult += "[" ;
			   
			   for(ProbeEvent pe: rd.events) {
				   if(pe.fields!=null) {
					   for(DataField df: pe.fields) {
						   FLResult += "==>" +  df.getValue() + newline; 
					   }
				   }
			   }
			   //FLResult += "]";
			
			   FLResult += newline;
			}
		}
				
		return FLResult;
	}
	
	
	public void faultLocalizationNew(int appindex, AnalysisInfo info) throws FLTimeoutException{				
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		String result="";
		approachindex = appindex;
		while(approachindex<solutions.size()) {
			
			//debugging, filter others and run only these solutions
			//if(approachindex!=2 && approachindex!=3 && approachindex!= 9 && approachindex!=10) {
			//if(approachindex!= 9 && approachindex!=10) {
			//	approachindex++;
			//	continue;
			//}
			
			if(approachindex!= 11 && approachindex!=12) {
				approachindex++;
				continue;
			}
			
            FaultLocalizationSolution solution = solutions.get(approachindex);
            result = faultLocalization(solution);
                        
            //save the result
            boolean completed = (approachindex >= solutions.size()-1);
            ServiceDebugging.saveIntermediateResults(info, result, approachindex, completed);
            approachindex++;
            
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
		}
		
	}	
		
	public String faultLocalization(FaultLocalizationSolution solution) throws FLTimeoutException{
		String result = "";						
		
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
						
		m_encapsulation.setSolution(solution);
		result += faultLocalizationGeneral(solution.exposePolicy);				
		
		if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
					
		return result;
	}

	private String faultLocalizationGeneral(int policy) throws FLTimeoutException {
		if(policy==FaultLocalizationSolution.EXPOSEALL || policy==FaultLocalizationSolution.EXPOSEALLWITHSD) 
			return faultLoalizationWithAllEvents();
		
		if(policy==FaultLocalizationSolution.EXPOSEPORTONLY)
			return faultLocalizationWithPortEventsOnly();
		
		if(policy==FaultLocalizationSolution.EXPOSEENCAPSULATION)
		    return faultLocalizationWithRefinementNew();
		
		if(policy==FaultLocalizationSolution.EXPOSEENCAPSULATIONNEW)
		    return faultLocalizationWithRefinementNew();
		
		if(policy==FaultLocalizationSolution.EXPOSEENCAPSULATIONFOLD)
		    return faultLocalizationWithRefinementNew();
		
		return "";
	}


	private ArrayList<FaultLocalizationSolution> solutions; 
	public String faultLocalization() throws FLTimeoutException{		
		return faultLocalization(0);
	}
		
/*		
		approachindex = -1;
		
		for(int rank=0;rank<=12;rank++) {		
			
			approachindex++;//0, all events
			FLResult += faultLoalizationWithAllEvents(approachindex);
            if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			
			approachindex++;//1, port events only
			FLResult += faultLocalizationWithPortEventsOnly(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			
			approachindex++;//2, encapsulation and refinement
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			
			approachindex++;//3, statistical debugging
			if(approachindex == 3)
				FLResult += faultLoalizationWithAllEvents(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			
			approachindex++;//4, more accurate encapsulation and refinement 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			
			approachindex++;//5, more accurate encapsulation and coursed refinement 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//6, more accurate encapsulation and hybrid refinement lying between 4 and 5 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();

			approachindex++;//7, all events with composite events  
			FLResult += faultLoalizationWithAllEvents(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//8, port events only with composite events
			FLResult += faultLocalizationWithPortEventsOnly(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//9, encapsulation and refinement with composite events
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//10, more accurate encapsulation and refinement with composite events 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//11, more accurate encapsulation and coursed refinement with composite events 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
            
			approachindex++;//12, more accurate encapsulation and hybrid refinement lying between 4 and 5 with composite events 
			FLResult += faultLocalizationWithRefinement(approachindex);
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			//line += newline;
		}
		
		return FLResult;
	}
	*/		
	
	public long totalEventsNum;
	private String faultLocalizationWithRefinement() throws FLTimeoutException {
				
		String line = "";
		for(int i=1;i<=3;i++) {					 
			SuspiciousEventRank rank = m_encapsulation.markSuspiciousEvents();		
			
			setPercentage(0.05 * i);
			
			//long examize_num = Math.round(totalEventsNum * percentage); 
			long examize_num = Math.round(totalLocations * percentage);
			if(examize_num<1) examize_num=1;
        
			boolean needRefine = m_encapsulation.needRefinement(rank, examize_num);
			while(needRefine) {
				if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
				m_encapsulation.refineEvents(rank); 				
				
				//rank the events again
				m_encapsulation.markSuspiciousEvents(rank);
				
				//totalEventsNum = rank.totalEventsNum;
				//examize_num = Math.round(totalEventsNum * percentage);				
				needRefine = m_encapsulation.needRefinement(rank, examize_num);
			}
								
			double distance = calculateRankedEventDistance(rank, examize_num);				 
			//double foundrate = faultCoveredRate(rank, examize_num);
			double foundrate = faultCoveredRateNew(rank, examize_num);
			double leakage = calculateInformationLeakage(rank);
			
            double ldist = leastDistance(rank, examize_num);
            double expense = calculateExpense(rank);
			
			//for debug
            //double foundratenew = faultCoveredRate_DU(rank, examize_num);
			
			long exposedNum = rank.totalEventsNum;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+ examize_num)  + formatString("" + leakage);;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+formatDouble(foundratenew)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
			line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
			line += newline;
			
			//for debugging
			//outputComposteEventInfo(rank);
			
		}
				
				
		return line;		
	}	
	
	private String faultLocalizationWithRefinementNew() throws FLTimeoutException {
		//added on 2017.01.01
		long start_time = System.currentTimeMillis();
		SuspiciousEventRank rank = m_encapsulation.markSuspiciousEvents();
				
		long topK1 = Math.round(totalLocations * 0.05);
		long topK = Math.round(totalLocations * 0.15);
		//first select the top examize_num encapsulated events to refine
		ArrayList<String> eIDs = refineTopKEncapsulatedEvent(rank, topK1);
		//m_encapsulation.markSuspiciousEvents(rank);
		m_encapsulation.reRankSuspiciousEvents(rank, eIDs);
		
		//next, refine topK ranking until there is no encapsulatedEvents left
		boolean needRefine = m_encapsulation.needRefinement(rank, topK);
		while(needRefine) {
			if(ServiceDebugging.needTerminated) throw new FLTimeoutException();
			eIDs = m_encapsulation.refineEvents(rank); 				
			
			//rank the events again
			//m_encapsulation.markSuspiciousEvents(rank);
			m_encapsulation.reRankSuspiciousEvents(rank, eIDs);
			
			//totalEventsNum = rank.totalEventsNum;
			//examize_num = Math.round(totalEventsNum * percentage);				
			needRefine = m_encapsulation.needRefinement(rank, topK);
		}
		
		//added on 2017.01.01
		long end_time = System.currentTimeMillis();
		long total_time = end_time - start_time;
		
		String line = "";
		for(int i=1;i<=3;i++) {					 
								
			setPercentage(0.05 * i);
			
			//long examize_num = Math.round(totalEventsNum * percentage); 
			long examize_num = Math.round(totalLocations * percentage);
			if(examize_num<1) examize_num=1;
									
			double distance = calculateRankedEventDistance(rank, examize_num);				 
			//double foundrate = faultCoveredRate(rank, examize_num);
			double foundrate = faultCoveredRateNew(rank, examize_num);
			//double leakage = 0;//calculateInformationLeakage(rank);
			double leakage = calculateInformationLeakage(rank);
			
            double ldist = leastDistance(rank, examize_num);
            double expense = calculateExpense(rank);
			
			//for debug
            //double foundratenew = faultCoveredRate_DU(rank, examize_num);
			
			long exposedNum = rank.totalEventsNum;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+ examize_num)  + formatString("" + leakage);;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+formatDouble(foundratenew)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
			
			//added on 2017.01.01
			line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate))  +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense) + formatString("  " + total_time) ;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate))  +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
			line += newline;			
		}
				
		//for debugging
		//outputComposteEventInfo(rank);
		
		return line;		
	}	

	private ArrayList<String> refineTopKEncapsulatedEvent(SuspiciousEventRank rank,
			long examize_num) {
		
		ArrayList<String> needRefined = new ArrayList<String>();		
		int index = 0;
		while(examize_num>needRefined.size() && index<rank.ranklist.length) {
		     RankRecord rd = rank.ranklist[index];
		     
		     for(String eID:rd.eventIDs)  
		         if (m_encapsulation.isEncapsulated(eID, rank.failureEvents, rank.successEvents)
		        	 && !needRefined.contains(eID)) 
		        	 needRefined.add(eID);	
		           		         
		     index++;		     
		}
		
		ArrayList<RankRecord> leftRecords = new ArrayList<RankRecord>();
		for(RankRecord rd: rank.ranklist) {
			if(!containEvents(rd, needRefined)) {
				leftRecords.add(rd);
			}
		}
		
		rank.ranklist = new RankRecord[leftRecords.size()];
		for(int i=0;i<rank.ranklist.length;i++)
			rank.ranklist[i] = leftRecords.get(i);
				
		return m_encapsulation.refineEvents(rank, needRefined);
	}


	private boolean containEvents(RankRecord rd, ArrayList<String> needRefined) {
		for(String eID:rd.eventIDs)
			if(needRefined.contains(eID)) return true;
		return false;
	}


	private String faultLocalizationWithPortEventsOnly() throws FLTimeoutException{
		String line="";
		
		//added on 2017.01.01
		long start_time = System.currentTimeMillis();
		SuspiciousEventRank rank = m_encapsulation.markSuspiciousEvents();
		
		//added on 2017.01.01
		long end_time = System.currentTimeMillis();
		long total_time = end_time - start_time;
		
		double expense = calculateExpense(rank);

		for(int i=1;i<=3;i++) {
			
			setPercentage(0.05 * i);
		
			//long examize_num = Math.round(totalEventsNum * percentage); 
			long examize_num = Math.round(totalLocations * percentage);
			if(examize_num<1) examize_num=1;
			
			double distance = calculateRankedEventDistance(rank, examize_num);	
		
            //double foundrate = faultCoveredRate(rank, examize_num);
            double foundrate = faultCoveredRateNew(rank, examize_num);	
            //double leakage = 0;//calculateInformationLeakage(rank);
            double leakage = calculateInformationLeakage(rank);
            double ldist = leastDistance(rank, examize_num);
            
			//for debug
            //double foundratenew = faultCoveredRate_DU(rank, examize_num);
            
            long exposedNum = rank.totalEventsNum;
			//line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+formatDouble(foundratenew)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
            
            //changed on 2017.01.01
            line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense) + formatString("  " + total_time);
            //line += formatString(""+approachindex) + formatString(""+exposedNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
            line += newline;
			          
		}	
		
		//for debugging
		//outputComposteEventInfo(rank);
		
		return line; 
		
	}	
	
	/**
	 * This function is for debugging 
	 * @param rank
	 */
	private void outputComposteEventInfo(SuspiciousEventRank rank) {
		
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(cpefilename, true));
		    //BufferedWriter writer = new BufferedWriter(new FileWriter("compositeEvent.txt", true));
			
		    String line = "";	
		    
		    double maxvalue = -1;		    		    
		    
		    if(rank.ranklist.length>0) {		    	
		    	
		    	ArrayList<String> mutation = m_encapsulation.getMutations();
		    	for(String mtl: mutation) 
		    		line += "Muation:" + mtl + newline; 
		    	
		    	maxvalue = rank.ranklist[0].rankValue;
		    	line += "Maximum rankvalue:" + rank.ranklist[0].rankValue + newline;
/*		    	for(RankRecord rd:rank.ranklist) {
		    		if(rd.eventIDs.length==1 && rd.rankValue>= maxvalue) {
		    			double fcr = faultCoverageRate(rd, rank);
						double dist = calculateRankedEventDistance(rd, rank);
						double probinc = rd.prob - calculateEventProb(rd.eventIDs[0], rank);
						line += "    " + rd.eventIDs[0] + ": rankvalue = " + rd.rankValue + ", prob= " + rd.prob  + ", cover: " 
						        + fcr + ", distance:" + dist + ", probInc=" + probinc + ", reverse rankvalue:" + rd.reverse_rankvalue +
						        ", reverse_prob:" + rd.reverse_prob;
						line +=newline;
		    		}
		    	}
		    	
		    	//line += "Maximum rankvalue:" + rank.ranklist[0].rankValue + " by " + rank.ranklist[0].recordID;		    	
		    	//line += newline;
		    	
		    	writer.write(line);
                line = "Compostie Events:" + newline;*/
		    }
		    
		    int num = 1;
			for(RankRecord rd:rank.ranklist) {				
				//if(rd.eventIDs.length>1) {
				if(num<30) { 	
					double fcr = faultCoverageRate(rd, rank);
					double dist = calculateRankedEventDistance(rd, rank);
					//double probinc = rd.prob - calculateCompositeEventProb(rd.eventIDs, rank);
					line += num + ". ";
					//line += rd.recordID + ": rankvalue = " + rd.rankValue + ", prob= " + rd.prob + ", cover: " + fcr + ", distance:" + dist + ", probInc=" + probinc + ", reverse rankvalue:" + rd.reverse_rankvalue +
					//        ", reverse_prob:" + rd.reverse_prob;
					line += rd.recordID + ": rankvalue = " + rd.rankValue + ", prob= " + rd.prob + ", cover: " + fcr + ", distance:" + dist + ", fn:" + rd.fn + ", sn:" + rd.sn; 							        
					        
					line += newline;
					
					if(rd.eventIDs.length>1)
					for(String eID: rd.eventIDs) {
						RankRecord srd = getSingleEventRankRecord(rank, eID);
						if(srd!=null) {
							fcr = faultCoverageRate(srd, rank);
							dist = calculateRankedEventDistance(srd, rank);
							//probinc = srd.prob - calculateEventProb(eID, rank);
							//line += "    " + eID + ": rankvalue = " + srd.rankValue + ", prob= " + srd.prob  + ", cover: " + fcr + ", distance:" + dist + ", probInc=" + probinc + ", reverse rankvalue:" + srd.reverse_rankvalue +							
							//        ", reverse_prob:" + srd.reverse_prob;
							line += "    " + eID + ": rankvalue = " + srd.rankValue + ", prob= " + srd.prob  + ", cover: " + fcr + ", distance:" + dist;
							line +=newline;
						}
					}
					
                    line += newline;
                    line += newline;
					
                    writer.write(line);
                    line = "";
                    num++;
				}
			}
			
			writer.close();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private double calculateCompositeEventProb(String[] eventIDs,
			SuspiciousEventRank rank) {
		
		if(eventIDs.length==0) return 0;
		
		ArrayList<String> finsts = new ArrayList<String>();
		ArrayList<String> sinsts = new ArrayList<String>();
		String eID = eventIDs[0];
		
		ArrayList<ProbeEvent> flist = rank.failureEvents.get(eID);
		ArrayList<ProbeEvent> slist = rank.successEvents.get(eID);
		
		if(flist!=null)
		for(ProbeEvent pe: flist)
			finsts.add(pe.instanceID);
		
		if(slist!=null)
		for(ProbeEvent pe: slist)
			sinsts.add(pe.instanceID);
		
		int fnum = statisticsEventNum(eventIDs, finsts, rank.failureEvents);
		int snum = statisticsEventNum(eventIDs, sinsts, rank.successEvents);				
		
		return (fnum*1.0)/(fnum + snum);
	}


	private int statisticsEventNum(String[] eventIDs, ArrayList<String> finsts,
			HashMap<String, ArrayList<ProbeEvent>> maps) {
		int count = 0;
		for(String inst: finsts) {
			if(existEventsWithInst(maps, inst, eventIDs)) count++;
		}
		
		return count;
	}


	private boolean existEventsWithInst(
			HashMap<String, ArrayList<ProbeEvent>> maps, String inst,
			String[] eventIDs) {
		
		for(String eID: eventIDs) {
			ArrayList<ProbeEvent> list = maps.get(eID);
			if(list==null) return false;
			boolean exist = false;
			for(ProbeEvent pe: list)
				if(pe.instanceID.equals(inst)) {
					exist = true;
					break;
				}
			
			if(!exist) return false;
		}
		
		return true;
	}


	private double calculateEventProb(String eID, SuspiciousEventRank rank) {
		
		ArrayList<ProbeEvent> flist = rank.failureEvents.get(eID);
		ArrayList<ProbeEvent> slist = rank.successEvents.get(eID);
		
		int fnum= flist!=null?flist.size():0;
		int snum= slist!=null?slist.size():0;
		
		return (fnum*1.0)/(fnum+snum);
	}


	private RankRecord getSingleEventRankRecord(SuspiciousEventRank rank,
			String eID) {
        
		for(RankRecord rd: rank.ranklist) {
			if(rd.eventIDs.length==1 && rd.eventIDs[0].equals(eID))
				return rd;
		}
		return null;
	}


    private double allevent_leakage = -1.0;
    private double sdevent_leakage = -1.0;
	private String faultLoalizationWithAllEvents() throws FLTimeoutException {
		//added on 2017.01.01
		long start_time = System.currentTimeMillis();
		
		SuspiciousEventRank rank = m_encapsulation.markSuspiciousEvents(); 		
		totalEventsNum = rank.totalEventsNum;
		
		//added on 2017.01.01
		long end_time = System.currentTimeMillis();
		long total_time = end_time - start_time;
                
        double leakage = -1.0;
        FaultLocalizationSolution solution = m_encapsulation.getSolution();
                
        if(solution.exposePolicy == FaultLocalizationSolution.EXPOSEALL) {
        	  //uncommented on 2017.01.01
              if(allevent_leakage < 0) 
                    allevent_leakage = calculateInformationLeakage4AllEvents(rank);

              leakage = allevent_leakage;
        }

        if(solution.exposePolicy == FaultLocalizationSolution.EXPOSEALLWITHSD) {
        	  //uncommented on 2017.01.01 
              if(sdevent_leakage < 0)
                    sdevent_leakage = calculateInformationLeakage4AllEvents(rank);

              leakage = sdevent_leakage; 
        }
		
		//double leakage = calculateInformationLeakage4AllEvents(rank);
		double expense = calculateExpense(rank);
		
		String line = "";
		for(int i=1;i<=3;i++) {
			
			setPercentage(0.05 * i);
			
			//long examize_num = Math.round(totalEventsNum * percentage);
			long examize_num = Math.round(totalLocations * percentage);
			if(examize_num<1) examize_num=1;
		
			double distance = calculateRankedEventDistance(rank, examize_num);			
			
			double foundrate = faultCoveredRateNew(rank, examize_num);	
			
			double ldist = leastDistance(rank, examize_num);
			
			//for debug
            //double foundratenew = faultCoveredRate_DU(rank, examize_num);
			
			//changed on 2017.01.01
			line += formatString(""+approachindex) + formatString(""+totalEventsNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense) + formatString("  " + total_time);
			//line += formatString(""+approachindex) + formatString(""+totalEventsNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
            //line += formatString(""+approachindex) + formatString(""+totalEventsNum) + formatString(formatDouble(distance)) + formatString(""+formatDouble(foundrate)) + formatString(""+formatDouble(foundratenew)) +formatString(""+ examize_num) + formatString("" + leakage) + formatString("     " + ldist) + formatString("  " + expense);
			line += newline;
		}
		
		//for debugging
		//outputComposteEventInfo(rank);
		
		return line; 		
	}	

	/**
	 * Calculate the mutual information between all events and the exposed events
	 * @param rank
	 * @return
	 */
	private double calculateInformationLeakage(SuspiciousEventRank rank) {
        /*
         * X: all events, Y: exposed events
         * I(X;Y) = \sum_{x\in X}\sum_{y in Y}p(x, y) log (p(x,y)/(p(x)p(y)))
         */
		HashMap<ProbeEvent, HashMap<ProbeEvent, Double>> probmaps = m_encapsulation.calculateEventProb();
		ArrayList<ProbeEvent> Y = getExposedEvents(rank);
		Set<ProbeEvent> X = probmaps.keySet();
		
		double result = 0;
		
		for(ProbeEvent ex: X) {
			HashMap<ProbeEvent, Double> submapx = probmaps.get(ex);
			double px = getProbValue(submapx, ex);
			for(ProbeEvent ey: Y) {
				double pxy = getProbValue(submapx, ey);
				HashMap<ProbeEvent, Double> submapy = getSubMap(probmaps, ey);				
				double py = getProbValue(submapy, ey);
				
				if(pxy!=0 && px !=0 && py!=0) {
					result += pxy * Math.log(pxy /(px * py));
				}
			}
		}
		
		return result;
	
	}

	/**
	 * Calculate the mutual information between all events and the exposed events
	 * @param rank
	 * @return
	 */
	private double calculateInformationLeakage4AllEvents(SuspiciousEventRank rank) {
        /*
         * X: all events, Y: exposed events
         * I(X;Y) = \sum_{x\in X}\sum_{y in Y}p(x, y) log (p(x,y)/(p(x)p(y)))
         */
		ArrayList<ProbeEvent> Y = getExposedEvents(rank);
		Double[][] probmaps = calculateEventProb(rank, Y);
		
		double result = 0;
		
                int num = Y.size();
		for(int i=0;i<num;i++) {
			Double[] submapx = probmaps[i];
			Double px = submapx[i];
			for(int j=0;j<num;j++) {
				Double pxy = submapx[j];
				Double[] submapy = probmaps[j];
                                Double py = null;
                                if(submapy != null) py = submapy[j];
		
				if(px!=null && pxy!=null && py!=null && pxy!=0 && px !=0 && py!=0) {
					result += pxy * Math.log(pxy /(px * py));
				}
			}
		}
		
		return result;
	
	}

    private Double[][] calculateEventProb(SuspiciousEventRank rank, ArrayList<ProbeEvent> Y) {
        int num = Y.size();
        Double[][] result = new Double[num][num];
             
        for(int i=0;i<num;i++) {
            ProbeEvent ex = Y.get(i);
            Double[] mapx = new Double[num];
            result[i] = mapx;
            for(int j=i; j<num; j++) {
                ProbeEvent ey = Y.get(j);
                mapx[j] = m_encapsulation.calculateEP(rank, ex, ey);
            }
        }   

        for(int i=0;i<num;i++) 
            for(int j=0;j<i;j++)
                result[i][j] = result[j][i];

        return result;
     }

/*
	private double calculateInformationLeakageNew(SuspiciousEventRank rank) {
        /*
         * X: all events, Y: exposed events
         * I(X;Y) = \sum_{x\in X}\sum_{y in Y}p(x, y) log (p(x,y)/(p(x)p(y)))
         *
        HashMap<String, Integer> contentMap = m_encapsulation.getEventContentmap();
		HashMap<Integer, HashMap<Integer, Double>> probmaps = m_encapsulation.getEventProbMap();
		ArrayList<ProbeEvent> Y = getExposedEvents(rank);
		Set<Integer> X = probmaps.keySet();
		
		double result = 0;
		
		for(Integer ex: X) {
			HashMap<Integer, Double> submapx = probmaps.get(ex);
			Double px = submapx.get(ex);
			if(px==null) px=0.0;
			for(ProbeEvent ey: Y) {
				String uID = ey.generateUID();
                                Integer yid = contentMap.get(uID);
				Double pxy = submapx.get(yid);
				if(pxy==null)pxy=0.0;
				HashMap<Integer, Double> submapy = probmaps.get(yid);				
				Double py = submapy.get(yid);
				if(py==null)py=0.0;
				
				if(pxy!=0 && px !=0 && py!=0) {
					result += pxy * Math.log(pxy /(px * py));
				}
			}
		}
		
		return result;
	}
*/
	private HashMap<ProbeEvent, Double> getSubMap(
			HashMap<ProbeEvent, HashMap<ProbeEvent, Double>> probmaps,
			ProbeEvent ey) {
		
		Set<ProbeEvent> keys = probmaps.keySet();
		for(ProbeEvent pe: keys)
			if(pe.isSameValueWithSameEventID(ey)) return probmaps.get(pe);
		
		return null;
	}


	private double getProbValue(HashMap<ProbeEvent, Double> submap,
			ProbeEvent ex) {
		
		if(submap==null) return 0;
		
		Set<ProbeEvent> keys = submap.keySet();
		for(ProbeEvent pe: keys)
			if(pe.isSameValueWithSameEventID(ex)) return submap.get(pe);
		
		return 0;
	}


	private ArrayList<ProbeEvent> getExposedEvents(SuspiciousEventRank rank) {
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		Set<String> eIDs = rank.failureEvents.keySet();
		ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
		for(String eID: eIDs) 			
			buffer.addAll(rank.failureEvents.get(eID));
				
		eIDs = rank.successEvents.keySet();
		for(String eID: eIDs)
			buffer.addAll(rank.successEvents.get(eID));
		
		for(ProbeEvent pe: buffer) 
			if(notIncluded(pe, result)) result.add(pe);
		
		return result;
	}


	private boolean notIncluded(ProbeEvent pe, ArrayList<ProbeEvent> result) {
		
		for(ProbeEvent event: result)
			if(pe.isSameValueWithSameEventID(event)) return false;
		
		return true;
	}


	private String formatString(String input) {
        String ret = input;
        for(int i=ret.length();i<10;i++)
        	ret += " ";
		return ret;
	}
	
	private ArrayList<String> getRankedLocationsNew(SuspiciousEventRank rank) {
		int index = 0;
		ArrayList<String> result = new ArrayList<String>();
		while(index < rank.ranklist.length) {
		    ArrayList<RankRecord> rlist = getRankedRecodList(rank, index);
		    index+= rlist.size();
		    ArrayList<String> tmp = getRankedLocationsWithSameRankedValue(rlist, rank);
		    for(String loc: tmp) 
		    	if(!result.contains(loc)) result.add(loc);
		}
		
		return result;
	}
	
	private ArrayList<String> getRankedLocationsWithSameRankedValue(
			ArrayList<RankRecord> rlist, SuspiciousEventRank rank) {
		//the list of RankRecord input has the same rank value and the same number of events
		ArrayList<String> eIDlist = new ArrayList<String>();
		for(RankRecord rd: rlist) {
		    for(String eID: rd.eventIDs)
		    	if(!eIDlist.contains(eID)) eIDlist.add(eID);
		}
		
		ArrayList<String> result = new ArrayList<String>();
			
		while(!eIDlist.isEmpty()) {
			int selected = 0;
		    for(int i=selected+1;i<eIDlist.size();i++) {
		    	   String comparedID = eIDlist.get(i);
		    	   String eID = eIDlist.get(selected);
		    	   if(hasHigherPriority(eID, comparedID, rank)) 
		    		   selected = i;
		    }
		    
		    //add the loc of the top priority one
		    String eID = eIDlist.get(selected);
			ProbeEvent pe = getProbeEventbyID(eID, rank);
			if(pe!=null) {
			     String Loc = pe.serviceName + ":" + pe.eventToken;
			     if(!result.contains(Loc)) result.add(Loc);
			}
			
			//remove eID from the eIDlist
			eIDlist.remove(selected);
		}
		
		return result;
	}
	
	private ProbeEvent getProbeEventbyID(String eID, SuspiciousEventRank rank) {
		ArrayList<ProbeEvent> eventf = rank.failureEvents.get(eID);
		ArrayList<ProbeEvent> events = rank.successEvents.get(eID);
		ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
		if(eventf!=null) buffer.addAll(eventf);
		if(events!=null) buffer.addAll(events);
		
		if(buffer.isEmpty()) return null;
		ProbeEvent pe = buffer.get(0);
		return pe;
	}

    /*
     * implement the heuristics here to compare two events
     */
	private boolean hasHigherPriority(String eID, String comparedID,
			SuspiciousEventRank rank) {
		ProbeEvent pe1 = getProbeEventbyID(eID, rank);
		ProbeEvent pe2 = getProbeEventbyID(comparedID, rank);
		String sn1 = pe1.serviceName;
		String sn2 = pe2.serviceName;
		
		if(sn1.equals(sn2) && hasSameField(pe1, pe2)) {
			 int type1 = getEventTypeByEventID(eID);
			 int type2 = getEventTypeByEventID(comparedID);
			 return type1 > type2;
		}
		
		return false;
	}

    /*
     *  3: transition_dataread
     *  2: task_datamodification
     *  1: task_dataread
     *  0: else
     */
	private int getEventTypeByEventID(String eID) {
		
		if(eID.contains("_dataread_Transition")) return 3;
		if(eID.contains("_datamodification_Task")) return 2;
		if(eID.contains("_dataread_Task")) return 1;
		
		return 0;
	}


	private boolean hasSameField(ProbeEvent pe1, ProbeEvent pe2) {
		
		ArrayList<DataField> fields1 = pe1.fields;
		ArrayList<DataField> fields2 = pe2.fields;
		
		if(fields1.size()!=fields2.size()) return false;
		
		for(DataField df1: fields1) {
			boolean matched = false;
			for(DataField df2: fields2) {
				if(df2.getName().equals(df1.getName())) {
					matched = true;
					break;
				}
			}
			
			if(!matched) return false;
		}
		
		for(DataField df2: fields2) {
			boolean matched = false;
			for(DataField df1: fields1) {
				if(df2.getName().equals(df1.getName())) {
					matched = true;
					break;
				}
			}
			
			if(!matched) return false;
		}
		
		return true;
	}


	private ArrayList<RankRecord> getRankedRecodList(SuspiciousEventRank rank,
			int index) {
		ArrayList<RankRecord> result = new ArrayList<RankRecord>();
		if(index<rank.ranklist.length) {
		     RankRecord rd = rank.ranklist[index];
		     double rvalue = rd.rankValue;
		     int eventnum = rd.eventIDs.length;
		     result.add(rd);
		     for(int i=index+1;i<rank.ranklist.length;i++) {
		           rd = rank.ranklist[i];
		           if(Math.abs(rvalue - rd.rankValue)<0.000001 && rd.eventIDs.length == eventnum) {
		        	   result.add(rd);
		           }
		     }
		}
		
		return result;
	}

	private String getRankedEventLocation(SuspiciousEventRank rank, String eID) {
		ArrayList<ProbeEvent> eventf = rank.failureEvents.get(eID);
		ArrayList<ProbeEvent> events = rank.successEvents.get(eID);
		ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
		if(eventf!=null) buffer.addAll(eventf);
		if(events!=null) buffer.addAll(events);
		
		if(buffer.isEmpty()) return null;
		ProbeEvent pe = buffer.get(0);
		String Loc = pe.serviceName + ":" + pe.eventToken;
		return Loc;
	}

	private ArrayList<String> getRankedLocations(SuspiciousEventRank rank) {
		ArrayList<String> result = new ArrayList<String>();
		
		for(RankRecord record: rank.ranklist) {
			String[] eventIDs = record.eventIDs;
			if(eventIDs == null || eventIDs.length==0) continue;
			
			for(String eID: eventIDs) {
				ArrayList<ProbeEvent> eventf = rank.failureEvents.get(eID);
				ArrayList<ProbeEvent> events = rank.successEvents.get(eID);
				ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
				if(eventf!=null) buffer.addAll(eventf);
				if(events!=null) buffer.addAll(events);
				
				if(buffer.isEmpty()) continue;
				ProbeEvent pe = buffer.get(0);
				String Loc = pe.serviceName + ":" + pe.eventToken;
				if(!result.contains(Loc)) result.add(Loc);
			}
		}
		
		return result;
	}
	
	private double calculateExpense(SuspiciousEventRank rank) {
		ArrayList<String> locs = getRankedLocations(rank);
		ArrayList<String> mLoc_list = m_encapsulation.getMutations();
		
		ArrayList<String> covered = new ArrayList<String>();
                int mtratio = Config.getConfig().mutationRatio;//used to differentiate single fault/inconsistency fault and multiple faults
		int count = 0;
		for(String loc: locs) {
			count++;
			if(mLoc_list.contains(loc) && !covered.contains(loc)) covered.add(loc);
			if(covered.size()* mtratio >=mLoc_list.size()) break;
		}
		
		return (count*1.0)/totalLocations;
	}
	
	private double leastDistance(SuspiciousEventRank rank, long num) {
		int mutationNum = m_encapsulation.getFaultNum();
	
		ArrayList<String> coveredM = new ArrayList<String>();
			
		int i=0;
		double distance=0;
		ArrayList<String> visitedLocs = new ArrayList<String>();

		while(rank.ranklist.length>i) {
				
			String[] eventIDs = rank.ranklist[i].eventIDs;
			if(eventIDs == null || eventIDs.length==0) continue;
							
			for(String eID: eventIDs) {
				String loc = getRankedEventLocation(rank, eID);
				if(loc == null || visitedLocs.contains(loc)) continue;
				visitedLocs.add(loc);
				
				ArrayList<ProbeEvent> events = rank.failureEvents.get(eID);
				
				if(events!=null) {
					CoverFaultsbyEvent(events, coveredM);				
					distance ++;				
				}
			}				
			
			i++;			
			
			if(coveredM.size()>=mutationNum) break;
			
		 }
		               
		 return distance;	
	}	
	
/*	private double leastDistance(SuspiciousEventRank rank, long num) {
		int mutationNum = m_encapsulation.getFaultNum();
	
		ArrayList<String> coveredM = new ArrayList<String>();
			
		int i=0;
		int ec = 0;
		double distance=0;
                boolean hasItem = false;
		while(ec<num && rank.ranklist.length>i) {
				
			String[] eventIDs = rank.ranklist[i].eventIDs;
			if(eventIDs == null) continue;
				
			for(String eID: eventIDs) {
				ArrayList<ProbeEvent> events = rank.failureEvents.get(eID);
				
				if(events!=null) {
					CoverFaultsbyEvent(events, coveredM);
					double edist = 0;
					for(ProbeEvent ev: events) {
						double t_d = m_encapsulation.calculateDistance(ev);
						edist += t_d;						
					}
					
					distance += edist/events.size();

                                        hasItem = true;
				}
			}
				
			ec+= eventIDs.length;
			
			i++;			
			
			if(coveredM.size()>=mutationNum) break;
			
		 }
		
                if(hasItem)
		   return distance;	
                else
                   return m_encapsulation.maxGlobalPath;			
	}	*/
	
	private double faultCoveredRate(SuspiciousEventRank rank, long num) {
		int mutationNum = m_encapsulation.getFaultNum();
	
		ArrayList<String> coveredM = new ArrayList<String>();
			
		int i=0;
		int ec = 0;
		while(ec<num && rank.ranklist.length>i) {
		//for(int i=0;i<num;i++) {
		
			//if(rank.ranklist.length>i) {
				
				String[] eventIDs = rank.ranklist[i].eventIDs;
				if(eventIDs == null) continue;
				
				for(String eID: eventIDs) {
					ArrayList<ProbeEvent> events = rank.failureEvents.get(eID);
				
					if(events!=null) CoverFaultsbyEvent(events, coveredM);	
				}
				
				ec+= eventIDs.length;
											
			//}
			
			i++;
			
		 }
		
		return coveredM.size()*1.0/mutationNum;				
	}	
	
	private double faultCoveredRate_DU(SuspiciousEventRank rank,
			long examize_num) {
		
		int mutationNum = m_encapsulation.getFaultNum();
		
		ArrayList<String> coveredM = new ArrayList<String>();
		ArrayList<String> locs = getRankedLocationsNew(rank);
		ArrayList<String> mLocs = m_encapsulation.getMutations();
		
		for(int i=0;i<examize_num && i<locs.size();i++) {
			String loc = locs.get(i);
			if(mLocs.contains(loc) && !coveredM.contains(loc)) coveredM.add(loc);
		}
 			
		return (coveredM.size()*1.0)/mutationNum;		
	}

	private double faultCoveredRateNew(SuspiciousEventRank rank, long num) {
		int mutationNum = m_encapsulation.getFaultNum();
	
		ArrayList<String> coveredM = new ArrayList<String>();
		ArrayList<String> locs = getRankedLocations(rank);
		ArrayList<String> mLocs = m_encapsulation.getMutations();
		
		for(int i=0;i<num && i<locs.size();i++) {
			String loc = locs.get(i);
			if(mLocs.contains(loc) && !coveredM.contains(loc)) coveredM.add(loc);
		}
 			
		return (coveredM.size()*1.0)/mutationNum;				
	}	
	
	private double faultCoverageRate(RankRecord rd, SuspiciousEventRank rank) {
		int mutationNum = m_encapsulation.getFaultNum();
		ArrayList<String> coveredM = new ArrayList<String>();
		
		for(String eID: rd.eventIDs) {

			ArrayList<ProbeEvent> eventf = rank.failureEvents.get(eID);
			ArrayList<ProbeEvent> events = rank.successEvents.get(eID);
			ArrayList<ProbeEvent> buffer = new ArrayList<ProbeEvent>();
			if(eventf!=null) buffer.addAll(eventf);
			if(events!=null) buffer.addAll(events);						
			
			CoverFaultsbyEvent(buffer, coveredM);	
		}
		
		return coveredM.size()*1.0/mutationNum;
	}

	private void CoverFaultsbyEvent(ArrayList<ProbeEvent> events, ArrayList<String> coveredM) {

		for(ProbeEvent ev: events) {
			ArrayList<String> mLoc_list = m_encapsulation.getMutationTokenByServicename(ev.serviceName);
			for(String mLoc: mLoc_list)
				if(!coveredM.contains(mLoc) && mLoc.equals(ev.serviceName+":"+ev.eventToken)) 
					coveredM.add(mLoc);
		}		
	}			

	private double calculateRankedEventDistance(SuspiciousEventRank rank, long num) {

		double distance = 0;
		int index = 0;
		
		int checkedNum = 0;	
		ArrayList<String> visitedLocs = new ArrayList<String>();

		while((index < rank.ranklist.length) && (checkedNum < num)) {
			
			String[] eventIDs = rank.ranklist[index].eventIDs;//event IDs
			for(String eID: eventIDs) {
				String loc = getRankedEventLocation(rank, eID);
				if(loc == null || visitedLocs.contains(loc)) continue;
				visitedLocs.add(loc);
				checkedNum++;
				
				ArrayList<ProbeEvent> events = rank.failureEvents.get(eID);
				boolean hasDistance = false;
				if(events!=null && !events.isEmpty()) {
					ProbeEvent ev = events.get(0);
					double t_d = m_encapsulation.calculateDistance(ev);
					distance += t_d;
					hasDistance = true;
				}		
				
				if(!hasDistance) distance += m_encapsulation.maxGlobalPath/2.0;
			}					    
		    		    		    
		    index++;
		}
		
		if(checkedNum == 0) //rank.ranklist.length==0
		   return m_encapsulation.maxGlobalPath/2.0;	
		else
		   return distance /checkedNum;
	}				
	
	private double calculateRankedEventDistance(RankRecord rd, SuspiciousEventRank rank) {

		double distance = 0;
		int count = 0;					

		for(String eID: rd.eventIDs) {						
			ArrayList<ProbeEvent> events = rank.failureEvents.get(eID);
			if(events!=null) {
				for(ProbeEvent ev: events) {
					double t_d = m_encapsulation.calculateDistance(ev);
					distance += t_d;
				}
					
				count += events.size();
			}							
		}					    
		    		    		    
		
		if(count == 0) 
		   return m_encapsulation.maxGlobalPath/2.0;	
		else
		   return distance /count;
	}

	//for debugging
    private String cpefilename="";
	public void setCPEFileName(String compositeEventOutputFileName) {
		cpefilename = compositeEventOutputFileName;
	}

    private long timeout = -1;
	public void setDuration(long timeout) {
		this.timeout = timeout;
	}


	public int getNextAppIndex() {		
		return approachindex;
	}


	public String getFLResult() {		
		return FLResult;
	}	
	
	public String getSolutionDescriptions() {
		String result="";
		int index=0;
		for(FaultLocalizationSolution sl: solutions) {
			 result += "app "+index + " => " ;
			 result += sl.name;
			 result += newline;
			 index++;
		}
		
		return result;
	}
}

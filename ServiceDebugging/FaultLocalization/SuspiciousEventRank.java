package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;
import java.util.HashMap;

public class SuspiciousEventRank {

	//total number of events before ranking
	//since the total number may be changed due to event(predicate) refinement
	public int totalEventsNum;
	
	//ranklist[i]: the number i event or event combination
	public RankRecord[] ranklist;
	
	//all the events classified into two categories: success and failure
	//use the event ID to index the events
	public HashMap<String, ArrayList<ProbeEvent>> failureEvents;
	public HashMap<String, ArrayList<ProbeEvent>> successEvents;
	
	public static RankRecord[] mertSort(RankRecord[] list1, RankRecord[] list2) {
		RankRecord[] result = new RankRecord[list1.length+list2.length];
		int index1=0,index2=0,index=0;
		while(index1<list1.length&&index2<list2.length) {
			RankRecord rd1 = list1[index1];
			RankRecord rd2 = list2[index2];
			if((rd1.rankValue>rd2.rankValue) || 
				(Math.abs(rd1.rankValue-rd2.rankValue)<0.000001 && rd1.eventIDs.length<=rd2.eventIDs.length)) {
				result[index] = rd1;
				index++; index1++;
			} else {
				result[index] = rd2;
				index++; index2++;
			}
		}
		
		while(index1<list1.length) {
			result[index] = list1[index1];
			index++;index1++;
		}
		
		while(index2<list2.length) {
			result[index] = list2[index2];
			index++; index2++;
		}
		
		return result;
	}
	
	public static RankRecord[] sortRank(ArrayList<RankRecord> rks) {
		
		//added on 2016.11.13
		//assign the max rank value to all the single failure events
		//in order to keep them always on the top rank
		assignMaxValue4FailureEvents(rks);
		
		RankRecord[] result = new RankRecord[rks.size()];
		
		for(int i=0;i<result.length;i++) {
		    double maxRankValue = -10000;
		    double minEventNum = 10000;
		    RankRecord selectedRecord=null;
		    
		    for(RankRecord rr: rks) {		    			    	
		    	if(rr.rankValue > maxRankValue) {
		    		selectedRecord = rr;
		    		maxRankValue = rr.rankValue;
		    		minEventNum = rr.eventIDs.length;
		    	} else {
		    		if(Math.abs(rr.rankValue-maxRankValue)<0.00001 && rr.eventIDs.length < minEventNum) {
		    			selectedRecord = rr;
			    		maxRankValue = rr.rankValue;
			    		minEventNum = rr.eventIDs.length;
		    		} 
		    	}
		    }
		    
		    result[i] = selectedRecord;
		    rks.remove(selectedRecord);
		}
		
		return result;
	}

	private static void assignMaxValue4FailureEvents(ArrayList<RankRecord> rks) {
		
		for(RankRecord rd: rks) {
			if(rd.eventIDs.length==1) {//for single failure event
				String eID = rd.eventIDs[0];
				if(eID.contains("_taskfailure_")||eID.contains("_scopefailure_"))
					rd.rankValue = 1.1;//make it to be the top rank
			}
		}
		
	}

	public ArrayList<ProbeEvent> getAllEventsbyID(String eID) {

		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		ArrayList<ProbeEvent> flist = failureEvents.get(eID);
		ArrayList<ProbeEvent> slist = successEvents.get(eID);
		
		if(flist!=null) result.addAll(flist);
		if(slist!=null) result.addAll(slist);
		
		return result;
	}	
}

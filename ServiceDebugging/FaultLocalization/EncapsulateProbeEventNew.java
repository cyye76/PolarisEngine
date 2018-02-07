package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;
import java.util.HashMap;

import engine.DataField;
import engine.DataType;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;

public class EncapsulateProbeEventNew extends ProbeEvent {
	
	//set data field option, added on 2017.07.31
	//0: map to different events
	//1: field folding
	private int fieldoption = 1;
	
	//for encapsulated events
	private String entry; //name of source task

	private HashMap<String, ArrayList<Event>> events = new HashMap<String,ArrayList<Event>>();
	private ArrayList<String> token_list = new ArrayList<String>();
	
	private int refinementPolicy;
	private HashMap<String, String> m_dminfo = null;
	
	public EncapsulateProbeEventNew(String srvN, String soN, String instID, int policy, int datafield_option) {
		serviceName = srvN;
		instanceID = instID;
		entry = soN;
		eventID = serviceName + "_encapsulate_" + entry; 
		isEncapsulated = true;
		refinementPolicy = policy;
		fieldoption = datafield_option;
	}
	
	public void addEncapsulateEvents(String token, ArrayList<Event> elist) {
		token_list.add(token);
		events.put(token, elist);
	}
	
	private void setDMInfo(HashMap<String, String> dminfo) {
		m_dminfo = dminfo;
	}
	
	public void genDataField() {
		DataField df = new DataField();
		df.setName("Path");
		df.setType(DataType.STRING);
		
		String value = "Path";
		for(String token:token_list) 
			//if(token.startsWith("Task:")) 
			value += "_"+ token;
		
		df.setValue(value);	
		fields.add(df);
				
	    genEncapsulateDataField();					
	}
	
	private void genEncapsulateDataField() {
		
		ArrayList<String> encapsulatedVariables = new ArrayList<String>();
		HashMap<String, Integer> nameIndexMaps = new HashMap<String, Integer>();
		for(String token: token_list) {
            ArrayList<Event> elist = events.get(token);
            for(Event evt: elist) {
            	if(evt instanceof DataReadEvent) {
            		String vname = ((DataReadEvent)evt).getVariableName();
            		if(!encapsulatedVariables.contains(vname)) {
            			DataField df = new DataField();
            			df.setName(vname);
            			df.setType(((DataReadEvent)evt).getVariableType());
            			df.setValue(((DataReadEvent)evt).getVariableReadValue());
            			fields.add(df);
            			
            			encapsulatedVariables.add(vname);
            		}
            	}
            	
            	if(evt instanceof DataModificationEvent) {
            		String vname = ((DataModificationEvent)evt).getVariableName();
            		if(!encapsulatedVariables.contains(vname)) {
            			DataField df = new DataField();
            			df.setName(vname + "_old");
            			df.setType(((DataModificationEvent)evt).getVariableType());
            			df.setValue(((DataModificationEvent)evt).getVariableUpdateOldValue());
            			fields.add(df);
            			
            			df = new DataField();
            			df.setName(vname + "_new");
            			df.setType(((DataModificationEvent)evt).getVariableType());
            			df.setValue(((DataModificationEvent)evt).getVariableUpdateNewValue());
            			fields.add(df);
            			
            			encapsulatedVariables.add(vname);
            			
            		} else {
            			
            			switch(fieldoption) {
            			case 0:             			
            				Integer nameIndex = nameIndexMaps.get(vname);
            			    if(nameIndex==null) nameIndex = 0;
            				
            	  		    for(DataField df: fields) {
            			        String nvn = vname + "_new";
            			        if(nvn.equals(df.getName())) {
            			            DataField ndf = new DataField();
            			            ndf.setName(vname+nameIndex);
            			            ndf.setType(df.getType());
            		                ndf.setValue(((DataModificationEvent)evt).getVariableUpdateNewValue());
            			            fields.add(ndf);
            			            nameIndex++;
                    	            nameIndexMaps.put(vname, nameIndex);
            			            break;
            				    }            				            				
            			    }

            	  		    break;
            	  		    
            			case 1:
            				Object newvalue = ((DataModificationEvent)evt).getVariableUpdateNewValue();
            				DataField df = getDataFieldbyName(vname+"_new");
            				if(df!=null) {
            					df.setValue(accumulateDataFieldValue(df.getValue(), newvalue));
            				}
            				
            				break;
            			}
            		}
            	}
            }
		}
	}
	
	public ArrayList<ProbeEvent> refine() {
		
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();

		String tokenID;
		boolean metTask = false;
		while(!token_list.isEmpty()&& !metTask) {
		   tokenID = token_list.remove(0);
		   if(tokenID.startsWith("Task:")) metTask = true;
		   
		   ArrayList<Event> elist = events.remove(tokenID);
		   if(elist!=null) {
			   for(Event evt: elist) {
				   ProbeEvent pe = new ProbeEvent(evt, instanceID);
				   result.add(pe);
			   }
		   }		   
		}
		
		//encapsulate the rest
		if(!token_list.isEmpty()) {
			result.addAll(encapsulateEvents(m_dminfo, token_list, serviceName, instanceID, refinementPolicy, events, fieldoption));
		}
		
		return result;
	}	
	
	public ArrayList<ProbeEvent> furtherRefine(ArrayList<String> eIDs) {
		
		/*ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		ArrayList<String> removedTokens = new ArrayList<String>();
		for(String tk: token_list) {
			ArrayList<Event> elist = events.get(tk);
			if(elist==null) continue;
			
			boolean removed = false;
			for(Event evt: elist) {
				ProbeEvent pe = new ProbeEvent(evt, instanceID);
				String eID = pe.getEventID();
				if(!eIDs.contains(eID)) break;
				result.add(pe);
				removed = true;
			}
			
			if(removed) removedTokens.add(tk);
		}

        token_list.removeAll(removedTokens);*/
        
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();

		for(String tk: eIDs) {
			ArrayList<Event> elist = events.get(tk);
			if(elist==null) continue;
			
			for(Event evt: elist) {
				ProbeEvent pe = new ProbeEvent(evt, instanceID);
				result.add(pe);
			}
		}

        token_list.removeAll(eIDs);
		
		//encapsulate the rest
		if(!token_list.isEmpty()) {
			result.addAll(encapsulateEvents(m_dminfo, token_list, serviceName, instanceID, refinementPolicy, events, fieldoption));
		}
		
		return result;
	}	
	
	public boolean Cover(ProbeEvent pe) {
		//if they are from different service, they cannot cover each other
		if(!serviceName.equals(pe.serviceName)) return false;
			
		//remove the keyword "Path:"		
		String cc_pathvalue =((String) fields.get(0).getValue()).substring(5);
		
		if(pe.canRefine()) {
			//remove the keyword "Path:"		    		    		   
		    String pe_pathvalue =((String) pe.fields.get(0).getValue()).substring(5); 		    
		    return cc_pathvalue.contains(pe_pathvalue);
			
		} else {
			String location = pe.eventToken;
			return cc_pathvalue.contains(location);
		}
		
	}	
	
	public ArrayList<String> getTokenList() {
		return token_list;
	}

	public boolean canRefine() {
		
		if(refinementPolicy == FaultLocalizationSolution.COARSEREFINEMENT) //solution 5, 11
		    return token_list.size()>1;

		return true;
	}
	
	public static ArrayList<ProbeEvent> encapsulateEvents(HashMap<String, String> dmInfo, 
			                ArrayList<String> needEncapsulate, String serv,
			       			String instanceID, int policy, 
			       			HashMap<String, ArrayList<Event>> buffer, int datafield_option) {
		
        ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();       
        ArrayList<String> tmpList = new ArrayList<String>();
        ArrayList<String> block = new ArrayList<String>();
        
        while(!needEncapsulate.isEmpty()) {
            String tokenID = needEncapsulate.remove(0);
            String tn = getTokenName(tokenID);
                        	
            if(block.isEmpty()) { 
            	block.add(tn);
            	tmpList.add(tokenID);
            } else { //check whether its dominated task is inside the block
            	String key = serv+ "_" + tn;
            	String dmtn = dmInfo.get(key);
            	if(block.contains(dmtn)) {
            		block.add(tn);
            		tmpList.add(tokenID);
            	} else { //not inside the block            			
            		//need to encapsulate the current block
            		if(tmpList.size()>1) 
                    	encapsulateBlock(block, serv, instanceID, policy, tmpList, buffer, dmInfo, result, datafield_option);
                    else
            		    exposeIndividual(tmpList, result, instanceID, buffer);  
            		
            		//clear the tmpList and block
            		tmpList.clear();
            		block.clear();            			
            		block.add(tn);
            		tmpList.add(tokenID);
            	}            	            
            }                        
        }
        
        if(tmpList.size()>1) 
        	encapsulateBlock(block, serv, instanceID, policy, tmpList, buffer, dmInfo, result, datafield_option);
        else
		    exposeIndividual(tmpList, result, instanceID, buffer);    
        
        return result;
	}

    private static void encapsulateBlock(ArrayList<String> block, String serv,
			String instanceID, int policy, ArrayList<String> tmpList,
			HashMap<String, ArrayList<Event>> buffer,
			HashMap<String, String> dmInfo, ArrayList<ProbeEvent> result, int datafield_option) {
    	
    	String first = block.get(0);
		EncapsulateProbeEventNew encapevent = new EncapsulateProbeEventNew(serv, first, instanceID, policy, datafield_option);
		for(String tkID: tmpList) {
			ArrayList<Event> evtlist = buffer.get(tkID);
			encapevent.addEncapsulateEvents(tkID, evtlist);
		}
		encapevent.setDMInfo(dmInfo);
		encapevent.genDataField();
		result.add(encapevent);		
	}

	public static String getTokenName(String tokenID) {
	    if(tokenID.startsWith("Task:")) return tokenID.substring(5);
	    if(tokenID.startsWith("Transition:")) return tokenID.substring(11);
	    return null;
    }

	private static void exposeSingle(String tokenID,
			HashMap<String, ArrayList<Event>> buffer, String instanceID,
			ArrayList<ProbeEvent> result) {

		ArrayList<Event> elist = buffer.get(tokenID);
    	for(Event et: elist) {
    	    ProbeEvent pe = new ProbeEvent(et, instanceID);
		    result.add(pe);
    	}		
	}

	private static void exposeIndividual(ArrayList<String> tmpList,
			ArrayList<ProbeEvent> result, String instanceID,
			HashMap<String, ArrayList<Event>> buffer) {
		
		for(String tkID: tmpList) 
			exposeSingle(tkID, buffer, instanceID, result);
        	       
	}

	private static ArrayList<String> getBlockOutcomingTrs(
			ArrayList<String> tmpList) {

		ArrayList<String> result = new ArrayList<String>();
		
		for(int i=tmpList.size()-1;i>=0;i--) {
			String tkID = tmpList.get(i);
			if(tkID.startsWith("Task:")) break;
			result.add(tkID);
		}
		
		tmpList.removeAll(result);
		return result;
	}
	
	
}

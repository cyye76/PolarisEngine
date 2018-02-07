package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import engine.DataField;
import engine.DataType;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;

public class EncapsulateProbeEvent extends ProbeEvent {
	
	//for encapsulated events
	//private String source; //name of source task
	//private String sink; //name of sink task

	private HashMap<String, ArrayList<Event>> events = new HashMap<String,ArrayList<Event>>();
	private ArrayList<String> token_list = new ArrayList<String>();
	//private HashMap<Event, Double> distance = new HashMap<Event, Double>();
	
	private boolean needFinedEncapsulation;
	private boolean improvedEncapsulation;
	private int refinementPolicy;
	private HashMap<String, String> ctrlInfo = null;

	
	public EncapsulateProbeEvent(String srvN, String instID, boolean needFEP, int policy, boolean improved, HashMap<String, String> controlFlowInfo) {
		serviceName = srvN;
		instanceID = instID;
		//source = soN;
		//sink = siN;
		//eventID = serviceName + "_encapsulate_" + source + "_" + sink; 
		isEncapsulated = true;
		needFinedEncapsulation = needFEP;
		refinementPolicy = policy;
		improvedEncapsulation = improved;
		ctrlInfo = controlFlowInfo;
	}
	
	public void addEncapsulateEvents(String token, ArrayList<Event> elist) {
		token_list.add(token);
		events.put(token, elist);
	}
	
	public void genDataField() {
		//DataField df = new DataField();
		//df.setName("Path");
		//df.setType(DataType.STRING);
		
		//String value = "Path";
		String value="";
		for(String token:token_list) 
			//if(token.startsWith("Task:")) 
			value += "_"+ token;
		
		//df.setValue(value);
		
		//fields.add(df);
		eventID = serviceName + "_encapsulate" + value;
		
		if(needFinedEncapsulation) {//more refined abstraction
		  genEncapsulateDataField(improvedEncapsulation);
		} 
			
	}
	
	private void genEncapsulateDataField(boolean improved) {
		
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
            			if(!improved) {
            			    for(DataField df: fields) {
            				    String nvn = vname + "_new";
            				    if(nvn.equals(df.getName())) {
            					      df.setValue(((DataModificationEvent)evt).getVariableUpdateNewValue());
            					      break;
            				    }
            			    }
            			} else {
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
            				
            			}
            		}
            	}
            }
		}
	}

	
	private ArrayList<ProbeEvent> CoarseRefine() {
		
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();

		
		boolean metTask = false;
		//String tokenID = source;
		String tokenID;
		while(!token_list.isEmpty() && !metTask) {
		   tokenID = token_list.remove(0);
		   if(tokenID.startsWith("Task:")) metTask = true;		   		   		   
			
		   ArrayList<Event> elist = events.remove(tokenID);
		   
		   if(elist!=null) {
			   EncapsulateProbeEvent epe = new EncapsulateProbeEvent(serviceName,instanceID, needFinedEncapsulation, refinementPolicy, improvedEncapsulation, ctrlInfo);
			   epe.addEncapsulateEvents(tokenID, elist);
			   epe.eventToken = tokenID;
			   epe.genDataField();
			   result.add(epe);
		   }		   
		}
											
		
		if(token_list.isEmpty()) {
			//2.update the data field
			fields.clear();
			genDataField();
			
			//3.add the updated probeEvent into the list as well
			result.add(this);			
		}
		
		return result;
	}
	
	public ArrayList<ProbeEvent> refine() {
		
		if(refinementPolicy == FaultLocalizationSolution.COARSEREFINEMENT) return CoarseRefine(); //solution 5, 11
		
		if(refinementPolicy == FaultLocalizationSolution.HYBRIDREFINEMENT && token_list.size()>1) return CoarseRefine(); //solution 6, 12
		
		return GrainedRefine();
	}
	
	private ArrayList<ProbeEvent> GrainedRefine() {
		
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();

		boolean metTask = false;
		//String tokenID = source;
		String tokenID;
		while(!token_list.isEmpty() && !metTask) {
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
		
		if(token_list.size()>1) {
			//2.update the data field
			fields.clear();
			genDataField();
			
			//3.add the updated probeEvent into the list as well
			result.add(this);			
		} else {
			for(String etkID: token_list) {
		        ArrayList<Event> tmplist = events.get(etkID);
		        for(Event et: tmplist) {
		            ProbeEvent pe = new ProbeEvent(et, instanceID);
		            result.add(pe);
		        }
	        }
		}
		
		return result;
	}	
	
    public ArrayList<ProbeEvent> furtherRefine(ArrayList<String> eIDs) {
		
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
      		result.addAll(generateEncapsulateEvents(ctrlInfo, serviceName, token_list, instanceID, events, needFinedEncapsulation, refinementPolicy, improvedEncapsulation));
      	}
      				
		return result;
	}	

	public boolean Cover(ProbeEvent pe) {
		//if they are from different service, they cannot cover each other
		if(!serviceName.equals(pe.serviceName)) return false;
		
		int fi = eventID.indexOf("Task:");
		if(fi<0) return false;		
		String cID = eventID.substring(fi);
		
		fi = pe.eventID.indexOf("Task:");
		if(fi<0) return false;
		String pID = pe.eventID.substring(fi);
		
		return cID.contains(pID);
		
		/*
		//remove the keyword "Path:"		
		String cc_pathvalue =((String) fields.get(0).getValue()).substring(5);
		
		if(pe.canRefine()) {
			//remove the keyword "Path:"		    		    		   
		    String pe_pathvalue =((String) pe.fields.get(0).getValue()).substring(5); 		    
		    return cc_pathvalue.contains(pe_pathvalue);
			
		} else {
			String location = pe.eventToken;
			return cc_pathvalue.contains(location);
		}*/
		
	}	
	
	public ArrayList<String> getTokenList() {
		return token_list;
	}

	public boolean canRefine() {
		
		if(refinementPolicy == FaultLocalizationSolution.COARSEREFINEMENT) //solution 5, 11
		    return token_list.size()>1;

		return true;
	}
	
	public static Collection<? extends ProbeEvent> generateEncapsulateEvents(
			HashMap<String, String> controlFlowInfo,
			String serv, ArrayList<String> needEncapsulate,
			String instID, HashMap<String, ArrayList<Event>> buffer,
			boolean needFinedEncapsulation,
			int refinementPolicy,
			boolean improvedEncapsulation) {
		
		ArrayList<ProbeEvent> result = new ArrayList<ProbeEvent>();
		
		ArrayList<ArrayList<String>> encapEventlist = getEncapEvents(controlFlowInfo, serv, needEncapsulate);
		
		for(ArrayList<String> encap: encapEventlist) {
			if(encap.size()>1) {
		        EncapsulateProbeEvent epe = new EncapsulateProbeEvent(serv, instID, needFinedEncapsulation, refinementPolicy, improvedEncapsulation, controlFlowInfo);					
		        for(String etkID: encap) {
			        ArrayList<Event> tmplist = buffer.get(etkID);
			        epe.addEncapsulateEvents(etkID, tmplist);
		        }
		
		        epe.genDataField();
		        result.add(epe);
			} else {
				for(String etkID: encap) {
			        ArrayList<Event> tmplist = buffer.get(etkID);
			        for(Event et: tmplist) {
			            ProbeEvent pe = new ProbeEvent(et, instID);
			            result.add(pe);
			        }
		        }
			}
		}
		
		return result;
	}
	
	private static ArrayList<ArrayList<String>> getEncapEvents(HashMap<String, String> controlFlowInfo, String sn, ArrayList<String> needEncapsulate) {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		int index=0;
		ArrayList<String> tmp = new ArrayList<String>();
		ArrayList<String> block = new ArrayList<String>();
		while(index<needEncapsulate.size()) {
			String tokenID = needEncapsulate.get(index);
			String tn = EncapsulateProbeEventNew.getTokenName(tokenID);
			index++;
			
			if(block.isEmpty()) {
				tmp.add(tokenID);
				block.add(tn);
			} else {
				String key = sn + "_" + tn;
				String pred = controlFlowInfo.get(key);
				if(block.contains(pred)) {
					tmp.add(tokenID);
					block.add(tn);
				} else {
					result.add(tmp);
					tmp = new ArrayList<String>();
					tmp.add(tokenID);
					block.clear();
					block.add(tn);
				}
			}
		}
		
		if(!tmp.isEmpty()) result.add(tmp);	
		
		return result;
	}
}

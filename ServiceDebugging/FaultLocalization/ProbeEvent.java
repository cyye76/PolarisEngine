package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;
import java.util.HashMap;

import Utils.XMLProcessing;
import Configuration.Config;
import engine.DataField;
import engine.DataType;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;
import engine.Event.ScopeFailureEvent;
import engine.Event.TaskCompleteEvent;
import engine.Event.TaskFailureEvent;
import engine.Event.TaskStartEvent;
import engine.Event.TransitionFiringEvent;
import engine.Event.TransitionNotFiringEvent;
import Configuration.Config;



/**
 * This class is used to localize faults
 * We did not rank the original exposed events, because
 * we need to encapsulate and refine them.
 * 
 * @author cyye
 *
 */
public class ProbeEvent {

	
	public String serviceName;
	public String instanceID;
	public boolean isEncapsulated = false;	
	private int lineno = -1;
	
	protected String eventID;//this is used to identify the same set of events
	                         //for single event, it is based on variable loc or task name
	                         //for encapsulated event, it is based on start_sink name
	
	public String eventToken="";//"Task:"+taskname or "Transition:"+transitionname
	                           //used to calculate distance and reason about fault coverage
	
	protected ArrayList<DataField> fields = new ArrayList<DataField>();
	
	
	public ProbeEvent() {}
	
	public ProbeEvent(Event evt, String instID) {
		this(evt, instID, null);
	}
		
	public ProbeEvent(Event evt, String instID, ArrayList<Event> drelist) {
		serviceName = evt.getServiceName();		
		instanceID = instID;		
		lineno = evt.getLineNumber();
				
		if(evt instanceof TaskCompleteEvent) {
			
			String taskName = ((TaskCompleteEvent)evt).getTaskName();
			eventToken = "Task:" + taskName;			
			eventID = serviceName + "_task_" + taskName;
			
			//initialize the data field
			DataField df = new DataField();
			df.setName("TaskName");
			df.setType(DataType.STRING);
			df.setValue(eventToken);
			fields.add(df);			
		}
		
		if(evt instanceof TransitionNotFiringEvent) {
			
			String transitionName = ((TransitionNotFiringEvent) evt).getTransitionName();
			eventToken = "Transition:" + transitionName;			
			eventID = serviceName + "_transition_" + transitionName + "_NotFired";			             
            			
			DataField df = new DataField();
			df.setName("TransitionName");
			df.setType(DataType.STRING);
			df.setValue(eventToken);
			fields.add(df);			
		}
		
		if(evt instanceof TransitionFiringEvent) {
						
			String transitionName = ((TransitionFiringEvent) evt).getTransitionName();
			eventToken = "Transition:" + transitionName;
			
			eventID = serviceName + "_transition_" + transitionName;			
            
			if(drelist==null || drelist.isEmpty()) {
				//initialize the data field
				DataField df = new DataField();
				df.setName("TransitionName");
				df.setType(DataType.STRING);
				df.setValue(eventToken);
				fields.add(df);
			
			} else {
				
				for(Event dre: drelist) {
					DataField df = new DataField();			
					df.setName(((DataReadEvent) dre).getVariableName());
					df.setType(((DataReadEvent) dre).getVariableType());
					df.setValue(((DataReadEvent) dre).getVariableReadValue());
					
					boolean existing = false;
					for(DataField ff: fields)
						if(ff.getName().equals(df.getName())) {
							existing = true;
							break;
						}
					if(!existing) fields.add(df);
				}
			}
		}
		
		if(evt instanceof DataReadEvent) {
			
			
			String vloc = ((DataReadEvent) evt).getVariableLoc();
			//eventID = serviceName + "_dataread_" + vloc;//comment on 2016.11.08
			String vname = ((DataReadEvent) evt).getVariableName();
			eventID = serviceName + "_dataread_" + vname + "_" + vloc; //added on 2016.11.08
			
			if(lineno>=0)
				eventToken =  "DataRead:" + vname + "_" + lineno;
			else 
				eventToken = extractLoc(vloc);
						
			//initialize the data field
			DataField df = new DataField();			
			df.setName(((DataReadEvent) evt).getVariableName());
			df.setType(((DataReadEvent) evt).getVariableType());
			df.setValue(((DataReadEvent) evt).getVariableReadValue());
			fields.add(df);
		}
		
		if(evt instanceof DataModificationEvent) {
			
			String vloc = ((DataModificationEvent) evt).getVariableLoc();
			//eventID = serviceName + "_datamodification_" + vloc;//comment on 2016.11.08
			String vname = ((DataModificationEvent) evt).getVariableName();
			eventID = serviceName + "_dataread_" + vname + "_" + vloc; //added on 2016.11.08
			if(lineno>=0)
				eventToken =  "DataModification:"+ vname + "_" +  lineno;
			else 
			    eventToken = extractLoc(vloc);
			
			//initialize the data field
			DataField df = new DataField();			
			//df.setName(((DataModificationEvent) evt).getVariableName()+"_old");
			//df.setType(((DataModificationEvent) evt).getVariableType());
			//df.setValue(((DataModificationEvent) evt).getVariableUpdateOldValue());
			//fields.add(df);
			
            //df = new DataField();			
			df.setName(((DataModificationEvent) evt).getVariableName()+"_new");
			df.setType(((DataModificationEvent) evt).getVariableType());
			df.setValue(((DataModificationEvent) evt).getVariableUpdateNewValue());
			fields.add(df);
		}
		
		if(evt instanceof TaskFailureEvent) {
			
			String taskName = ((TaskFailureEvent)evt).getTaskName();
			eventToken = "Task:" + taskName;			
			eventID = serviceName + "_taskfailure_" + taskName;
			String faultlreason = ((TaskFailureEvent)evt).getFaultReason();
			
			//initialize the data field
			DataField df = new DataField();
			df.setName("TaskName");
			df.setType(DataType.STRING);
			df.setValue(eventToken);
			fields.add(df);	
			
			df = new DataField();
			df.setName("FaultReason");
			df.setType(DataType.STRING);
			df.setValue(faultlreason);
			fields.add(df);
		}
		
		if(evt instanceof ScopeFailureEvent) {
			
			String scopeName = ((ScopeFailureEvent)evt).getScopeName();
			eventToken = "Scope:" + scopeName;			
			eventID = serviceName + "_scopefailure_" + scopeName;
			int faultlineno = ((ScopeFailureEvent)evt).getFaultLineNo();
			String faultlreason = ((ScopeFailureEvent)evt).getScopeFaultReason();
			
			//initialize the data field
			DataField df = new DataField();
			df.setName("ScopeName");
			df.setType(DataType.STRING);
			df.setValue(eventToken);
			fields.add(df);	
			
			df = new DataField();
			df.setName("FaultLineNo");
			df.setType(DataType.INTEGER);
			df.setValue(faultlineno);
			fields.add(df);	
			
			df = new DataField();
			df.setName("FaultReason");
			df.setType(DataType.STRING);
			df.setValue(faultlreason);
			fields.add(df);	
		}
	}
	                            
	private String extractLoc(String loc) {
		
		int sI = 0;//loc.indexOf(':')+1;
		int eI = loc.indexOf('_');
		
		return loc.substring(sI, eI);
	}


	public String getEventID() {
		
		return eventID;
	}
	
	public boolean isSameValueWithSameEventID(ProbeEvent event) {
		if(!event.serviceName.equals(serviceName)) return false;
		if(!event.eventID.equals(eventID)) return false;
		return isSameValue(event);
	}

	public boolean isSameValue(ProbeEvent event) {
		
		if(!event.eventID.equals(eventID)) return false;
		
		if(fields.size()!=event.fields.size()) return false;
		
		boolean isSame;
		for(DataField df: fields) {
			String dfname = df.getName();
			String dfType = df.getType();
			Object dfValue = df.getValue();
			isSame = false;
			
			for(DataField cp_df: event.fields) {
				if(dfname.equals(cp_df.getName()) && 
					dfType.equals(cp_df.getType())) {
					
					Object cp_dfValue = cp_df.getValue();									
					
					isSame = compareValue(dfValue,cp_dfValue);
					break;
				}									
			}
			
			if(!isSame) return false;
		}

		return true;
	}
	
	protected static Object accumulateDataFieldValue(Object value1, Object value2) {
		if((value1==null) && (value2==null)) return null;
		if((value1 instanceof Integer) && (value2 instanceof Integer)) {
			int v1 = (Integer)value1;
		    int v2 = (Integer)value2;
			return (v1 + v2)%Config.getConfig().variableDomain; 
		
		} else 
		
		if((value1 instanceof String) && (value2 instanceof String)) {
			String v1 = (String) value1;
			String v2 = (String) value2;
			return v1+v2;		
			
		} else 
				
		if((value1 instanceof Boolean) && (value2 instanceof Boolean))	{
			Boolean v1 = (Boolean) value1;
			Boolean v2 = (Boolean) value2;
			return v1&&v2; 
		}
			
		return value1;	
	}
	
	public static boolean compareValue(Object value1, Object value2) {

		if((value1==null) && (value2==null)) return true;
		
		if((value1 instanceof Integer) && (value2 instanceof Integer)) {
			int v1 = (Integer)value1;
		    int v2 = (Integer)value2;
			return v1 == v2; 
		
		} else 
		
		if((value1 instanceof String) && (value2 instanceof String)) {
			String v1 = (String) value1;
			String v2 = (String) value2;
			return v1.equals(v2);		
		
		} else 
			
		if((value1 instanceof Boolean) && (value2 instanceof Boolean))	{
		    Boolean v1 = (Boolean) value1;
		    Boolean v2 = (Boolean) value2;
		    return v1.equals(v2); 
		}
		
		return false;
		
	}
	
	public static double calculateValueDistance(Object value1, Object value2, String type) {

		if((value1==null) && (value2==null)) return 0;
		
		if((value1 instanceof Integer) && (value2 instanceof Integer)) {
			int v1 = (Integer)value1;
		    int v2 = (Integer)value2;
			return Math.abs(v1 - v2); 		
		}
		
		if((value1 instanceof String) && (value2 instanceof String)) {
			String v1 = (String) value1;
			String v2 = (String) value2;
			if(type.equals(DataType.XML)) 
				return Math.abs( 1 - calculateXMLSimilarity(v1, v2)); 
			else
			    return v1.equals(v2)?0:1;				
		} 
			
		if((value1 instanceof Boolean) && (value2 instanceof Boolean))	{
		    Boolean v1 = (Boolean) value1;
		    Boolean v2 = (Boolean) value2;
		    return v1.equals(v2)?0:1; 
		}				
		
		return 0;		
	}


	public double calculateEventDistance(ProbeEvent fevent) {
		return calculateEventDistance(fevent, null);
	}
	
	public double calculateEventDistance(ProbeEvent fevent, HashMap<String, Integer> durations) {
		
		double distance = 0;
		boolean hasMatch = false;

		for(DataField df: fields) {
			
			String dfName = df.getName();
			String dfType = df.getType();
			Object dfValue = df.getValue();
			hasMatch=false;
						
			for(DataField cp_df: fevent.fields) {
				
				if(dfName.equals(cp_df.getName()) && 
					dfType.equals(cp_df.getType())) {
					hasMatch = true;
					
					Object cp_dfValue = cp_df.getValue();					
					if(!compareValue(dfValue, cp_dfValue)) {
						
						if(dfType.equals(DataType.BOOLEAN) || dfType.equals(DataType.STRING)) {
							//double dist = Config.getConfig().variableDomain/2;
							double dist = 1;
							distance+= dist * dist;						
						} 
						
						else 	
							
						if(dfValue==null || cp_dfValue==null) {
								//double dist = Config.getConfig().variableDomain/2;
								double dist = 1;
								distance+= dist * dist;							
						}
						
						else
							
						if(dfType.equals(DataType.XML) ) {
							   double dist = 1 - calculateXMLSimilarity(dfValue, cp_dfValue);
							   distance+= dist * dist;   
						}
						
						else {
								//double dist = Math.abs((Integer)dfValue - (Integer)cp_dfValue);
								double dist = Math.abs((Integer)dfValue - (Integer)cp_dfValue) * 1.0;
								Integer period = null;
								if(durations!=null) period = durations.get(dfName);
								dist = (period ==null || period==0) ? dist /Config.getConfig().variableDomain: dist/period;
							    distance+= dist*dist;
						}						
					}
										
					break;
				}
			}
			
			if(!hasMatch) {
				//double dist = Config.getConfig().variableDomain/2;
				double dist = 1;
				distance+= dist * dist;
			}			
		}
		
		//return distance and normalize it;
		//int fieldnum = fields.size() + 1;
		//double result = fieldnum>0 ? distance/fieldnum: distance;
		//double result =  distance/(fieldnum * fieldnum);
		//double result =  distance/fieldnum;
		double result =  distance;
		return Math.sqrt(result);
	}

	private static float calculateXMLSimilarity(Object dfValue, Object cp_dfValue) {
		return XMLProcessing.calculateSimilarity((String)dfValue, (String)cp_dfValue);
	}

	public ArrayList<ProbeEvent> refine() {
		return new ArrayList<ProbeEvent>();
	}
	
	public ArrayList<ProbeEvent> furtherRefine(ArrayList<String> eIDs) {
		return new ArrayList<ProbeEvent>();
	}

	public boolean Cover(ProbeEvent pe) {
		return false;
	}	
	
	public boolean canRefine() {
		return false;
	}
	
	public ArrayList<DataField> getDataField() {
		return fields;
	}

	public String generateUID() {
		String uID=serviceName + eventID;
        for(DataField df: fields) {
            String type =  df.getType();
            Object value = df.getValue();
            if(value==null) uID += "NULL";
            else
                    
            if(type.equals(DataType.STRING)) uID +=value;
            else 

            if(type.equals(DataType.BOOLEAN)) {
                   boolean bv = (Boolean)value;
                   uID += bv?"TRUE":"FALSE";
            }
            else

            if(type.equals(DataType.INTEGER)) {
                  int iv = (Integer)value;
                  String buf = "" + iv;
                  uID += buf; 
            }
       }
       
      return uID;
	}
	
	protected DataField getDataFieldbyName(String name) {
		
		for(DataField df: fields) 			 
			 if (df.getName().equals(name)) return df;
		
		return null;
	}
}

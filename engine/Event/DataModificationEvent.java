package engine.Event;

import engine.DataType;
import engine.Queue.Subscription;


public class DataModificationEvent extends VariableEvent {
	
	public DataModificationEvent(String vName, String vType, Object old_value, Object new_value, String instanceID, String vLoc, String serviceName) {
		
		setEventType(EventType.datawrite_type);
		setInstanceID(instanceID);
		setServiceName(serviceName);
		
		setVariableName(vName);
		setVariableType(vType);
		setVariableUpdateValues(vType, old_value, new_value);				
		setVariableLOC(vLoc);
	}
	
    public static Subscription createDataModificationEventSubscription(String vName, String vType, String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_variableName, DataType.STRING);//variable name
		sub.addID(fieldname_variableType, DataType.STRING);//variable type
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.datawrite_type +
		                   " && " + fieldname_variableName + " == " + vName +
		                   " && " + fieldname_variableType + " == " + vType +
		                   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
    
    public static Subscription createAllDataModificationEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.datawrite_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}
    
    public boolean isSameEvent(Event event) {
    	
    	return false;
    	
    	/*
    	if(!(event instanceof DataModificationEvent)) return false;
    	
    	String sn1 = getServiceName();
    	String loc1 = getVariableLoc();
    	
    	String sn2 = event.getServiceName();
    	String loc2 = ((DataModificationEvent)event).getVariableLoc(); 
    	
    	return sn1.equals(sn2) && loc1.equals(loc2);*/
    }
}

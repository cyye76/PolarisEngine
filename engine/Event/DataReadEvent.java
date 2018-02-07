package engine.Event;

import engine.DataType;
import engine.Queue.Subscription;

public class DataReadEvent extends VariableEvent {
	
	public DataReadEvent(String vName, String vType, Object value, String serviceInstanceID, String vLoc, String serviceName) {
		//set event type
		setEventType(EventType.dataread_type);
		setInstanceID(serviceInstanceID);
		setServiceName(serviceName);
		
		setVariableName(vName);
		setVariableType(vType);
		setVariableReadValue(vType, value);		
		setVariableLOC(vLoc);	
	}
	
	public static Subscription createDataReadEventSubscription(String vName, String vType, String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_variableName, DataType.STRING);//variable name
		sub.addID(fieldname_variableType, DataType.STRING);//variable type
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.dataread_type +
		                   " && " + fieldname_variableName + " == " + vName +
		                   " && " + fieldname_variableType + " == " + vType +
		                   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
    
    public static Subscription createAllDataReadEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.dataread_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}
    
    public boolean isSameEvent(Event event) {
    	
    	return false;
    	/*
    	if(!(event instanceof DataReadEvent)) return false;
    	
    	String sn1 = getServiceName();
    	String loc1 = getVariableLoc();
    	
    	String sn2 = event.getServiceName();
    	String loc2 = ((DataReadEvent)event).getVariableLoc(); 
    	
    	return sn1.equals(sn2) && loc1.equals(loc2);*/
    }
}

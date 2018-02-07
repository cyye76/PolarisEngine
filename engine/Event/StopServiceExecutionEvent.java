package engine.Event;

import engine.Queue.Subscription;

public class StopServiceExecutionEvent extends EngineEvent {

	public StopServiceExecutionEvent(String serviceInstanceID) {
		setEventType(EventType.stopServiceExecution_type);
		setInstanceID(serviceInstanceID);
	}
	
	public static Subscription createStopServiceExecutionEventSubscription(String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);				
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.stopServiceExecution_type 
		                   + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
}

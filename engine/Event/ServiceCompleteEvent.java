package engine.Event;

import engine.Queue.Subscription;

public class ServiceCompleteEvent extends ServiceStatusEvent {
	
	public ServiceCompleteEvent(String serviceInstanceID) {
		setEventType(EventType.servicecomplete_type);
		setInstanceID(serviceInstanceID);
	}

	public static Subscription createServiceCompleteEventSubscription(String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);				
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.servicecomplete_type 
		                   + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}	
}

package engine.Event;

import engine.Queue.Subscription;

public class ServiceScheduleEvent extends TaskStatusEvent {
	
	public ServiceScheduleEvent(String taskName, String serviceInstanceID) {
		//set event type
		setEventType(EventType.scheduleService_type);
		setInstanceID(serviceInstanceID);
	
		setTaskName(taskName);
	}
	
	public static Subscription createServiceScheduleSubscription(String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);
		
		//subscribe to taskname
		sub.addID(fieldname_taskName, EventType.STRING);
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.scheduleService_type 
		                   + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
		
}

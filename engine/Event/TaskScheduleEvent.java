package engine.Event;

import engine.Queue.Subscription;

public class TaskScheduleEvent extends TaskStatusEvent {

	public TaskScheduleEvent(String taskName, String serviceInstanceID) {
		//set event type
		setEventType(EventType.scheduelTask_type);
		setInstanceID(serviceInstanceID);
		
		setTaskName(taskName);
	}
	
	public static Subscription createTaskScheduleSubscription(String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);
		
		//subscribe to taskname
		sub.addID(fieldname_taskName, EventType.STRING);
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.scheduelTask_type 
		                   + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
}

package engine.Event;

import engine.DataType;
import engine.Queue.Subscription;

public class TaskCompleteEvent extends TaskStatusEvent {
	
	public TaskCompleteEvent(String taskName, String serviceInstanceID, String serviceName) {
		
		//set event type
		setEventType(EventType.taskcomplete_type);
		setInstanceID(serviceInstanceID);
		setServiceName(serviceName);
		
		setTaskName(taskName);
	}	
	
	public static Subscription createTaskCompleteEventSubscription(String taskName, String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_taskName, DataType.STRING);//variable name		
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.taskcomplete_type +
		                   " && " + fieldname_taskName + " == " + taskName +
		                   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
    
    public static Subscription createAllTaskCompleteEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.taskcomplete_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}
    
    public boolean isSameEvent(Event event) {
    	return false;
    	/*
    	if(!(event instanceof TaskCompleteEvent)) return false;
    	
		String sn1 = getServiceName();
		String tn1 = getTaskName();
		
		String sn2 = event.getServiceName();
		String tn2 = ((TaskCompleteEvent)event).getTaskName();
		
		return sn1.equals(sn2) && tn1.equals(tn2);*/
	}
}

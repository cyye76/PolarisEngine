package engine.Event;

import engine.DataType;
import engine.Queue.Subscription;

public class TaskStartEvent extends TaskStatusEvent {

	public TaskStartEvent(String taskName, String serviceInstanceID, String serviceName) {
		//set event type
		setEventType(EventType.taskstart_type);
		setInstanceID(serviceInstanceID);
		setServiceName(serviceName);
		
		setTaskName(taskName);
	}
	
	public static Subscription createTaskStartEventSubscription(String taskName, String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_taskName, DataType.STRING);//variable name		
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.taskstart_type +	
		                  " && " + fieldname_taskName + " == " + taskName +
		                  " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
    
    public static Subscription createAllTaskStartEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.taskstart_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}
}

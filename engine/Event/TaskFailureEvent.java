package engine.Event;

import engine.DataType;
import engine.Queue.Message;
import engine.Queue.Subscription;

public class TaskFailureEvent extends TaskStatusEvent {
	
	public static final String fieldname_reason = "_________TaskFaultReason_________";
	
	public TaskFailureEvent(String servicename, String serviceInstanceID, String taskname, String reason) {
		setEventType(EventType.taskfailure_type);
		setServiceName(servicename);
		setInstanceID(serviceInstanceID);
		setTaskName(taskname);
		setFaultReason(reason);
	}
	
	/**
	 * Not used yet
	 * @param taskName
	 * @param serviceInstanceID
	 * @return
	 */
	public static Subscription createTaskFailureEventSubscription(String taskName, String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);				
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.taskfailure_type 
				           + " && " + fieldname_taskName + " == " + taskName + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}	
	
    public static Subscription createAllTaskFailureEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.taskfailure_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}
	
	public void setFaultReason(String faultreason) {
		Message msg = new Message();
		msg.setName(fieldname_reason);
		msg.setType(EventType.STRING);
		msg.setValue(faultreason);
		addMessage(msg);
	}
	
	public String getFaultReason() {
		
		Message msg = getMessage(fieldname_reason);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
	}	
}

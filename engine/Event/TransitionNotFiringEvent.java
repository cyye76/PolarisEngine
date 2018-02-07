package engine.Event;

import engine.DataType;
import engine.Queue.Message;
import engine.Queue.Subscription;

public class TransitionNotFiringEvent extends Event {

	public static final String fieldname_transitionName = "_________TransitionName_________";
	
	public void setTransitionName(String tn) {
		Message msg = new Message();
		msg.setName(fieldname_transitionName);
		msg.setType(EventType.STRING);
		msg.setValue(tn);
		addMessage(msg);
	}
	
	public String getTransitionName() {
		Message msg = getMessage(fieldname_transitionName);
		if(msg!=null) 
			return (String)msg.getValue();
		
		return null;
	}
	
	public TransitionNotFiringEvent(String instanceID, String serviceName, String transitionName) {
		setEventType(EventType.transitionnotfiring_type);
		setInstanceID(instanceID);
		setServiceName(serviceName);
		setTransitionName(transitionName);
	}
	
	public static Subscription createTransitionNotFiringEventSubscription(String transitionName, String instanceID) {
		Subscription sub = new Subscription();
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_transitionName, DataType.STRING);//transition name		
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.transitionnotfiring_type +	
		                  " && " + fieldname_transitionName + " == " + transitionName +
		                  " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		return sub;
	}
	
	public static Subscription createAllTransitionNotFiringEventSubscription(String instanceID) {
		Subscription sub = new Subscription();
		sub.addID(fieldname_headName, DataType.STRING);//event_head	
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		
		String condition = fieldname_headName + " == " + EventType.transitionnotfiring_type +			                  
		                  " && " + fieldname_serviceInstanceID + " == " + instanceID;
		
		sub.setCondition(condition);
		return sub;
	}
}

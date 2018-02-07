package engine.Event;

import engine.Queue.Message;
import engine.Queue.Subscription;

public class ExceptionEvent extends Event {
	public static final String fieldname_exceptionName = "_________ExceptionName_________";
	public static final String fieldname_exceptionContext = "_________ExceptionContext_________";
	
	public ExceptionEvent(String exceptionName, String exceptionContext, String serviceInstanceID) {
		//set type
		setEventType(EventType.exception_type);
		setInstanceID(serviceInstanceID);
		
		setExceptionName(exceptionName);
		setExceptionContext(exceptionContext);
	}
	
	private void setExceptionContext(String exceptionContext) {
		Message msg = new Message();
		msg.setName(fieldname_exceptionName);
		msg.setType(EventType.STRING);
		msg.setValue(exceptionContext);
		
		addMessage(msg);		
	}

	private void setExceptionName(String exceptionName) {
		Message msg = new Message();
		msg.setName(fieldname_exceptionContext);
		msg.setType(EventType.STRING);
		msg.setValue(exceptionName);
		
		addMessage(msg);
	}
	
	public static Subscription createExceptionSubscription(String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);
		
		//subscribe to exception name
		sub.addID(fieldname_exceptionName, EventType.STRING);
		
		//subscribe to exception context
		sub.addID(fieldname_exceptionContext, EventType.STRING);
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.exception_type 
		                   + " && " +  
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
	
	public static Subscription createDefaultExceptionSubscription() {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);					
		
		String condition = fieldname_headName + " == " + EventType.exception_type;		        
		
		sub.setCondition(condition);
		
		return sub;
	}
}

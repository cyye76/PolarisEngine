package engine.Event;

import engine.DataType;
import engine.Queue.Message;
import engine.Queue.Subscription;

public class ScopeFailureEvent extends ServiceStatusEvent {
	
	public static final String fieldname_ScopefaultLineNo = "_________ScopeFaultLineNo_________";
	public static final String fieldname_ScopeName = "_________ScopeName_________";
	public static final String fieldname_ScopefaultReason = "_________ScopeFaultReason_________";

	public ScopeFailureEvent(String servicename, String serviceInstanceID, String scopename, String reason, int lineno) {
		setEventType(EventType.scopefailure_type);
		setServiceName(servicename);
		setInstanceID(serviceInstanceID);
		setScopeName(scopename);
		setScopeFaultReason(reason);
		setFaultLineNo(lineno);
	}
	
	/**
	 * Not used yet
	 * @param scopename
	 * @param serviceInstanceID
	 * @return
	 */
	public static Subscription createScopeFailureEventSubscription(String scopename, String serviceInstanceID) {
		Subscription sub = new Subscription();
		//subscribe to event type
		sub.addID(fieldname_headName, EventType.STRING);				
		
		//subscribe to instanceID
		sub.addID(fieldname_serviceInstanceID, EventType.STRING);
		
		String condition = fieldname_headName + " == " + EventType.scopefailure_type 
				           + " && " + fieldname_ScopeName + " == " + scopename + " && " +   
		                   fieldname_serviceInstanceID + " == " + serviceInstanceID;
		
		sub.setCondition(condition);
		
		return sub;
	}
	
    public static Subscription createAllScopeFailureEventSubscription(String instanceID) {
		Subscription sub = new Subscription();		
		sub.addID(fieldname_headName, DataType.STRING);//event_head
		sub.addID(fieldname_serviceInstanceID, DataType.STRING);//serviceInstanceID
		String condition = fieldname_headName + " == " + EventType.scopefailure_type +
						   " && " + fieldname_serviceInstanceID + " == " + instanceID;
		                   
		sub.setCondition(condition);
		
		return sub;
	}

	public void setFaultLineNo(int faultlineno) {
		Message msg = new Message();
		msg.setName(fieldname_ScopefaultLineNo);
		msg.setType(EventType.INTEGER);
		msg.setValue(faultlineno);
		addMessage(msg);
	}
	
	public void setScopeName(String name) {
		Message msg = new Message();
		msg.setName(fieldname_ScopeName);
		msg.setType(EventType.STRING);
		msg.setValue(name);
		addMessage(msg);		
	}
	
	public void setScopeFaultReason(String reason) {
		Message msg = new Message();
		msg.setName(fieldname_ScopefaultReason);
		msg.setType(EventType.STRING);
		msg.setValue(reason);
		addMessage(msg);		
	}	
	
	public int getFaultLineNo() {
		
		Message msg = getMessage(fieldname_ScopefaultLineNo);		
		if(msg!=null)
			return (Integer) msg.getValue();
		
		return -1;
	}	
	
	public String getScopeName() {
		Message msg = getMessage(fieldname_ScopeName);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
	}
	
	public String getScopeFaultReason() {
		Message msg = getMessage(fieldname_ScopefaultReason);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
	}	
}

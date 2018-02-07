package engine.Event;

import java.io.Serializable;
import java.util.ArrayList;

import engine.Queue.Message;
import engine.Queue.Publication;

abstract public class Event extends Publication implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -834258462702852734L;
	public static final String fieldname_headName = "_________Event_Type_________";
	public static final String fieldname_serviceInstanceID = "_________ServiceInstanceID_________";
	public static final String fieldname_serviceName = "_________ServiceName_________";
	public static final String fieldname_lineNumber = "_________LineNumber_________";
	
	public void setEventType(String type) {
		Message msg = new Message();
		msg.setName(fieldname_headName);
		msg.setType(EventType.STRING);
		msg.setValue(type);
		
		addMessage(msg);
	}
	
	public void setInstanceID(String instanceID) {
		Message msg = new Message();
		msg.setName(fieldname_serviceInstanceID);
		msg.setType(EventType.STRING);
		msg.setValue(instanceID);
		
		addMessage(msg);
	}
	
	public void setServiceName(String serviceName) {
		Message msg = new Message();
		msg.setName(fieldname_serviceName);
		msg.setType(EventType.STRING);
		msg.setValue(serviceName);
		
		addMessage(msg);
	}
	
	public void setLineNumber(int lineno) {
		Message msg = new Message();
		msg.setName(fieldname_lineNumber);
		msg.setType(EventType.INTEGER);
		msg.setValue(lineno);
		
		addMessage(msg);
	}
	

	public String getServiceName() {
		Message msg = getMessage(fieldname_serviceName);
		if(msg!=null)
			return (String)msg.getValue();
		return null;
	}
	
	public String getInstanceID() {
		Message msg = getMessage(fieldname_serviceInstanceID);
		if(msg!=null)
			return (String)msg.getValue();
		return null;
	}
	
	public int getLineNumber() {
		Message msg = getMessage(fieldname_lineNumber);
		if(msg!=null)
			return (Integer)msg.getValue();
		return -1;
	}
	
	public void setExposed(boolean isExposed) {
		this.isExposed = isExposed;
	}

	public boolean isExposed() {
		return isExposed;
	}

	private boolean isExposed = false;
	private boolean isPublic = false;		
	
	public boolean isSameEvent(Event event) {	
		
		return false;
		/*
		ArrayList<Message> m_list0 = getMessages();
		ArrayList<Message> m_list1 = event.getMessages();
		
		if(m_list0.size()!=m_list1.size()) return false;
		
		for(int i=0;i<m_list0.size();i++) {
			Message m0 = m_list0.get(i);
			if(m0.getName().equals(fieldname_serviceInstanceID)) continue;
			
			boolean hasSame = false;
			for(int j=0;j<m_list1.size();j++) {
				Message m1 = m_list1.get(j);
				if(m1.isSameMessage(m0)) {
					hasSame = true;
					break;
				}
			}
			 	
			if(!hasSame) return false;
		}				
		
		return true;*/
	}
	
    public boolean hasSameEventContent(Event event) {	
		
		ArrayList<Message> m_list0 = getMessages();
		ArrayList<Message> m_list1 = event.getMessages();
		
		if(m_list0.size()!=m_list1.size()) return false;
		
		for(int i=0;i<m_list0.size();i++) {
			Message m0 = m_list0.get(i);
			//if(m0.getName().equals(fieldname_serviceInstanceID)) continue;
			
			boolean hasSame = false;
			for(int j=0;j<m_list1.size();j++) {
				Message m1 = m_list1.get(j);
				if(m1.isSameMessage(m0)) {
					hasSame = true;
					break;
				}
			}
			 	
			if(!hasSame) return false;
		}				
		
		return true;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isPublic() {
		return isPublic;
	}
}

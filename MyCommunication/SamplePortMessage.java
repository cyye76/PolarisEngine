package MyCommunication;

import java.util.ArrayList;

import engine.DataType;
import engine.Queue.Publication;
import engine.Queue.Message;
import engine.Queue.Subscription;

public class SamplePortMessage extends Publication {
	//Meta protocols
	public static final String fieldname_headname = "_________CommunicationService_________";
	public static final String communication_message = "_________Message_________";
	
	//reserved for future use
	public static final String communication_exception = "_________Exception_________";
	
	public static final String fieldname_servicename = "_________ServiceName_________";
	public static final String fieldname_serviceID = "_________ServiceID_________";
	
	public SamplePortMessage(String serviceName, String serviceID, ArrayList<Message> messages) {
		//add publication head
		Message msg = new Message();
		msg.setName(fieldname_headname);
		msg.setType(DataType.STRING);
		msg.setValue(communication_message);
		addMessage(msg);
		
		//add service name
		msg = new Message();
		msg.setName(fieldname_servicename);
		msg.setType(DataType.STRING);
		msg.setValue(serviceName);
		addMessage(msg);
		
		//add service ID
		msg = new Message();
		msg.setName(fieldname_serviceID);
		msg.setType(DataType.STRING);
		msg.setValue(serviceID);
		addMessage(msg);
		
		//add message body
		for(Message bodyitem: messages) 
			addMessage(bodyitem);
		
	}
	
    public static Subscription createSamplePortSubscription() {
		Subscription sub = new Subscription();
		sub.addID(fieldname_headname, DataType.STRING);
		String condition = fieldname_headname + " == " + communication_message;
		sub.setCondition(condition);
		
		return sub;
	}
    
    public static String getServiceName(Publication pub) {
    	Message msg = pub.getMessage(fieldname_servicename);
    	if(msg!=null)
    		return (String)msg.getValue();
    	
    	return null;
    }
    
    public static String getServiceID(Publication pub) {
    	Message msg = pub.getMessage(fieldname_serviceID);
    	if(msg!=null)
    		return (String) msg.getValue();
    	
    	return null;
    }
    
    public static ArrayList<Message> getMessageBody(Publication pub) {
    	ArrayList<Message> body = new ArrayList<Message>();
    	
    	ArrayList<Message> msg_list = pub.getMessages();
    	for(Message msg: msg_list) {
    		String name = msg.getName();
    		if(name.equals(fieldname_headname)) continue;
    		if(name.equals(fieldname_servicename)) continue;
    		if(name.equals(fieldname_serviceID)) continue;
    		
    		body.add(msg);
    	}
    	
    	return body;
    }

	
}

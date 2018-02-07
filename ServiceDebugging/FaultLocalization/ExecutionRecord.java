package ServiceDebugging.FaultLocalization;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.Event.Event;


public class ExecutionRecord implements Serializable {
			
	/**
	 * 
	 */
	private static final long serialVersionUID = 7603301438495710770L;

	//used to store the event queues for each service
	private HashMap<String, ArrayList<Event>> events = new HashMap<String, ArrayList<Event>>();
	
	//used to differentiate events from different instance
	private String instanceID = generateUnitInstanceID();	
	public String getInstanceID() {
		return instanceID;
	}
	
	public void setInstanceID(String ID) {
		instanceID = ID;
	}
	
	static private int IDCounter = 0;
	private String generateUnitInstanceID() {
		IDCounter++;
		return IDCounter + "_" + System.currentTimeMillis();
	}
	
	private ArrayList<String> globallog = new ArrayList<String>();
	private boolean passed = true;
	
	public ArrayList<Event> getServiceEvents(String serviceName) {
		
		return events.get(serviceName);
	}
	
	

	public ArrayList<Event> getAllEvents() {
		ArrayList<Event> result = new ArrayList<Event>();
		Set<String> sn_list = events.keySet();
		for(String sn:sn_list)
			result.addAll(events.get(sn));
		
		return result;
	}
	
	public ArrayList<String> getAllServiceNames() {
		ArrayList<String> result = new ArrayList<String>();
		result.addAll(events.keySet());
		return result;
	}
	

	public void setPassed(boolean nv) {
		passed = passed && nv;
	}

	public boolean isPassed() {
		return passed;
	}
	
	public void addServiceEvent(String serviceName, Event evt) {
		ArrayList<Event> queue; 
		queue = events.get(serviceName);
		if(queue==null) {
			queue = new ArrayList<Event>();
			events.put(serviceName, queue);
		}
		
		queue.add(evt);
	}
	
	public void addGlobalLog(String tn) {
		globallog.add(tn);		
	}
	
	public ArrayList<String> getGlobalLog() {
		return globallog;
	}
	
	public void removeDuplicatedEvents() {
		Set<String> sns = events.keySet();
		for(String sn: sns) {
			   ArrayList<Event> queue = events.get(sn);
			   removeDuplicatedEvents(queue);
		}
	}

	private void removeDuplicatedEvents(ArrayList<Event> queue) {
		 ArrayList<Event> buffer = new ArrayList<Event>();
		 for(Event evt: queue) {
			    if(!hasSameEvent(buffer, evt)) buffer.add(evt);
		 }
		 
		 queue.clear();
		 queue.addAll(buffer);
	}

	private boolean hasSameEvent(ArrayList<Event> buffer, Event evt) {		
		for(Event be: buffer) 
			 if(be.hasSameEventContent(evt)) return true;
		return false;
	}
	
}

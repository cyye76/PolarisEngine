package engine.Event;

import Service.AbstractService;
import Service.Task;
import engine.engine;
import engine.Queue.AbstractQueue;
import engine.Queue.Publication;


public class EventExposure {

	public static void exposeDataModificationEvent(String name, String type, Object old_value, Object new_value, String serviceInstanceID, String vLoc, String serviceName) {
		Event pub = new DataModificationEvent(name, type, old_value, new_value, serviceInstanceID, vLoc, serviceName);
		
		//exposed public data send/receive events
        if(vLoc.startsWith("Task")) {
        	int start = vLoc.indexOf(':') + 1;
        	int end = vLoc.indexOf('_');
        	String taskname = vLoc.substring(start, end);
        	AbstractService service = engine.getEngine().getService(serviceInstanceID);
        	
        	if(service!=null) {
        		Task tk = service.getTaskbyName(taskname);
        		if(tk!=null && tk.isPort())
        			pub.setPublic(true);
        	}
        }
        
		exposeEvent(pub);		
	}
	
	private static void exposeEvent(Publication pub) {
		AbstractQueue queue = engine.getEngine().getQueue();
		
		queue.publish(pub);
	}

	public static void exposeDataReadEvent(String name, String type, Object value, String serviceInstanceID, String vLoc, String serviceName) {
        Event pub = new DataReadEvent(name, type, value, serviceInstanceID, vLoc, serviceName);
        
        //exposed public data send/receive events
        if(vLoc.startsWith("Task")) {
        	int start = vLoc.indexOf(':') + 1;
        	int end = vLoc.indexOf('_');
        	String taskname = vLoc.substring(start, end);
        	AbstractService service = engine.getEngine().getService(serviceInstanceID);
        	if(service!=null) {
        		Task tk = service.getTaskbyName(taskname);
        		if(tk!=null && tk.isPort())
        			pub.setPublic(true);
        	}
        }
		
		exposeEvent(pub);
	}
	
	public static void exposeTaskStartEvent(String taskName, String serviceInstanceID, String serviceName) {
		TaskStartEvent pub = new TaskStartEvent(taskName, serviceInstanceID, serviceName);
        AbstractService service = engine.getEngine().getService(serviceInstanceID);
        if(service!=null) {
        	Task task = service.getTaskbyName(taskName);
        	pub.setPublic(task.isPort());
        
		    exposeEvent(pub);
        }
	}
	
	public static void exposeTaskCompleteEvent(String taskName, String serviceInstanceID, String serviceName) {
		TaskCompleteEvent pub = new TaskCompleteEvent(taskName, serviceInstanceID, serviceName);
		AbstractService service = engine.getEngine().getService(serviceInstanceID);
		if(service!=null) {
			Task task = service.getTaskbyName(taskName);
			pub.setPublic(task.isPort());
			
			exposeEvent(pub);
		}
				
	}
}

package ServiceTesting.Monitoring;

import java.util.ArrayList;

import Service.AbstractService;
import engine.engine;
import engine.Event.ServiceCompleteEvent;
import engine.Event.Event;
import engine.Event.EventType;
import engine.Queue.AbstractListener;
import engine.Queue.Message;
import engine.Queue.Publication;
import engine.Queue.Subscription;

public class TestingMonitor extends AbstractListener {

	public boolean terminated = false;
	
	private ArrayList<String> running_instances = new ArrayList<String>();
	
	private ArrayList<String> completeConditions = new ArrayList<String>();

	@Override
	public void onNotification(Publication pub) {
        Message msg = pub.getMessage(Event.fieldname_headName);
        if(msg!=null) {
        	
        	String type = (String)msg.getValue();
        	if(EventType.servicecomplete_type.equals(type)) {
        		
        		msg = pub.getMessage(Event.fieldname_serviceInstanceID);
        		if(msg!=null) {
        	
        			String instanceID = (String) msg.getValue();
        			running_instances.remove(instanceID);
        			completeConditions.remove(instanceID);
        			
        			if(completeConditions.isEmpty())
        				terminated = true;
        		}
        	}
        }
	}

	public void register(ArrayList<AbstractService> services) {		
		for(AbstractService service: services) {
			String instanceID = service.getInstanceID();
			running_instances.add(instanceID);
			
			//subscribe to each service complete event
			Subscription sub = ServiceCompleteEvent.createServiceCompleteEventSubscription(instanceID);
			engine.getEngine().getQueue().subscribe(sub, this);
		}
	}
	
	public void registerCompleteConditions(AbstractService service) {		
		
		String instanceID = service.getInstanceID();
		completeConditions.add(instanceID);					
	}
	
	public void gabarageCollection() {
		engine.getEngine().getQueue().unsubscribe(this);
	}

	public ArrayList<AbstractService> getRemainingServices(
			ArrayList<AbstractService> services) {

		ArrayList<AbstractService> result = new ArrayList<AbstractService>();
		for(AbstractService service: services) {
			String ID = service.getInstanceID();
			if(running_instances.contains(ID))
				result.add(service);
		}			
		
		return result;
	}

}

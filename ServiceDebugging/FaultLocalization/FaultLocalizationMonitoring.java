package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;
import java.util.HashMap;

import Service.AbstractService;
import engine.engine;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;
import engine.Event.ScopeFailureEvent;
import engine.Event.TaskCompleteEvent;
import engine.Event.TaskFailureEvent;
import engine.Event.TransitionFiringEvent;
import engine.Event.TransitionNotFiringEvent;
import engine.Event.VariableEvent;
import engine.Queue.AbstractListener;
import engine.Queue.Publication;
import engine.Queue.Subscription;

public class FaultLocalizationMonitoring extends AbstractListener {
		
		
	private Encapsulation m_encapsulation = new Encapsulation();
	private ExecutionRecord m_record = new ExecutionRecord();			
	private HashMap<String, ArrayList<Subscription>> submap = new HashMap<String, ArrayList<Subscription>>();
		
	
	synchronized public void setFinalResult(boolean passed) {
		m_record.setPassed(passed);
	}

	public void recordExecutionInstances() {
		m_encapsulation.addInstanceRecord(m_record);
		m_record = new ExecutionRecord();
	}
	
	
	public void addGlobalLog(String tn) {
    	m_record.addGlobalLog(tn);
    }
	

    public Encapsulation getEncapsulation() {
    	return m_encapsulation;
    }
	
	@Override
	public void onNotification(Publication pub) {
		//keep the four types of events
		if(pub instanceof VariableEvent || 
				pub instanceof TaskCompleteEvent ||
				pub instanceof TransitionFiringEvent ||
				pub instanceof TransitionNotFiringEvent) {
			
			String serviceName = ((Event) pub).getServiceName();
			m_record.addServiceEvent(serviceName, (Event)pub);
		}
			 
		
	}	
	
	public void register(ArrayList<AbstractService> services) {
		//subscribe to events related to the services
		for(AbstractService service: services) {
			subscribeEvent4Service(service);
		}
		
		m_encapsulation.snapshotServiceInstance(services);
	}

	private void subscribeEvent4Service(AbstractService service) {
		ArrayList<Subscription> sublist = new ArrayList<Subscription>(); 
		String instanceID = service.getInstanceID();
		
		//subscribe to task complete event
		Subscription sub = TaskCompleteEvent.createAllTaskCompleteEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to task start event
		//sub = TaskStartEvent.createAllTaskStartEventSubscription(instanceID);
		//engine.getEngine().getQueue().subscribe(sub, this);
		//sublist.add(sub);
		
		//subscribe to service complete event
		//sub = ServiceCompleteEvent.createServiceCompleteEventSubscription(instanceID);
		//engine.getEngine().getQueue().subscribe(sub, this);
		//sublist.add(sub);
		
		//subscribe to data read event
		sub = DataReadEvent.createAllDataReadEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to data modification event
		sub = DataModificationEvent.createAllDataModificationEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to transition firing event
		sub = TransitionFiringEvent.createAllTransitionFiringEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);

		//subscribe to transition not firing event
		sub = TransitionNotFiringEvent.createAllTransitionNotFiringEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//added two more events: task failure event && scope failure event 2016.11.13
		//subscribe to task failure event
		sub = TaskFailureEvent.createAllTaskFailureEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to scope failure event
		sub = ScopeFailureEvent.createAllScopeFailureEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		submap.put(instanceID, sublist);
	}
	
	public void unregister(ArrayList<AbstractService> services) {
		//unsubscribe to events from the services
		for(AbstractService service: services) {
			unsubscribeEvent(service);
		}
	}

	private void unsubscribeEvent(AbstractService service) {
		String instanceID = service.getInstanceID();
		
		//clear the map
		ArrayList<Subscription> sublist = submap.remove(instanceID);
		
		//unsubscribe the list
		engine.getEngine().getQueue().unsubscribe(sublist);
	}

	public void registerMutation(String mToken) {
		m_encapsulation.addMutation(mToken);
		
	}

	private boolean registerMutationFlag = false;
	public boolean notRegisterMutation() {
		return !registerMutationFlag;
	}

	public void setMutationRegisterFlag() {		
		registerMutationFlag = true;
	}

}

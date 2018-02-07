package ServiceTesting.Monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import Service.AbstractService;
import ServiceTesting.ConstraintSolver.ConstraintTransformer;
import ServiceTesting.ConstraintSolver.MyConstraint;

import engine.engine;
import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;
import engine.Event.ServiceCompleteEvent;
import engine.Event.TaskCompleteEvent;
import engine.Event.TaskStartEvent;
import engine.Event.TransitionFiringEvent;
import engine.Queue.AbstractListener;
import engine.Queue.Publication;
import engine.Queue.Subscription;

public class CoverageMonitor extends AbstractListener {
	
	private ArrayList<Event> observed_events = new ArrayList<Event>();
	
	private HashMap<String, ArrayList<Subscription>> submap = new HashMap<String, ArrayList<Subscription>>();
	
	private Observation m_observation_testing;
	private Observation m_observation_global;
	
	
	public Observation getTestingObservation() {
		return m_observation_testing;
	}
	
	public Observation getGlobalObservation() {
		return m_observation_global;
	}
	
	@Override
	public void onNotification(Publication pub) {		
		if(pub instanceof Event) {			
			observed_events.add((Event)pub);
		}
		
		//for debugging
		//pub.printMessage();
	}
	
	public void matchTestingObservation() {
		//feed events
		m_observation_testing.addObservedEvent(observed_events);
	}
	
	public void matchGlobalObservation() {
		//feed events		
		if(!m_observation_global.equals(m_observation_testing)) {
			m_observation_global.addObservedEvent(observed_events);
		}				
	}
	
	public void clearObservationEvents() {
		//remove the observed events
		observed_events.clear();
	}
	
	public ArrayList<Event> getObservationEvents() {
		return observed_events;
	}

	public double calculateTestingCoverage() {		
		return m_observation_testing.getCoverage();		
	}
	
	public double calculateRealCoverage() {
		return m_observation_global.getCoverage();
	}

	public void register(ArrayList<AbstractService> services) {
		//subscribe to events related to the services
		for(AbstractService service: services) {
			subscribeEvent4Service(service);
		}
	}

	private void subscribeEvent4Service(AbstractService service) {
		ArrayList<Subscription> sublist = new ArrayList<Subscription>(); 
		String instanceID = service.getInstanceID();
		
		//subscribe to task complete event
		Subscription sub = TaskCompleteEvent.createAllTaskCompleteEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to task start event
		sub = TaskStartEvent.createAllTaskStartEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
		//subscribe to service complete event
		sub = ServiceCompleteEvent.createServiceCompleteEventSubscription(instanceID);
		engine.getEngine().getQueue().subscribe(sub, this);
		sublist.add(sub);
		
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

	public void setTestingObservation(Observation observation) {
		this.m_observation_testing = observation;
	}
	
	public void setGlobalObservation(Observation observation) {
		this.m_observation_global = observation;
	}
	
	private int[] weight;
	public int[] getConstraintWeights() {
		return weight;
	}
	
	public ArrayList<ArrayList<MyConstraint>> getConstraints(int coverage, String testscript) {		
		ArrayList<ObservationUnit> obunits = m_observation_global.getObservationUnit();
		if(obunits==null) return null;
		
		ArrayList<ArrayList<MyConstraint>> constraint_list = new ArrayList<ArrayList<MyConstraint>>();		
		for(ObservationUnit unit: obunits) {
			if(unit instanceof EventSequenceObservationUnit) {
				ArrayList<MyConstraint> cs_list = ((EventSequenceObservationUnit)unit).getConstraints();
				constructHybridEventSequenceConstraint(cs_list, unit.getExposedPercentage());
				constraint_list.add(cs_list);
			}
			
			if(unit instanceof DUObservationUnit) {
				ArrayList<ArrayList<MyConstraint>> mc_list = ((DUObservationUnit)unit).getConstraints();
				if(((DUObservationUnit)unit).isExposed()) {
					for(ArrayList<MyConstraint> mc_item: mc_list)
						addConstraints(constraint_list, mc_item);
				}
			}
		}
		
		ConstraintTransformer.removeTrueConstraints(constraint_list);
				
		if(coverage == 4)			
			filterConstraints(testscript, constraint_list);
		
		if(coverage == 3) {
			complementDUConstraints(testscript, constraint_list);
			
			for(ArrayList<MyConstraint> cs_list: constraint_list)
			    ConstraintTransformer.filterInvalidConstraints(cs_list);
			
			HashMap<ArrayList<MyConstraint>, Integer> ws = ConstraintTransformer.removeRedundantConstraints(constraint_list);
			//filterDUConstraints(testscript, constraint_list);	
			
			weight = new int[constraint_list.size()];
			int index=0;
			for(ArrayList<MyConstraint> mc_list: constraint_list) {
				Integer w1 = ws.get(mc_list);
				if(w1==null) w1 = 1;
				weight[index] =	w1;
				index++;
			}
		}				

		return constraint_list;
	}
	
	private void complementDUConstraints(String testscript, ArrayList<ArrayList<MyConstraint>> constraint_list) {
		String sn="", condition="";
		if(testscript.equals("Applications/BookOrdering/testscript.xml")) {
			sn = "BookOrderingClient";
			condition = "send amount account_credit confirm hasInterests customerID new_amount creditcard_credit";
		}
		
		if(testscript.equals("Applications/LoanApproval/testscript.xml")) {
			sn = "LoanClient";
			condition = "send amount customerID hasrecord rejected hasdeposit";
		}
		
		if(testscript.equals("Applications/SupplyChain/testscript.xml")) {
			sn = "SupplyChainClient";
			condition = "send amount product_type stockA stockB stockC manufacturer_stock factoryA_can_produce factoryB_can_produce factoryC_can_produce";
		}
		
		for(ArrayList<MyConstraint> mc_list: constraint_list){
			MyConstraint mc = mc_list.get(0);
			if(!mc.serviceName.equals(sn)) {
				MyConstraint pmc = new MyConstraint();
				pmc.serviceName = sn;
				pmc.condition = condition;
				mc_list.add(0, pmc);
			}
		}
	}
	
	private void filterDUConstraints(String testscript, ArrayList<ArrayList<MyConstraint>> constraint_list) {
		ArrayList<ArrayList<MyConstraint>> nosolution = new ArrayList<ArrayList<MyConstraint>>();
		if(testscript.equals("Applications/BookOrdering/testscript.xml")) {
			//3, 8
			int[] ns = {3, 8};
			for(int index: ns) {
				nosolution.add(constraint_list.get(index));
			}
		}
		
		constraint_list.removeAll(nosolution);
	}
	
	private void filterConstraints(String testscript, ArrayList<ArrayList<MyConstraint>> constraint_list) {
		ArrayList<ArrayList<MyConstraint>> nosolution = new ArrayList<ArrayList<MyConstraint>>();
		if(testscript.equals("Applications/LoanApproval/testscript.xml")) {
			int[] ns = {1, 2, 3, 4, 7, 8, 11, 12, 15};			
			for(int index: ns) {
				nosolution.add(constraint_list.get(index));
			}
		} 
		
		if(testscript.equals("Applications/BookOrdering/testscript.xml")) {
			//2, 5, 7, 8, 10, 13, 
			int[] ns = {2, 5, 7, 8, 10, 13, 15, 16, 18, 21, 23, 24, 26, 29, 31, 32};
			for(int index: ns) {
				nosolution.add(constraint_list.get(index));
			}
		} 
		
		constraint_list.removeAll(nosolution);		
	}
	
	private void constructHybridEventSequenceConstraint(ArrayList<MyConstraint> cs_list, double exposedPercentage) {
			
		HashMap<String, ArrayList<MyConstraint>> buffer = new HashMap<String, ArrayList<MyConstraint>>();
			
		for(MyConstraint mc: cs_list) {
			if(mc.isAssignment()||mc.isBooleanExpression()) {
			    String sn = mc.serviceName;
			    ArrayList<MyConstraint> ic_list = buffer.get(sn);
			    if(ic_list==null) {
			    	ic_list = new ArrayList<MyConstraint>();
			    	buffer.put(sn, ic_list);
			    }
			    ic_list.add(mc);				    
			}
		}
			
		Set<String> sn_list = buffer.keySet();
		for(String sn: sn_list) {
			ArrayList<MyConstraint> ic_list = buffer.get(sn);
			int length = (int) (ic_list.size() * exposedPercentage);
			for(int i=length;i<ic_list.size();i++) {
				cs_list.remove(ic_list.get(i));
			}
		}		
	}

	private void addConstraints(
			ArrayList<ArrayList<MyConstraint>> constraint_list,
			ArrayList<MyConstraint> mc_list) {
		
		boolean existing = false;
		for(ArrayList<MyConstraint> item_list: constraint_list) {
			if(same(item_list, mc_list)) {
				existing = true;
				break;
			}
		}
		
		if(!existing) constraint_list.add(mc_list);		
	}

	private boolean same(ArrayList<MyConstraint> item_list,
			ArrayList<MyConstraint> mc_list) {
		
		if(item_list.size()!=mc_list.size()) return false;
		for(int i=0;i<item_list.size();i++) {
			MyConstraint mc1 = item_list.get(i);
			MyConstraint mc2 = mc_list.get(i);
			if(!mc1.isSame(mc2)) return false;
		}
		
		return true;
	}

	public void setExposedPercentage(double exposedPercentage) {
		m_observation_global.setExposedPercentage(exposedPercentage);		
	}

}

package ServiceTesting.InformationLeakage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;
import engine.Event.TaskCompleteEvent;

public class InformationLeakage {
	
	private HashMap<Event, Integer> exposedEvents = new HashMap<Event, Integer>();
	//private HashMap<Event, Integer> internalEvents = new HashMap<Event, Integer>();
	private HashMap<Event, HashMap<Event, Integer>> coP = new HashMap<Event, HashMap<Event, Integer>>();

	private boolean needExposed = false;
	//private int total = 0;
	//private int total_exposed = 0;
	private int totalRun = 0;
	//private HashMap<Event, ArrayList<Integer>> exposedEventRecords = new HashMap<Event, ArrayList<Integer>>();
	//private HashMap<Event, ArrayList<Integer>> internalEventRecords = new HashMap<Event, ArrayList<Integer>>();
	//private HashMap<Event, HashMap<Event, ArrayList<Integer>>> coRecords = new HashMap<Event, HashMap<Event, ArrayList<Integer>>>();
	
	private ArrayList<ArrayList<Event>> runRecords = new ArrayList<ArrayList<Event>>();
	public void addEvents(ArrayList<Event> events) {
				
		ArrayList<Event> exposed = new ArrayList<Event>();
		for(Event event: events) {
			if(event.isPublic() || (event.isExposed() && needExposed)) {
				addEvent(exposedEvents, event);				
				exposed.add(event);
			} else {
				if(hasSameExposedID(exposedEvents, event)) {
					addEvent(exposedEvents, event);				
					exposed.add(event);
				}
			}
		}
		
		runRecords.add(exposed);		
		totalRun++;
		
//		for(Event event: events) {
//			if(event.isPublic() || (event.isExposed() && needExposed)) {
//				addEvent(exposedEvents, event);
//				total_exposed++;
//			} else
//				addEvent(internalEvents, event);
//			
//			total++;
//		}
//		
//		for(Event event1: events) {
//			HashMap<Event, Integer> map = getEventCoPMap(event1);			
//			for(Event event2: events) {
//				if(event1.equals(event2)) continue;
//				   addEventCoCount(map, event2);
//			}
//		}
	}
	
	private boolean hasSameExposedID(HashMap<Event, Integer> exposed,
			Event event) {
		Set<Event> keys = exposed.keySet();
		for(Event key:keys) {
			if(event instanceof TaskCompleteEvent && key instanceof TaskCompleteEvent) {
				String sn1 = ((TaskCompleteEvent)event).getServiceName();
				String tn1 = ((TaskCompleteEvent)event).getTaskName();
				
				String sn2 = ((TaskCompleteEvent)key).getServiceName();
				String tn2 = ((TaskCompleteEvent)key).getTaskName();
				
				if(sn1.equals(sn2) && tn1.equals(tn2)) return true;
			}
			
			if(event instanceof DataReadEvent && key instanceof DataReadEvent) {
				String sn1 = ((DataReadEvent)event).getServiceName();
				String loc1 = ((DataReadEvent)event).getVariableLoc();
				
				String sn2 = ((DataReadEvent)key).getServiceName();
				String loc2 = ((DataReadEvent)key).getVariableLoc();
				
				if(sn1.equals(sn2) && loc1.equals(loc2)) return true;
			}
			
			if(event instanceof DataModificationEvent && key instanceof DataModificationEvent) {
				String sn1 = ((DataModificationEvent)event).getServiceName();
                String loc1 = ((DataModificationEvent)event).getVariableLoc();
				
				String sn2 = ((DataModificationEvent)key).getServiceName();
				String loc2 = ((DataModificationEvent)key).getVariableLoc();
				
				if(sn1.equals(sn2) && loc1.equals(loc2)) return true;
			}
		}
		
		return false;
	}

	private void addEventCoCount(HashMap<Event, Integer> map, Event event) {
		Set<Event> keys = map.keySet();
		boolean existing = false;
		for(Event key: keys) {
			if(event.isSameEvent(key)) {
				Integer num = map.get(key);
				map.put(key, num+1);
				existing = true;
				break;
			}
		}
		
		if(!existing) {
			map.put(event, 1);
		}
	}

	private HashMap<Event, Integer> getEventCoPMap(Event event1) {
		
		Set<Event> keys = coP.keySet();
		for(Event key: keys) {
			if(event1.isSameEvent(key))
				return coP.get(key);
		}
		
		HashMap<Event, Integer> map = new HashMap<Event, Integer>();
		coP.put(event1, map);
		
		return map;
	}

	public void setNeedExposed(boolean value) {
		needExposed = value;
	}

	private void addEvent(HashMap<Event, Integer> eventmap, Event event) {
		Set<Event> keys = eventmap.keySet();
		boolean isfound = false;
		for(Event key: keys)
			if(key.isSameEvent(event)) {
				int num = eventmap.get(key) + 1;
				eventmap.put(key, num);
				isfound = true;
				break;
			}
		
		if(!isfound) 
			eventmap.put(event, 1);
	}
	
	public double[] getInformationLeakage() {
	    HashMap<Event, Double> pbs = calculateEventPbs();
	    
	    double[] result = new double[totalRun];
	    for(int i=0;i<totalRun;i++) {
	    	result[i] = getInformationLeakage(i, pbs);
	    }
	    
	    return result;
	}
	
	private HashMap<Event, Double> calculateEventPbs() {
		
		HashMap<Event, Double> result = new HashMap<Event, Double>();
		Set<Event> keys = exposedEvents.keySet();
		for(Event event: keys) {
			double pb = exposedEvents.get(event)*1.0/totalRun;
			if(pb>1) pb = 1;
			result.put(event, pb);
		}
		
		return result;
	}

	private double getInformationLeakage(int runIndex, HashMap<Event, Double> pbs) {
						
		double HA = 0;
		ArrayList<Event> events = getUniqueEvents(runRecords,runIndex);
		for(Event event: events) {
			double p_x1 = getEventPb(pbs, event);
			double p_x0 = 1 - p_x1;
			HA = HA - calculateH(p_x1) - calculateH(p_x0);
		}
		
		
		/*		
		Set<Event> epkeys = exposedEvents.keySet();
		for(Event event: epkeys) {
			int count = exposedEvents.get(event);
			double pv = count * 1.0 / total_exposed;
			HA -=  pv * Math.log(pv);
		}
		

		Set<Event> itkeys = internalEvents.keySet();
		for(Event event: itkeys) {
			int count = internalEvents.get(event);
			double ptv = count * 1.0 /total;
			HA -= ptv * Math.log(ptv);
			
			double ppv = count * 1.0 / (total - total_exposed);
			HA += (total - total_exposed) * 1.0/total * ppv * Math.log(ppv);
		}*/

		/*
		for(Event event: epkeys) {
			int count = exposedEvents.get(event);
			double pv = count * 1.0 / total_exposed;
			HashMap<Event, Integer> map = getEventCoPMap(event);
			Set<Event> cekeys = map.keySet();
			for(Event ce: cekeys) {
				int cc = map.get(ce);
				double cpv = cc* 1.0 / (count * total);
				HA += pv * cpv * Math.log(cpv);
			}
			
		}*/				
		
		return HA;
	}

	private ArrayList<Event> getUniqueEvents(
			ArrayList<ArrayList<Event>> records, int runIndex) {
        
		ArrayList<Event> result = new ArrayList<Event>();  
		for(int i=0;i<=runIndex;i++) {
			ArrayList<Event> buffer = records.get(i);
			for(Event event1: buffer) {
				boolean existing = false;
				for(Event event2: result) 
					if(event2.isSameEvent(event1)) {
						existing = true;
						break;
					}
				
				if(!existing) result.add(event1);
			}
		}
		
		return result;
	}

	private double getEventPb(HashMap<Event, Double> pbs, Event event) {
		Set<Event> keys = pbs.keySet();
		for(Event key:keys) 
			if(key.isSameEvent(event)) return pbs.get(key);
		
		return 0;
	}

	private double calculateH(double p_x1) {
		if(p_x1 == 0) return 0;
		return p_x1 * Math.log(p_x1)/Math.log(2);
	}
}

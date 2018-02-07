package ServiceTesting.EventInterface;

import java.util.ArrayList;

import Service.AbstractService;
import Service.Transition;

public class BranchCoverageEventInterface extends EventInterface {
	
	private ArrayList<String> transitionID = new ArrayList<String>();

	@Override
	public void deriveEventInterface(AbstractService service) {
		
		setServiceName(service.getName());
		
		String taskname = service.getStartTask();
		ArrayList<String> search_queue = new ArrayList<String>();
		ArrayList<String> visited = new ArrayList<String>();
		
		search_queue.add(taskname);
		while(!search_queue.isEmpty()) {
			taskname = search_queue.remove(0);
			visited.add(taskname);
			
            ArrayList<Transition> trans = service.getAvailableNextTransitions(taskname);
            
            if(trans!=null) {//branch
            	for(Transition tr:trans) {
            		String nextTask = tr.getSink();
            		String transitionName = tr.getName();
            		
            		if(trans.size()>1)
            		    addTransitionID(transitionName);
            		
            		if(!visited.contains(nextTask) && !search_queue.contains(nextTask)) {
            			search_queue.add(nextTask);
            		}            		
            	}
            }
            
            
		}
	}

	private void addTransitionID(String transitionName) {
		if(!transitionID.contains(transitionName))
			transitionID.add(transitionName);
		
	}
	
	public ArrayList<String> getTransitionList() {
		return transitionID;
	}

}

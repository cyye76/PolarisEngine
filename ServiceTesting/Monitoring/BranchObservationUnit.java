package ServiceTesting.Monitoring;

import engine.Event.Event;
import engine.Event.TransitionFiringEvent;

public class BranchObservationUnit extends ObservationUnit {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2432446842900246875L;
	private String transitionID;
	private String serviceName;
	private boolean isObserved = false;
	
	@Override
	public void feedEvent(Event event) {		
		String sn = event.getServiceName();
		if(serviceName.equals(sn)) {
			if(!isObserved && event instanceof TransitionFiringEvent) {
				String trName = ((TransitionFiringEvent)event).getTransitionName();
				if(transitionID.equals(trName))
					isObserved = true;
			}
		}
	}

	@Override
	public boolean isObserved() {
		return this.isObserved;
	}

	@Override
	public void rollback() {		
		// no need to reset for this unit
		// do nothing
	}

	@Override
	public boolean equals(ObservationUnit unit) {
		if(!(unit instanceof BranchObservationUnit))
		   return false;
			
		String p_serviceName = ((BranchObservationUnit)unit).getServiceName();
		String p_trID = ((BranchObservationUnit)unit).getTransitionName();
			
		return serviceName.equals(p_serviceName) && transitionID.equals(p_trID);		
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
		
	}
	
	public String getServiceName() {
		return this.serviceName;
	}

	public void setTransitionName(String trName) {
		this.transitionID = trName;
	}
	
	public String getTransitionName() {
		return this.transitionID;
	}

	@Override
	public void print() {		
		System.out.println(serviceName + ":" + transitionID);
	}

}

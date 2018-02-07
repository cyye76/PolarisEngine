package ServiceTesting.Monitoring;

import engine.Event.Event;
import engine.Event.TaskCompleteEvent;

public class ActivityObservationUnit extends ObservationUnit {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5285269011021439755L;
	private String taskID;
	private String serviceName;
	private boolean isObserved = false;

	@Override
	public void feedEvent(Event event) {	
		String sn = event.getServiceName();
		if(serviceName.equals(sn)) {
			if(!isObserved && event instanceof TaskCompleteEvent) {
				String taskName = ((TaskCompleteEvent)event).getTaskName();
				if(taskID.equals(taskName))
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

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
		
	}
	
	public String getServiceName() {
		return this.serviceName;
	}

	public void setTaskName(String acname) {
		this.taskID = acname;
	}
	
	public String getTaskName() {
		return this.taskID;
	}

	@Override
	public boolean equals(ObservationUnit unit) {
		if(!(unit instanceof ActivityObservationUnit))
		   return false;
		
		String p_serviceName = ((ActivityObservationUnit)unit).getServiceName();
		String p_taskID = ((ActivityObservationUnit)unit).getTaskName();
		
		return serviceName.equals(p_serviceName) && taskID.equals(p_taskID);
	}

	@Override
	public void print() {		
		System.out.println(serviceName + ":" + taskID);
	}

}

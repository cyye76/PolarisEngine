package ServiceTesting.Monitoring;

import java.io.Serializable;

import engine.Event.Event;

abstract public class ObservationUnit implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	abstract public void feedEvent(Event event);
	abstract public boolean isObserved();
	
	//to reset the status of the unit if it is only partially matched
	//e.g., DU match, event sequence match etc.
	abstract public void rollback();
	
	abstract public boolean equals(ObservationUnit unit);
	abstract public void print();//for debugging	
	
	protected double exposedPercentage = 1;
	public void setExposedPercentage(double exposedPercentage) {
		this.exposedPercentage = exposedPercentage;
	}

	public double getExposedPercentage() {
		return exposedPercentage;
	}
	//abstract public String getServiceName();
}

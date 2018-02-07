package ServiceTesting.EventInterface;

import Service.AbstractService;

abstract public class EventInterface {
	
	public abstract void deriveEventInterface(AbstractService service);
	
	private String serviceName;
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public String getServiceName() {
		return this.serviceName;
	}
}

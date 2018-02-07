package engine.Communication;

import java.util.ArrayList;

import Service.Choreography;

public interface AbstractCorrelation {

	public void register(Choreography cho); 

	public ArrayList<String> getReceivingServices(String serviceName,
			String serviceID, String msg_name);

	public void unregister(Choreography cho);
}

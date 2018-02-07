package MyCommunication;

import java.util.ArrayList;
import java.util.HashMap;

import Service.Choreography;

import engine.Communication.AbstractCorrelation;

public class SamplePortCorrelation implements AbstractCorrelation {
	
	private HashMap<String, Choreography> m_correlation = new HashMap<String, Choreography>();

	@Override
	public ArrayList<String> getReceivingServices(String serviceName,
			String serviceID, String msg_name) {
		
		ArrayList<String> result = new ArrayList<String>();
		
		Choreography cho = m_correlation.get(serviceID);
		if(cho!=null) {		
			ArrayList<String> ids = cho.getServiceIDs();
			for(String id: ids) {
				if(id.equals(serviceID)) continue;
				
				ArrayList<String> ports = cho.getServicePorts(id);
				if((ports!=null) && ports.contains(msg_name)) 
					result.add(id);				
			}			
		}
		
		return result;
	}

	@Override
	public void register(Choreography cho) {
		if(cho!=null) {
			ArrayList<String> ids = cho.getServiceIDs();
			for(String serviceID: ids) {
				m_correlation.put(serviceID, cho);
			}
		}
		
	}

	@Override
	public void unregister(Choreography cho) {
		if(cho!=null) {
			ArrayList<String> ids = cho.getServiceIDs();
			for(String serviceID: ids) {
				m_correlation.remove(serviceID);
			}
		}
		
	}

}

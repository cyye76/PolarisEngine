package Service;

import java.util.ArrayList;
import java.util.HashMap;

public class Choreography {

	private HashMap<String, ArrayList<String>> m_choreography;
	
	public Choreography() {
		m_choreography = new HashMap<String, ArrayList<String>>();
	}
	
	public void bind(String serviceID, ArrayList<String> ports) {
		m_choreography.put(serviceID, ports);
	}
	
	public ArrayList<String> getServiceIDs() {
		return new ArrayList<String>(m_choreography.keySet());
	}
	
	public ArrayList<String> getServicePorts(String serviceID) {
		return m_choreography.get(serviceID);
	}
}

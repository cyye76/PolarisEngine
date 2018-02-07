package ServiceTesting.Monitoring;

import java.util.HashMap;
import java.util.Set;

public class LocalObservation extends Observation {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8709661525701951320L;
	private HashMap<ObservationUnit, String> mapping;

	@Override
	public double getCoverage() {
        HashMap<String, int[]> cache = new HashMap<String, int[]>();		
		
		for(ObservationUnit unit: my_observations) {
			String sn = mapping.get(unit);
			
			int[] vl = cache.get(sn);
			if(vl==null) {
				vl = new int[2];
				vl[0] = 0;
				vl[1] = 0;
				cache.put(sn, vl);
			}
			
			vl[0]++;
		}
		
		for(ObservationUnit unit: non_observations) {
			String sn = mapping.get(unit);
			
			int[] vl = cache.get(sn);
			if(vl==null) {
				vl = new int[2];
				vl[0] = 0;
				vl[1] = 0;
				cache.put(sn, vl);
			}
			
			vl[1]++;
		}
		
		Set<String> keys = cache.keySet();
		double coverage=0;
		for(String sn:keys) {
			int[] vl = cache.get(sn);
			coverage += vl[0]*1.0/(vl[0]+vl[1]);
		}
		
		return coverage/keys.size();
	}

	public void setMapping(HashMap<ObservationUnit, String> mapping) {
		this.mapping = mapping;
	}

	public HashMap<ObservationUnit, String> getMapping() {
		return mapping;
	}
	
	public void addMapping(String sn, ObservationUnit unit) {
		if(mapping == null) {
			mapping = new HashMap<ObservationUnit, String>();
		}
		
		mapping.put(unit, sn);
	}

}

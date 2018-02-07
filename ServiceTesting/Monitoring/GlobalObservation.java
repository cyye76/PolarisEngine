package ServiceTesting.Monitoring;

public class GlobalObservation extends Observation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4873267967261315012L;

	@Override
	public double getCoverage() {
		return my_observations.size()*1.0/(my_observations.size() + non_observations.size());
	}

}

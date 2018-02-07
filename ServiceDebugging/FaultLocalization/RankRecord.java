package ServiceDebugging.FaultLocalization;

public class RankRecord {

	public String[] eventIDs;
	public String[] groupIDs;
	public double rankValue;
	public double prob;
	public String recordID;
	
	public double reverse_rankvalue;
	public double reverse_prob;
	
	public double sn;
	public double fn;
	
	//added on 2016.11.10
	//used to record the event values in the cluster
	public ProbeEvent[] events = null;
}

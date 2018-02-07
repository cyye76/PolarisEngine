package ServiceDebugging.FaultLocalization;

import java.util.ArrayList;

import engine.DataField;

/**
 * This class is used to generate composite probe events
 * Such events are used temporally for calculating the cluster and ranking only
 * If sub events are refined, the composite events are re-constructed from the scratch.
 */

public class CompositeProbeEvent extends ProbeEvent {
	
	private ArrayList<ProbeEvent> events = null;		
	
	public CompositeProbeEvent(ArrayList<ProbeEvent> events, String eID) {
         
         this.events = events;
         instanceID = events.get(0).instanceID;         
         eventID = eID;     
         
         genDataField();
	}
	
	private void genDataField() {
		
		for(ProbeEvent evt: events) {
		    String eID = evt.getEventID();
		    ArrayList<DataField> dfs = evt.getDataField();
		    
		    for(DataField df: dfs) {
		    	DataField ndf = new DataField();
		    	ndf.setName(eID + df.getName());
		    	ndf.setType(df.getType());
		    	ndf.setValue(df.getValue());
		    	
		    	fields.add(ndf);
		    }
		}						
	}	

}

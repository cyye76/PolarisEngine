package ServiceTesting.Monitoring;

import java.util.ArrayList;

import ServiceTesting.ConstraintSolver.MyConstraint;
import ServiceTesting.EventInterface.DUPair;

import engine.Event.DataModificationEvent;
import engine.Event.DataReadEvent;
import engine.Event.Event;

public class DUObservationUnit extends ObservationUnit {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -8872676315224142950L;
	private ArrayList<DUItem> queue = new ArrayList<DUItem>();
	private int matchIndex = 0;

	@Override
	public void feedEvent(Event event) {
	   //String sn = event.getServiceName();
	   
	   if(matchIndex<queue.size()) {
		   DUItem item = queue.get(matchIndex);		   
		   
		   if(matchDUItem(event,item)) {
			   matchIndex++;
			   event.setExposed(needExposed);
		   }
	   }	   	   
	}

	private boolean matchDUItem(Event event, DUItem item) {
		int eventType = -1;
		String vLoc = null;
		if(event instanceof DataReadEvent) {
			eventType = 0;
			vLoc = ((DataReadEvent)event).getVariableLoc();
		}
		
		if(event instanceof DataModificationEvent) {		
			eventType = 1;
			vLoc = ((DataModificationEvent)event).getVariableLoc();
		}
		
		if(item.type!=eventType) return false;				
				
		if(item.ID == null && vLoc!=null) return false;
		
		String sn = event.getServiceName();
		return item.ID.equals(vLoc) && item.serviceName.equals(sn);	   
	}

	@Override
	public boolean isObserved() {		
		return matchIndex>=queue.size();
	}

	@Override
	public void rollback() {
		matchIndex = 0;		
	}
	
	public DUItem getDUItem(int index) {
		if(index<queue.size()) return queue.get(index);
		
		return null;
	}
	
	public int getDUItemNum() {
		return queue.size();
	}
		
    public DUObservationUnit(String serviceName1, String defID, String serviceName2, String useID) {
    	
    	DUItem item1 = new DUItem();
    	item1.serviceName = serviceName1;
    	item1.ID = defID;
    	item1.type = 1;//write
    	if(item1.ID!=null) queue.add(item1);
    	
    	DUItem item2 = new DUItem();
    	item2.serviceName = serviceName2;
    	item2.ID = useID;
    	item2.type = 0; //read
    	if(item2.ID!=null) queue.add(item2);    	    	
    }
    
    public DUObservationUnit(ArrayList<String> serviceNames, ArrayList<String> IDs, ArrayList<Integer> types) {
    	
    	for(int i=0;i<serviceNames.size();i++) {
    		DUItem item = new DUItem();
    		item.serviceName = serviceNames.get(i);
    		item.ID = IDs.get(i);
    		item.type = types.get(i);
    		if(item.ID!=null) queue.add(item);
    	}    		
    }

	public DUObservationUnit(DUPair sourcedu, DUPair sinkdu) {
		DUItem item = new DUItem();
		item.serviceName = sourcedu.getServiceName();
		item.ID = sourcedu.getDef();
		item.type = 1;//write
		if(item.ID!=null) queue.add(item);
		
		item = new DUItem();
		item.serviceName = sourcedu.getServiceName();
		item.ID = sourcedu.getUse();
		item.type = 0;//read
		if(item.ID!=null) queue.add(item);
		
		item = new DUItem();
		item.serviceName = sinkdu.getServiceName();
		item.ID = sinkdu.getDef();
		item.type = 1;//write
		if(item.ID!=null) queue.add(item);
		
		item = new DUItem();
		item.serviceName = sinkdu.getServiceName();
		item.ID = sinkdu.getUse();
		item.type = 0;//read
		if(item.ID!=null) queue.add(item);
	}

	@Override
	public boolean equals(ObservationUnit unit) {
        if(!(unit instanceof DUObservationUnit)) 		
		     return false;
        
        int itemNum = ((DUObservationUnit)unit).getDUItemNum();
        if(queue.size()!=itemNum) return false;
        
        for(int i=0;i<queue.size();i++) {
        	DUItem oItem = getDUItem(i);
        	DUItem nItem = ((DUObservationUnit)unit).getDUItem(i);
        	
        	if(!DUItem.same(oItem, nItem)) return false;
        }
        
        return true;        
	}

	@Override
	public void print() {
		for(int i=0;i<queue.size();i++) {
			DUItem item = queue.get(i);
		    System.out.print(item.serviceName + ":" + item.ID + " -> ");
		}
		System.out.println();
	}

	private ArrayList<ArrayList<MyConstraint>> m_constraint = new ArrayList<ArrayList<MyConstraint>>();	
	public void addConstraint(ArrayList<MyConstraint> csts) {
		m_constraint.add(csts);
	}

	public ArrayList<ArrayList<MyConstraint>> getConstraints() {		
		return m_constraint;
	}

	private boolean needExposed = true;
	public boolean isExposed() {
		return needExposed;
	}	
	
	public void setExposed(boolean value) {
		needExposed = value;
	}
}
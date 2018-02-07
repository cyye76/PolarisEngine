package ServiceTesting.Monitoring;

public class DUItem {
	public String serviceName;
	public String ID;
	
	public int type; //0: DataReadEvent
	                 //1: DataModificationEvent
	                 //-1: Others	

	public static boolean same(DUItem oItem, DUItem nItem) {
		
		if(oItem==null && nItem==null) return true;
		if(oItem==null && nItem!=null) return false;
		if(oItem!=null && nItem==null) return false;
		
		if(oItem.type!=nItem.type) return false;
		if(!oItem.serviceName.equals(nItem.serviceName)) return false;
		if(oItem.ID==null && nItem.ID==null) return true;
		if(oItem.ID==null && nItem.ID!=null) return false;
		if(oItem.ID!=null && nItem.ID==null) return false;		
		
		return oItem.equals(nItem);
	}
}

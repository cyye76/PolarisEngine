package Service;

import Configuration.Config;
import engine.DataField;
import engine.DataType;
import engine.Event.EventExposure;


public class Variable extends DataField {
	
	private String instanceID;//keep service instanceID
	public void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}
	
	private String serviceName;
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public void setValue(Object value, String vLoc) {
		old_value = this.value;
		this.value = value;
		
		//expose an event if the value is different
		//if(!DataType.sameValue(this.value,old_value) && Config.getConfig().exposeevent) {
		if(Config.getConfig().exposeevent) {
			EventExposure.exposeDataModificationEvent(name, type, old_value, this.value, instanceID, vLoc, serviceName);
		}
	}
	
	public Object getValue(String vLoc) {
		//expose an event since the value is read
		if(Config.getConfig().exposeevent) {
			EventExposure.exposeDataReadEvent(name, type, this.value, instanceID, vLoc, serviceName);
		}
		
		return this.value;
	}
}

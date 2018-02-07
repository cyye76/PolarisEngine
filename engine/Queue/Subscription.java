package engine.Queue;

import java.util.ArrayList;

import engine.Event.EventType;

public class Subscription {

	//make sure each subscription has different ID
	private static int subscription_ID = 0;
	
	private ArrayList<Message> ids;
	private String condition = "true";//default, no constraint
	private int identify;

	public Subscription() {
		ids = new ArrayList<Message>();
		identify = getUniqueID();
	}
	
	private synchronized int getUniqueID() {
		subscription_ID++;
		return subscription_ID;
	}

	public void setIds(ArrayList<Message> ids) {
		this.ids = ids;
	}

	public ArrayList<Message> getIds() {
		return ids;
	}
	
	public void addID(Message id) {
		ids.add(id);
	}
	
	public void addID(String name, String type) {
		
		Message msg = new Message();
		msg.setName(name);
		msg.setType(type);
		
		ids.add(msg);
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}
		
}

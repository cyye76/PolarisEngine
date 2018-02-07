package engine.Event;

import engine.Queue.Message;

abstract public class TaskStatusEvent extends Event {
	public static final String fieldname_taskName = "_________TaskName_________";	
	
	protected void setTaskName(String taskName) {
	
		Message msg = new Message();
		msg.setName(fieldname_taskName);
		msg.setType(EventType.STRING);
		msg.setValue(taskName);
		addMessage(msg);			
				
	}
	
	public String getTaskName() {
		
		Message msg = getMessage(fieldname_taskName);
		
		if(msg!=null)
			return (String)msg.getValue();
		
		return null;
	}
}

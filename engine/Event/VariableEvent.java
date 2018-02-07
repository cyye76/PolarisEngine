package engine.Event;

import engine.Queue.Message;

abstract public class VariableEvent extends Event {

	public static final String fieldname_variableName = "_________VariableName_________";
	public static final String fieldname_variableType = "_________VariableType_________";
	public static final String fieldname_variableOldValue = "_________VariableOldValue_________";
	public static final String fieldname_variableNewValue = "_________VariableNewValue_________";
	public static final String fieldname_variableReadValue = "_________VariableReadValue_________";
	public static final String fieldname_variableLOC = "_________VariableLocation________";
	
	public void setVariableName(String vName) {
		Message msg = new Message();
		msg.setName(fieldname_variableName);
		msg.setType(EventType.STRING);
		msg.setValue(vName);
		addMessage(msg);
	}
	
	public void setVariableType(String vType) {
		Message msg = new Message();
		msg.setName(fieldname_variableType);
		msg.setType(EventType.STRING);
		msg.setValue(vType);
		addMessage(msg);
	}
	
	public void setVariableReadValue(String vType, Object value) {
		Message msg = new Message();
		msg.setName(fieldname_variableReadValue);
		msg.setType(vType);
		msg.setValue(value);
		addMessage(msg);
	}
	
	public void setVariableUpdateValues(String vType, Object old_value, Object new_value) {
		Message msg = new Message();
		msg.setName(fieldname_variableOldValue);
		msg.setType(vType);
		msg.setValue(old_value);
		addMessage(msg);
		
		msg = new Message();
		msg.setName(fieldname_variableNewValue);
		msg.setType(vType);
		msg.setValue(new_value);
		addMessage(msg);		
	}
	
	public void setVariableLOC(String vLoc) {
		Message msg = new Message();
		msg.setName(fieldname_variableLOC);
		msg.setType(EventType.STRING);
		msg.setValue(vLoc);
		addMessage(msg);
	}
	
	public String getVariableLoc() {
		
		Message msg = getMessage(fieldname_variableLOC);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
	}	
	
    public String getVariableName() {		
		Message msg = getMessage(fieldname_variableName);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
	}	
    
    public String getVariableType() {
    	Message msg = getMessage(fieldname_variableType);		
		if(msg!=null)
			return (String) msg.getValue();
		
		return null;
    }
    
    public Object getVariableReadValue() {    	
    	Message msg = getMessage(fieldname_variableReadValue);		
		if(msg!=null)
			return msg.getValue();
		
		return null;
    }
    
    public Object getVariableUpdateOldValue() {    	
    	Message msg = getMessage(fieldname_variableOldValue);		
		if(msg!=null)
			return msg.getValue();
		
		return null;
    }
    
    public Object getVariableUpdateNewValue() {    	
    	Message msg = getMessage(fieldname_variableNewValue);		
		if(msg!=null)
			return msg.getValue();
		
		return null;
    }

}

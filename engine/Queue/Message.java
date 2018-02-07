package engine.Queue;

import engine.DataField;

public class Message extends DataField {

	public boolean isSameMessage(Message m1) {
				
		String name0 = getName();
		String type0 = getType();
		Object value0 = getValue();
		
		String name1 = m1.getName();
		String type1 = m1.getType();
		Object value1 = m1.getValue();
				
		if(!name0.equals(name1)) return false;
		if(!type0.equals(type1)) return false;
		if(value0 == null && value1 != null) return false;
		if(value0 !=null && value1 == null) return false;
		if(value0 == null && value1 == null) return true;
		
		return value0.equals(value1);
	}   		
	
}

package engine;

import java.io.Serializable;

/*
 * Basic Data Unit used in the engine
 */
public class DataField implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1487629492972325025L;
	protected String name;
	protected String type;
	protected Object value;
	protected Object old_value;
	//public static final String NA = "___DATAFIELD_RESERVERD_NOTAVAILABLE___";

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setValue(Object value) {
		old_value = this.value;
		this.value = value;				
	}
	
	public Object getValue() {		
		return this.value;
	}

}

package ServiceTesting.ConstraintSolver;

import Configuration.Config;
import engine.DataField;
import engine.DataType;

public class CSLVariable extends DataField {

	private String serviceName;

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}
	
	public static String[] StringDomain = {"low", "high"};	
	
	public boolean isBegin() {		
		if(type.equals(DataType.BOOLEAN))
			return (!(Boolean) value);//if it is false
		
		if(type.equals(DataType.STRING))
			return StringDomain[0].equals(value);
		
		if(type.equals(DataType.INTEGER))
			return ((Integer)value)==0;
		
		return false;
	}
	
	public boolean isEnd() {
		if(type.equals(DataType.BOOLEAN))
			return ((Boolean) value);//if it is true
		
		if(type.equals(DataType.STRING))
			return StringDomain[1].equals(value);
		
		if(type.equals(DataType.INTEGER))
			return ((Integer)value)==Config.getConfig().variableDomain;
		
		return false;
	}
	
	public boolean hasNext() {
		return !isEnd();
	}
	
	public boolean hasPrevious() {
		return !isBegin();
	}
	
	public void next() {
		if(hasNext()) {
			if(type.equals(DataType.BOOLEAN)) setValue(true);				
			
			if(type.equals(DataType.STRING)) setValue(StringDomain[1]);
			
			if(type.equals(DataType.INTEGER)) setValue(((Integer)value)+1);				
		}
	}
	
	public void previous() {
		if(hasPrevious()) {
			if(type.equals(DataType.BOOLEAN)) setValue(false);				
			
			if(type.equals(DataType.STRING)) setValue(StringDomain[0]);
			
			if(type.equals(DataType.INTEGER)) setValue(((Integer)value)-1);				
		}
	}

	public static CSLVariable initialize(String name, String type, String sn) {
		CSLVariable result = new CSLVariable();
		result.setName(name);
		result.setType(type);
		result.setServiceName(sn);
		
		if(type.equals(DataType.BOOLEAN)) result.setValue(false);				
		
		if(type.equals(DataType.STRING)) result.setValue(StringDomain[0]);
		
		if(type.equals(DataType.INTEGER)) result.setValue(0);	
		
		return result;
	}

	public void init() {//re-initialize the variable
		
        if(type.equals(DataType.BOOLEAN)) setValue(false);				
		
		if(type.equals(DataType.STRING)) setValue(StringDomain[0]);
		
		if(type.equals(DataType.INTEGER)) setValue(0);
	}
	
	public CSLVariable backup() {
		CSLVariable bk = new CSLVariable();
		bk.restore(this);
		return bk;
	}
	
	public void restore(CSLVariable csl) {
		setServiceName(csl.getServiceName());
		setName(csl.getName());
		setType(csl.getType());
		setValue(csl.getValue());
	}
	
	public void setBinding(boolean isbinding) {
		this.binding = isbinding;
	}

	public boolean isBinding() {
		return binding;
	}

	private boolean binding = false;	
}

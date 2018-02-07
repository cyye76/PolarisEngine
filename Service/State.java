package Service;

import java.util.ArrayList;

import engine.DataField;
import engine.Event.EventExposure;

import Configuration.Config;


public class State {

	private String name;
	private ArrayList<Variable> variables = null;
	private String instanceID;
	private String serviceName;
	
	//these two variables are used to track the status of running tasks
	private String current_task = null;
	private String last_task = null;
	
	public State(String serviceInstanceID, String serviceName) {		
		setInstanceID(serviceInstanceID);
		this.serviceName = serviceName;
		variables = new ArrayList<Variable>();				
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addVariable(Variable v) {
		v.setInstanceID(instanceID);
		v.setServiceName(serviceName);
		variables.add(v);
	}

	public ArrayList<Variable> getVariables() {
		return variables;
	}

	public void setInstanceID(String instanceID) {
		this.instanceID = instanceID;
	}

	public String getInstanceID() {
		return instanceID;
	}

//	public void updateVariables(ArrayList<DataField> dfs) {
//
//		for(DataField df: dfs) {
//			
//			updateVariable(df);			
//		}
//	}
//	
//	private void updateVariable(DataField df) {
//		String df_name = df.getName();
//		Object new_value = df.getValue();
//		
//		Variable v = getVariablebyName(df_name);
//		if(v!=null) {           		
//			v.setValue(new_value);											
//		} else {
//			if(Config.getConfig().debugModel) {
//				System.out.println("Warning: update a datafield " + df_name + " that does not exist!");
//			}
//		}				
//		
//	}

	public Variable getVariablebyName(String df_name) {
		
		if(df_name==null) return null;

		for(Variable v: variables) {
			if(df_name.equals(v.getName())) 
				return v;
		}
		
		return null;
	}
	
	public void updateTaskStartStatus(String taskname) {
		current_task = taskname;
		
		if(Config.getConfig().exposeevent) {
			EventExposure.exposeTaskStartEvent(current_task, instanceID, serviceName);
		}
	}
	
	public void updateTaskCompleteStatus(String taskname) {
		last_task = taskname;
		
		if(Config.getConfig().exposeevent) {
			EventExposure.exposeTaskCompleteEvent(last_task, instanceID, serviceName);
		}
	}
	
}

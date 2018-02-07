package Deployment;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import engine.DataType;
import scripts.ChoreographyScript.PartnerList;
import scripts.ChoreographyScript.PartnerType;
import scripts.ChoreographyScript.ServiceChoreographyType;
import Service.AbstractService;
import Service.Guard;
import Service.State;
import Service.Task;
import Service.Transition;
import Service.Variable;
import scripts.ServiceScript.ActivityDefinition;
import scripts.ServiceScript.ActivityList;
import scripts.ServiceScript.ActivityType;
import scripts.ServiceScript.ServiceDefinition;
import scripts.ServiceScript.TransitionList;
import scripts.ServiceScript.TransitionType;
import scripts.ServiceScript.VariableType;
import scripts.ServiceScript.VariablesListType;
import Utils.XMLProcessing;

// TODO: Auto-generated Javadoc
/**
 * The Class LoadServiceScript.
 */
public class LoadServiceScript {
	
	/**
	 * Load service.
	 *
	 * @param name the name
	 * @return the abstract service
	 */
	public static AbstractService loadService(String name) {
		AbstractService result = null;
		
		try {
			FileInputStream input = new FileInputStream(name);
			
			ServiceDefinition service = XMLProcessing.unmarshal(ServiceDefinition.class, input);
			assert(service!=null);
			
			//extract service name
			String serviceName = service.getName();
			assert(serviceName!=null);			
			result = new AbstractService(serviceName);
			
			//extract Variables
			ArrayList<Variable> variables = extractVariables(service, result.getInstanceID());
			HashMap<String, String> variableSchema = extractVariableSchema(variables);
			result.setVariableSchema(variableSchema);
			State cs = result.getState();
			for(Variable v: variables)
				cs.addVariable(v);
			
			
			//extract Activities
			ArrayList<Task> tklist = extractTasks(service, variableSchema);
			result.setTasks(tklist);
			
			//extract Transitions
			ArrayList<Transition> transitions = extractTransitions(service, variableSchema);
			result.setTransitions(transitions);									
			
			//extract first task
			String firstTask = service.getStartTask();
			assert(firstTask!=null);
			result.setStartTask(firstTask);											
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * Extract variable schema.
	 *
	 * @param variables the variables
	 * @return the hash map
	 */
	private static HashMap<String, String> extractVariableSchema(
			ArrayList<Variable> variables) {

		HashMap<String, String> schema = new HashMap<String, String>();
		for(Variable v: variables) {
			String name = v.getName();
			String type = v.getType();
			schema.put(name, type);
		}
		
		return schema;
	}

	/**
	 * Extract variables.
	 *
	 * @param service the service
	 * @param instanceID the instance id
	 * @return the array list
	 */
	private static ArrayList<Variable> extractVariables(
			ServiceDefinition service, String instanceID) {
		
		ArrayList<Variable> variables = new ArrayList<Variable>();
		
		String serviceName = service.getName();
		VariablesListType vlt = service.getVariables();
		assert(vlt!=null);
		List<VariableType> vts = vlt.getVariable();
		
		for(VariableType vt: vts) {
			String name = vt.getName();
			scripts.ServiceScript.DataType dt = vt.getType();
			String value = vt.getValue();
			assert(name!=null);
			assert(dt!=null);
			assert(value!=null);
			
			Variable v = new Variable();
			v.setInstanceID(instanceID);//for publishing events
			v.setServiceName(serviceName);//for publishing events
			v.setName(name);
			if(dt.equals(scripts.ServiceScript.DataType.BOOLEAN)) {
				v.setType(DataType.BOOLEAN);
			} else 
			
			if(dt.equals(scripts.ServiceScript.DataType.INTEGER)) {			
				v.setType(DataType.INTEGER);
			} else
				
			if(dt.equals(scripts.ServiceScript.DataType.STRING)) {
				v.setType(DataType.STRING);
			}
			
			v.setValue(intializeValue(v.getType(), value));
			
			variables.add(v);
		}
		
		return variables;
	}

	private static int count = 1;
	/**
	 * Intialize value.
	 *
	 * @param type the type
	 * @param value the value
	 * @return the object
	 */
	private static Random init_rd = new Random(System.currentTimeMillis());
	private static Object intializeValue(String type, String value) {
		if(value == null || value.isEmpty())		
		    return null;
		
		if(type.equals(DataType.BOOLEAN))
			return new Boolean(value);
		
		if(type.equals(DataType.INTEGER)) {
			String[] sv = value.split("~");
			if(sv.length<=1)
			   return new Integer(value);
			else {
				int min = new Integer(sv[0]);
				int max = new Integer(sv[1]);
				//Random rd = new Random(System.currentTimeMillis() * ++count);				 
				//return rd.nextInt(max-min+1) + min;
				return init_rd.nextInt(max-min+1) + min;
			}
				
		}
		
		if(type.equals(DataType.STRING))
			return value;
		
		return null;
	}

	/**
	 * Extract transitions.
	 *
	 * @param service the service
	 * @param variableSchema 
	 * @return the array list
	 */
	private static ArrayList<Transition> extractTransitions(
			ServiceDefinition service, HashMap<String, String> variableSchema) {
		
		ArrayList<Transition> transitions = new ArrayList<Transition>();
		
		TransitionList tl = service.getTransitions();
		assert(tl!=null);
		List<TransitionType> trlist = tl.getTransition();
		assert(trlist!=null);
		
		for(TransitionType tr: trlist) {
			String name = tr.getName();
			String source = tr.getSource();
			String sink = tr.getSink();
			String guard = tr.getGuard();
			
			assert(name!=null);
			assert(source!=null);
			assert(sink!=null);
			assert(guard!=null);
			
			Transition trans = new Transition();
			trans.setName(name);
			trans.setSink(sink);
			trans.setSource(source);
		    Guard gd = new Guard(variableSchema, name);
		    gd.setGuard(guard);
		    trans.setGuard(gd);
			
		    transitions.add(trans);
		}
		
		return transitions;
	}

	/**
	 * Extract tasks.
	 *
	 * @param service the service
	 * @param variableSchema 
	 * @return the array list
	 */
	private static ArrayList<Task> extractTasks(ServiceDefinition service, HashMap<String, String> variableSchema) {
		
		ActivityList aclist = service.getActivities();
		assert(aclist!=null);
		
		ArrayList<Task> tklist = new ArrayList<Task>();
		
		List<ActivityDefinition> activities = aclist.getActivity();
		assert(activities!=null);
		for(ActivityDefinition task: activities) {
			String taskName = task.getName();
			ActivityType taskType = task.getType();
			String effect = task.getEffect();
			assert(taskName!=null);
			assert(taskType!=null);
			assert(effect!=null);
			
			Task tk = new Task(variableSchema);
			tk.setName(taskName);			
			
			if(taskType.equals(ActivityType.LOCAL_TASK)) {
				tk.setPort(false);
				tk.setSend(false);
				tk.setIsService(false);
			} else
				
			if(taskType.equals(ActivityType.RECEIVE_TASK)) {
				tk.setPort(true);
				tk.setSend(false);
				tk.setIsService(false);
			} else
				
			if(taskType.equals(ActivityType.SEND_TASK)) {
				tk.setPort(true);
				tk.setSend(true);
				tk.setIsService(false);
			} else 
			
			if(taskType.equals(ActivityType.SERVICE)) {
				tk.setPort(false);
				tk.setSend(false);
				tk.setIsService(true);
			
			} else {//left for future possible types
				tk.setPort(false);
				tk.setSend(false);
				tk.setIsService(false);
			}
			
			//should set effect at the end
			tk.setEffect(effect);
			
			tklist.add(tk);			
		}
		
		return tklist;
	}	
	
	/**
	 * Load choreography.
	 *
	 * @param name the name
	 * @return the hash map
	 */
	public static HashMap<String, ArrayList<String>> loadChoreography(String name) {
		HashMap<String, ArrayList<String>> cho = new HashMap<String, ArrayList<String>>();
		
		try {
			FileInputStream input = new FileInputStream(name);
			
			ServiceChoreographyType choreography = XMLProcessing.unmarshal(ServiceChoreographyType.class, input);
			assert(choreography!=null);
			
			PartnerList pl = choreography.getPartners();
			assert(pl!=null);
			List<PartnerType> pts = pl.getPartner();
			assert(pts!=null);
			
			for(PartnerType partner: pts) {
				String serviceName = partner.getName();
				List<String> ports = partner.getPort();
				assert(serviceName!=null);
				assert(ports!=null);
				
				ArrayList<String> port_list = new ArrayList<String>();
				port_list.addAll(ports);
				
				cho.put(serviceName, port_list);
			}
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return cho;
	}

}

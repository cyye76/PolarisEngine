package Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import engine.InvalidExecutionException;
import engine.Queue.Message;
import engine.expression.PortExpression.PortVariable;

public class AbstractService {
	
	//A state stores all the variables and constants of the service
	protected State my_state;	
	
	//tasks of services
	protected ArrayList<Task> my_tasks;
	
	//A queue to receive all incoming messages from partners
	private ArrayList<Message> m_messages;
	
	private String startTaskName;
	
	//Transitions of services
	protected ArrayList<Transition> my_transitions;
	
	protected HashMap<String, String> my_variableSchema;	
	
	private String instanceID;
	private String name;
	
	public AbstractService(String name) {
		this.name = name;
		setInstanceID(name + "_" + System.currentTimeMillis());	
		m_messages = new ArrayList<Message>();
		
		my_state = new State(getInstanceID(), name);
	}
	

	public State getState() {
		return my_state;
	}

	public void setTasks(ArrayList<Task> tasks) {
		this.my_tasks = tasks;
	}

	public ArrayList<Task> getTasks() {
		return my_tasks;
	}
	

	public void setTransitions(ArrayList<Transition> transitions) {
		this.my_transitions = transitions;
	}

	public ArrayList<Transition> getTransitions() {
		return my_transitions;
	}
	
	/*
	 * For debugging
	 */
	protected ArrayList<Variable> initVariables() {

		ArrayList<Variable> result = new ArrayList<Variable>();
		
		Set<String> variables = my_variableSchema.keySet();
		Iterator<String> it = variables.iterator();
		while(it.hasNext()) {
			String variable = it.next();
			String type = my_variableSchema.get(variable);
			Variable v = initVariable(variable, type);
			result.add(v);
		}
		return result;
	}

	/*
	 * For debugging
	 */
	private Variable initVariable(String variable, String type) {

		Variable v = new Variable();
		v.setName(variable);
		v.setType(type);
		v.setValue(null);
		return v;
	}		
	
	public ArrayList<Transition> getAvailableNextTransitions(String task) {
		if(task==null) return null;
		
		ArrayList<Transition> result = new ArrayList<Transition>();
		for(Transition tr: my_transitions) {
			if(task.equals(tr.getSource())) 
				result.add(tr);
		}
		
		return result;
	}
	
	public void setVariableSchema(HashMap<String, String> schema) {
		my_variableSchema = schema;
	}	
	
	public HashMap<String, String> getVariableSchema() {
		return my_variableSchema;
	}	

	private void setInstanceID(String instanceID) {
		this.instanceID = instanceID;		
	}

	public String getInstanceID() {
		return instanceID;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Transition getTransitionbyName(String value) {
		if(value == null) return null;

		for(Transition tr:my_transitions) {			
			if(tr.getName().equals(value))
				return tr;
		}
		return null;
	}
	
	/*
	 * For debugging
	 */
	public void init(){
		
		ArrayList<Variable> vs = initVariables();		
		for(Variable v: vs)
			my_state.addVariable(v);
	}

	public String getStartTask() {
		
		return startTaskName;
	}
	
	public void setStartTask(String taskName) {
		startTaskName = taskName;
	}

	public Task getTaskbyName(String taskName) {
        if(taskName==null) return null; 
		
		for(Task tk: my_tasks) {
			if(taskName.equals(tk.getName())) return tk;
		}
		
		return null;
	}	

	public synchronized void addIncomingMessage(Message msg) {
        m_messages.add(msg);		
	}
	
	private synchronized ArrayList<Message> getIncomingMessage(ArrayList<PortVariable> port_list) {		
		
		ArrayList<Message> result = new ArrayList<Message>();
		ArrayList<String> tp_list = new ArrayList<String>();
		
		for(PortVariable pv: port_list)			
			tp_list.add(pv.getName());		
		
		for(Message msg:m_messages) {
			String name = msg.getName();
			if(tp_list.contains(name)) {
				result.add(msg);
				tp_list.remove(name);
			}										
		}
		
		if(tp_list.isEmpty()) {
		    m_messages.removeAll(result);
		    return result;
		} 
		
		return null;
		
	}
	
	public boolean readIncomingMessage(ArrayList<PortVariable> port_list) throws InvalidExecutionException {
		
		ArrayList<Message> msg_list = getIncomingMessage(port_list);
		if(msg_list == null) return false; //message is not ready
							
		for(PortVariable portv: port_list) {									
			
			String vName = portv.getName();
			String vLoc = portv.getvLoc();
						
			Variable v = my_state.getVariablebyName(vName);
			if(v==null) throw new InvalidExecutionException();
			
			Message msg = getMessageByName(vName, msg_list);
			if(msg==null) throw new InvalidExecutionException();
			
			String vType = v.getType();
			if(!vType.equals(msg.getType())) throw new InvalidExecutionException();
			
			//update the value of variable
			v.setValue(msg.getValue(), vLoc);
		}
		
		return true;
	}

	private Message getMessageByName(String vName, ArrayList<Message> msg_list) {

		for(Message msg: msg_list)
			if(vName.equals(msg.getName())) return msg;
		
		return null;
	}


	private int getPortIndex(String vName, ArrayList<String> port_list) {
		for(int i=0;i<port_list.size();i++)
			if(vName.equals(port_list.get(i)))
				return i+1;
		
		return 0;
	}


	public ArrayList<Message> constructMessage(ArrayList<PortVariable> port_variables) {
        
		ArrayList<Message> result = new ArrayList<Message>();
		
		int index = 1;
		for(PortVariable portv: port_variables) {	
			String portname = portv.getName();
			Variable v = my_state.getVariablebyName(portname);
			String vLoc = portv.getvLoc();
			
			Message msg = new Message();
			msg.setName(v.getName());
			msg.setType(v.getType());
			msg.setValue(v.getValue(vLoc));
			result.add(msg);
			
			index++;
		}
		
		return result;
	}
	
}
